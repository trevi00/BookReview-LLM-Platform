"""
데이터 검증 유틸리티 모듈
Pydantic 모델 외 추가적인 비즈니스 로직 검증
"""

import re
import html
from typing import List, Dict, Any, Optional, Tuple
from urllib.parse import urlparse
import langdetect
from langdetect.lang_detect_exception import LangDetectException

from .exceptions import ValidationError


class ContentValidator:
    """콘텐츠 검증 클래스"""
    
    # 허용되지 않는 패턴들
    FORBIDDEN_PATTERNS = [
        r'<script[^>]*>.*?</script>',  # JavaScript
        r'<iframe[^>]*>.*?</iframe>',  # iframe
        r'<object[^>]*>.*?</object>',  # object
        r'<embed[^>]*>.*?</embed>',    # embed
        r'javascript:',                # JavaScript URL
        r'vbscript:',                 # VBScript URL
        r'on\w+\s*=',                 # Event handlers
    ]
    
    # 의심스러운 키워드들
    SUSPICIOUS_KEYWORDS = [
        'eval', 'exec', 'system', 'shell_exec', 'passthru',
        'file_get_contents', 'curl_exec', 'base64_decode'
    ]
    
    @classmethod
    def validate_text_content(cls, content: str, max_length: int = 10000) -> Tuple[bool, List[str]]:
        """텍스트 콘텐츠 검증"""
        errors = []
        
        if not content or not content.strip():
            errors.append("내용이 비어있습니다.")
            return False, errors
        
        # 길이 검증
        if len(content) > max_length:
            errors.append(f"내용이 너무 깁니다. 최대 {max_length}자까지 허용됩니다.")
        
        # HTML/Script 태그 검증
        for pattern in cls.FORBIDDEN_PATTERNS:
            if re.search(pattern, content, re.IGNORECASE | re.DOTALL):
                errors.append("허용되지 않는 HTML 태그나 스크립트가 포함되어 있습니다.")
                break
        
        # 의심스러운 키워드 검증
        content_lower = content.lower()
        for keyword in cls.SUSPICIOUS_KEYWORDS:
            if keyword in content_lower:
                errors.append("보안상 허용되지 않는 키워드가 포함되어 있습니다.")
                break
        
        # 과도한 반복 문자 검증
        if re.search(r'(.)\1{20,}', content):
            errors.append("과도한 반복 문자가 포함되어 있습니다.")
        
        # 과도한 특수문자 검증
        special_char_ratio = len(re.findall(r'[^\w\s가-힣]', content)) / len(content)
        if special_char_ratio > 0.5:
            errors.append("특수문자 비율이 너무 높습니다.")
        
        return len(errors) == 0, errors
    
    @classmethod
    def sanitize_content(cls, content: str) -> str:
        """콘텐츠 정제"""
        # HTML 엔티티 디코딩
        content = html.unescape(content)
        
        # HTML 태그 제거 (허용된 태그 제외)
        content = re.sub(r'<[^>]+>', '', content)
        
        # 과도한 공백 정리
        content = re.sub(r'\s+', ' ', content)
        
        # 앞뒤 공백 제거
        content = content.strip()
        
        return content
    
    @classmethod
    def detect_language(cls, text: str) -> Optional[str]:
        """텍스트 언어 감지"""
        try:
            # 짧은 텍스트는 감지가 어려우므로 최소 길이 확인
            if len(text.strip()) < 10:
                return None
            
            detected_lang = langdetect.detect(text)
            return detected_lang
        except LangDetectException:
            return None
    
    @classmethod
    def validate_language_consistency(cls, text: str, expected_lang: str) -> bool:
        """언어 일관성 검증"""
        detected_lang = cls.detect_language(text)
        if detected_lang is None:
            return True  # 감지 실패 시 통과
        
        # 언어 코드 매핑 (langdetect -> ISO 639-1)
        lang_mapping = {
            'ko': 'ko', 'en': 'en', 'ja': 'ja', 'zh-cn': 'zh', 
            'zh-tw': 'zh', 'es': 'es', 'fr': 'fr', 'de': 'de'
        }
        
        detected_mapped = lang_mapping.get(detected_lang, detected_lang)
        expected_mapped = lang_mapping.get(expected_lang, expected_lang)
        
        return detected_mapped == expected_mapped


