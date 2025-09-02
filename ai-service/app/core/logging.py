"""
구조화된 로깅 설정
"""

import structlog
import logging
import sys
from typing import Any
from .config import settings


def setup_logging() -> None:
    """로깅 설정 초기화"""
    
    # 로그 레벨 설정
    log_level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
    
    # structlog 설정
    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.StackInfoRenderer(),
            structlog.dev.set_exc_info,
            structlog.processors.TimeStamper(fmt="ISO"),
            structlog.dev.ConsoleRenderer() if settings.ENVIRONMENT == "development" 
            else structlog.processors.JSONRenderer()
        ],
        wrapper_class=structlog.make_filtering_bound_logger(log_level),
        logger_factory=structlog.WriteLoggerFactory(sys.stdout),
        cache_logger_on_first_use=True,
    )
    
    # 표준 로깅 설정
    logging.basicConfig(
        level=log_level,
        format="%(message)s",
        stream=sys.stdout,
    )
    
    # 외부 라이브러리 로그 레벨 조정
    logging.getLogger("uvicorn").setLevel(logging.INFO)
    logging.getLogger("fastapi").setLevel(logging.INFO)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("openai").setLevel(logging.WARNING)


def get_logger(name: str = None) -> Any:
    """구조화된 로거 반환"""
    return structlog.get_logger(name)