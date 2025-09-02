"""
피드백 서비스
"""

import asyncio
from typing import List, Dict, Any
import structlog
from datetime import datetime

from ..models.feedback import (
    FeedbackRequest, FeedbackResponse, BatchFeedbackRequest, BatchFeedbackResponse,
    FeedbackRatingRequest
)
from ..core.exceptions import ValidationException, RateLimitException
from ..core.redis import redis_client
from .openai_service import openai_service

logger = structlog.get_logger(__name__)


class FeedbackService:
    """피드백 관리 서비스"""
    
    def __init__(self):
        self.rate_limit_key_prefix = "rate_limit:feedback"
        self.feedback_storage_prefix = "feedback:store"
    
    async def _check_rate_limit(self, user_id: str) -> bool:
        """사용자별 요청 제한 확인"""
        key = f"{self.rate_limit_key_prefix}:{user_id}"
        
        # 현재 요청 수 확인
        current_count = await redis_client.incr(key)
        
        if current_count == 1:
            # 첫 요청이면 TTL 설정 (1분)
            await redis_client.expire(key, 60)
        
        from ..core.config import settings
        if current_count > settings.RATE_LIMIT_PER_MINUTE:
            logger.warning(
                "요청 제한 초과",
                user_id=user_id,
                current_count=current_count,
                limit=settings.RATE_LIMIT_PER_MINUTE
            )
            return False
        
        return True
    
    async def _store_feedback(self, feedback: FeedbackResponse) -> bool:
        """피드백을 Redis에 저장"""
        key = f"{self.feedback_storage_prefix}:{feedback.id}"
        
        feedback_data = feedback.dict()
        feedback_data['stored_at'] = datetime.now().isoformat()
        
        return await redis_client.set_json(key, feedback_data, ttl=86400)  # 24시간 보관
    
    async def _get_stored_feedback(self, feedback_id: str) -> Dict[str, Any]:
        """저장된 피드백 조회"""
        key = f"{self.feedback_storage_prefix}:{feedback_id}"
        return await redis_client.get_json(key)
    
    async def generate_feedback(
        self, 
        request: FeedbackRequest, 
        user_id: str = None
    ) -> FeedbackResponse:
        """단일 피드백 생성"""
        
        # 요청 제한 확인
        if user_id and not await self._check_rate_limit(user_id):
            raise RateLimitException("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")
        
        # 입력 데이터 검증
        await self._validate_feedback_request(request)
        
        logger.info(
            "피드백 생성 요청",
            note_id=request.note_id,
            feedback_type=request.feedback_type,
            user_id=user_id
        )
        
        try:
            # OpenAI 서비스를 통한 피드백 생성
            feedback = await openai_service.generate_feedback(request)
            
            # 생성된 피드백 저장
            await self._store_feedback(feedback)
            
            logger.info(
                "피드백 생성 완료",
                feedback_id=feedback.id,
                note_id=request.note_id,
                tokens_used=feedback.tokens_used
            )
            
            return feedback
            
        except Exception as e:
            logger.error(
                "피드백 생성 실패",
                note_id=request.note_id,
                error=str(e),
                exc_info=True
            )
            raise
    
    async def generate_batch_feedback(
        self, 
        request: BatchFeedbackRequest, 
        user_id: str = None
    ) -> BatchFeedbackResponse:
        """배치 피드백 생성"""
        
        logger.info(
            "배치 피드백 생성 시작",
            request_count=len(request.requests),
            user_id=user_id
        )
        
        feedbacks = []
        errors = []
        total_tokens = 0
        
        # 동시 실행을 위한 태스크 생성
        tasks = []
        for feedback_request in request.requests:
            task = asyncio.create_task(
                self._generate_single_feedback_safe(feedback_request, user_id)
            )
            tasks.append(task)
        
        # 모든 태스크 실행 및 결과 수집
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                error_info = {
                    "request_index": i,
                    "note_id": request.requests[i].note_id,
                    "error": str(result),
                    "error_type": type(result).__name__
                }
                errors.append(error_info)
                logger.error(
                    "개별 피드백 생성 실패",
                    **error_info
                )
            else:
                feedbacks.append(result)
                total_tokens += result.tokens_used
        
        response = BatchFeedbackResponse(
            success_count=len(feedbacks),
            total_count=len(request.requests),
            feedbacks=feedbacks,
            errors=errors,
            total_tokens_used=total_tokens
        )
        
        logger.info(
            "배치 피드백 생성 완료",
            success_count=response.success_count,
            total_count=response.total_count,
            total_tokens=total_tokens
        )
        
        return response
    
    async def _generate_single_feedback_safe(
        self, 
        request: FeedbackRequest, 
        user_id: str = None
    ) -> FeedbackResponse:
        """안전한 단일 피드백 생성 (예외 처리 포함)"""
        try:
            return await self.generate_feedback(request, user_id)
        except Exception as e:
            # 배치 처리에서는 개별 실패를 예외로 전파
            raise e
    
    async def rate_feedback(self, request: FeedbackRatingRequest) -> Dict[str, Any]:
        """피드백 평가"""
        
        # 저장된 피드백 조회
        feedback_data = await self._get_stored_feedback(request.feedback_id)
        if not feedback_data:
            raise ValidationException("해당 피드백을 찾을 수 없습니다.")
        
        # 평가 정보 추가
        rating_data = {
            "feedback_id": request.feedback_id,
            "is_useful": request.is_useful,
            "rating": request.rating,
            "comment": request.comment,
            "rated_at": datetime.now().isoformat()
        }
        
        # 평가 정보 저장
        rating_key = f"feedback:rating:{request.feedback_id}"
        await redis_client.set_json(rating_key, rating_data, ttl=86400 * 30)  # 30일 보관
        
        logger.info(
            "피드백 평가 저장",
            feedback_id=request.feedback_id,
            is_useful=request.is_useful,
            rating=request.rating
        )
        
        return {
            "success": True,
            "message": "피드백 평가가 저장되었습니다.",
            "feedback_id": request.feedback_id
        }
    
    async def get_feedback_by_id(self, feedback_id: str) -> Dict[str, Any]:
        """피드백 ID로 조회"""
        feedback_data = await self._get_stored_feedback(feedback_id)
        if not feedback_data:
            raise ValidationException("해당 피드백을 찾을 수 없습니다.")
        
        return feedback_data
    
    async def _validate_feedback_request(self, request: FeedbackRequest):
        """피드백 요청 검증"""
        
        # 내용 길이 검증
        if len(request.note_context.content) < 10:
            raise ValidationException("독서 기록이 너무 짧습니다. 최소 10자 이상 입력해주세요.")
        
        # 책 정보 검증
        if not request.book_context.title.strip():
            raise ValidationException("책 제목이 필요합니다.")
        
        if not request.book_context.author.strip():
            raise ValidationException("저자 정보가 필요합니다.")
        
        # 목차 정보 검증
        if request.chapter_context.chapter_number < 1:
            raise ValidationException("올바른 목차 번호를 입력해주세요.")
        
        logger.debug("피드백 요청 검증 완료", note_id=request.note_id)
    
    async def get_feedback_statistics(self, user_id: str = None) -> Dict[str, Any]:
        """피드백 통계 조회"""
        
        # Redis에서 해당 사용자의 피드백 통계 조회
        # 실제 구현에서는 더 정교한 통계 계산 필요
        
        return {
            "total_feedbacks": 0,
            "average_rating": 0.0,
            "feedback_types": {
                "comment": 0,
                "question": 0,
                "suggestion": 0
            },
            "useful_feedback_ratio": 0.0
        }


# 전역 피드백 서비스 인스턴스
feedback_service = FeedbackService()