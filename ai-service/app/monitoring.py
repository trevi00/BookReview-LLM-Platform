"""
AI 서비스 모니터링 및 메트릭 수집 모듈
Prometheus 메트릭, 성능 모니터링, 헬스체크 기능 제공
"""

import time
import psutil
import asyncio
from typing import Dict, Any, Optional, List
from datetime import datetime, timedelta
from collections import defaultdict, deque
from contextlib import asynccontextmanager

from prometheus_client import Counter, Histogram, Gauge, Info, CollectorRegistry, generate_latest
from fastapi import Request, Response, BackgroundTasks
import structlog

from .core.config import get_current_settings
from .logging_config import PerformanceLogger, BusinessLogger

logger = structlog.get_logger(__name__)

# Prometheus 메트릭 정의
registry = CollectorRegistry()

# HTTP 요청 메트릭
http_requests_total = Counter(
    'http_requests_total',
    'Total HTTP requests',
    ['method', 'endpoint', 'status_code'],
    registry=registry
)

http_request_duration_seconds = Histogram(
    'http_request_duration_seconds',
    'HTTP request duration in seconds',
    ['method', 'endpoint'],
    registry=registry
)

# AI 서비스 메트릭
ai_requests_total = Counter(
    'ai_requests_total',
    'Total AI API requests',
    ['model', 'type'],
    registry=registry
)

ai_request_duration_seconds = Histogram(
    'ai_request_duration_seconds',
    'AI request duration in seconds',
    ['model', 'type'],
    registry=registry
)

ai_tokens_used_total = Counter(
    'ai_tokens_used_total',
    'Total AI tokens used',
    ['model'],
    registry=registry
)

ai_cost_total = Counter(
    'ai_cost_total',
    'Total AI API cost in USD',
    ['model'],
    registry=registry
)

# 시스템 메트릭
system_memory_usage_bytes = Gauge(
    'system_memory_usage_bytes',
    'System memory usage in bytes',
    registry=registry
)

system_cpu_usage_percent = Gauge(
    'system_cpu_usage_percent',
    'System CPU usage percentage',
    registry=registry
)

# Redis 메트릭
redis_operations_total = Counter(
    'redis_operations_total',
    'Total Redis operations',
    ['operation', 'status'],
    registry=registry
)

redis_operation_duration_seconds = Histogram(
    'redis_operation_duration_seconds',
    'Redis operation duration in seconds',
    ['operation'],
    registry=registry
)

# 캐시 메트릭
cache_hits_total = Counter(
    'cache_hits_total',
    'Total cache hits',
    ['cache_type'],
    registry=registry
)

cache_misses_total = Counter(
    'cache_misses_total',
    'Total cache misses',
    ['cache_type'],
    registry=registry
)

# 에러 메트릭
errors_total = Counter(
    'errors_total',
    'Total errors',
    ['error_type', 'endpoint'],
    registry=registry
)

# 애플리케이션 정보
app_info = Info(
    'app_info',
    'Application information',
    registry=registry
)


