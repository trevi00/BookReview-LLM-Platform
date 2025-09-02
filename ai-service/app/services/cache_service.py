"""
AI 서비스용 캐싱 시스템
"""

import json
import hashlib
from typing import Any, Dict, Optional, List
from datetime import datetime, timedelta
import structlog
from ..core.redis import redis_client
from ..core.config import settings

logger = structlog.get_logger(__name__)


class CacheService:
    """AI 응답 캐싱 서비스"""
    
    def __init__(self):
        self.default_ttl = 3600 * 24  # 24시간
        self.feedback_ttl = 3600 * 24 * 7  # 7일
        self.analysis_ttl = 3600 * 6  # 6시간
        
    def _generate_cache_key(self, service_type: str, content: str, **kwargs) -> str:
        """캐시 키 생성"""
        # 캐시 키에 포함할 모든 매개변수를 정렬하여 일관성 보장
        cache_data = {
            "service_type": service_type,
            "content": content,
            **kwargs
        }
        
        # JSON으로 직렬화하여 해시 생성
        cache_string = json.dumps(cache_data, sort_keys=True, ensure_ascii=False)
        cache_hash = hashlib.sha256(cache_string.encode('utf-8')).hexdigest()
        
        return f"ai_cache:{service_type}:{cache_hash[:16]}"
    
    async def get_cached_response(self, service_type: str, content: str, **kwargs) -> Optional[Dict[str, Any]]:
        """캐시된 응답 조회"""
        try:
            cache_key = self._generate_cache_key(service_type, content, **kwargs)
            cached_data = await redis_client.get_json(cache_key)
            
            if cached_data:
                # 캐시 히트 로깅
                logger.info(
                    "캐시 히트",
                    service_type=service_type,
                    cache_key=cache_key,
                    content_length=len(content)
                )
                
                # 캐시 통계 업데이트
                await self._update_cache_stats("hit", service_type)
                
                return cached_data
            
            # 캐시 미스
            await self._update_cache_stats("miss", service_type)
            return None
            
        except Exception as e:
            logger.error("캐시 조회 실패", service_type=service_type, error=str(e))
            return None
    
    async def cache_response(self, service_type: str, content: str, response: Dict[str, Any], 
                           custom_ttl: Optional[int] = None, **kwargs):
        """응답 캐시 저장"""
        try:
            cache_key = self._generate_cache_key(service_type, content, **kwargs)
            
            # TTL 결정
            ttl = custom_ttl or self._get_ttl_for_service(service_type)
            
            # 메타데이터 추가
            cache_data = {
                **response,
                "cached_at": datetime.now().isoformat(),
                "service_type": service_type,
                "ttl": ttl
            }
            
            await redis_client.set_json(cache_key, cache_data, ttl=ttl)
            
            logger.info(
                "응답 캐시 저장",
                service_type=service_type,
                cache_key=cache_key,
                ttl=ttl,
                content_length=len(content)
            )
            
        except Exception as e:
            logger.error("캐시 저장 실패", service_type=service_type, error=str(e))
    
    def _get_ttl_for_service(self, service_type: str) -> int:
        """서비스 타입별 TTL 반환"""
        ttl_map = {
            "feedback": self.feedback_ttl,
            "sentiment": self.analysis_ttl,
            "summary": self.analysis_ttl,
            "keywords": self.analysis_ttl,
            "questions": self.feedback_ttl
        }
        
        return ttl_map.get(service_type, self.default_ttl)
    
    async def _update_cache_stats(self, stat_type: str, service_type: str):
        """캐시 통계 업데이트"""
        try:
            stats_key = f"cache_stats:{service_type}"
            current_stats = await redis_client.get_json(stats_key) or {}
            
            current_stats[stat_type] = current_stats.get(stat_type, 0) + 1
            current_stats["last_updated"] = datetime.now().isoformat()
            
            await redis_client.set_json(stats_key, current_stats, ttl=86400 * 30)  # 30일
            
        except Exception as e:
            logger.error("캐시 통계 업데이트 실패", error=str(e))
    
    async def get_cache_stats(self, service_type: str) -> Dict[str, Any]:
        """캐시 통계 조회"""
        try:
            stats_key = f"cache_stats:{service_type}"
            stats = await redis_client.get_json(stats_key) or {}
            
            hits = stats.get("hit", 0)
            misses = stats.get("miss", 0)
            total = hits + misses
            
            hit_rate = (hits / total * 100) if total > 0 else 0.0
            
            return {
                "service_type": service_type,
                "hits": hits,
                "misses": misses,
                "total_requests": total,
                "hit_rate_percentage": round(hit_rate, 2),
                "last_updated": stats.get("last_updated")
            }
            
        except Exception as e:
            logger.error("캐시 통계 조회 실패", service_type=service_type, error=str(e))
            return {}
    
    async def invalidate_cache(self, service_type: str, pattern: str = None):
        """캐시 무효화"""
        try:
            if pattern:
                # 패턴 기반 삭제
                search_pattern = f"ai_cache:{service_type}:*{pattern}*"
            else:
                # 서비스 타입 전체 삭제
                search_pattern = f"ai_cache:{service_type}:*"
            
            deleted_count = await redis_client.delete_pattern(search_pattern)
            
            logger.info(
                "캐시 무효화 완료",
                service_type=service_type,
                pattern=pattern,
                deleted_count=deleted_count
            )
            
            return deleted_count
            
        except Exception as e:
            logger.error("캐시 무효화 실패", service_type=service_type, error=str(e))
            return 0
    
    async def get_cache_info(self) -> Dict[str, Any]:
        """전체 캐시 정보 조회"""
        try:
            service_types = ["feedback", "sentiment", "summary", "keywords", "questions"]
            cache_info = {}
            
            for service_type in service_types:
                cache_info[service_type] = await self.get_cache_stats(service_type)
            
            # 전체 통계 계산
            total_hits = sum(info.get("hits", 0) for info in cache_info.values())
            total_misses = sum(info.get("misses", 0) for info in cache_info.values())
            total_requests = total_hits + total_misses
            
            overall_hit_rate = (total_hits / total_requests * 100) if total_requests > 0 else 0.0
            
            return {
                "overall": {
                    "total_hits": total_hits,
                    "total_misses": total_misses,
                    "total_requests": total_requests,
                    "hit_rate_percentage": round(overall_hit_rate, 2)
                },
                "by_service": cache_info
            }
            
        except Exception as e:
            logger.error("전체 캐시 정보 조회 실패", error=str(e))
            return {}


