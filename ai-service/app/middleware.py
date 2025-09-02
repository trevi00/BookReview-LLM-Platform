"""
FastAPI 미들웨어 모음

요청 추적, 보안, 로깅 등을 위한 미들웨어를 정의합니다.
"""

import time
import uuid
from typing import Callable

from fastapi import Request, Response
from fastapi.middleware.base import BaseHTTPMiddleware
from starlette.middleware.base import RequestResponseEndpoint
import structlog

logger = structlog.get_logger(__name__)


class RequestTrackingMiddleware(BaseHTTPMiddleware):
    """요청 추적 미들웨어
    
    각 요청에 고유 ID를 부여하고 요청/응답 시간을 로깅합니다.
    """
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        # 요청 고유 ID 생성
        request_id = str(uuid.uuid4())
        request.state.request_id = request_id
        
        # 요청 시작 시간 기록
        start_time = time.time()
        
        # 요청 정보 로깅
        logger.info(
            "Request started",
            request_id=request_id,
            method=request.method,
            path=request.url.path,
            query_params=str(request.query_params),
            client_ip=self._get_client_ip(request),
            user_agent=request.headers.get("user-agent", "unknown")
        )
        
        try:
            # 다음 미들웨어/핸들러 호출
            response = await call_next(request)
            
            # 응답 시간 계산
            process_time = time.time() - start_time
            
            # 응답 정보 로깅
            logger.info(
                "Request completed",
                request_id=request_id,
                method=request.method,
                path=request.url.path,
                status_code=response.status_code,
                process_time=round(process_time, 4),
                response_size=response.headers.get("content-length", "unknown")
            )
            
            # 응답 헤더에 요청 ID 추가
            response.headers["X-Request-ID"] = request_id
            response.headers["X-Process-Time"] = str(round(process_time, 4))
            
            return response
            
        except Exception as exc:
            # 예외 발생 시간 계산
            process_time = time.time() - start_time
            
            # 예외 정보 로깅
            logger.error(
                "Request failed with exception",
                request_id=request_id,
                method=request.method,
                path=request.url.path,
                process_time=round(process_time, 4),
                exception=str(exc),
                exception_type=type(exc).__name__
            )
            
            # 예외 재발생 (글로벌 예외 핸들러에서 처리)
            raise exc
    
    def _get_client_ip(self, request: Request) -> str:
        """클라이언트 IP 주소 추출"""
        # X-Forwarded-For 헤더 확인 (로드밸런서/프록시 뒤에 있는 경우)
        forwarded_for = request.headers.get("x-forwarded-for")
        if forwarded_for:
            # 첫 번째 IP가 실제 클라이언트 IP
            return forwarded_for.split(",")[0].strip()
        
        # X-Real-IP 헤더 확인
        real_ip = request.headers.get("x-real-ip")
        if real_ip:
            return real_ip.strip()
        
        # 직접 연결된 클라이언트 IP
        return getattr(request.client, "host", "unknown")


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """보안 헤더 미들웨어
    
    응답에 보안 관련 헤더를 자동으로 추가합니다.
    """
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        response = await call_next(request)
        
        # 보안 헤더 추가
        security_headers = {
            # XSS 공격 방어
            "X-Content-Type-Options": "nosniff",
            "X-Frame-Options": "DENY",
            "X-XSS-Protection": "1; mode=block",
            
            # HTTPS 강제 (프로덕션 환경에서)
            "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
            
            # 콘텐츠 보안 정책
            "Content-Security-Policy": "default-src 'self'",
            
            # 리퍼러 정책
            "Referrer-Policy": "strict-origin-when-cross-origin",
            
            # 권한 정책
            "Permissions-Policy": "geolocation=(), microphone=(), camera=()"
        }
        
        for header, value in security_headers.items():
            response.headers[header] = value
        
        return response


