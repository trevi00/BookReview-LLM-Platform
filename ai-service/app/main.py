"""
BookReview AI Service - FastAPI 메인 애플리케이션
리팩토링된 보안 강화 및 예외 처리 시스템 적용
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhosts import TrustedHostMiddleware
from contextlib import asynccontextmanager
import structlog
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# 개선된 예외 처리 및 미들웨어 임포트
from .exception_handlers import setup_exception_handlers
from .middleware import setup_middleware
from .core.config import get_settings
from .logging_config import initialize_logging
from .monitoring import start_background_monitoring, get_metrics_endpoint_response, health_checker

# Initialize logging system
initialize_logging()

logger = structlog.get_logger(__name__)
settings = get_settings()

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("🚀 BookReview AI Service starting up...")
    
    # Verify environment
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key or api_key == "YOUR_NEW_API_KEY_HERE":
        logger.error("❌ OPENAI_API_KEY not properly configured!")
    else:
        logger.info("✅ OPENAI_API_KEY configured")
    
    # Test Redis connection
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0)
        r.ping()
        logger.info("✅ Redis connection successful")
    except Exception as e:
        logger.error(f"❌ Redis connection failed: {e}")
    
    # Start background monitoring
    logger.info("🔍 Starting background monitoring...")
    await start_background_monitoring()
    
    yield
    
    # Shutdown
    logger.info("🛑 BookReview AI Service shutting down...")

# Create FastAPI app
app = FastAPI(
    title="BookReview AI Service",
    description="""
    AI-powered feedback and analysis service for book reviews
    
    ## Features
    - 📝 AI-powered text feedback and correction
    - 🔍 Content analysis and sentiment detection  
    - 📊 Batch processing support
    - 🛡️ Security and rate limiting
    - 📈 Monitoring and metrics
    
    ## Authentication
    Most endpoints require authentication via:
    - **API Key**: `Authorization: Bearer <api_key>`
    - **JWT Token**: `Authorization: Bearer <jwt_token>`
    
    ## Rate Limits
    - **General API**: 60 requests per minute
    - **AI Endpoints**: 20 requests per minute
    - **Batch Endpoints**: 5 requests per minute
    """,
    version=settings.VERSION,
    lifespan=lifespan,
    docs_url="/docs" if settings.ENVIRONMENT == "development" else None,
    redoc_url="/redoc" if settings.ENVIRONMENT == "development" else None,
    openapi_url="/openapi.json" if settings.ENVIRONMENT == "development" else None,
    contact={
        "name": "BookReview AI Service Team",
        "email": "support@bookreview.com",
    },
    license_info={
        "name": "MIT License",
        "url": "https://opensource.org/licenses/MIT",
    },
    tags_metadata=[
        {
            "name": "feedback",
            "description": "AI 피드백 생성 및 관리",
        },
        {
            "name": "analytics", 
            "description": "텍스트 분석 및 통계",
        },
        {
            "name": "health",
            "description": "시스템 상태 확인",
        },
        {
            "name": "monitoring",
            "description": "메트릭 및 모니터링",
        }
    ]
)

# 보안 및 예외 처리 시스템 설정
setup_exception_handlers(app)
setup_middleware(app, settings)

# TrustedHost 미들웨어 (환경별 설정)
if settings.ENVIRONMENT == "production":
    app.add_middleware(
        TrustedHostMiddleware, 
        allowed_hosts=settings.ALLOWED_HOSTS
    )

# CORS middleware (환경별 설정)
cors_origins = settings.CORS_ORIGINS
if settings.ENVIRONMENT == "development":
    cors_origins.extend([
        "http://localhost:3000", 
        "http://localhost:8080", 
        "http://127.0.0.1:3000",
        "http://localhost:3001"
    ])

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["*"],
)

# Health check endpoint
@app.get("/health", 
         tags=["health"],
         summary="서비스 상태 확인",
         description="모든 시스템 컴포넌트의 상태를 확인합니다.")
async def health_check():
    """Health check endpoint"""
    health_status = await health_checker.perform_health_check()
    return health_status

# Metrics endpoint
@app.get("/metrics",
         tags=["monitoring"], 
         summary="Prometheus 메트릭",
         description="Prometheus 형식의 메트릭 데이터를 반환합니다.")
async def metrics():
    """Prometheus metrics endpoint"""
    if not settings.METRICS_ENABLED:
        raise HTTPException(status_code=404, detail="Metrics disabled")
    
    from fastapi.responses import Response
    metrics_data = get_metrics_endpoint_response()
    return Response(content=metrics_data, media_type="text/plain")

# Performance stats endpoint  
@app.get("/stats/performance",
         tags=["monitoring"],
         summary="성능 통계",
         description="시스템 성능 통계 및 메트릭을 반환합니다.")
async def performance_stats():
    """Performance statistics endpoint"""
    from .monitoring import get_performance_stats
    stats = await get_performance_stats()
    return stats

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "BookReview AI Service is running!",
        "docs": "/docs",
        "health": "/health"
    }

# Simple test endpoints
@app.get("/test/redis")
async def test_redis():
    """Test Redis connection"""
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0)
        r.ping()
        return {"status": "success", "message": "Redis connection OK"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Redis connection failed: {str(e)}")

@app.get("/test/openai")
async def test_openai():
    """Test OpenAI API configuration (보안 강화)"""
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key or api_key == "YOUR_NEW_API_KEY_HERE":
        from .exceptions import AuthenticationError
        raise AuthenticationError(
            message="OpenAI API 키가 설정되지 않았습니다.",
            details={"config_required": "OPENAI_API_KEY environment variable"}
        )
    
    return {
        "status": "success", 
        "message": "OpenAI API key configured",
        "key_preview": f"{api_key[:8]}{'*' * 12}..."  # 보안 강화: 앞 8자만 표시
    }

# Include routers (if they exist)
try:
    from .api.v1 import feedback, analytics
    app.include_router(
        feedback.router, 
        prefix="/api/v1/feedback", 
        tags=["feedback"],
        responses={
            401: {"description": "인증 실패"},
            403: {"description": "권한 없음"},
            429: {"description": "요청 제한 초과"},
            500: {"description": "서버 오류"}
        }
    )
    app.include_router(
        analytics.router, 
        prefix="/api/v1/analytics", 
        tags=["analytics"],
        responses={
            401: {"description": "인증 실패"},
            403: {"description": "권한 없음"},
            429: {"description": "요청 제한 초과"},
            500: {"description": "서버 오류"}
        }
    )
    logger.info("✅ API routers loaded successfully")
except ImportError as e:
    logger.warning(f"⚠️ Some API routers not available: {e}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8001,
        reload=True,
        log_level="info"
    )