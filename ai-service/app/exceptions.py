"""
FastAPI 글로벌 예외 처리 시스템

BookReview AI Service의 통합된 예외 처리를 위한 모듈입니다.
백엔드와 일관된 에러 코드 체계와 응답 형식을 제공합니다.
"""

from enum import Enum
from typing import Any, Dict, Optional
from fastapi import HTTPException, status
from pydantic import BaseModel


class ErrorCode(str, Enum):
    """표준화된 에러 코드 체계"""
    
    # Common Errors (COMMON)
    INVALID_INPUT = "COMMON001"
    INVALID_TYPE_VALUE = "COMMON002"
    ENTITY_NOT_FOUND = "COMMON003"
    INTERNAL_SERVER_ERROR = "COMMON004"
    INVALID_JSON_FORMAT = "COMMON005"
    ACCESS_DENIED = "COMMON006"
    
    # Authentication & Authorization (AUTH)
    AUTHENTICATION_FAILED = "AUTH001"
    INVALID_API_KEY = "AUTH002"
    EXPIRED_TOKEN = "AUTH003"
    INSUFFICIENT_PERMISSIONS = "AUTH004"
    RATE_LIMIT_EXCEEDED = "AUTH005"
    
    # AI Service Specific (AI)
    OPENAI_API_ERROR = "AI001"
    OPENAI_API_TIMEOUT = "AI002"
    OPENAI_QUOTA_EXCEEDED = "AI003"
    INVALID_PROMPT = "AI004"
    MODEL_NOT_AVAILABLE = "AI005"
    TOKEN_LIMIT_EXCEEDED = "AI006"
    CONTENT_FILTER_TRIGGERED = "AI007"
    
    # Feedback Service (FEEDBACK)
    FEEDBACK_NOT_FOUND = "FEEDBACK001"
    INVALID_FEEDBACK_TYPE = "FEEDBACK002"
    FEEDBACK_GENERATION_FAILED = "FEEDBACK003"
    BATCH_SIZE_EXCEEDED = "FEEDBACK004"
    
    # Analysis Service (ANALYSIS)
    ANALYSIS_FAILED = "ANALYSIS001"
    UNSUPPORTED_LANGUAGE = "ANALYSIS002"
    TEXT_TOO_LONG = "ANALYSIS003"
    TEXT_TOO_SHORT = "ANALYSIS004"
    
    # Cache Service (CACHE)
    CACHE_ERROR = "CACHE001"
    CACHE_TIMEOUT = "CACHE002"
    REDIS_CONNECTION_ERROR = "CACHE003"
    
    # External API (EXTERNAL)
    EXTERNAL_API_ERROR = "EXT001"
    EXTERNAL_API_TIMEOUT = "EXT002"
    EXTERNAL_API_UNAVAILABLE = "EXT003"


class ErrorDetail(BaseModel):
    """에러 상세 정보 모델"""
    code: ErrorCode
    message: str
    details: Optional[Dict[str, Any]] = None


class APIError(BaseModel):
    """API 에러 응답 표준 모델"""
    success: bool = False
    error: ErrorDetail
    timestamp: float
    request_id: Optional[str] = None


class BaseAPIException(HTTPException):
    """기본 API 예외 클래스"""
    
    def __init__(
        self,
        error_code: ErrorCode,
        message: str,
        status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR,
        details: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None
    ):
        self.error_code = error_code
        self.message = message
        self.details = details
        
        super().__init__(
            status_code=status_code,
            detail={
                "error_code": error_code.value,
                "message": message,
                "details": details
            },
            headers=headers
        )


class ValidationError(BaseAPIException):
    """입력 검증 실패 예외"""
    
    def __init__(self, message: str = "입력값 검증에 실패했습니다.", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            error_code=ErrorCode.INVALID_INPUT,
            message=message,
            status_code=status.HTTP_400_BAD_REQUEST,
            details=details
        )


class AuthenticationError(BaseAPIException):
    """인증 실패 예외"""
    
    def __init__(self, message: str = "인증에 실패했습니다.", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            error_code=ErrorCode.AUTHENTICATION_FAILED,
            message=message,
            status_code=status.HTTP_401_UNAUTHORIZED,
            details=details
        )


class AuthorizationError(BaseAPIException):
    """인가 실패 예외"""
    
    def __init__(self, message: str = "접근 권한이 없습니다.", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            error_code=ErrorCode.ACCESS_DENIED,
            message=message,
            status_code=status.HTTP_403_FORBIDDEN,
            details=details
        )


class NotFoundError(BaseAPIException):
    """리소스 없음 예외"""
    
    def __init__(self, message: str = "요청한 리소스를 찾을 수 없습니다.", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            error_code=ErrorCode.ENTITY_NOT_FOUND,
            message=message,
            status_code=status.HTTP_404_NOT_FOUND,
            details=details
        )


class RateLimitError(BaseAPIException):
    """요청 한도 초과 예외"""
    
    def __init__(self, message: str = "요청 한도를 초과했습니다.", details: Optional[Dict[str, Any]] = None):
        super().__init__(
            error_code=ErrorCode.RATE_LIMIT_EXCEEDED,
            message=message,
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            details=details
        )


