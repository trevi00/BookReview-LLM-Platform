"""
책 독후감 및 피드백 플랫폼 AI 서비스
FastAPI 기반 AI 피드백 생성 서비스
"""

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.responses import JSONResponse
import structlog
from contextlib import asynccontextmanager

from app.core.config import settings
from app.core.logging import setup_logging
from app.api.v1.api import api_router
from app.core.exceptions import setup_exception_handlers


# 로깅 설정
setup_logging()
logger = structlog.get_logger()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 라이프사이클 관리"""
    # 시작 시 초기화
    logger.info("AI 서비스 시작 중...")
    
    # Redis 연결 확인
    from app.core.redis import redis_client
    try:
        await redis_client.ping()
        logger.info("Redis 연결 성공")
    except Exception as e:
        logger.error("Redis 연결 실패", error=str(e))
    
    # OpenAI API 키 확인
    if not settings.OPENAI_API_KEY:
        logger.warning("OpenAI API 키가 설정되지 않았습니다")
    else:
        logger.info("OpenAI API 키 설정 확인됨")
    
    yield
    
    # 종료 시 정리
    logger.info("AI 서비스 종료 중...")
    await redis_client.close()


# FastAPI 앱 생성
app = FastAPI(
    title="BookReview AI Service",
    description="책 독후감 및 피드백을 위한 AI 서비스",
    version="1.0.0",
    docs_url="/docs" if settings.ENVIRONMENT == "development" else None,
    redoc_url="/redoc" if settings.ENVIRONMENT == "development" else None,
    lifespan=lifespan
)

# CORS 미들웨어 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_HOSTS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 신뢰할 수 있는 호스트 미들웨어
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=settings.ALLOWED_HOSTS
)

# 예외 처리기 설정
setup_exception_handlers(app)

# API 라우터 포함
app.include_router(api_router, prefix="/api/v1")


@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "service": "BookReview AI Service",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs" if settings.ENVIRONMENT == "development" else "disabled"
    }


@app.get("/health")
async def health_check():
    """헬스 체크 엔드포인트"""
    try:
        # Redis 연결 확인
        from app.core.redis import redis_client
        await redis_client.ping()
        
        return {
            "status": "healthy",
            "services": {
                "redis": "connected",
                "openai": "configured" if settings.OPENAI_API_KEY else "not_configured"
            }
        }
    except Exception as e:
        logger.error("헬스 체크 실패", error=str(e))
        raise HTTPException(status_code=503, detail="Service unavailable")


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.ENVIRONMENT == "development",
        log_level="info"
    )