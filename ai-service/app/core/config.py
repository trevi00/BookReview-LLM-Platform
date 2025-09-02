"""
AI 서비스 설정 관리 (리팩토링 버전)
환경별 설정 분리 및 보안 강화
"""

from pydantic_settings import BaseSettings
from typing import List, Optional
import os
from functools import lru_cache


class Settings(BaseSettings):
    """애플리케이션 설정 (환경별 분리)"""
    
    # 환경 설정
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    VERSION: str = "1.0.0"
    
    # 서버 설정
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    
    # 보안 설정
    ALLOWED_HOSTS: List[str] = ["*"]
    CORS_ORIGINS: List[str] = ["http://localhost:8080"]
    
    # 인증 설정
    JWT_SECRET_KEY: str = "ai-service-default-secret-change-in-production"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    AI_SERVICE_API_KEY: str = ""
    
    # Rate Limiting
    RATE_LIMIT_CALLS: int = 100
    RATE_LIMIT_PERIOD: int = 60  # seconds
    
    # Redis 설정
    REDIS_URL: str = "redis://localhost:6379/0"
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str = ""
    REDIS_DB: int = 0
    REDIS_MAX_CONNECTIONS: int = 20
    REDIS_TIMEOUT: int = 5
    
    # OpenAI 설정
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4"
    OPENAI_MAX_TOKENS: int = 1000
    OPENAI_TEMPERATURE: float = 0.7
    OPENAI_TIMEOUT: int = 30
    OPENAI_MAX_RETRIES: int = 3
    
    # AI 서비스 설정
    MAX_FEEDBACK_LENGTH: int = 2000
    MAX_BATCH_SIZE: int = 10
    FEEDBACK_CACHE_TTL: int = 3600  # 1시간
    ANALYSIS_CACHE_TTL: int = 1800  # 30분
    
    # 로깅 설정
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"
    LOG_FILE: Optional[str] = None
    
    # 메트릭 및 모니터링
    METRICS_ENABLED: bool = True
    METRICS_PORT: int = 8001
    SENTRY_DSN: Optional[str] = None
    
    # 성능 설정
    WORKER_TIMEOUT: int = 300  # 5분
    MAX_CONCURRENT_REQUESTS: int = 100
    
    class Config:
        case_sensitive = True
        extra = "ignore"
        
        # 환경변수 우선순위 설정
        env_prefix = ""
        
        @classmethod
        def customise_sources(cls, init_settings, env_settings, file_secret_settings):
            # 환경별 설정 파일 경로 결정
            environment = os.getenv("ENVIRONMENT", "development").lower()
            env_files = []
            
            # 기본 .env 파일
            if os.path.exists(".env"):
                env_files.append(".env")
            
            # 환경별 설정 파일
            env_file_path = f"configs/{environment}.env"
            if os.path.exists(env_file_path):
                env_files.append(env_file_path)
            
            # 로컬 오버라이드 파일 (git에서 제외)
            local_env_file = f"configs/{environment}.local.env"
            if os.path.exists(local_env_file):
                env_files.append(local_env_file)
            
            return (
                init_settings,
                env_settings,
                file_secret_settings,
            )
        
        @property
        def env_file(self):
            """동적 환경 파일 설정"""
            environment = os.getenv("ENVIRONMENT", "development").lower()
            return f"configs/{environment}.env"


class DevelopmentSettings(Settings):
    """개발 환경 설정"""
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    LOG_LEVEL: str = "DEBUG"
    
    # 개발용 보안 완화
    ALLOWED_HOSTS: List[str] = ["*"]
    CORS_ORIGINS: List[str] = [
        "http://localhost:3000",
        "http://localhost:8080", 
        "http://127.0.0.1:3000",
        "http://127.0.0.1:8080"
    ]
    
    # 개발용 캐시 설정 (짧은 TTL)
    FEEDBACK_CACHE_TTL: int = 300  # 5분
    ANALYSIS_CACHE_TTL: int = 180  # 3분


