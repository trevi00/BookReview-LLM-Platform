"""
API 응답 스키마 정의
일관된 응답 포맷과 상세한 문서화 제공
"""

from pydantic import BaseModel, Field, ConfigDict
from typing import Optional, List, Dict, Any, Union
from datetime import datetime
from enum import Enum

from .requests import FeedbackType, AnalysisType


class ResponseStatus(str, Enum):
    """응답 상태 열거형"""
    SUCCESS = "success"
    ERROR = "error"
    PARTIAL_SUCCESS = "partial_success"
    PROCESSING = "processing"


class BaseResponse(BaseModel):
    """기본 응답 스키마"""
    model_config = ConfigDict(
        use_enum_values=True,
        json_encoders={
            datetime: lambda v: v.isoformat()
        }
    )
    
    status: ResponseStatus = Field(
        ...,
        description="응답 상태"
    )
    
    message: str = Field(
        ...,
        description="응답 메시지",
        examples=["요청이 성공적으로 처리되었습니다."]
    )
    
    request_id: Optional[str] = Field(
        None,
        description="요청 ID"
    )
    
    timestamp: datetime = Field(
        default_factory=datetime.utcnow,
        description="응답 시간"
    )
    
    processing_time_ms: Optional[float] = Field(
        None,
        description="처리 시간 (밀리초)",
        ge=0
    )


class ErrorDetail(BaseModel):
    """에러 상세 정보"""
    
    code: str = Field(
        ...,
        description="에러 코드",
        examples=["VALIDATION_ERROR", "AI_SERVICE_ERROR"]
    )
    
    field: Optional[str] = Field(
        None,
        description="에러가 발생한 필드"
    )
    
    message: str = Field(
        ...,
        description="에러 메시지"
    )
    
    details: Optional[Dict[str, Any]] = Field(
        None,
        description="추가 에러 정보"
    )


class ErrorResponse(BaseResponse):
    """에러 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.ERROR
    
    error: ErrorDetail = Field(
        ...,
        description="에러 상세 정보"
    )
    
    suggestion: Optional[str] = Field(
        None,
        description="해결 방법 제안"
    )


class FeedbackItem(BaseModel):
    """피드백 항목"""
    
    category: str = Field(
        ...,
        description="피드백 카테고리",
        examples=["문법", "스타일", "내용", "구조"]
    )
    
    issue: str = Field(
        ...,
        description="발견된 문제",
        examples=["주어와 서술어의 불일치"]
    )
    
    suggestion: str = Field(
        ...,
        description="개선 제안",
        examples=["'그는 간다'를 '그는 간다'로 수정하세요."]
    )
    
    severity: str = Field(
        ...,
        description="심각도",
        examples=["high", "medium", "low"]
    )
    
    position: Optional[Dict[str, int]] = Field(
        None,
        description="텍스트 내 위치 정보",
        examples=[{"start": 10, "end": 20}]
    )
    
    confidence: Optional[float] = Field(
        None,
        description="신뢰도 (0.0 ~ 1.0)",
        ge=0.0,
        le=1.0
    )


class WritingMetrics(BaseModel):
    """글쓰기 지표"""
    
    word_count: int = Field(
        ...,
        description="단어 수",
        ge=0
    )
    
    sentence_count: int = Field(
        ...,
        description="문장 수",
        ge=0
    )
    
    paragraph_count: int = Field(
        ...,
        description="문단 수",
        ge=0
    )
    
    avg_sentence_length: float = Field(
        ...,
        description="평균 문장 길이",
        ge=0
    )
    
    readability_score: Optional[float] = Field(
        None,
        description="가독성 점수 (0 ~ 100)",
        ge=0,
        le=100
    )
    
    complexity_level: Optional[str] = Field(
        None,
        description="복잡도 수준",
        examples=["초급", "중급", "고급"]
    )


class FeedbackResponse(BaseResponse):
    """피드백 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    data: Dict[str, Any] = Field(
        ...,
        description="피드백 데이터"
    )
    
    feedback_type: FeedbackType = Field(
        ...,
        description="피드백 타입"
    )
    
    feedback_items: List[FeedbackItem] = Field(
        default_factory=list,
        description="피드백 항목 목록"
    )
    
    writing_metrics: Optional[WritingMetrics] = Field(
        None,
        description="글쓰기 지표"
    )
    
    overall_score: Optional[float] = Field(
        None,
        description="전체 점수 (0 ~ 100)",
        ge=0,
        le=100
    )
    
    improvement_summary: Optional[str] = Field(
        None,
        description="개선사항 요약"
    )
    
    ai_model_used: Optional[str] = Field(
        None,
        description="사용된 AI 모델"
    )
    
    tokens_used: Optional[int] = Field(
        None,
        description="사용된 토큰 수",
        ge=0
    )


class BatchFeedbackResponse(BaseResponse):
    """배치 피드백 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    results: List[Union[FeedbackResponse, ErrorResponse]] = Field(
        ...,
        description="개별 피드백 결과 목록"
    )
    
    success_count: int = Field(
        ...,
        description="성공한 요청 수",
        ge=0
    )
    
    error_count: int = Field(
        ...,
        description="실패한 요청 수",
        ge=0
    )
    
    total_tokens_used: Optional[int] = Field(
        None,
        description="총 사용된 토큰 수",
        ge=0
    )


class AnalysisResult(BaseModel):
    """분석 결과"""
    
    analysis_type: AnalysisType = Field(
        ...,
        description="분석 타입"
    )
    
    result: Dict[str, Any] = Field(
        ...,
        description="분석 결과 데이터"
    )
    
    confidence: Optional[float] = Field(
        None,
        description="분석 신뢰도",
        ge=0.0,
        le=1.0
    )
    
    metadata: Optional[Dict[str, Any]] = Field(
        None,
        description="분석 메타데이터"
    )


class AnalysisResponse(BaseResponse):
    """분석 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    analysis_results: List[AnalysisResult] = Field(
        ...,
        description="분석 결과 목록"
    )
    
    summary: Optional[str] = Field(
        None,
        description="분석 요약"
    )


