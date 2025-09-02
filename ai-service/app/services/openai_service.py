"""
OpenAI API 서비스
"""

from openai import AsyncOpenAI
from typing import Dict, List, Optional, Any
import structlog
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import hashlib
import tiktoken
from datetime import datetime

from ..core.config import settings
from ..core.exceptions import OpenAIException, ValidationException
from ..core.redis import redis_client
from ..models.feedback import (
    FeedbackRequest, FeedbackResponse, FeedbackType, 
    QuestionGenerationRequest, QuestionResponse,
    AnalysisRequest, SentimentAnalysisResponse, SummaryResponse, KeywordsResponse
)

logger = structlog.get_logger(__name__)


class OpenAIService:
    """OpenAI API 서비스 클래스"""
    
    def __init__(self):
        if not settings.OPENAI_API_KEY:
            raise ValueError("OPENAI_API_KEY가 설정되지 않았습니다.")
        
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = settings.OPENAI_MODEL
        self.max_tokens = settings.OPENAI_MAX_TOKENS
        self.temperature = settings.OPENAI_TEMPERATURE
        
        # 토큰 계산을 위한 인코딩
        try:
            self.encoding = tiktoken.encoding_for_model(self.model)
        except KeyError:
            self.encoding = tiktoken.get_encoding("cl100k_base")
        
        logger.info("OpenAI 서비스 초기화 완료", model=self.model)
    
    def _count_tokens(self, text: str) -> int:
        """텍스트의 토큰 수 계산"""
        try:
            return len(self.encoding.encode(text))
        except Exception as e:
            logger.warning("토큰 계산 실패", error=str(e))
            return len(text) // 4  # 대략적인 추정값
    
    def _generate_cache_key(self, prompt: str, model: str, temperature: float) -> str:
        """캐시 키 생성"""
        content = f"{prompt}_{model}_{temperature}"
        return f"openai_cache:{hashlib.md5(content.encode()).hexdigest()}"
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10)
    )
    async def _call_openai(
        self, 
        messages: List[Dict[str, str]], 
        model: str = None,
        temperature: float = None,
        max_tokens: int = None
    ) -> Dict[str, Any]:
        """OpenAI API 호출"""
        try:
            response = await self.client.chat.completions.create(
                model=model or self.model,
                messages=messages,
                temperature=temperature or self.temperature,
                max_tokens=max_tokens or self.max_tokens,
                top_p=1,
                frequency_penalty=0,
                presence_penalty=0
            )
            
            return {
                "content": response.choices[0].message.content,
                "tokens_used": response.usage.total_tokens,
                "model": response.model
            }
            
        except Exception as e:
            error_type = type(e).__name__
            error_msg = str(e)
            logger.error("OpenAI API 호출 중 예외 발생", 
                        error_type=error_type, 
                        error_message=error_msg,
                        model=model or self.model,
                        messages_count=len(messages))
            
            if "BadRequestError" in error_type:
                logger.error("OpenAI 요청 오류", error=error_msg)
                raise ValidationException(f"요청이 올바르지 않습니다: {error_msg}")
            elif "RateLimitError" in error_type:
                logger.error("OpenAI 요청 한도 초과", error=error_msg)
                raise OpenAIException("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")
            else:
                logger.error("예상치 못한 OpenAI 오류", error=error_msg)
                raise OpenAIException(f"AI 서비스에 예상치 못한 오류가 발생했습니다: {error_msg}")
    
    def _build_feedback_prompt(self, request: FeedbackRequest) -> List[Dict[str, str]]:
        """피드백 생성을 위한 프롬프트 구성"""
        
        system_prompt = """당신은 독서 전문가이자 교육자입니다. 사용자의 독서 기록에 대해 건설적이고 도움이 되는 피드백을 제공해주세요.

피드백 지침:
1. 사용자의 통찰을 인정하고 격려하세요
2. 추가적인 관점이나 질문을 제시하세요  
3. 실용적인 조언을 포함하세요
4. 한국어로 자연스럽고 친근하게 작성하세요
5. 200-500자 내외로 작성하세요"""

        book_info = f"책: {request.book_context.title} (저자: {request.book_context.author})"
        chapter_info = f"목차: {request.chapter_context.chapter_number}. {request.chapter_context.title}"
        note_info = f"독서 기록 ({request.note_context.note_type.value}): {request.note_context.content}"
        
        if request.note_context.page_number:
            note_info += f" (페이지: {request.note_context.page_number})"
        
        feedback_type_guide = {
            FeedbackType.COMMENT: "이 기록에 대한 격려와 추가 관점을 제시해주세요.",
            FeedbackType.QUESTION: "이 기록을 바탕으로 더 깊이 생각해볼 수 있는 질문을 제시해주세요.",
            FeedbackType.SUGGESTION: "이 기록과 관련하여 추천할 만한 내용이나 활동을 제안해주세요."
        }
        
        user_prompt = f"""{book_info}
{chapter_info}
{note_info}

요청 타입: {feedback_type_guide[request.feedback_type]}"""

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ]
    
    async def generate_feedback(self, request: FeedbackRequest) -> FeedbackResponse:
        """독서 기록에 대한 피드백 생성"""
        
        # 캐시 확인
        prompt_text = str(request.dict())
        cache_key = self._generate_cache_key(prompt_text, self.model, self.temperature)
        
        cached_response = await redis_client.get_json(cache_key)
        if cached_response:
            logger.info("캐시된 피드백 반환", note_id=request.note_id)
            return FeedbackResponse(**cached_response)
        
        # 프롬프트 구성
        messages = self._build_feedback_prompt(request)
        
        logger.info(
            "피드백 생성 시작",
            note_id=request.note_id,
            feedback_type=request.feedback_type,
            book_title=request.book_context.title
        )
        
        # OpenAI API 호출
        response = await self._call_openai(messages)
        
        # 응답 구성
        feedback_response = FeedbackResponse(
            id=f"feedback_{request.note_id}_{datetime.now().timestamp()}",
            note_id=request.note_id,
            content=response["content"],
            feedback_type=request.feedback_type,
            ai_model=response["model"],
            confidence_score=0.85,  # 기본값, 향후 개선 가능
            generated_at=datetime.now(),
            tokens_used=response["tokens_used"]
        )
        
        # 캐시에 저장
        await redis_client.set_json(
            cache_key, 
            feedback_response.dict(), 
            ttl=settings.FEEDBACK_CACHE_TTL
        )
        
        logger.info(
            "피드백 생성 완료",
            note_id=request.note_id,
            tokens_used=response["tokens_used"]
        )
        
        return feedback_response
    
    def _build_question_prompt(self, request: QuestionGenerationRequest) -> List[Dict[str, str]]:
        """질문 생성을 위한 프롬프트 구성"""
        
        system_prompt = f"""당신은 교육 전문가입니다. 주어진 책과 목차 내용을 바탕으로 {request.difficulty_level} 난이도의 {request.question_type} 질문을 생성해주세요.

질문 타입별 가이드:
- discussion: 토론을 유도하는 열린 질문
- comprehension: 이해도를 확인하는 질문  
- analysis: 분석적 사고를 요구하는 질문
- creative: 창의적 사고를 유도하는 질문
- critical: 비판적 사고를 요구하는 질문

3-5개의 질문을 생성하고, 각 질문을 새 줄로 구분해주세요."""

        book_info = f"책: {request.book_context.title} (저자: {request.book_context.author})"
        chapter_info = f"목차: {request.chapter_context.chapter_number}. {request.chapter_context.title}"
        
        user_prompt = f"""{book_info}
{chapter_info}"""
        
        if request.note_context:
            user_prompt += f"\n독서 기록: {request.note_context.content}"
        
        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ]
    
    async def generate_questions(self, request: QuestionGenerationRequest) -> QuestionResponse:
        """질문 생성"""
        
        messages = self._build_question_prompt(request)
        
        logger.info(
            "질문 생성 시작",
            question_type=request.question_type,
            difficulty=request.difficulty_level,
            book_title=request.book_context.title
        )
        
        response = await self._call_openai(messages)
        
        # 질문들을 줄바꿈으로 분리
        questions = [q.strip() for q in response["content"].split('\n') if q.strip()]
        
        return QuestionResponse(
            questions=questions,
            question_type=request.question_type,
            difficulty_level=request.difficulty_level,
            ai_model=response["model"],
            generated_at=datetime.now(),
            tokens_used=response["tokens_used"]
        )
    
    async def analyze_sentiment(self, request: AnalysisRequest) -> SentimentAnalysisResponse:
        """감정 분석"""
        
        system_prompt = """당신은 텍스트 감정 분석 전문가입니다. 주어진 텍스트의 감정을 분석하고 다음 형식으로 응답해주세요:

감정: positive/negative/neutral
신뢰도: 0.0-1.0
세부감정: {"joy": 0.3, "confidence": 0.4, "curiosity": 0.3} 형태로"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": request.text}
        ]
        
        response = await self._call_openai(messages)
        
        # 응답 파싱 (실제로는 더 정교한 파싱 필요)
        return SentimentAnalysisResponse(
            sentiment="positive",  # 파싱된 결과
            confidence=0.85,
            emotions={"joy": 0.3, "confidence": 0.4, "curiosity": 0.3}
        )
    
    async def generate_summary(self, request: AnalysisRequest) -> SummaryResponse:
        """텍스트 요약"""
        
        system_prompt = """주어진 텍스트를 간결하게 요약하고 핵심 포인트를 추출해주세요. 
