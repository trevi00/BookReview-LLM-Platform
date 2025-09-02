"""
Redis 클라이언트 설정
"""

import redis.asyncio as redis
from typing import Optional, Any
import json
import pickle
from .config import settings
from .logging import get_logger

logger = get_logger(__name__)


class RedisClient:
    """비동기 Redis 클라이언트 래퍼"""
    
    def __init__(self):
        self.client: Optional[redis.Redis] = None
        self._connect()
    
    def _connect(self):
        """Redis 연결 설정"""
        try:
            self.client = redis.Redis(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                password=settings.REDIS_PASSWORD if settings.REDIS_PASSWORD else None,
                db=settings.REDIS_DB,
                decode_responses=False,  # 바이너리 데이터 지원
                socket_timeout=5,
                socket_connect_timeout=5,
                retry_on_timeout=True
            )
            logger.info("Redis 클라이언트 생성됨")
        except Exception as e:
            logger.error("Redis 연결 실패", error=str(e))
            raise
    
    async def ping(self) -> bool:
        """Redis 연결 확인"""
        try:
            await self.client.ping()
            return True
        except Exception as e:
            logger.error("Redis ping 실패", error=str(e))
            return False
    
    async def set_json(self, key: str, value: Any, ttl: Optional[int] = None) -> bool:
        """JSON 데이터를 Redis에 저장"""
        try:
            json_data = json.dumps(value, ensure_ascii=False)
            if ttl:
                await self.client.setex(key, ttl, json_data)
            else:
                await self.client.set(key, json_data)
            return True
        except Exception as e:
            logger.error("Redis JSON 저장 실패", key=key, error=str(e))
            return False
    
    async def get_json(self, key: str) -> Optional[Any]:
        """Redis에서 JSON 데이터 조회"""
        try:
            data = await self.client.get(key)
            if data:
                return json.loads(data.decode('utf-8'))
            return None
        except Exception as e:
            logger.error("Redis JSON 조회 실패", key=key, error=str(e))
            return None
    
    async def set_pickle(self, key: str, value: Any, ttl: Optional[int] = None) -> bool:
        """Python 객체를 pickle로 직렬화하여 저장"""
        try:
            pickled_data = pickle.dumps(value)
            if ttl:
                await self.client.setex(key, ttl, pickled_data)
            else:
                await self.client.set(key, pickled_data)
            return True
        except Exception as e:
            logger.error("Redis pickle 저장 실패", key=key, error=str(e))
            return False
    
    async def get_pickle(self, key: str) -> Optional[Any]:
        """Redis에서 pickle 데이터 조회"""
        try:
            data = await self.client.get(key)
            if data:
                return pickle.loads(data)
            return None
        except Exception as e:
            logger.error("Redis pickle 조회 실패", key=key, error=str(e))
            return None
    
    async def delete(self, key: str) -> bool:
        """키 삭제"""
        try:
            result = await self.client.delete(key)
            return result > 0
        except Exception as e:
            logger.error("Redis 키 삭제 실패", key=key, error=str(e))
            return False
    
    async def exists(self, key: str) -> bool:
        """키 존재 여부 확인"""
        try:
            result = await self.client.exists(key)
            return result > 0
        except Exception as e:
            logger.error("Redis 키 존재 확인 실패", key=key, error=str(e))
            return False
    
    async def incr(self, key: str, amount: int = 1) -> Optional[int]:
        """카운터 증가"""
        try:
            result = await self.client.incrby(key, amount)
            return result
        except Exception as e:
            logger.error("Redis 카운터 증가 실패", key=key, error=str(e))
            return None
    
    async def expire(self, key: str, ttl: int) -> bool:
        """키에 TTL 설정"""
        try:
            result = await self.client.expire(key, ttl)
            return result
        except Exception as e:
            logger.error("Redis TTL 설정 실패", key=key, error=str(e))
            return False
    
    async def close(self):
        """연결 종료"""
        if self.client:
            await self.client.close()
            logger.info("Redis 연결 종료됨")


# 전역 Redis 클라이언트 인스턴스
redis_client = RedisClient()