class ComparisonDifference(BaseModel):
    """텍스트 비교 차이점"""
    
    type: str = Field(
        ...,
        description="변경 타입",
        examples=["addition", "deletion", "modification"]
    )
    
    original: Optional[str] = Field(
        None,
        description="원본 텍스트"
    )
    
    revised: Optional[str] = Field(
        None,
        description="수정된 텍스트"
    )
    
    position: Dict[str, int] = Field(
        ...,
        description="위치 정보",
        examples=[{"start": 10, "end": 20}]
    )
    
    category: Optional[str] = Field(
        None,
        description="변경 카테고리",
        examples=["grammar", "style", "content"]
    )


class ComparisonResponse(BaseResponse):
    """비교 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    differences: List[ComparisonDifference] = Field(
        ...,
        description="차이점 목록"
    )
    
    similarity_score: Optional[float] = Field(
        None,
        description="유사도 점수 (0 ~ 1)",
        ge=0.0,
        le=1.0
    )
    
    improvement_assessment: Optional[str] = Field(
        None,
        description="개선도 평가"
    )
    
    change_summary: Dict[str, int] = Field(
        default_factory=dict,
        description="변경사항 요약",
        examples=[{"additions": 5, "deletions": 2, "modifications": 3}]
    )


class TranslationResponse(BaseResponse):
    """번역 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    translated_text: str = Field(
        ...,
        description="번역된 텍스트"
    )
    
    source_language: str = Field(
        ...,
        description="감지된 원본 언어"
    )
    
    target_language: str = Field(
        ...,
        description="대상 언어"
    )
    
    confidence: Optional[float] = Field(
        None,
        description="번역 신뢰도",
        ge=0.0,
        le=1.0
    )


class SummaryPoint(BaseModel):
    """요약 포인트"""
    
    content: str = Field(
        ...,
        description="요약 내용"
    )
    
    importance: Optional[float] = Field(
        None,
        description="중요도 (0 ~ 1)",
        ge=0.0,
        le=1.0
    )
    
    source_position: Optional[Dict[str, int]] = Field(
        None,
        description="원본 텍스트에서의 위치"
    )


class SummarizationResponse(BaseResponse):
    """요약 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    summary: str = Field(
        ...,
        description="요약 텍스트"
    )
    
    key_points: Optional[List[SummaryPoint]] = Field(
        None,
        description="주요 포인트 목록"
    )
    
    summary_length: str = Field(
        ...,
        description="요약 길이 수준"
    )
    
    compression_ratio: Optional[float] = Field(
        None,
        description="압축 비율",
        ge=0.0,
        le=1.0
    )
    
    original_word_count: int = Field(
        ...,
        description="원본 단어 수",
        ge=0
    )
    
    summary_word_count: int = Field(
        ...,
        description="요약 단어 수",
        ge=0
    )


class HealthStatus(BaseModel):
    """헬스 상태"""
    
    component: str = Field(
        ...,
        description="컴포넌트 이름",
        examples=["redis", "openai", "database"]
    )
    
    status: str = Field(
        ...,
        description="상태",
        examples=["healthy", "unhealthy", "degraded"]
    )
    
    response_time_ms: Optional[float] = Field(
        None,
        description="응답 시간 (밀리초)",
        ge=0
    )
    
    last_check: datetime = Field(
        default_factory=datetime.utcnow,
        description="마지막 체크 시간"
    )
    
    details: Optional[Dict[str, Any]] = Field(
        None,
        description="상세 정보"
    )


class HealthCheckResponse(BaseResponse):
    """헬스체크 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    overall_status: str = Field(
        ...,
        description="전체 상태",
        examples=["healthy", "unhealthy", "degraded"]
    )
    
    components: List[HealthStatus] = Field(
        ...,
        description="컴포넌트별 상태"
    )
    
    uptime_seconds: Optional[float] = Field(
        None,
        description="가동 시간 (초)",
        ge=0
    )
    
    version: Optional[str] = Field(
        None,
        description="서비스 버전"
    )
    
    environment: Optional[str] = Field(
        None,
        description="실행 환경"
    )


class MetricsResponse(BaseResponse):
    """메트릭 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    metrics: Dict[str, Any] = Field(
        ...,
        description="메트릭 데이터"
    )
    
    collection_time: datetime = Field(
        default_factory=datetime.utcnow,
        description="메트릭 수집 시간"
    )


class PersonalizationResponse(BaseResponse):
    """개인화 설정 응답 스키마"""
    
    status: ResponseStatus = ResponseStatus.SUCCESS
    
    user_profile: Dict[str, Any] = Field(
        ...,
        description="사용자 프로필 설정"
    )
    
    recommendations: Optional[List[str]] = Field(
        None,
        description="개인화 추천사항"
    )
    
    updated_at: datetime = Field(
        default_factory=datetime.utcnow,
        description="설정 업데이트 시간"
    )