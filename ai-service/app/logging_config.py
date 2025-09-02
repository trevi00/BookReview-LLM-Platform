"""
AI 서비스 로깅 설정 모듈
구조화된 로깅, 메트릭 수집, 성능 모니터링 기능 제공
"""

import os
import sys
import json
import logging
import logging.config
from datetime import datetime
from typing import Dict, Any, Optional
from pathlib import Path

import structlog
from structlog.processors import JSONRenderer
from pythonjsonlogger import jsonlogger

from .core.config import get_current_settings


def setup_logging():
    """로깅 시스템 초기화"""
    settings = get_current_settings()
    
    # 로그 디렉토리 생성
    if settings.LOG_FILE:
        log_path = Path(settings.LOG_FILE)
        log_path.parent.mkdir(parents=True, exist_ok=True)
    
    # structlog 설정
    structlog.configure(
        processors=[
            # 시간 추가
            structlog.processors.TimeStamper(fmt="iso"),
            # 로그 레벨 추가
            structlog.processors.add_log_level,
            # 스택 정보 렌더링
            structlog.processors.StackInfoRenderer(),
            # 예외 정보 포맷팅
            structlog.processors.format_exc_info,
            # 로그 레벨별 컬러링 (개발환경)
            structlog.dev.ConsoleRenderer() if settings.DEBUG else JSONRenderer()
        ],
        # 로그 레벨 필터링
        wrapper_class=structlog.make_filtering_bound_logger(
            getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
        ),
        logger_factory=structlog.PrintLoggerFactory(),
        cache_logger_on_first_use=True,
    )
    
    # 표준 logging 설정
    logging_config = {
        "version": 1,
        "disable_existing_loggers": False,
        "formatters": {
            "json": {
                "()": jsonlogger.JsonFormatter,
                "format": "%(asctime)s %(name)s %(levelname)s %(message)s"
            },
            "standard": {
                "format": "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
            }
        },
        "handlers": {
            "console": {
                "class": "logging.StreamHandler",
                "level": settings.LOG_LEVEL,
                "formatter": "json" if settings.LOG_FORMAT == "json" else "standard",
                "stream": sys.stdout
            }
        },
        "loggers": {
            "": {  # root logger
                "level": settings.LOG_LEVEL,
                "handlers": ["console"],
                "propagate": False
            },
            "uvicorn": {
                "level": "INFO",
                "handlers": ["console"],
                "propagate": False
            },
            "uvicorn.access": {
                "level": "INFO",
                "handlers": ["console"],
                "propagate": False
            }
        }
    }
    
    # 파일 핸들러 추가 (설정된 경우)
    if settings.LOG_FILE:
        logging_config["handlers"]["file"] = {
            "class": "logging.handlers.RotatingFileHandler",
            "level": settings.LOG_LEVEL,
            "formatter": "json",
            "filename": settings.LOG_FILE,
            "maxBytes": 10 * 1024 * 1024,  # 10MB
            "backupCount": 5,
            "encoding": "utf-8"
        }
        logging_config["loggers"][""]["handlers"].append("file")
    
    logging.config.dictConfig(logging_config)


class RequestLogger:
    """요청별 로깅 유틸리티"""
    
    @staticmethod
    def log_request_start(request_id: str, method: str, path: str, 
                         client_ip: str, user_agent: str = None) -> structlog.BoundLogger:
        """요청 시작 로그"""
        logger = structlog.get_logger("request").bind(
            request_id=request_id,
            method=method,
            path=path,
            client_ip=client_ip,
            user_agent=user_agent,
            event="request_start"
        )
        logger.info("Request started")
        return logger
    
    @staticmethod
    def log_request_end(request_id: str, status_code: int, 
                       duration_ms: float, response_size: int = None):
        """요청 완료 로그"""
        logger = structlog.get_logger("request").bind(
            request_id=request_id,
            status_code=status_code,
            duration_ms=round(duration_ms, 2),
            response_size=response_size,
            event="request_end"
        )
        
        if status_code >= 500:
            logger.error("Request completed with server error")
        elif status_code >= 400:
            logger.warning("Request completed with client error")
        else:
            logger.info("Request completed successfully")
    
    @staticmethod
    def log_ai_request(request_id: str, model: str, tokens_used: int, 
                      cost: float = None, duration_ms: float = None):
        """AI API 요청 로그"""
        logger = structlog.get_logger("ai_service").bind(
            request_id=request_id,
            model=model,
            tokens_used=tokens_used,
            cost=cost,
            duration_ms=duration_ms,
            event="ai_request"
        )
        logger.info("AI API request completed")


class SecurityLogger:
    """보안 관련 로깅"""
    
    @staticmethod
    def log_authentication_attempt(user_id: str = None, client_ip: str = None, 
                                 success: bool = False, reason: str = None):
        """인증 시도 로그"""
        logger = structlog.get_logger("security").bind(
            user_id=user_id,
            client_ip=client_ip,
            success=success,
            reason=reason,
            event="authentication_attempt"
        )
        
        if success:
            logger.info("Authentication successful")
        else:
            logger.warning("Authentication failed")
    
    @staticmethod
    def log_rate_limit_exceeded(client_ip: str, endpoint: str, limit: int):
        """Rate limit 초과 로그"""
        logger = structlog.get_logger("security").bind(
            client_ip=client_ip,
            endpoint=endpoint,
            limit=limit,
            event="rate_limit_exceeded"
        )
        logger.warning("Rate limit exceeded")
    
    @staticmethod
    def log_suspicious_activity(client_ip: str, activity_type: str, 
                              details: Dict[str, Any] = None):
        """의심스러운 활동 로그"""
        logger = structlog.get_logger("security").bind(
            client_ip=client_ip,
            activity_type=activity_type,
            details=details or {},
            event="suspicious_activity"
        )
        logger.error("Suspicious activity detected")


