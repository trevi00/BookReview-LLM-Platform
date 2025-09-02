"""
피드백 API 엔드포인트
"""

from fastapi import APIRouter, HTTPException, BackgroundTasks, Header
from typing import Optional
import structlog

from ...schemas.requests import (
    FeedbackRequest, BatchFeedbackRequest, AnalysisRequest,
    ComparisonRequest, SummarizationRequest
)
from ...schemas.responses import (
    FeedbackResponse, BatchFeedbackResponse, AnalysisResponse,
    ComparisonResponse, SummarizationResponse, ErrorResponse
)
from ...services.feedback_service import feedback_service
from ...services.openai_service import openai_service
from ...exceptions import AIServiceException, ValidationError
from ...validators import validate_comprehensive_request, create_validation_error_response
from ...monitoring import metrics_collector
from ...logging_config import RequestLogger, BusinessLogger

logger = structlog.get_logger(__name__)

router = APIRouter()


@router.post("/generate", 
            response_model=FeedbackResponse,
            responses={
                400: {"model": ErrorResponse, "description": "잘못된 요청"},
                422: {"model": ErrorResponse, "description": "검증 오류"},
                500: {"model": ErrorResponse, "description": "서버 오류"}
            },
            summary="AI 피드백 생성",
            description="""
            텍스트 내용에 대한 AI 피드백을 생성합니다.
            
            **지원하는 피드백 타입:**
            - grammar: 문법 교정
            - style: 문체 개선
            - content: 내용 분석
            - structure: 구조 개선
            - comprehensive: 종합 피드백
            
            **제한사항:**
            - 최대 텍스트 길이: 10,000자
            - 최소 텍스트 길이: 10자
            - HTML 태그 불허용
            """)
