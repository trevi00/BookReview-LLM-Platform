"""
텍스트 분석 API 엔드포인트
"""

from fastapi import APIRouter, HTTPException, Header
from typing import Optional
import structlog

from ...models.feedback import (
    AnalysisRequest, SentimentAnalysisResponse, SummaryResponse, KeywordsResponse
)
from ...services.openai_service import openai_service
from ...core.exceptions import AIServiceException

logger = structlog.get_logger(__name__)

router = APIRouter()


@router.post("/sentiment", response_model=SentimentAnalysisResponse)
async def analyze_sentiment(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    텍스트 감정 분석
    
    - **text**: 분석할 텍스트 (최대 10,000자)
    - **analysis_type**: "sentiment"로 고정
    
    Returns:
    - **sentiment**: positive, negative, neutral 중 하나
    - **confidence**: 신뢰도 (0.0-1.0)
    - **emotions**: 세부 감정 분석 결과
    """
    try:
        if request.analysis_type != "sentiment":
            raise HTTPException(status_code=400, detail="analysis_type은 'sentiment'여야 합니다.")
        
        result = await openai_service.analyze_sentiment(request.text)
        
        logger.info(
            "감정 분석 완료",
            text_length=len(request.text),
            sentiment=result.sentiment,
            confidence=result.confidence,
            user_id=user_id
        )
        
        return result
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("감정 분석 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="감정 분석 중 오류가 발생했습니다.")


@router.post("/summary", response_model=SummaryResponse)
async def generate_summary(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    텍스트 요약
    
    - **text**: 요약할 텍스트 (최대 10,000자)
    - **analysis_type**: "summary"로 고정
    
    Returns:
    - **summary**: 요약된 텍스트
    - **key_points**: 핵심 포인트 목록
    - **compression_ratio**: 압축률
    """
    try:
        if request.analysis_type != "summary":
            raise HTTPException(status_code=400, detail="analysis_type은 'summary'여야 합니다.")
        
        result = await openai_service.generate_summary(request.text)
        
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


@router.post("/keywords", response_model=KeywordsResponse)
async def extract_keywords(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    키워드 추출
    
    - **text**: 분석할 텍스트 (최대 10,000자)
    - **analysis_type**: "keywords"로 고정
    
    Returns:
    - **keywords**: 추출된 키워드 목록
    - **themes**: 주요 테마 목록
    - **relevance_scores**: 키워드별 관련성 점수
    """
    try:
        if request.analysis_type != "keywords":
            raise HTTPException(status_code=400, detail="analysis_type은 'keywords'여야 합니다.")
        
        # 키워드 추출 로직 (향후 구현)
        # 현재는 기본 응답 반환
        result = KeywordsResponse(
            keywords=["독서", "학습", "성장", "지식"],
            themes=["자기계발", "교육"],
            relevance_scores={
                "독서": 0.9,
                "학습": 0.8,
                "성장": 0.7,
                "지식": 0.6
            }
        )
        
        logger.info(
            "키워드 추출 완료",
            text_length=len(request.text),
            keyword_count=len(result.keywords),
            user_id=user_id
        )
        
        return result
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("키워드 추출 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="키워드 추출 중 오류가 발생했습니다.")


@router.post("/readability")
async def analyze_readability(
    request: AnalysisRequest,
    user_id: Optional[str] = Header(None, alias="X-User-ID")
):
    """
    텍스트 가독성 분석
    
    - **text**: 분석할 텍스트 (최대 10,000자)
    - **analysis_type**: "readability"로 고정
    
    Returns:
    - **readability_score**: 가독성 점수 (0-100)
    - **reading_level**: 읽기 수준 (elementary, middle, high, college)
    - **suggestions**: 가독성 개선 제안
    """
    try:
        if request.analysis_type != "readability":
            raise HTTPException(status_code=400, detail="analysis_type은 'readability'여야 합니다.")
        
        # 가독성 분석 로직 (향후 구현)
        # 현재는 기본 응답 반환
        result = {
            "readability_score": 75,
            "reading_level": "middle",
            "word_count": len(request.text.split()),
            "sentence_count": len([s for s in request.text.split('.') if s.strip()]),
            "avg_words_per_sentence": round(len(request.text.split()) / max(1, len([s for s in request.text.split('.') if s.strip()])), 2),
            "suggestions": [
                "문장을 더 짧게 나누어 보세요",
                "어려운 단어를 쉬운 단어로 바꾸어 보세요"
            ]
        }
        
        logger.info(
            "가독성 분석 완료",
            text_length=len(request.text),
            readability_score=result["readability_score"],
            reading_level=result["reading_level"],
            user_id=user_id
        )
        
        return {
            "success": True,
            "data": result
        }
        
    except AIServiceException:
        raise
    except Exception as e:
        logger.error("가독성 분석 중 예상치 못한 오류", error=str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="가독성 분석 중 오류가 발생했습니다.")