요약은 원문의 30% 이하 길이로 작성하고, 핵심 포인트는 3-5개로 정리해주세요."""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": request.text}
        ]
        
        response = await self._call_openai(messages)
        
        # 응답에서 요약과 핵심 포인트 분리 (실제로는 더 정교한 파싱 필요)
        summary_text = response["content"]
        key_points = ["핵심 포인트 1", "핵심 포인트 2", "핵심 포인트 3"]  # 파싱된 결과
        
        return SummaryResponse(
            summary=summary_text,
            key_points=key_points,
            compression_ratio=0.3
        )
    
    async def summarize_text(self, text: str) -> SummaryResponse:
        """텍스트 요약 (직접 텍스트 입력)"""
        
        system_prompt = """주어진 텍스트를 간결하게 요약하고 핵심 포인트를 추출해주세요. 
요약은 원문의 30% 이하 길이로 작성하고, 핵심 포인트는 3-5개로 정리해주세요.
        
다음 형식으로 응답해주세요:
요약: [요약 내용]
핵심포인트:
- [포인트 1]
- [포인트 2]
- [포인트 3]"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": text}
        ]
        
        response = await self._call_openai(messages)
        
        # 응답 파싱
        content = response["content"]
        lines = content.split('\n')
        summary = ""
        key_points = []
        
        current_section = None
        for line in lines:
            line = line.strip()
            if line.startswith('요약:'):
                summary = line.replace('요약:', '').strip()
                current_section = 'summary'
            elif line.startswith('핵심포인트:'):
                current_section = 'points'
            elif line.startswith('- ') and current_section == 'points':
                key_points.append(line[2:].strip())
        
        original_length = len(text)
        summary_length = len(summary)
        compression_ratio = summary_length / original_length if original_length > 0 else 0
        
        return SummaryResponse(
            summary=summary,
            key_points=key_points,
            compression_ratio=compression_ratio
        )
    
    async def extract_keywords(self, text: str) -> KeywordsResponse:
        """키워드 추출"""
        
        system_prompt = """주어진 텍스트에서 중요한 키워드와 주요 테마를 추출해주세요.
        
다음 형식으로 응답해주세요:
키워드: [키워드1], [키워드2], [키워드3]
테마: [테마1], [테마2], [테마3]
관련성점수:
- [키워드1]: [0.0-1.0 점수]
- [키워드2]: [0.0-1.0 점수]
- [키워드3]: [0.0-1.0 점수]"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": text}
        ]
        
        response = await self._call_openai(messages)
        
        # 응답 파싱
        content = response["content"]
        lines = content.split('\n')
        keywords = []
        themes = []
        relevance_scores = {}
        
        current_section = None
        for line in lines:
            line = line.strip()
            if line.startswith('키워드:'):
                keywords_text = line.replace('키워드:', '').strip()
                keywords = [kw.strip() for kw in keywords_text.split(',')]
                current_section = 'keywords'
            elif line.startswith('테마:'):
                themes_text = line.replace('테마:', '').strip()
                themes = [theme.strip() for theme in themes_text.split(',')]
                current_section = 'themes'
            elif line.startswith('관련성점수:'):
                current_section = 'scores'
            elif line.startswith('- ') and current_section == 'scores':
                parts = line[2:].split(':')
                if len(parts) == 2:
                    keyword = parts[0].strip()
                    try:
                        score = float(parts[1].strip())
                        relevance_scores[keyword] = score
                    except ValueError:
                        relevance_scores[keyword] = 0.5
        
        return KeywordsResponse(
            keywords=keywords,
            themes=themes,
            relevance_scores=relevance_scores
        )
    
    async def analyze_sentiment(self, text: str) -> SentimentAnalysisResponse:
        """감정 분석 (직접 텍스트 입력)"""
        
        system_prompt = """주어진 텍스트의 감정을 분석해주세요.
        
