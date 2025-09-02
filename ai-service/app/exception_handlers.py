"""
FastAPI 글로벌 예외 핸들러

모든 예외를 중앙집중식으로 처리하고 일관된 에러 응답을 제공합니다.
"""

import time
import traceback
import uuid
from typing import Any, Dict

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError, HTTPException
from fastapi.responses import JSONResponse
from pydantic import ValidationError as PydanticValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

import structlog

from .exceptions import (
    APIError,
    BaseAPIException,
    ErrorCode,
    ErrorDetail,
    create_openai_error,
    create_cache_error
)

# 구조화된 로거 설정
logger = structlog.get_logger(__name__)


def create_error_response(
    error_code: ErrorCode,
    message: str,
    status_code: int,
    details: Dict[str, Any] = None,
    request_id: str = None
) -> JSONResponse:
    """표준 에러 응답 생성"""
    
    error_response = APIError(
        error=ErrorDetail(
            code=error_code,
            message=message,
            details=details
        ),
        timestamp=time.time(),
        request_id=request_id
    )
    
    return JSONResponse(
        status_code=status_code,
        content=error_response.model_dump()
    )


async def base_api_exception_handler(request: Request, exc: BaseAPIException) -> JSONResponse:
    """커스텀 API 예외 처리"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # 로깅
    logger.warning(
        "API exception occurred",
        error_code=exc.error_code.value,
        message=exc.message,
        details=exc.details,
        path=request.url.path,
        method=request.method,
        request_id=request_id
    )
    
    return create_error_response(
        error_code=exc.error_code,
        message=exc.message,
        status_code=exc.status_code,
        details=exc.details,
        request_id=request_id
    )


async def http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    """표준 HTTP 예외 처리"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # 상태 코드별 에러 코드 매핑
    error_code_mapping = {
        400: ErrorCode.INVALID_INPUT,
        401: ErrorCode.AUTHENTICATION_FAILED,
        403: ErrorCode.ACCESS_DENIED,
        404: ErrorCode.ENTITY_NOT_FOUND,
        429: ErrorCode.RATE_LIMIT_EXCEEDED,
        500: ErrorCode.INTERNAL_SERVER_ERROR,
    }
    
    error_code = error_code_mapping.get(exc.status_code, ErrorCode.INTERNAL_SERVER_ERROR)
    
    logger.warning(
        "HTTP exception occurred",
        status_code=exc.status_code,
        detail=exc.detail,
        path=request.url.path,
        method=request.method,
        request_id=request_id
    )
    
    return create_error_response(
        error_code=error_code,
        message=str(exc.detail) if exc.detail else "요청 처리 중 오류가 발생했습니다.",
        status_code=exc.status_code,
        request_id=request_id
    )


async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    """요청 검증 예외 처리"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # 검증 오류 정보 파싱
    validation_errors = {}
    for error in exc.errors():
        field_path = " -> ".join(str(loc) for loc in error["loc"][1:])  # 'body' 제외
        validation_errors[field_path] = error["msg"]
    
    logger.warning(
        "Request validation failed",
        validation_errors=validation_errors,
        path=request.url.path,
        method=request.method,
        request_id=request_id
    )
    
    return create_error_response(
        error_code=ErrorCode.INVALID_INPUT,
        message="입력값 검증에 실패했습니다.",
        status_code=status.HTTP_400_BAD_REQUEST,
        details={
            "validation_errors": validation_errors,
            "error_count": len(validation_errors)
        },
        request_id=request_id
    )


async def pydantic_validation_exception_handler(request: Request, exc: PydanticValidationError) -> JSONResponse:
    """Pydantic 검증 예외 처리"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # Pydantic 검증 오류 파싱
    validation_errors = {}
    for error in exc.errors():
        field_path = " -> ".join(str(loc) for loc in error["loc"])
        validation_errors[field_path] = error["msg"]
    
    logger.warning(
        "Pydantic validation failed",
        validation_errors=validation_errors,
        path=request.url.path,
        method=request.method,
        request_id=request_id
    )
    
    return create_error_response(
        error_code=ErrorCode.INVALID_INPUT,
        message="데이터 검증에 실패했습니다.",
        status_code=status.HTTP_400_BAD_REQUEST,
        details={
            "validation_errors": validation_errors,
            "error_count": len(validation_errors)
        },
        request_id=request_id
    )


