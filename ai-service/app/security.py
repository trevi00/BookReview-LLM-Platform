"""
보안 및 인증 관련 유틸리티

JWT 기반 인증, API 키 검증, 보안 헤더 관리 등을 담당합니다.
python-jose 대신 PyJWT를 사용하여 보안 이슈를 해결합니다.
"""

import os
import hashlib
import secrets
from datetime import datetime, timedelta
from typing import Optional, Dict, Any

import jwt
from passlib.context import CryptContext
from fastapi import HTTPException, Security, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import structlog

from .exceptions import AuthenticationError, AuthorizationError

logger = structlog.get_logger(__name__)

# 비밀번호 해싱 컨텍스트
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# JWT 보안 설정
JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "ai-service-default-secret-change-in-production")
JWT_ALGORITHM = "HS256"
JWT_ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("JWT_ACCESS_TOKEN_EXPIRE_MINUTES", "30"))

# API 키 설정
API_KEY = os.getenv("AI_SERVICE_API_KEY", "")

# HTTP Bearer 보안 스키마
security = HTTPBearer(auto_error=False)


class SecurityUtils:
    """보안 관련 유틸리티 클래스"""
    
    @staticmethod
    def hash_password(password: str) -> str:
        """비밀번호 해싱"""
        return pwd_context.hash(password)
    
    @staticmethod
    def verify_password(plain_password: str, hashed_password: str) -> bool:
        """비밀번호 검증"""
        return pwd_context.verify(plain_password, hashed_password)
    
    @staticmethod
    def generate_api_key(length: int = 32) -> str:
        """안전한 API 키 생성"""
        return secrets.token_urlsafe(length)
    
    @staticmethod
    def hash_api_key(api_key: str) -> str:
        """API 키 해싱 (저장용)"""
        return hashlib.sha256(api_key.encode()).hexdigest()
    
    @staticmethod
    def verify_api_key(provided_key: str, stored_hash: str) -> bool:
        """API 키 검증"""
        provided_hash = SecurityUtils.hash_api_key(provided_key)
        return secrets.compare_digest(provided_hash, stored_hash)
    
    @staticmethod
    def mask_sensitive_data(data: str, show_chars: int = 8) -> str:
        """민감한 데이터 마스킹"""
        if not data or len(data) <= show_chars:
            return "*" * len(data) if data else ""
        return data[:show_chars] + "*" * (len(data) - show_chars)


class JWTManager:
    """JWT 토큰 관리 클래스"""
    
    def __init__(self):
        self.secret_key = JWT_SECRET_KEY
        self.algorithm = JWT_ALGORITHM
        self.access_token_expire_minutes = JWT_ACCESS_TOKEN_EXPIRE_MINUTES
        
        # JWT Secret 키 검증
        if self.secret_key == "ai-service-default-secret-change-in-production":
            logger.warning("🚨 Using default JWT secret! Change JWT_SECRET_KEY in production!")
        
        if len(self.secret_key.encode()) < 32:
            logger.warning(f"🚨 JWT secret key is shorter than recommended 256 bits. Current: {len(self.secret_key.encode())} bytes")
    
    def create_access_token(self, data: Dict[str, Any], expires_delta: Optional[timedelta] = None) -> str:
        """액세스 토큰 생성"""
        to_encode = data.copy()
        
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(minutes=self.access_token_expire_minutes)
        
        to_encode.update({
            "exp": expire,
            "iat": datetime.utcnow(),
            "type": "access"
        })
        
        try:
            encoded_jwt = jwt.encode(to_encode, self.secret_key, algorithm=self.algorithm)
            
            logger.info(
                "JWT token created",
                subject=data.get("sub", "unknown"),
                expires_at=expire.isoformat(),
                token_type="access"
            )
            
            return encoded_jwt
        except Exception as e:
            logger.error("JWT token creation failed", error=str(e))
            raise AuthenticationError(
                message="토큰 생성에 실패했습니다.",
                details={"error": str(e)}
            )
    
    def verify_token(self, token: str) -> Dict[str, Any]:
        """토큰 검증 및 페이로드 반환"""
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            
            # 토큰 타입 검증
            if payload.get("type") != "access":
                raise AuthenticationError(
                    message="유효하지 않은 토큰 타입입니다.",
                    details={"expected_type": "access", "actual_type": payload.get("type")}
                )
            
            logger.debug(
                "JWT token verified",
                subject=payload.get("sub", "unknown"),
                expires_at=datetime.fromtimestamp(payload.get("exp", 0)).isoformat()
            )
            
            return payload
            
        except jwt.ExpiredSignatureError:
            logger.warning("JWT token expired")
            raise AuthenticationError(
                message="토큰이 만료되었습니다.",
                details={"error_type": "expired"}
            )
        except jwt.InvalidTokenError as e:
            logger.warning("JWT token invalid", error=str(e))
            raise AuthenticationError(
                message="유효하지 않은 토큰입니다.",
                details={"error_type": "invalid", "error": str(e)}
            )
        except Exception as e:
            logger.error("JWT token verification failed", error=str(e))
            raise AuthenticationError(
                message="토큰 검증에 실패했습니다.",
                details={"error": str(e)}
            )
    
    def get_token_payload(self, token: str) -> Optional[Dict[str, Any]]:
        """토큰 페이로드 추출 (검증 없이, 디버깅용)"""
        try:
            # JWT 디코딩 (검증 없이)
            unverified_payload = jwt.decode(token, options={"verify_signature": False})
            return unverified_payload
        except Exception as e:
            logger.debug("Failed to decode token payload", error=str(e))
            return None