class MetricsCollector:
    """메트릭 수집 및 관리 클래스"""
    
    def __init__(self):
        self.settings = get_current_settings()
        self.request_stats = defaultdict(lambda: {
            'count': 0,
            'total_duration': 0.0,
            'errors': 0
        })
        self.recent_requests = deque(maxlen=1000)  # 최근 1000개 요청 저장
        
        # 앱 정보 설정
        app_info.info({
            'version': self.settings.VERSION,
            'environment': self.settings.ENVIRONMENT,
            'debug': str(self.settings.DEBUG)
        })
    
    def record_http_request(self, method: str, endpoint: str, 
                          status_code: int, duration: float):
        """HTTP 요청 메트릭 기록"""
        # Prometheus 메트릭
        http_requests_total.labels(
            method=method,
            endpoint=endpoint,
            status_code=status_code
        ).inc()
        
        http_request_duration_seconds.labels(
            method=method,
            endpoint=endpoint
        ).observe(duration)
        
        # 내부 통계
        key = f"{method}:{endpoint}"
        stats = self.request_stats[key]
        stats['count'] += 1
        stats['total_duration'] += duration
        if status_code >= 400:
            stats['errors'] += 1
        
        # 최근 요청 기록
        self.recent_requests.append({
            'timestamp': datetime.utcnow(),
            'method': method,
            'endpoint': endpoint,
            'status_code': status_code,
            'duration': duration
        })
    
    def record_ai_request(self, model: str, request_type: str, 
                         duration: float, tokens_used: int, cost: float = None):
        """AI 요청 메트릭 기록"""
        ai_requests_total.labels(model=model, type=request_type).inc()
        ai_request_duration_seconds.labels(model=model, type=request_type).observe(duration)
        ai_tokens_used_total.labels(model=model).inc(tokens_used)
        
        if cost:
            ai_cost_total.labels(model=model).inc(cost)
    
    def record_redis_operation(self, operation: str, duration: float, 
                             success: bool = True):
        """Redis 작업 메트릭 기록"""
        status = 'success' if success else 'error'
        redis_operations_total.labels(operation=operation, status=status).inc()
        redis_operation_duration_seconds.labels(operation=operation).observe(duration)
    
    def record_cache_hit(self, cache_type: str):
        """캐시 히트 기록"""
        cache_hits_total.labels(cache_type=cache_type).inc()
    
    def record_cache_miss(self, cache_type: str):
        """캐시 미스 기록"""
        cache_misses_total.labels(cache_type=cache_type).inc()
    
    def record_error(self, error_type: str, endpoint: str = None):
        """에러 메트릭 기록"""
        errors_total.labels(error_type=error_type, endpoint=endpoint or 'unknown').inc()
    
    def update_system_metrics(self):
        """시스템 메트릭 업데이트"""
        try:
            # 메모리 사용량
            memory = psutil.virtual_memory()
            system_memory_usage_bytes.set(memory.used)
            
            # CPU 사용률
            cpu_percent = psutil.cpu_percent(interval=1)
            system_cpu_usage_percent.set(cpu_percent)
            
            # 성능 로깅
            PerformanceLogger.log_memory_usage(memory.used / 1024 / 1024)  # MB 단위
            
        except Exception as e:
            logger.error("Failed to update system metrics", error=str(e))
    
    def get_request_stats(self) -> Dict[str, Any]:
        """요청 통계 반환"""
        total_requests = sum(stats['count'] for stats in self.request_stats.values())
        total_errors = sum(stats['errors'] for stats in self.request_stats.values())
        
        # 평균 응답 시간 계산
        avg_response_times = {}
        for endpoint, stats in self.request_stats.items():
            if stats['count'] > 0:
                avg_response_times[endpoint] = stats['total_duration'] / stats['count']
        
        return {
            'total_requests': total_requests,
            'total_errors': total_errors,
            'error_rate': total_errors / total_requests if total_requests > 0 else 0,
            'average_response_times': avg_response_times,
            'recent_requests_count': len(self.recent_requests)
        }
    
    def get_prometheus_metrics(self) -> str:
        """Prometheus 메트릭 문자열 반환"""
        return generate_latest(registry).decode('utf-8')


# 전역 메트릭 수집기
metrics_collector = MetricsCollector()


class PerformanceMonitor:
    """성능 모니터링 유틸리티"""
    
    def __init__(self):
        self.slow_queries = deque(maxlen=100)
        self.performance_alerts = deque(maxlen=50)
    
    @asynccontextmanager
    async def monitor_operation(self, operation_name: str, 
                              slow_threshold: float = 1.0):
        """작업 성능 모니터링 컨텍스트 매니저"""
        start_time = time.time()
        try:
            yield
        finally:
            duration = time.time() - start_time
            
            if duration > slow_threshold:
                self.slow_queries.append({
                    'operation': operation_name,
                    'duration': duration,
                    'timestamp': datetime.utcnow()
                })
                
                PerformanceLogger.log_slow_query(
                    query_type=operation_name,
                    duration_ms=duration * 1000,
                    threshold_ms=slow_threshold * 1000
                )
    
    def add_performance_alert(self, alert_type: str, message: str, 
                            severity: str = 'warning'):
        """성능 알림 추가"""
        self.performance_alerts.append({
            'type': alert_type,
            'message': message,
            'severity': severity,
            'timestamp': datetime.utcnow()
        })
        
        logger.warning(
            "Performance alert",
            alert_type=alert_type,
            message=message,
            severity=severity
        )
    
    def get_slow_queries(self, limit: int = 10) -> List[Dict[str, Any]]:
        """최근 느린 쿼리 반환"""
        return list(self.slow_queries)[-limit:]
    
    def get_performance_alerts(self, limit: int = 10) -> List[Dict[str, Any]]:
        """최근 성능 알림 반환"""
        return list(self.performance_alerts)[-limit:]