class TestSettings(Settings):
    """테스트 환경 설정"""
    ENVIRONMENT: str = "test"
    DEBUG: bool = True
    LOG_LEVEL: str = "WARNING"
    
    # 테스트용 DB
    REDIS_DB: int = 1
    
    # 테스트용 캐시 (매우 짧은 TTL)
    FEEDBACK_CACHE_TTL: int = 60  # 1분
    ANALYSIS_CACHE_TTL: int = 30   # 30초
    
    # 테스트용 Mock 설정
    OPENAI_API_KEY: str = "test-api-key"
    OPENAI_MODEL: str = "gpt-3.5-turbo"  # 테스트용 저렴한 모델
    
    # 테스트용 Rate Limiting (관대하게)
    RATE_LIMIT_CALLS: int = 1000
    RATE_LIMIT_PERIOD: int = 60


class ProductionSettings(Settings):
    """프로덕션 환경 설정"""
    ENVIRONMENT: str = "production"
    DEBUG: bool = False
    LOG_LEVEL: str = "INFO"
    
    # 프로덕션 보안 강화
    ALLOWED_HOSTS: List[str] = [
        "bookreview-ai.yourdomain.com",
        "api.bookreview.com"
    ]
    CORS_ORIGINS: List[str] = [
        "https://bookreview.com",
        "https://www.bookreview.com"
    ]
    
    # 프로덕션용 캐시 (긴 TTL)
    FEEDBACK_CACHE_TTL: int = 7200  # 2시간
    ANALYSIS_CACHE_TTL: int = 3600  # 1시간
    
    # 프로덕션용 성능 최적화
    REDIS_MAX_CONNECTIONS: int = 50
    MAX_CONCURRENT_REQUESTS: int = 200
    
    # 프로덕션용 Rate Limiting (엄격)
    RATE_LIMIT_CALLS: int = 60
    RATE_LIMIT_PERIOD: int = 60


@lru_cache()
def get_settings() -> Settings:
    """환경별 설정 인스턴스 반환 (캐싱됨)"""
    environment = os.getenv("ENVIRONMENT", "development").lower()
    
    if environment == "production":
        return ProductionSettings()
    elif environment == "test":
        return TestSettings()
    else:
        return DevelopmentSettings()


def validate_settings(settings: Settings) -> tuple[bool, list[str]]:
    """설정 검증"""
    errors = []
    
    # 필수 환경변수 검증
    if not settings.OPENAI_API_KEY or settings.OPENAI_API_KEY == "YOUR_NEW_API_KEY_HERE":
        errors.append("OPENAI_API_KEY is not configured")
    
    # JWT Secret 검증
    if settings.JWT_SECRET_KEY == "ai-service-default-secret-change-in-production":
        if settings.ENVIRONMENT == "production":
            errors.append("JWT_SECRET_KEY must be changed in production")
        else:
            # 개발환경에서는 경고만
            print("⚠️ Using default JWT secret key in development")
    
    # JWT Secret 길이 검증
    if len(settings.JWT_SECRET_KEY.encode()) < 32:
        errors.append(f"JWT_SECRET_KEY is too short. Current: {len(settings.JWT_SECRET_KEY.encode())} bytes, Required: 32+ bytes")
    
    # Redis 연결 정보 검증
    if not settings.REDIS_HOST:
        errors.append("REDIS_HOST is required")
    
    # 프로덕션 환경 추가 검증
    if settings.ENVIRONMENT == "production":
        if settings.DEBUG:
            errors.append("DEBUG must be False in production")
        
        if "*" in settings.ALLOWED_HOSTS:
            errors.append("ALLOWED_HOSTS cannot contain '*' in production")
        
        if not settings.SENTRY_DSN:
            print("⚠️ SENTRY_DSN not configured for production monitoring")
    
    return len(errors) == 0, errors


# 설정 초기화 및 검증
def initialize_settings():
    """설정 초기화 및 검증"""
    settings = get_settings()
    is_valid, validation_errors = validate_settings(settings)
    
    if not is_valid:
        print("❌ Configuration validation failed:")
        for error in validation_errors:
            print(f"  - {error}")
        
        if settings.ENVIRONMENT == "production":
            raise ValueError("Invalid production configuration")
        else:
            print("⚠️ Continuing with invalid configuration (non-production environment)")
    
    return settings


# 전역 설정 인스턴스 (지연 초기화)
_settings = None

def get_current_settings() -> Settings:
    """현재 설정 인스턴스 반환"""
    global _settings
    if _settings is None:
        _settings = initialize_settings()
    return _settings