class ErrorLogger:
    """에러 로깅 유틸리티"""
    
    @staticmethod
    def log_application_error(error: Exception, request_id: str = None, 
                            context: Dict[str, Any] = None):
        """애플리케이션 에러 로그"""
        logger = structlog.get_logger("error").bind(
            error_type=type(error).__name__,
            error_message=str(error),
            request_id=request_id,
            context=context or {},
            event="application_error"
        )
        logger.error("Application error occurred", exc_info=error)
    
    @staticmethod
    def log_external_service_error(service_name: str, error: Exception, 
                                 request_id: str = None, retry_count: int = 0):
        """외부 서비스 에러 로그"""
        logger = structlog.get_logger("external_service").bind(
            service_name=service_name,
            error_type=type(error).__name__,
            error_message=str(error),
            request_id=request_id,
            retry_count=retry_count,
            event="external_service_error"
        )
        logger.error(f"External service error: {service_name}")
    
    @staticmethod
    def log_validation_error(field: str, value: Any, error_message: str, 
                           request_id: str = None):
        """검증 에러 로그"""
        logger = structlog.get_logger("validation").bind(
            field=field,
            value=str(value)[:100],  # 긴 값은 잘라서 로그
            error_message=error_message,
            request_id=request_id,
            event="validation_error"
        )
        logger.warning("Validation error occurred")


class PerformanceLogger:
    """성능 로깅 유틸리티"""
    
    @staticmethod
    def log_slow_query(query_type: str, duration_ms: float, 
                      threshold_ms: float = 1000, details: Dict[str, Any] = None):
        """느린 쿼리 로그"""
        if duration_ms > threshold_ms:
            logger = structlog.get_logger("performance").bind(
                query_type=query_type,
                duration_ms=round(duration_ms, 2),
                threshold_ms=threshold_ms,
                details=details or {},
                event="slow_query"
            )
            logger.warning("Slow query detected")
    
    @staticmethod
    def log_memory_usage(memory_mb: float, threshold_mb: float = 512):
        """메모리 사용량 로그"""
        if memory_mb > threshold_mb:
            logger = structlog.get_logger("performance").bind(
                memory_mb=round(memory_mb, 2),
                threshold_mb=threshold_mb,
                event="high_memory_usage"
            )
            logger.warning("High memory usage detected")
    
    @staticmethod
    def log_cache_performance(cache_type: str, hit_rate: float, 
                            total_requests: int):
        """캐시 성능 로그"""
        logger = structlog.get_logger("performance").bind(
            cache_type=cache_type,
            hit_rate=round(hit_rate, 3),
            total_requests=total_requests,
            event="cache_performance"
        )
        
        if hit_rate < 0.7:  # 70% 미만이면 경고
            logger.warning("Low cache hit rate")
        else:
            logger.info("Cache performance metrics")


class BusinessLogger:
    """비즈니스 로직 로깅"""
    
    @staticmethod
    def log_feedback_generated(request_id: str, user_id: str, 
                             feedback_type: str, book_id: str = None):
        """피드백 생성 로그"""
        logger = structlog.get_logger("business").bind(
            request_id=request_id,
            user_id=user_id,
            feedback_type=feedback_type,
            book_id=book_id,
            event="feedback_generated"
        )
        logger.info("Feedback generated for user")
    
    @staticmethod
    def log_analysis_completed(request_id: str, analysis_type: str, 
                             processing_time_ms: float, data_size: int = None):
        """분석 완료 로그"""
        logger = structlog.get_logger("business").bind(
            request_id=request_id,
            analysis_type=analysis_type,
            processing_time_ms=round(processing_time_ms, 2),
            data_size=data_size,
            event="analysis_completed"
        )
        logger.info("Analysis completed")


def get_logger(name: str = None) -> structlog.BoundLogger:
    """구조화된 로거 반환"""
    return structlog.get_logger(name)


def setup_health_check_logging():
    """헬스체크 로깅 설정 (노이즈 감소)"""
    # 헬스체크 엔드포인트는 로그 레벨을 높여서 노이즈 감소
    logging.getLogger("uvicorn.access").addFilter(
        lambda record: "/health" not in record.getMessage()
    )


# 로깅 시스템 초기화
def initialize_logging():
    """로깅 시스템 전체 초기화"""
    try:
        setup_logging()
        setup_health_check_logging()
        
        logger = get_logger("system")
        logger.info(
            "Logging system initialized",
            log_level=get_current_settings().LOG_LEVEL,
            log_format=get_current_settings().LOG_FORMAT
        )
        
    except Exception as e:
        # 로깅 설정 실패 시 기본 로깅으로 폴백
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
        )
        logging.error(f"Failed to initialize structured logging: {e}")
        logging.info("Fallback to basic logging")