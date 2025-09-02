"""
예외 처리 및 에러 핸들러
"""

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
import structlog
from typing import Any, Dict

logger = structlog.get_logger(__name__)


class AIServiceException(Exception):
    """AI 서비스 기본 예외"""
    def __init__(self, message: str, code: str = "AI_SERVICE_ERROR"):
        self.message = message
        self.code = code
        super().__init__(self.message)


class OpenAIException(AIServiceException):
    """OpenAI API 관련 예외"""
    def __init__(self, message: str, code: str = "OPENAI_ERROR"):
        super().__init__(message, code)


class RateLimitException(AIServiceException):
    """요청 제한 예외"""
    def __init__(self, message: str = "요청 한도를 초과했습니다.", code: str = "RATE_LIMIT_EXCEEDED"):
        super().__init__(message, code)


class ValidationException(AIServiceException):
    """데이터 검증 예외"""
    def __init__(self, message: str, code: str = "VALIDATION_ERROR"):
        super().__init__(message, code)


class CacheException(AIServiceException):
    """캐시 관련 예외"""
    def __init__(self, message: str, code: str = "CACHE_ERROR"):
        super().__init__(message, code)


def create_error_response(
    status_code: int,
    message: str,
    code: str = "ERROR",
    details: Dict[str, Any] = None
) -> JSONResponse:
    """표준 에러 응답 생성"""
    error_data = {
        "success": False,
        "error": {
            "code": code,
            "message": message,
            "details": details or {}
        }
    }
    return JSONResponse(status_code=status_code, content=error_data)


def setup_exception_handlers(app: FastAPI):
    """예외 핸들러 설정"""

    @app.exception_handler(AIServiceException)
    async def ai_service_exception_handler(request: Request, exc: AIServiceException):
        logger.error(
            "AI 서비스 예외 발생",
            code=exc.code,
            message=exc.message,
            path=request.url.path,
            method=request.method
        )
        return create_error_response(
            status_code=400,
            message=exc.message,
            code=exc.code
        )

    @app.exception_handler(OpenAIException)
    async def openai_exception_handler(request: Request, exc: OpenAIException):
        logger.error(
            "OpenAI API 예외 발생",
            code=exc.code,
            message=exc.message,
            path=request.url.path,
            method=request.method
        )
        return create_error_response(
            status_code=502,
            message="AI 서비스에 일시적인 문제가 발생했습니다.",
            code=exc.code
        )

    @app.exception_handler(RateLimitException)
    async def rate_limit_exception_handler(request: Request, exc: RateLimitException):
        logger.warning(
            "요청 제한 초과",
            code=exc.code,
            message=exc.message,
            path=request.url.path,
            method=request.method,
            client_ip=request.client.host
        )
        return create_error_response(
            status_code=429,
            message=exc.message,
            code=exc.code
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        logger.warning(
            "요청 데이터 검증 실패",
            errors=exc.errors(),
            path=request.url.path,
            method=request.method
        )
        return create_error_response(
            status_code=422,
            message="요청 데이터가 올바르지 않습니다.",
            code="VALIDATION_ERROR",
            details={"validation_errors": exc.errors()}
        )

    @app.exception_handler(HTTPException)
    async def http_exception_handler(request: Request, exc: HTTPException):
        logger.warning(
            "HTTP 예외 발생",
            status_code=exc.status_code,
            detail=exc.detail,
            path=request.url.path,
            method=request.method
        )
        return create_error_response(
            status_code=exc.status_code,
            message=exc.detail,
            code="HTTP_ERROR"
        )

    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception):
        logger.error(
            "예상치 못한 예외 발생",
            error=str(exc),
            error_type=type(exc).__name__,
            path=request.url.path,
            method=request.method,
            exc_info=True
        )
        return create_error_response(
            status_code=500,
            message="서버 내부 오류가 발생했습니다.",
            code="INTERNAL_SERVER_ERROR"
        )