class OpenAIError(BaseAPIException):
    """OpenAI API 관련 예외"""
    
    def __init__(
        self, 
        message: str = "AI 서비스 처리 중 오류가 발생했습니다.", 
        error_code: ErrorCode = ErrorCode.OPENAI_API_ERROR,
        details: Optional[Dict[str, Any]] = None
    ):
        status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        
        # 에러 코드별 HTTP 상태 코드 매핑
        if error_code == ErrorCode.OPENAI_QUOTA_EXCEEDED:
            status_code = status.HTTP_429_TOO_MANY_REQUESTS
        elif error_code == ErrorCode.INVALID_PROMPT:
            status_code = status.HTTP_400_BAD_REQUEST
        elif error_code == ErrorCode.TOKEN_LIMIT_EXCEEDED:
            status_code = status.HTTP_413_REQUEST_ENTITY_TOO_LARGE
        elif error_code == ErrorCode.CONTENT_FILTER_TRIGGERED:
            status_code = status.HTTP_400_BAD_REQUEST
            
        super().__init__(
            error_code=error_code,
            message=message,
            status_code=status_code,
            details=details
        )


class FeedbackError(BaseAPIException):
    """피드백 서비스 관련 예외"""
    
    def __init__(
        self, 
        message: str = "피드백 처리 중 오류가 발생했습니다.", 
        error_code: ErrorCode = ErrorCode.FEEDBACK_GENERATION_FAILED,
        details: Optional[Dict[str, Any]] = None
    ):
        status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        
        if error_code == ErrorCode.FEEDBACK_NOT_FOUND:
            status_code = status.HTTP_404_NOT_FOUND
        elif error_code == ErrorCode.INVALID_FEEDBACK_TYPE:
            status_code = status.HTTP_400_BAD_REQUEST
        elif error_code == ErrorCode.BATCH_SIZE_EXCEEDED:
            status_code = status.HTTP_413_REQUEST_ENTITY_TOO_LARGE
            
        super().__init__(
            error_code=error_code,
            message=message,
            status_code=status_code,
            details=details
        )


class AnalysisError(BaseAPIException):
    """텍스트 분석 서비스 관련 예외"""
    
    def __init__(
        self, 
        message: str = "텍스트 분석 중 오류가 발생했습니다.", 
        error_code: ErrorCode = ErrorCode.ANALYSIS_FAILED,
        details: Optional[Dict[str, Any]] = None
    ):
        status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        
        if error_code in [ErrorCode.TEXT_TOO_LONG, ErrorCode.TEXT_TOO_SHORT, ErrorCode.UNSUPPORTED_LANGUAGE]:
            status_code = status.HTTP_400_BAD_REQUEST
            
        super().__init__(
            error_code=error_code,
            message=message,
            status_code=status_code,
            details=details
        )


class CacheError(BaseAPIException):
    """캐시 서비스 관련 예외"""
    
    def __init__(
        self, 
        message: str = "캐시 처리 중 오류가 발생했습니다.", 
        error_code: ErrorCode = ErrorCode.CACHE_ERROR,
        details: Optional[Dict[str, Any]] = None
    ):
        super().__init__(
            error_code=error_code,
            message=message,
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            details=details
        )


class ExternalAPIError(BaseAPIException):
    """외부 API 관련 예외"""
    
    def __init__(
        self, 
        message: str = "외부 서비스 연동 중 오류가 발생했습니다.", 
        error_code: ErrorCode = ErrorCode.EXTERNAL_API_ERROR,
        details: Optional[Dict[str, Any]] = None
    ):
        super().__init__(
            error_code=error_code,
            message=message,
            status_code=status.HTTP_502_BAD_GATEWAY,
            details=details
        )


# 편의 함수들
def create_validation_error(field: str, message: str) -> ValidationError:
    """필드별 검증 에러 생성"""
    return ValidationError(
        message=f"{field}: {message}",
        details={"field": field, "validation_error": message}
    )


def create_openai_error(original_error: Exception) -> OpenAIError:
    """OpenAI 에러를 래핑하여 표준 에러로 변환"""
    error_message = str(original_error)
    
    # OpenAI 에러 타입별 분류
    if "rate limit" in error_message.lower():
        return OpenAIError(
            message="AI 서비스 요청 한도를 초과했습니다.",
            error_code=ErrorCode.OPENAI_QUOTA_EXCEEDED,
            details={"original_error": error_message}
        )
    elif "timeout" in error_message.lower():
        return OpenAIError(
            message="AI 서비스 응답 시간이 초과되었습니다.",
            error_code=ErrorCode.OPENAI_API_TIMEOUT,
            details={"original_error": error_message}
        )
    elif "content policy" in error_message.lower():
        return OpenAIError(
            message="콘텐츠 정책 위반으로 요청이 거부되었습니다.",
            error_code=ErrorCode.CONTENT_FILTER_TRIGGERED,
            details={"original_error": error_message}
        )
    else:
        return OpenAIError(
            message="AI 서비스 처리 중 오류가 발생했습니다.",
            error_code=ErrorCode.OPENAI_API_ERROR,
            details={"original_error": error_message}
        )


def create_cache_error(original_error: Exception) -> CacheError:
    """캐시 에러를 래핑하여 표준 에러로 변환"""
    error_message = str(original_error)
    
    if "connection" in error_message.lower() or "redis" in error_message.lower():
        return CacheError(
            message="캐시 서버 연결에 실패했습니다.",
            error_code=ErrorCode.REDIS_CONNECTION_ERROR,
            details={"original_error": error_message}
        )
    elif "timeout" in error_message.lower():
        return CacheError(
            message="캐시 응답 시간이 초과되었습니다.",
            error_code=ErrorCode.CACHE_TIMEOUT,
            details={"original_error": error_message}
        )
    else:
        return CacheError(
            message="캐시 처리 중 오류가 발생했습니다.",
            error_code=ErrorCode.CACHE_ERROR,
            details={"original_error": error_message}
        )