async def openai_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """OpenAI 관련 예외 처리"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # OpenAI 예외를 표준 예외로 변환
    api_exception = create_openai_error(exc)
    
    logger.error(
        "OpenAI API error occurred",
        error_code=api_exception.error_code.value,
        message=api_exception.message,
        original_error=str(exc),
        path=request.url.path,
        method=request.method,
        request_id=request_id
    )
    
    return create_error_response(
        error_code=api_exception.error_code,
        message=api_exception.message,
        status_code=api_exception.status_code,
        details=api_exception.details,
        request_id=request_id
    )


async def redis_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Redis 관련 예외 처리"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # 캐시 예외를 표준 예외로 변환
    api_exception = create_cache_error(exc)
    
    logger.error(
        "Redis/Cache error occurred",
        error_code=api_exception.error_code.value,
        message=api_exception.message,
        original_error=str(exc),
        path=request.url.path,
        method=request.method,
        request_id=request_id
    )
    
    return create_error_response(
        error_code=api_exception.error_code,
        message=api_exception.message,
        status_code=api_exception.status_code,
        details=api_exception.details,
        request_id=request_id
    )


async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """일반적인 예외 처리 (최후 방어선)"""
    
    request_id = getattr(request.state, 'request_id', str(uuid.uuid4()))
    
    # 스택 트레이스 로깅 (개발 환경에서만)
    logger.error(
        "Unexpected error occurred",
        error_type=type(exc).__name__,
        error_message=str(exc),
        path=request.url.path,
        method=request.method,
        request_id=request_id,
        exc_info=True  # 스택 트레이스 포함
    )
    
    return create_error_response(
        error_code=ErrorCode.INTERNAL_SERVER_ERROR,
        message="서버 내부 오류가 발생했습니다.",
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        details={
            "error_type": type(exc).__name__,
            "error_message": str(exc) if str(exc) else "Unknown error"
        },
        request_id=request_id
    )


def setup_exception_handlers(app: FastAPI) -> None:
    """FastAPI 앱에 예외 핸들러 등록"""
    
    # 커스텀 API 예외
    app.add_exception_handler(BaseAPIException, base_api_exception_handler)
    
    # 표준 HTTP 예외
    app.add_exception_handler(HTTPException, http_exception_handler)
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    
    # 검증 예외
    app.add_exception_handler(RequestValidationError, validation_exception_handler)
    app.add_exception_handler(PydanticValidationError, pydantic_validation_exception_handler)
    
    # OpenAI 관련 예외 (openai 패키지의 예외들)
    try:
        import openai
        if hasattr(openai, 'OpenAIError'):
            app.add_exception_handler(openai.OpenAIError, openai_exception_handler)
        if hasattr(openai, 'RateLimitError'):
            app.add_exception_handler(openai.RateLimitError, openai_exception_handler)
        if hasattr(openai, 'APITimeoutError'):
            app.add_exception_handler(openai.APITimeoutError, openai_exception_handler)
    except ImportError:
        logger.warning("OpenAI package not found, skipping OpenAI exception handlers")
    
    # Redis 관련 예외
    try:
        import redis
        app.add_exception_handler(redis.RedisError, redis_exception_handler)
        app.add_exception_handler(redis.ConnectionError, redis_exception_handler)
        app.add_exception_handler(redis.TimeoutError, redis_exception_handler)
    except ImportError:
        logger.warning("Redis package not found, skipping Redis exception handlers")
    
    # 모든 예외의 최후 방어선
    app.add_exception_handler(Exception, generic_exception_handler)
    
    logger.info("Exception handlers registered successfully")