class SmartCacheService(CacheService):
    """스마트 캐싱 서비스 - 콘텐츠 유사도 기반"""
    
    def __init__(self):
        super().__init__()
        self.similarity_threshold = 0.85  # 유사도 임계값
    
    async def get_similar_cached_response(self, service_type: str, content: str, 
                                        **kwargs) -> Optional[Dict[str, Any]]:
        """유사한 콘텐츠의 캐시된 응답 조회"""
        try:
            # 먼저 정확한 매치 시도
            exact_match = await self.get_cached_response(service_type, content, **kwargs)
            if exact_match:
                return exact_match
            
            # 유사한 콘텐츠 검색
            similar_responses = await self._find_similar_cache_entries(service_type, content)
            
            if similar_responses:
                best_match = similar_responses[0]  # 가장 유사한 것 선택
                
                logger.info(
                    "유사 캐시 히트",
                    service_type=service_type,
                    similarity_score=best_match.get("similarity_score"),
                    original_length=len(content),
                    cached_length=len(best_match.get("original_content", ""))
                )
                
                return best_match["response"]
            
            return None
            
        except Exception as e:
            logger.error("유사 캐시 조회 실패", service_type=service_type, error=str(e))
            return None
    
    async def _find_similar_cache_entries(self, service_type: str, content: str) -> List[Dict[str, Any]]:
        """유사한 캐시 엔트리 검색"""
        try:
            # 간단한 구현 - 실제로는 더 정교한 유사도 계산 필요
            # 여기서는 기본 캐시 서비스만 사용
            return []
            
        except Exception as e:
            logger.error("유사 캐시 엔트리 검색 실패", error=str(e))
            return []


# 전역 캐시 서비스 인스턴스
cache_service = CacheService()
smart_cache_service = SmartCacheService()