class APIKeyValidator:
    """API 키 검증 클래스"""
    
    @staticmethod
    def validate_api_key_format(api_key: str) -> Tuple[bool, str]:
        """API 키 형식 검증"""
        if not api_key:
            return False, "API 키가 제공되지 않았습니다."
        
        # 기본적인 형식 검증
        if len(api_key) < 20:
            return False, "API 키가 너무 짧습니다."
        
        if len(api_key) > 200:
            return False, "API 키가 너무 깁니다."
        
        # 허용되는 문자만 포함하는지 확인
        if not re.match(r'^[a-zA-Z0-9\-_\.]+$', api_key):
            return False, "API 키에 허용되지 않는 문자가 포함되어 있습니다."
        
        return True, "유효한 API 키 형식입니다."


class FileUploadValidator:
    """파일 업로드 검증 클래스"""
    
    ALLOWED_EXTENSIONS = {'.txt', '.pdf', '.docx', '.doc', '.rtf'}
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
    
    @classmethod
    def validate_file_extension(cls, filename: str) -> bool:
        """파일 확장자 검증"""
        if '.' not in filename:
            return False
        
        extension = '.' + filename.rsplit('.', 1)[1].lower()
        return extension in cls.ALLOWED_EXTENSIONS
    
    @classmethod
    def validate_file_size(cls, file_size: int) -> bool:
        """파일 크기 검증"""
        return file_size <= cls.MAX_FILE_SIZE
    
    @classmethod
    def validate_filename(cls, filename: str) -> Tuple[bool, str]:
        """파일명 검증"""
        if not filename:
            return False, "파일명이 제공되지 않았습니다."
        
        # 파일명 길이 검증
        if len(filename) > 255:
            return False, "파일명이 너무 깁니다."
        
        # 허용되지 않는 문자 검증
        forbidden_chars = r'[<>:"/\\|?*]'
        if re.search(forbidden_chars, filename):
            return False, "파일명에 허용되지 않는 문자가 포함되어 있습니다."
        
        # 확장자 검증
        if not cls.validate_file_extension(filename):
            return False, f"허용되지 않는 파일 형식입니다. 허용되는 형식: {', '.join(cls.ALLOWED_EXTENSIONS)}"
        
        return True, "유효한 파일명입니다."


class URLValidator:
    """URL 검증 클래스"""
    
    ALLOWED_SCHEMES = {'http', 'https'}
    BLOCKED_DOMAINS = {
        'localhost', '127.0.0.1', '0.0.0.0',
        '10.', '172.', '192.168.'  # 내부 네트워크
    }
    
    @classmethod
    def validate_url(cls, url: str) -> Tuple[bool, str]:
        """URL 검증"""
        try:
            parsed = urlparse(url)
            
            # 스키마 검증
            if parsed.scheme not in cls.ALLOWED_SCHEMES:
                return False, f"허용되지 않는 프로토콜입니다. 허용: {', '.join(cls.ALLOWED_SCHEMES)}"
            
            # 도메인 검증
            if not parsed.netloc:
                return False, "유효하지 않은 도메인입니다."
            
            # 내부 네트워크 접근 차단
            host = parsed.netloc.split(':')[0].lower()
            for blocked in cls.BLOCKED_DOMAINS:
                if host.startswith(blocked) or host == blocked:
                    return False, "내부 네트워크 주소는 허용되지 않습니다."
            
            return True, "유효한 URL입니다."
            
        except Exception as e:
            return False, f"URL 파싱 오류: {str(e)}"