async def generate_feedback(
    request: FeedbackRequest,
    background_tasks: BackgroundTasks,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """텍스트에 대한 AI 피드백 생성"""
    import time
    start_time = time.time()
    
    try:
        # 요청 검증
        request_dict = request.model_dump()
        user_context = {"user_id": user_id, "request_count": 1}
        
        is_valid, validation_errors = validate_comprehensive_request(request_dict, user_context)
        if not is_valid:
            raise create_validation_error_response(validation_errors, "request")
        
        # 피드백 생성
        feedback = await feedback_service.generate_feedback(request, user_id)
        
        # 메트릭 기록
        duration = time.time() - start_time
        metrics_collector.record_ai_request(
            model="gpt-4",
            request_type="feedback",
            duration=duration,
            tokens_used=getattr(feedback, 'tokens_used', 0)
        )
        
        # 비즈니스 로깅
        BusinessLogger.log_feedback_generated(
            request_id=getattr(request, 'request_id', 'unknown'),
            user_id=user_id or 'anonymous',
            feedback_type=request.feedback_type.value,
            book_id=getattr(request, 'book_title', None)
        )
        
        # 백그라운드에서 통계 업데이트
        background_tasks.add_task(
            _update_feedback_statistics,
            user_id,
            request.feedback_type.value,
            getattr(feedback, 'tokens_used', 0)
        )
        
        return feedback
        
    except ValidationError:
        raise
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("피드백 생성 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="피드백 생성 중 오류가 발생했습니다.")


@router.post("/batch", response_model=BatchFeedbackResponse)
async def generate_batch_feedback(
    request: BatchFeedbackRequest,
    background_tasks: BackgroundTasks,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    여러 독서 기록에 대한 배치 피드백 생성
    
    - **requests**: 최대 10개의 피드백 요청 목록
    """
    try:
        response = await feedback_service.generate_batch_feedback(request, user_id)
        
        # 백그라운드에서 배치 통계 업데이트
        background_tasks.add_task(
            _update_batch_statistics,
            user_id,
            response.success_count,
            response.total_tokens_used
        )
        
        return response
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("배치 피드백 생성 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="배치 피드백 생성 중 오류가 발생했습니다.")


@router.get("/{feedback_id}")
async def get_feedback(feedback_id: str):
    """
    피드백 ID로 피드백 조회
    
    - **feedback_id**: 조회할 피드백 ID
    """
    try:
        feedback = await feedback_service.get_feedback_by_id(feedback_id)
        return {
            "success": True,
            "data": feedback
        }
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("피드백 조회 중 오류", feedback_id=feedback_id, error=str(e))
        raise HTTPException(status_code=500, detail="피드백 조회 중 오류가 발생했습니다.")


@router.put("/{feedback_id}/rating")
async def rate_feedback(
    feedback_id: str,
    rating_request: FeedbackRatingRequest
):
    """
    피드백에 대한 사용자 평가
    
    - **feedback_id**: 평가할 피드백 ID
    - **is_useful**: 유용성 평가 (true/false)
    - **rating**: 1-5점 평가 (선택사항)
    - **comment**: 추가 의견 (선택사항)
    """
    try:
        # 요청에 피드백 ID 설정
        rating_request.feedback_id = feedback_id
        
        result = await feedback_service.rate_feedback(rating_request)
        return result
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("피드백 평가 중 오류", feedback_id=feedback_id, error=str(e))
        raise HTTPException(status_code=500, detail="피드백 평가 중 오류가 발생했습니다.")


@router.post("/questions/generate", response_model=QuestionResponse)
async def generate_questions(
    request: QuestionGenerationRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    독서 관련 질문 생성
    
    - **book_context**: 책 정보
    - **chapter_context**: 목차 정보
    - **note_context**: 독서 기록 (선택사항)
    - **question_type**: 질문 타입 (discussion, comprehension, analysis, creative, critical)
    - **difficulty_level**: 난이도 (easy, medium, hard)
    """
    try:
        questions = await openai_service.generate_questions(request)
        
        logger.info(
            "질문 생성 완료",
            question_count=len(questions.questions),
            question_type=request.question_type,
            user_id=user_id
        )
        
        return questions
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("질문 생성 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="질문 생성 중 오류가 발생했습니다.")


@router.get("/statistics/{user_id}")
async def get_feedback_statistics(user_id: str):
    """
    사용자별 피드백 통계 조회
    
    - **user_id**: 사용자 ID
    """
    try:
        stats = await feedback_service.get_feedback_statistics(user_id)
        return {
            "success": True,
            "data": stats
        }
    except Exception as e:
        logger.error("피드백 통계 조회 중 오류", user_id=user_id, error=str(e))
        raise HTTPException(status_code=500, detail="통계 조회 중 오류가 발생했습니다.")


async def _update_feedback_statistics(user_id: str, feedback_type: str, tokens_used: int):
    """백그라운드에서 피드백 통계 업데이트"""
    try:
        if not user_id:
            return
        
        # Redis에 통계 업데이트
        from ...core.redis import redis_client
        
        stats_key = f"stats:feedback:{user_id}"
        current_stats = await redis_client.get_json(stats_key) or {}
        
        # 통계 업데이트
        current_stats["total_feedbacks"] = current_stats.get("total_feedbacks", 0) + 1
        current_stats["total_tokens"] = current_stats.get("total_tokens", 0) + tokens_used
        
        feedback_types = current_stats.get("feedback_types", {})
        feedback_types[feedback_type] = feedback_types.get(feedback_type, 0) + 1
        current_stats["feedback_types"] = feedback_types
        
        await redis_client.set_json(stats_key, current_stats, ttl=86400 * 30)  # 30일 보관
        
        logger.debug("피드백 통계 업데이트 완료", user_id=user_id)
        
    except Exception as e:
        logger.error("피드백 통계 업데이트 실패", user_id=user_id, error=str(e))


@router.post("/analysis/sentiment", response_model=SentimentAnalysisResponse)
async def analyze_sentiment(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    텍스트 감정 분석
    
    - **text**: 분석할 텍스트
    - **analysis_type**: sentiment로 고정
    """
    try:
        if request.analysis_type != "sentiment":
            raise HTTPException(status_code=400, detail="analysis_type은 'sentiment'여야 합니다.")
        
        result = await openai_service.analyze_sentiment(request.text)
        
        logger.info(
            "감정 분석 완료",
            text_length=len(request.text),
            sentiment=result.sentiment,
            user_id=user_id
        )
        
        return result
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("감정 분석 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="감정 분석 중 오류가 발생했습니다.")


@router.post("/analysis/summary", response_model=SummaryResponse)
async def analyze_summary(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    텍스트 요약
    
    - **text**: 요약할 텍스트
    - **analysis_type**: summary로 고정
    """
    try:
        if request.analysis_type != "summary":
            raise HTTPException(status_code=400, detail="analysis_type은 'summary'여야 합니다.")
        
        result = await openai_service.summarize_text(request.text)
        
        logger.info(
            "텍스트 요약 완료",
            original_length=len(request.text),
            summary_length=len(result.summary),
            compression_ratio=result.compression_ratio,
            user_id=user_id
        )
        
        return result
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("텍스트 요약 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="텍스트 요약 중 오류가 발생했습니다.")


@router.post("/analysis/keywords", response_model=KeywordsResponse)
async def extract_keywords(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    키워드 추출
    
    - **text**: 분석할 텍스트
    - **analysis_type**: keywords로 고정
    """
    try:
        if request.analysis_type != "keywords":
            raise HTTPException(status_code=400, detail="analysis_type은 'keywords'여야 합니다.")
        
        result = await openai_service.extract_keywords(request.text)
        
        logger.info(
            "키워드 추출 완료",
            text_length=len(request.text),
            keyword_count=len(result.keywords),
            theme_count=len(result.themes),
            user_id=user_id
        )
        
        return result
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("키워드 추출 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="키워드 추출 중 오류가 발생했습니다.")


async def _update_batch_statistics(user_id: str, success_count: int, total_tokens: int):
    """백그라운드에서 배치 피드백 통계 업데이트"""
    try:
        if not user_id:
            return
        
        from ...core.redis import redis_client
        
        stats_key = f"stats:batch:{user_id}"
        current_stats = await redis_client.get_json(stats_key) or {}
        
        current_stats["total_batch_requests"] = current_stats.get("total_batch_requests", 0) + 1
        current_stats["total_batch_feedbacks"] = current_stats.get("total_batch_feedbacks", 0) + success_count
        current_stats["total_batch_tokens"] = current_stats.get("total_batch_tokens", 0) + total_tokens
        
        await redis_client.set_json(stats_key, current_stats, ttl=86400 * 30)
        
        logger.debug("배치 피드백 통계 업데이트 완료", user_id=user_id)
        
    except Exception as e:
        logger.error("배치 피드백 통계 업데이트 실패", user_id=user_id, error=str(e))