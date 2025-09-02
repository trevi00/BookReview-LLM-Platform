"""
API v1 라우터 통합
"""

from fastapi import APIRouter

from .feedback import router as feedback_router
from .analysis import router as analysis_router

# API v1 메인 라우터
api_router = APIRouter()

# 각 도메인별 라우터 포함
api_router.include_router(
    feedback_router,
    prefix="/feedback",
    tags=["피드백"],
    responses={404: {"description": "Not found"}}
)

api_router.include_router(
    analysis_router,
    prefix="/analysis", 
    tags=["텍스트 분석"],
    responses={404: {"description": "Not found"}}
)


@api_router.get("/")
async def api_info():
    """API 정보"""
    return {
        "name": "BookReview AI Service API",
        "version": "1.0.0",
        "description": "책 독후감 및 피드백을 위한 AI 서비스 API",
        "endpoints": {
            "feedback": {
                "POST /feedback/generate": "단일 피드백 생성",
                "POST /feedback/batch": "배치 피드백 생성", 
                "GET /feedback/{feedback_id}": "피드백 조회",
                "PUT /feedback/{feedback_id}/rating": "피드백 평가",
                "POST /feedback/questions/generate": "질문 생성",
                "GET /feedback/statistics/{user_id}": "피드백 통계"
            },
            "analysis": {
                "POST /analysis/sentiment": "감정 분석",
                "POST /analysis/summary": "텍스트 요약",
                "POST /analysis/keywords": "키워드 추출",
                "POST /analysis/readability": "가독성 분석"
            }
        }
    }