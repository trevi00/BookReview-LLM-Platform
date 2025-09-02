"""
AI 서비스 분석 및 모니터링
"""

import asyncio
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
from collections import defaultdict
import structlog
from ..core.redis import redis_client
from ..core.config import settings

logger = structlog.get_logger(__name__)


class AnalyticsService:
    """AI 서비스 분석 및 모니터링 서비스"""
    
    def __init__(self):
        self.metrics_ttl = 86400 * 30  # 30일
        self.realtime_ttl = 3600  # 1시간
    
    async def track_api_call(self, endpoint: str, user_id: str, response_time: float, 
                           tokens_used: int, success: bool, model: str = None):
        """API 호출 추적"""
        try:
            timestamp = datetime.now()
            
            # 메트릭 데이터 구성
            metrics_data = {
                "endpoint": endpoint,
                "user_id": user_id,
                "response_time": response_time,
                "tokens_used": tokens_used,
                "success": success,
                "model": model,
                "timestamp": timestamp.isoformat()
            }
            
            # 여러 키에 동시 저장
            await asyncio.gather(
                self._update_endpoint_metrics(endpoint, metrics_data),
                self._update_user_metrics(user_id, metrics_data),
                self._update_model_metrics(model, metrics_data) if model else asyncio.sleep(0),
                self._update_realtime_metrics(metrics_data),
                self._update_daily_metrics(timestamp.date(), metrics_data)
            )
            
        except Exception as e:
            logger.error("API 호출 추적 실패", error=str(e))
    
    async def _update_endpoint_metrics(self, endpoint: str, metrics_data: Dict[str, Any]):
        """엔드포인트별 메트릭 업데이트"""
        key = f"metrics:endpoint:{endpoint}"
        current_metrics = await redis_client.get_json(key) or self._get_empty_metrics()
        
        # 메트릭 업데이트
        current_metrics["total_calls"] += 1
        if metrics_data["success"]:
            current_metrics["successful_calls"] += 1
        else:
            current_metrics["failed_calls"] += 1
        
        current_metrics["total_tokens"] += metrics_data["tokens_used"]
        current_metrics["total_response_time"] += metrics_data["response_time"]
        
        # 평균 계산
        current_metrics["avg_response_time"] = (
            current_metrics["total_response_time"] / current_metrics["total_calls"]
        )
        current_metrics["avg_tokens_per_call"] = (
            current_metrics["total_tokens"] / current_metrics["total_calls"]
        )
        current_metrics["success_rate"] = (
            current_metrics["successful_calls"] / current_metrics["total_calls"] * 100
        )
        
        current_metrics["last_updated"] = datetime.now().isoformat()
        
        await redis_client.set_json(key, current_metrics, ttl=self.metrics_ttl)
    
    async def _update_user_metrics(self, user_id: str, metrics_data: Dict[str, Any]):
        """사용자별 메트릭 업데이트"""
        key = f"metrics:user:{user_id}"
        current_metrics = await redis_client.get_json(key) or self._get_empty_metrics()
        
        # 기본 메트릭 업데이트
        current_metrics["total_calls"] += 1
        current_metrics["total_tokens"] += metrics_data["tokens_used"]
        
        # 엔드포인트별 사용량
        endpoint_usage = current_metrics.get("endpoint_usage", {})
        endpoint = metrics_data["endpoint"]
        endpoint_usage[endpoint] = endpoint_usage.get(endpoint, 0) + 1
        current_metrics["endpoint_usage"] = endpoint_usage
        
        current_metrics["last_activity"] = datetime.now().isoformat()
        
        await redis_client.set_json(key, current_metrics, ttl=self.metrics_ttl)
    
    async def _update_model_metrics(self, model: str, metrics_data: Dict[str, Any]):
        """모델별 메트릭 업데이트"""
        if not model:
            return
            
        key = f"metrics:model:{model}"
        current_metrics = await redis_client.get_json(key) or self._get_empty_metrics()
        
        current_metrics["total_calls"] += 1
        current_metrics["total_tokens"] += metrics_data["tokens_used"]
        current_metrics["total_response_time"] += metrics_data["response_time"]
        
        if metrics_data["success"]:
            current_metrics["successful_calls"] += 1
        else:
            current_metrics["failed_calls"] += 1
        
        await redis_client.set_json(key, current_metrics, ttl=self.metrics_ttl)
    
    async def _update_realtime_metrics(self, metrics_data: Dict[str, Any]):
        """실시간 메트릭 업데이트"""
        minute_key = f"realtime:minute:{datetime.now().strftime('%Y%m%d%H%M')}"
        hour_key = f"realtime:hour:{datetime.now().strftime('%Y%m%d%H')}"
        
        # 분단위 메트릭
        await redis_client.incr(f"{minute_key}:calls")
        await redis_client.incr(f"{minute_key}:tokens", metrics_data["tokens_used"])
        await redis_client.expire(minute_key + ":calls", self.realtime_ttl)
        await redis_client.expire(minute_key + ":tokens", self.realtime_ttl)
        
        # 시간단위 메트릭
        await redis_client.incr(f"{hour_key}:calls")
        await redis_client.incr(f"{hour_key}:tokens", metrics_data["tokens_used"])
        await redis_client.expire(hour_key + ":calls", self.realtime_ttl * 24)
        await redis_client.expire(hour_key + ":tokens", self.realtime_ttl * 24)
    
    async def _update_daily_metrics(self, date, metrics_data: Dict[str, Any]):
        """일별 메트릭 업데이트"""
        date_str = date.strftime('%Y%m%d')
        key = f"metrics:daily:{date_str}"
        
        current_metrics = await redis_client.get_json(key) or {
            "date": date_str,
            "total_calls": 0,
            "total_tokens": 0,
            "successful_calls": 0,
            "failed_calls": 0,
            "unique_users": set(),
            "endpoints": defaultdict(int)
        }
        
        current_metrics["total_calls"] += 1
        current_metrics["total_tokens"] += metrics_data["tokens_used"]
        
        if metrics_data["success"]:
            current_metrics["successful_calls"] += 1
        else:
            current_metrics["failed_calls"] += 1
        
        # 고유 사용자 추적 (JSON 직렬화를 위해 리스트로 변환)
        unique_users = set(current_metrics.get("unique_users", []))
        unique_users.add(metrics_data["user_id"])
        current_metrics["unique_users"] = list(unique_users)
        
        # 엔드포인트별 사용량
        endpoints = current_metrics.get("endpoints", {})
        endpoints[metrics_data["endpoint"]] = endpoints.get(metrics_data["endpoint"], 0) + 1
        current_metrics["endpoints"] = endpoints
        
        await redis_client.set_json(key, current_metrics, ttl=self.metrics_ttl)
    
    def _get_empty_metrics(self) -> Dict[str, Any]:
        """빈 메트릭 구조 반환"""
        return {
            "total_calls": 0,
            "successful_calls": 0,
            "failed_calls": 0,
            "total_tokens": 0,
            "total_response_time": 0.0,
            "avg_response_time": 0.0,
            "avg_tokens_per_call": 0.0,
            "success_rate": 0.0,
            "created_at": datetime.now().isoformat()
        }
    
    async def get_endpoint_analytics(self, endpoint: str) -> Dict[str, Any]:
        """엔드포인트 분석 데이터 조회"""
        try:
            key = f"metrics:endpoint:{endpoint}"
            metrics = await redis_client.get_json(key)
            
            if not metrics:
                return {"endpoint": endpoint, "message": "데이터 없음"}
            
            return {
                "endpoint": endpoint,
                "total_calls": metrics.get("total_calls", 0),
                "success_rate": round(metrics.get("success_rate", 0), 2),
                "avg_response_time": round(metrics.get("avg_response_time", 0), 3),
                "avg_tokens_per_call": round(metrics.get("avg_tokens_per_call", 0), 1),
                "total_tokens": metrics.get("total_tokens", 0),
                "last_updated": metrics.get("last_updated")
            }
            
        except Exception as e:
            logger.error("엔드포인트 분석 조회 실패", endpoint=endpoint, error=str(e))
            return {}
    
    async def get_user_analytics(self, user_id: str) -> Dict[str, Any]:
        """사용자 분석 데이터 조회"""
        try:
            key = f"metrics:user:{user_id}"
            metrics = await redis_client.get_json(key)
            
            if not metrics:
                return {"user_id": user_id, "message": "데이터 없음"}
            
            return {
                "user_id": user_id,
                "total_calls": metrics.get("total_calls", 0),
                "total_tokens": metrics.get("total_tokens", 0),
                "endpoint_usage": metrics.get("endpoint_usage", {}),
                "last_activity": metrics.get("last_activity"),
                "avg_tokens_per_call": round(
                    metrics.get("total_tokens", 0) / max(metrics.get("total_calls", 1), 1), 1
                )
            }
            
        except Exception as e:
            logger.error("사용자 분석 조회 실패", user_id=user_id, error=str(e))
            return {}
    
    async def get_daily_analytics(self, days: int = 7) -> List[Dict[str, Any]]:
        """일별 분석 데이터 조회"""
        try:
            daily_data = []
            
            for i in range(days):
                date = (datetime.now().date() - timedelta(days=i))
                date_str = date.strftime('%Y%m%d')
                key = f"metrics:daily:{date_str}"
                
                metrics = await redis_client.get_json(key) or {}
                
                daily_data.append({
                    "date": date_str,
                    "total_calls": metrics.get("total_calls", 0),
                    "total_tokens": metrics.get("total_tokens", 0),
                    "unique_users": len(metrics.get("unique_users", [])),
                    "success_rate": round(
                        metrics.get("successful_calls", 0) / 
                        max(metrics.get("total_calls", 1), 1) * 100, 2
                    ),
                    "top_endpoints": dict(
                        sorted(
                            metrics.get("endpoints", {}).items(), 
                            key=lambda x: x[1], 
                            reverse=True
                        )[:5]
                    )
                })
            
            return sorted(daily_data, key=lambda x: x["date"])
            
        except Exception as e:
            logger.error("일별 분석 조회 실패", error=str(e))
            return []
    
    async def get_realtime_metrics(self) -> Dict[str, Any]:
        """실시간 메트릭 조회"""
        try:
            now = datetime.now()
            
            # 현재 분과 시간
            current_minute = now.strftime('%Y%m%d%H%M')
            current_hour = now.strftime('%Y%m%d%H')
            
            # 현재 분 메트릭
            minute_calls = await redis_client.get(f"realtime:minute:{current_minute}:calls") or 0
            minute_tokens = await redis_client.get(f"realtime:minute:{current_minute}:tokens") or 0
            
            # 현재 시간 메트릭
            hour_calls = await redis_client.get(f"realtime:hour:{current_hour}:calls") or 0
            hour_tokens = await redis_client.get(f"realtime:hour:{current_hour}:tokens") or 0
            
            return {
                "current_minute": {
                    "calls": int(minute_calls),
                    "tokens": int(minute_tokens)
                },
                "current_hour": {
                    "calls": int(hour_calls),
                    "tokens": int(hour_tokens)
                },
                "timestamp": now.isoformat()
            }
            
        except Exception as e:
            logger.error("실시간 메트릭 조회 실패", error=str(e))
            return {}
    
    async def get_system_health(self) -> Dict[str, Any]:
        """시스템 건강도 조회"""
        try:
            # 최근 24시간 데이터
            yesterday = (datetime.now() - timedelta(days=1)).date()
            today = datetime.now().date()
            
            yesterday_key = f"metrics:daily:{yesterday.strftime('%Y%m%d')}"
            today_key = f"metrics:daily:{today.strftime('%Y%m%d')}"
            
            yesterday_metrics = await redis_client.get_json(yesterday_key) or {}
            today_metrics = await redis_client.get_json(today_key) or {}
            
            # 성공률 계산
            yesterday_success_rate = (
                yesterday_metrics.get("successful_calls", 0) / 
                max(yesterday_metrics.get("total_calls", 1), 1) * 100
            )
            
            today_success_rate = (
                today_metrics.get("successful_calls", 0) / 
                max(today_metrics.get("total_calls", 1), 1) * 1
            )
            
            # 실시간 메트릭
            realtime = await self.get_realtime_metrics()
            
            return {
                "status": "healthy" if today_success_rate >= 95 else "degraded",
                "success_rate_24h": round(today_success_rate, 2),
                "success_rate_trend": round(today_success_rate - yesterday_success_rate, 2),
                "current_load": {
                    "calls_per_minute": realtime["current_minute"]["calls"],
                    "tokens_per_minute": realtime["current_minute"]["tokens"]
                },
                "daily_comparison": {
                    "yesterday": {
                        "calls": yesterday_metrics.get("total_calls", 0),
                        "success_rate": round(yesterday_success_rate, 2)
                    },
                    "today": {
                        "calls": today_metrics.get("total_calls", 0),
                        "success_rate": round(today_success_rate, 2)
                    }
                },
                "timestamp": datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error("시스템 건강도 조회 실패", error=str(e))
            return {"status": "unknown", "error": str(e)}


# 전역 분석 서비스 인스턴스
analytics_service = AnalyticsService()