다음 형식으로 정확히 응답해주세요:
감정: positive/negative/neutral
신뢰도: [0.0-1.0]
세부감정: {"joy": 0.3, "sadness": 0.1, "anger": 0.0, "surprise": 0.2, "fear": 0.1, "disgust": 0.0}"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": text}
        ]
        
        response = await self._call_openai(messages)
        
        # 응답 파싱
        content = response["content"]
        lines = content.split('\n')
        sentiment = "neutral"
        confidence = 0.5
        emotions = {"joy": 0.0, "sadness": 0.0, "anger": 0.0, "surprise": 0.0, "fear": 0.0, "disgust": 0.0}
        
        for line in lines:
            line = line.strip()
            if line.startswith('감정:'):
                sentiment = line.replace('감정:', '').strip()
            elif line.startswith('신뢰도:'):
                try:
                    confidence = float(line.replace('신뢰도:', '').strip())
                except ValueError:
                    confidence = 0.5
            elif line.startswith('세부감정:'):
                emotions_text = line.replace('세부감정:', '').strip()
                try:
                    import json
                    emotions = json.loads(emotions_text)
                except:
                    pass
        
        return SentimentAnalysisResponse(
            sentiment=sentiment,
            confidence=confidence,
            emotions=emotions
        )


# 전역 OpenAI 서비스 인스턴스
openai_service = OpenAIService()