# JWT 관리자 인스턴스
jwt_manager = JWTManager()


async def get_current_user(credentials: Optional[HTTPAuthorizationCredentials] = Security(security)) -> Dict[str, Any]:
    """현재 사용자 정보 가져오기 (JWT 기반)"""
    if not credentials:
        raise AuthenticationError(
            message="인증 토큰이 필요합니다.",
            details={"auth_scheme": "Bearer"}
        )
    
    token = credentials.credentials
    payload = jwt_manager.verify_token(token)
    
    return {
        "user_id": payload.get("user_id"),
        "username": payload.get("sub"),
        "permissions": payload.get("permissions", []),
        "expires_at": payload.get("exp")
    }


async def verify_api_key(credentials: Optional[HTTPAuthorizationCredentials] = Security(security)) -> bool:
    """API 키 검증"""
    if not API_KEY:
        logger.warning("API_KEY not configured, skipping API key verification")
        return True
    
    if not credentials:
        raise AuthenticationError(
            message="API 키가 필요합니다.",
            details={"required_header": "Authorization: Bearer <api_key>"}
        )
    
    provided_key = credentials.credentials
    
    if provided_key != API_KEY:
        logger.warning(
            "Invalid API key provided",
            provided_key_preview=SecurityUtils.mask_sensitive_data(provided_key, 8)
        )
        raise AuthenticationError(
            message="유효하지 않은 API 키입니다.",
            details={"provided_key_preview": SecurityUtils.mask_sensitive_data(provided_key, 8)}
        )
    
    logger.debug("API key verified successfully")
    return True


async def get_optional_user(credentials: Optional[HTTPAuthorizationCredentials] = Security(security)) -> Optional[Dict[str, Any]]:
    """선택적 사용자 인증 (토큰이 없어도 허용)"""
    if not credentials:
        return None
    
    try:
        return await get_current_user(credentials)
    except AuthenticationError:
        return None


def require_permissions(required_permissions: list):
    """권한 확인 데코레이터"""
    def decorator(func):
        async def wrapper(*args, **kwargs):
            # 현재 사용자 정보에서 권한 확인
            current_user = kwargs.get('current_user')
            if not current_user:
                raise AuthorizationError(
                    message="사용자 인증이 필요합니다.",
                    details={"required_permissions": required_permissions}
                )
            
            user_permissions = current_user.get("permissions", [])
            
            # 필요한 권한이 모두 있는지 확인
            missing_permissions = set(required_permissions) - set(user_permissions)
            if missing_permissions:
                logger.warning(
                    "Insufficient permissions",
                    user_id=current_user.get("user_id"),
                    required=required_permissions,
                    user_permissions=user_permissions,
                    missing=list(missing_permissions)
                )
                raise AuthorizationError(
                    message="필요한 권한이 없습니다.",
                    details={
                        "required_permissions": required_permissions,
                        "user_permissions": user_permissions,
                        "missing_permissions": list(missing_permissions)
                    }
                )
            
            return await func(*args, **kwargs)
        return wrapper
    return decorator


class SecurityHeaders:
    """보안 헤더 관리"""
    
    @staticmethod
    def get_security_headers() -> Dict[str, str]:
        """보안 헤더 딕셔너리 반환"""
        return {
            # XSS 공격 방어
            "X-Content-Type-Options": "nosniff",
            "X-Frame-Options": "DENY",
            "X-XSS-Protection": "1; mode=block",
            
            # HTTPS 강제 (프로덕션 환경)
            "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
            
            # 콘텐츠 보안 정책
            "Content-Security-Policy": "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'",
            
            # 리퍼러 정책
            "Referrer-Policy": "strict-origin-when-cross-origin",
            
            # 권한 정책
            "Permissions-Policy": "geolocation=(), microphone=(), camera=()",
            
            # 캐시 제어
            "Cache-Control": "no-cache, no-store, must-revalidate",
            "Pragma": "no-cache",
            "Expires": "0"
        }


# 편의 함수들
def create_service_token(service_name: str, permissions: list = None) -> str:
    """서비스 간 통신용 토큰 생성"""
    token_data = {
        "sub": service_name,
        "type": "service",
        "permissions": permissions or [],
        "service": True
    }
    
    # 서비스 토큰은 더 긴 만료 시간 (24시간)
    expires_delta = timedelta(hours=24)
    return jwt_manager.create_access_token(token_data, expires_delta)


def validate_password_strength(password: str) -> tuple[bool, list]:
    """비밀번호 강도 검증"""
    errors = []
    
    if len(password) < 8:
        errors.append("비밀번호는 최소 8자 이상이어야 합니다.")
    
    if not any(c.isupper() for c in password):
        errors.append("비밀번호는 최소 1개의 대문자를 포함해야 합니다.")
    
    if not any(c.islower() for c in password):
        errors.append("비밀번호는 최소 1개의 소문자를 포함해야 합니다.")
    
    if not any(c.isdigit() for c in password):
        errors.append("비밀번호는 최소 1개의 숫자를 포함해야 합니다.")
    
    if not any(c in "!@#$%^&*()_+-=[]{}|;:,.<>?" for c in password):
        errors.append("비밀번호는 최소 1개의 특수문자를 포함해야 합니다.")
    
    return len(errors) == 0, errors