# 전역 성능 모니터
performance_monitor = PerformanceMonitor()


class HealthChecker:
    """헬스체크 유틸리티"""
    
    def __init__(self):
        self.health_status = {
            'database': True,
            'redis': True,
            'openai': True,
            'memory': True,
            'disk': True
        }
        self.last_check = {}
    
    async def check_redis_health(self) -> bool:
        """Redis 헬스체크"""
        try:
            import redis
            r = redis.Redis(
                host=self.settings.REDIS_HOST,
                port=self.settings.REDIS_PORT,
                db=self.settings.REDIS_DB,
                socket_connect_timeout=5
            )
            await asyncio.wait_for(
                asyncio.get_event_loop().run_in_executor(None, r.ping),
                timeout=5.0
            )
            return True
        except Exception as e:
            logger.error("Redis health check failed", error=str(e))
            return False
    
    async def check_openai_health(self) -> bool:
        """OpenAI API 헬스체크"""
        try:
            # OpenAI API 키가 설정되어 있는지만 확인
            import os
            api_key = os.getenv("OPENAI_API_KEY")
            return bool(api_key and api_key != "YOUR_NEW_API_KEY_HERE")
        except Exception as e:
            logger.error("OpenAI health check failed", error=str(e))
            return False
    
    def check_system_health(self) -> Dict[str, bool]:
        """시스템 리소스 헬스체크"""
        try:
            # 메모리 사용률 체크 (90% 이상이면 불건전)
            memory = psutil.virtual_memory()
            memory_healthy = memory.percent < 90
            
            # 디스크 사용률 체크 (95% 이상이면 불건전)
            disk = psutil.disk_usage('/')
            disk_healthy = (disk.used / disk.total) < 0.95
            
            return {
                'memory': memory_healthy,
                'disk': disk_healthy
            }
        except Exception as e:
            logger.error("System health check failed", error=str(e))
            return {'memory': False, 'disk': False}
    
    async def perform_health_check(self) -> Dict[str, Any]:
        """전체 헬스체크 수행"""
        now = datetime.utcnow()
        
        # 각 컴포넌트 헬스체크
        redis_healthy = await self.check_redis_health()
        openai_healthy = await self.check_openai_health()
        system_health = self.check_system_health()
        
        # 헬스 상태 업데이트
        self.health_status.update({
            'redis': redis_healthy,
            'openai': openai_healthy,
            **system_health
        })
        
        # 전체 상태 판단
        overall_healthy = all(self.health_status.values())
        
        health_report = {
            'status': 'healthy' if overall_healthy else 'unhealthy',
            'timestamp': now.isoformat(),
            'checks': self.health_status,
            'uptime_seconds': (now - datetime.utcnow()).total_seconds(),
            'version': get_current_settings().VERSION
        }
        
        if not overall_healthy:
            logger.warning("Health check failed", checks=self.health_status)
        
        return health_report


# 전역 헬스체커
health_checker = HealthChecker()


async def start_background_monitoring():
    """백그라운드 모니터링 시작"""
    async def monitoring_loop():
        while True:
            try:
                # 시스템 메트릭 업데이트
                metrics_collector.update_system_metrics()
                
                # 5분마다 헬스체크 수행
                await health_checker.perform_health_check()
                
                await asyncio.sleep(60)  # 1분마다 실행
                
            except Exception as e:
                logger.error("Background monitoring error", error=str(e))
                await asyncio.sleep(60)
    
    # 백그라운드 태스크로 실행
    asyncio.create_task(monitoring_loop())


def get_metrics_endpoint_response() -> str:
    """메트릭 엔드포인트 응답 생성"""
    return metrics_collector.get_prometheus_metrics()


def get_health_status() -> Dict[str, Any]:
    """현재 헬스 상태 반환"""
    return health_checker.health_status


async def get_performance_stats() -> Dict[str, Any]:
    """성능 통계 반환"""
    request_stats = metrics_collector.get_request_stats()
    slow_queries = performance_monitor.get_slow_queries()
    performance_alerts = performance_monitor.get_performance_alerts()
    
    return {
        'request_stats': request_stats,
        'slow_queries': slow_queries,
        'performance_alerts': performance_alerts,
        'system_metrics': {
            'memory_usage_mb': psutil.virtual_memory().used / 1024 / 1024,
            'cpu_usage_percent': psutil.cpu_percent(),
            'disk_usage_percent': psutil.disk_usage('/').percent
        }
    }