class RateLimitMiddleware(BaseHTTPMiddleware):
    """요청 제한 미들웨어
    
    IP별 요청 횟수를 제한합니다.
    """
    
    def __init__(self, app, calls: int = 100, period: int = 60):
        super().__init__(app)
        self.calls = calls  # 허용 요청 수
        self.period = period  # 시간 윈도우 (초)
        self.clients = {}  # IP별 요청 기록
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        client_ip = self._get_client_ip(request)
        current_time = time.time()
        
        # 클라이언트별 요청 기록 정리 (만료된 기록 제거)
        if client_ip in self.clients:
            self.clients[client_ip] = [
                req_time for req_time in self.clients[client_ip]
                if current_time - req_time < self.period
            ]
        else:
            self.clients[client_ip] = []
        
        # 요청 횟수 확인
        if len(self.clients[client_ip]) >= self.calls:
            logger.warning(
                "Rate limit exceeded",
                client_ip=client_ip,
                request_count=len(self.clients[client_ip]),
                limit=self.calls,
                period=self.period,
                path=request.url.path
            )
            
            from .exceptions import RateLimitError
            raise RateLimitError(
                message=f"요청 한도 초과: {self.period}초 동안 최대 {self.calls}회 요청 가능",
                details={
                    "limit": self.calls,
                    "period": self.period,
                    "current_requests": len(self.clients[client_ip])
                }
            )
        
        # 현재 요청 기록
        self.clients[client_ip].append(current_time)
        
        response = await call_next(request)
        
        # 응답 헤더에 요청 제한 정보 추가
        remaining = self.calls - len(self.clients[client_ip])
        response.headers["X-RateLimit-Limit"] = str(self.calls)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        response.headers["X-RateLimit-Reset"] = str(int(current_time + self.period))
        
        return response
    
    def _get_client_ip(self, request: Request) -> str:
        """클라이언트 IP 주소 추출 (RequestTrackingMiddleware와 동일)"""
        forwarded_for = request.headers.get("x-forwarded-for")
        if forwarded_for:
            return forwarded_for.split(",")[0].strip()
        
        real_ip = request.headers.get("x-real-ip")
        if real_ip:
            return real_ip.strip()
        
        return getattr(request.client, "host", "unknown")


class APIKeyValidationMiddleware(BaseHTTPMiddleware):
    """API 키 검증 미들웨어
    
    보호된 엔드포인트에 대한 API 키 인증을 처리합니다.
    """
    
    def __init__(self, app, api_key: str, protected_paths: list = None):
        super().__init__(app)
        self.api_key = api_key
        self.protected_paths = protected_paths or ["/api/v1/"]
        self.excluded_paths = [
            "/docs",
            "/redoc",
            "/openapi.json",
            "/health",
            "/metrics"
        ]
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        # 보호되지 않는 경로인지 확인
        if any(request.url.path.startswith(path) for path in self.excluded_paths):
            return await call_next(request)
        
        # 보호된 경로인지 확인
        is_protected = any(request.url.path.startswith(path) for path in self.protected_paths)
        
        if is_protected:
            # API 키 검증
            api_key = request.headers.get("X-API-Key") or request.headers.get("Authorization")
            
            if not api_key:
                logger.warning(
                    "API key missing",
                    path=request.url.path,
                    client_ip=self._get_client_ip(request)
                )
                
                from .exceptions import AuthenticationError
                raise AuthenticationError(
                    message="API 키가 필요합니다.",
                    details={"required_header": "X-API-Key"}
                )
            
            # Bearer 토큰 형식인 경우 추출
            if api_key.startswith("Bearer "):
                api_key = api_key[7:]
            
            if api_key != self.api_key:
                logger.warning(
                    "Invalid API key",
                    path=request.url.path,
                    client_ip=self._get_client_ip(request),
                    provided_key_prefix=api_key[:8] + "..." if len(api_key) > 8 else api_key
                )
                
                from .exceptions import AuthenticationError
                raise AuthenticationError(
                    message="유효하지 않은 API 키입니다.",
                    details={"provided_key_prefix": api_key[:8] + "..." if len(api_key) > 8 else api_key}
                )
            
            # API 키가 유효한 경우 요청 상태에 저장
            request.state.authenticated = True
            request.state.api_key_validated = True
        
        return await call_next(request)
    
    def _get_client_ip(self, request: Request) -> str:
        """클라이언트 IP 주소 추출"""
        forwarded_for = request.headers.get("x-forwarded-for")
        if forwarded_for:
            return forwarded_for.split(",")[0].strip()
        
        real_ip = request.headers.get("x-real-ip")
        if real_ip:
            return real_ip.strip()
        
        return getattr(request.client, "host", "unknown")


def setup_middleware(app, config=None):
    """미들웨어 설정 함수"""
    
    # 요청 추적 미들웨어 (가장 먼저)
    app.add_middleware(RequestTrackingMiddleware)
    
    # 보안 헤더 미들웨어
    app.add_middleware(SecurityHeadersMiddleware)
    
    # Rate Limiting 미들웨어
    if config:
        rate_limit_calls = getattr(config, 'RATE_LIMIT_CALLS', 100)
        rate_limit_period = getattr(config, 'RATE_LIMIT_PERIOD', 60)
    else:
        rate_limit_calls = 100
        rate_limit_period = 60
    
    app.add_middleware(RateLimitMiddleware, calls=rate_limit_calls, period=rate_limit_period)
    
    # API 키 검증 미들웨어 (설정이 있는 경우에만)
    if config and hasattr(config, 'API_KEY') and config.API_KEY:
        protected_paths = getattr(config, 'PROTECTED_PATHS', ["/api/v1/"])
        app.add_middleware(APIKeyValidationMiddleware, api_key=config.API_KEY, protected_paths=protected_paths)
    
    logger.info("Middleware setup completed")