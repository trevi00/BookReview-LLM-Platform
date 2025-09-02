"""
API 요청 스키마 정의
Pydantic 모델을 사용한 요청 데이터 검증
"""

from pydantic import BaseModel, Field, validator, ConfigDict
from typing import Optional, List, Dict, Any, Union
from enum import Enum
from datetime import datetime
import re


class FeedbackType(str, Enum):
    """피드백 타입 열거형"""
    GRAMMAR = "grammar"
    STYLE = "style"
    CONTENT = "content"
    STRUCTURE = "structure"
    COMPREHENSIVE = "comprehensive"


class AnalysisType(str, Enum):
    """분석 타입 열거형"""
    SENTIMENT = "sentiment"
    READABILITY = "readability"
    COMPLEXITY = "complexity"
    KEYWORDS = "keywords"
    SUMMARY = "summary"


class Priority(str, Enum):
    """우선순위 열거형"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    URGENT = "urgent"


class BaseRequest(BaseModel):
    """기본 요청 스키마"""
    model_config = ConfigDict(
        str_strip_whitespace=True,
        validate_assignment=True,
        use_enum_values=True,
        extra="forbid"
    )
    
    request_id: Optional[str] = Field(
        None,
        description="요청 ID (자동 생성됨)",
        min_length=1,
        max_length=100
    )
    
    user_id: Optional[str] = Field(
        None,
        description="사용자 ID",
        min_length=1,
        max_length=50
    )
    
    priority: Optional[Priority] = Field(
        Priority.MEDIUM,
        description="요청 우선순위"
    )


class FeedbackRequest(BaseRequest):
    """피드백 생성 요청 스키마"""
    
    content: str = Field(
        ...,
        description="분석할 텍스트 내용",
        min_length=10,
        max_length=10000,
        examples=["이 책은 정말 흥미로운 이야기를 담고 있습니다. 주인공의 성장 과정이 잘 그려져 있어요."]
    )
    
    feedback_type: FeedbackType = Field(
        FeedbackType.COMPREHENSIVE,
        description="피드백 타입"
    )
    
    book_title: Optional[str] = Field(
        None,
        description="책 제목",
        max_length=200,
        examples=["해리 포터와 마법사의 돌"]
    )
    
    book_author: Optional[str] = Field(
        None,
        description="책 저자",
        max_length=100,
        examples=["J.K. 롤링"]
    )
    
    book_genre: Optional[str] = Field(
        None,
        description="책 장르",
        max_length=50,
        examples=["판타지", "로맨스", "추리소설"]
    )
    
    target_audience: Optional[str] = Field(
        "general",
        description="대상 독자",
        max_length=50,
        examples=["초등학생", "중고등학생", "성인", "전문가"]
    )
    
    language: Optional[str] = Field(
        "ko",
        description="언어 코드 (ISO 639-1)",
        min_length=2,
        max_length=5,
        examples=["ko", "en", "ja"]
    )
    
    include_suggestions: bool = Field(
        True,
        description="개선 제안 포함 여부"
    )
    
    detail_level: Optional[str] = Field(
        "medium",
        description="피드백 상세 수준",
        examples=["brief", "medium", "detailed"]
    )

    @validator('content')
    def validate_content(cls, v):
        if not v or not v.strip():
            raise ValueError('내용이 비어있을 수 없습니다.')
        
        # HTML 태그 검사
        if re.search(r'<[^>]+>', v):
            raise ValueError('HTML 태그는 허용되지 않습니다.')
        
        # 과도한 반복 문자 검사
        if re.search(r'(.)\1{10,}', v):
            raise ValueError('과도한 반복 문자가 포함되어 있습니다.')
        
        return v
    
    @validator('language')
    def validate_language(cls, v):
        supported_languages = ['ko', 'en', 'ja', 'zh', 'es', 'fr', 'de']
        if v not in supported_languages:
            raise ValueError(f'지원되지 않는 언어입니다. 지원 언어: {", ".join(supported_languages)}')
        return v
    
    @validator('detail_level')
    def validate_detail_level(cls, v):
        if v not in ['brief', 'medium', 'detailed']:
            raise ValueError('detail_level은 brief, medium, detailed 중 하나여야 합니다.')
        return v


class BatchFeedbackRequest(BaseRequest):
    """배치 피드백 요청 스키마"""
    
    items: List[FeedbackRequest] = Field(
        ...,
        description="피드백 요청 목록",
        min_items=1,
        max_items=20
    )
    
    parallel_processing: bool = Field(
        True,
        description="병렬 처리 여부"
    )
    
    @validator('items')
    def validate_items(cls, v):
        if len(v) > 20:
            raise ValueError('배치 요청은 최대 20개까지 가능합니다.')
        return v


class AnalysisRequest(BaseRequest):
    """분석 요청 스키마"""
    
    content: str = Field(
        ...,
        description="분석할 텍스트 내용",
        min_length=10,
        max_length=10000
    )
    
    analysis_type: AnalysisType = Field(
        ...,
        description="분석 타입"
    )
    
    options: Optional[Dict[str, Any]] = Field(
        {},
        description="분석 옵션"
    )
    
    @validator('content')
    def validate_content(cls, v):
        if not v or not v.strip():
            raise ValueError('분석할 내용이 비어있을 수 없습니다.')
        return v


class ComparisonRequest(BaseRequest):
    """텍스트 비교 요청 스키마"""
    
    original_text: str = Field(
        ...,
        description="원본 텍스트",
        min_length=10,
        max_length=5000
    )
    
    revised_text: str = Field(
        ...,
        description="수정된 텍스트",
        min_length=10,
        max_length=5000
    )
    
    comparison_type: str = Field(
        "comprehensive",
        description="비교 타입",
        examples=["grammar", "style", "content", "comprehensive"]
    )
    
    highlight_changes: bool = Field(
        True,
        description="변경사항 강조 표시 여부"
    )


class TranslationRequest(BaseRequest):
    """번역 요청 스키마"""
    
    text: str = Field(
        ...,
        description="번역할 텍스트",
        min_length=1,
        max_length=5000
    )
    
    source_language: str = Field(
        "auto",
        description="원본 언어 (auto: 자동 감지)",
        min_length=2,
        max_length=5
    )
    
    target_language: str = Field(
        ...,
        description="대상 언어",
        min_length=2,
        max_length=5
    )
    
    preserve_formatting: bool = Field(
        True,
        description="포맷팅 보존 여부"
    )


class SummarizationRequest(BaseRequest):
    """요약 요청 스키마"""
    
    content: str = Field(
        ...,
        description="요약할 텍스트 내용",
        min_length=100,
        max_length=20000
    )
    
    summary_length: Optional[str] = Field(
        "medium",
        description="요약 길이",
        examples=["brief", "medium", "detailed"]
    )
    
    summary_type: Optional[str] = Field(
        "extractive",
        description="요약 타입",
        examples=["extractive", "abstractive", "bullet_points"]
    )
    
    key_points: Optional[int] = Field(
        None,
        description="주요 포인트 개수",
        ge=1,
        le=20
    )
    
    @validator('summary_length')
    def validate_summary_length(cls, v):
        if v not in ['brief', 'medium', 'detailed']:
            raise ValueError('summary_length는 brief, medium, detailed 중 하나여야 합니다.')
        return v
    
    @validator('summary_type')
    def validate_summary_type(cls, v):
        allowed_types = ['extractive', 'abstractive', 'bullet_points']
        if v not in allowed_types:
            raise ValueError(f'summary_type은 {", ".join(allowed_types)} 중 하나여야 합니다.')
        return v


class PersonalizationRequest(BaseRequest):
    """개인화 설정 요청 스키마"""
    
    writing_style_preference: Optional[str] = Field(
        None,
        description="선호하는 글쓰기 스타일",
        examples=["formal", "casual", "academic", "creative"]
    )
    
    complexity_level: Optional[str] = Field(
        None,
        description="복잡도 수준",
        examples=["beginner", "intermediate", "advanced", "expert"]
    )
    
    feedback_focus: Optional[List[str]] = Field(
        None,
        description="피드백 중점 영역",
        examples=[["grammar", "style"], ["content", "structure"]]
    )
    
    tone_preference: Optional[str] = Field(
        None,
        description="선호하는 톤",
        examples=["encouraging", "constructive", "direct", "gentle"]
    )
    
    @validator('writing_style_preference')
    def validate_writing_style(cls, v):
        if v and v not in ['formal', 'casual', 'academic', 'creative']:
            raise ValueError('유효하지 않은 글쓰기 스타일입니다.')
        return v
    
    @validator('complexity_level')
    def validate_complexity_level(cls, v):
        if v and v not in ['beginner', 'intermediate', 'advanced', 'expert']:
            raise ValueError('유효하지 않은 복잡도 수준입니다.')
        return v


class HealthCheckRequest(BaseModel):
    """헬스체크 요청 스키마"""
    
    component: Optional[str] = Field(
        None,
        description="특정 컴포넌트 체크",
        examples=["redis", "openai", "database", "memory"]
    )
    
    detailed: bool = Field(
        False,
        description="상세 정보 포함 여부"
    )