class BusinessRuleValidator:
    """비즈니스 룰 검증 클래스"""
    
    @staticmethod
    def validate_feedback_request_limits(user_id: str, request_count: int, 
                                       time_window_hours: int = 24) -> Tuple[bool, str]:
        """피드백 요청 제한 검증"""
        # 시간당 요청 제한
        hourly_limit = 50
        daily_limit = 200
        
        if request_count > hourly_limit and time_window_hours == 1:
            return False, f"시간당 최대 {hourly_limit}개의 피드백 요청만 가능합니다."
        
        if request_count > daily_limit and time_window_hours == 24:
            return False, f"일일 최대 {daily_limit}개의 피드백 요청만 가능합니다."
        
        return True, "요청 제한 내에 있습니다."
    
    @staticmethod
    def validate_content_complexity(word_count: int, sentence_count: int) -> Tuple[bool, str]:
        """콘텐츠 복잡도 검증"""
        if word_count < 10:
            return False, "분석하기에는 내용이 너무 짧습니다. 최소 10단어 이상 필요합니다."
        
        if word_count > 5000:
            return False, "내용이 너무 깁니다. 최대 5000단어까지 처리 가능합니다."
        
        if sentence_count == 0:
            return False, "유효한 문장이 감지되지 않았습니다."
        
        # 평균 문장 길이 검증
        avg_words_per_sentence = word_count / sentence_count
        if avg_words_per_sentence > 100:
            return False, "문장이 너무 깁니다. 더 짧은 문장으로 나누어 주세요."
        
        return True, "적절한 복잡도입니다."
    
    @staticmethod
    def validate_batch_request_size(item_count: int, max_batch_size: int = 20) -> Tuple[bool, str]:
        """배치 요청 크기 검증"""
        if item_count == 0:
            return False, "배치 요청에 항목이 없습니다."
        
        if item_count > max_batch_size:
            return False, f"배치 요청은 최대 {max_batch_size}개까지 가능합니다."
        
        return True, "적절한 배치 크기입니다."


class SecurityValidator:
    """보안 검증 클래스"""
    
    @staticmethod
    def validate_request_rate(client_ip: str, endpoint: str, 
                            requests_per_minute: int, limit: int = 60) -> Tuple[bool, str]:
        """요청 속도 제한 검증"""
        if requests_per_minute > limit:
            return False, f"요청 속도 제한 초과: {requests_per_minute}/분 (제한: {limit}/분)"
        
        return True, "정상적인 요청 속도입니다."
    
    @staticmethod
    def validate_user_agent(user_agent: str) -> Tuple[bool, str]:
        """User-Agent 검증"""
        if not user_agent:
            return False, "User-Agent 헤더가 필요합니다."
        
        # 의심스러운 User-Agent 패턴
        suspicious_patterns = [
            r'bot', r'crawler', r'spider', r'scraper',
            r'curl', r'wget', r'python-requests'
        ]
        
        ua_lower = user_agent.lower()
        for pattern in suspicious_patterns:
            if re.search(pattern, ua_lower):
                return False, f"허용되지 않는 User-Agent입니다: {user_agent[:50]}"
        
        return True, "유효한 User-Agent입니다."
    
    @staticmethod
    def validate_request_origin(origin: str, allowed_origins: List[str]) -> Tuple[bool, str]:
        """요청 출처 검증"""
        if not origin:
            return True, "Origin 헤더가 없습니다."  # Origin이 없는 경우는 허용
        
        if origin in allowed_origins:
            return True, "허용된 출처입니다."
        
        # 와일드카드 패턴 확인
        for allowed in allowed_origins:
            if allowed == "*":
                return True, "모든 출처가 허용됩니다."
            
            if allowed.startswith("*."):
                domain = allowed[2:]
                if origin.endswith(domain):
                    return True, "허용된 도메인입니다."
        
        return False, f"허용되지 않는 출처입니다: {origin}"


def validate_comprehensive_request(request_data: Dict[str, Any], 
                                 user_context: Dict[str, Any] = None) -> Tuple[bool, List[str]]:
    """종합적인 요청 검증"""
    errors = []
    
    # 콘텐츠 검증
    if 'content' in request_data:
        is_valid, content_errors = ContentValidator.validate_text_content(
            request_data['content']
        )
        if not is_valid:
            errors.extend(content_errors)
    
    # 사용자 컨텍스트 기반 검증
    if user_context:
        user_id = user_context.get('user_id')
        request_count = user_context.get('request_count', 0)
        
        is_valid, limit_error = BusinessRuleValidator.validate_feedback_request_limits(
            user_id, request_count
        )
        if not is_valid:
            errors.append(limit_error)
    
    # 배치 요청 검증
    if 'items' in request_data:
        item_count = len(request_data['items'])
        is_valid, batch_error = BusinessRuleValidator.validate_batch_request_size(item_count)
        if not is_valid:
            errors.append(batch_error)
    
    return len(errors) == 0, errors


def create_validation_error_response(errors: List[str], field: str = None) -> ValidationError:
    """검증 에러 응답 생성"""
    return ValidationError(
        message="입력 데이터 검증에 실패했습니다.",
        details={
            "field": field,
            "errors": errors,
            "error_count": len(errors)
        }
    )