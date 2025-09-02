"""
AI 서비스 분석 및 모니터링 API 엔드포인트
"""

from fastapi import APIRouter, HTTPException, Header, Query
from typing import Optional, List
import structlog
from ...services.analytics_service import analytics_service
from ...services.cache_service import cache_service

logger = structlog.get_logger(__name__)

router = APIRouter()


@router.get("/system/health")
async def get_system_health():
    """
    시스템 건강 상태 조회
    """
    try:
        health_data = await analytics_service.get_system_health()
        return {
            "success": True,
            "data": health_data
        }
    except Exception as e:
        logger.error("시스템 건강 상태 조회 실패", error=str(e))
        raise HTTPException(status_code=500, detail="시스템 상태를 조회할 수 없습니다.")


@router.get("/metrics/realtime")
async def get_realtime_metrics():
    """
    실시간 메트릭 조회
    """
    try:
        realtime_data = await analytics_service.get_realtime_metrics()
        return {
            "success": True,
            "data": realtime_data
        }
    except Exception as e:
        logger.error("실시간 메트릭 조회 실패", error=str(e))
        raise HTTPException(status_code=500, detail="실시간 메트릭을 조회할 수 없습니다.")


@router.get("/metrics/daily")
async def get_daily_metrics(
    days: int = Query(default=7, ge=1, le=30, description="조회할 일수")
):
    """
    일별 메트릭 조회
    
    - **days**: 조회할 일수 (1-30일)
    """
    try:
        daily_data = await analytics_service.get_daily_analytics(days)
        return {
            "success": True,
            "data": daily_data
        }
    except Exception as e:
        logger.error("일별 메트릭 조회 실패", error=str(e))
        raise HTTPException(status_code=500, detail="일별 메트릭을 조회할 수 없습니다.")


@router.get("/metrics/endpoint/{endpoint}")
async def get_endpoint_analytics(endpoint: str):
    """
    특정 엔드포인트 분석 데이터 조회
    
    - **endpoint**: 분석할 엔드포인트 경로
    """
    try:
        # 엔드포인트 이름에서 슬래시를 언더스코어로 변경하여 Redis 키로 사용
        clean_endpoint = endpoint.replace('/', '_')
        analytics_data = await analytics_service.get_endpoint_analytics(clean_endpoint)
        
        return {
            "success": True,
            "data": analytics_data
        }
    except Exception as e:
        logger.error("엔드포인트 분석 조회 실패", endpoint=endpoint, error=str(e))
        raise HTTPException(status_code=500, detail="엔드포인트 분석 데이터를 조회할 수 없습니다.")


@router.get("/metrics/user/{user_id}")
async def get_user_analytics(user_id: str):
    """
    특정 사용자 분석 데이터 조회
    
    - **user_id**: 분석할 사용자 ID
    """
    try:
        user_data = await analytics_service.get_user_analytics(user_id)
        return {
            "success": True,
            "data": user_data
        }
    except Exception as e:
        logger.error("사용자 분석 조회 실패", user_id=user_id, error=str(e))
        raise HTTPException(status_code=500, detail="사용자 분석 데이터를 조회할 수 없습니다.")


@router.get("/cache/stats")
async def get_cache_statistics():
    """
    캐시 통계 조회
    """
    try:
        cache_info = await cache_service.get_cache_info()
        return {
            "success": True,
            "data": cache_info
        }
    except Exception as e:
        logger.error("캐시 통계 조회 실패", error=str(e))
        raise HTTPException(status_code=500, detail="캐시 통계를 조회할 수 없습니다.")


@router.get("/cache/stats/{service_type}")
async def get_service_cache_stats(service_type: str):
    """
    특정 서비스의 캐시 통계 조회
    
    - **service_type**: 서비스 타입 (feedback, sentiment, summary, keywords, questions)
    """
    try:
        if service_type not in ["feedback", "sentiment", "summary", "keywords", "questions"]:
            raise HTTPException(status_code=400, detail="유효하지 않은 서비스 타입입니다.")
        
        stats = await cache_service.get_cache_stats(service_type)
        return {
            "success": True,
            "data": stats
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error("서비스 캐시 통계 조회 실패", service_type=service_type, error=str(e))
        raise HTTPException(status_code=500, detail="캐시 통계를 조회할 수 없습니다.")


@router.delete("/cache/{service_type}")
async def invalidate_cache(
    service_type: str,
    pattern: Optional[str] = Query(None, description="삭제할 캐시 패턴")
):
    """
    캐시 무효화
    
    - **service_type**: 서비스 타입
    - **pattern**: 특정 패턴의 캐시만 삭제 (선택사항)
    """
    try:
        if service_type not in ["feedback", "sentiment", "summary", "keywords", "questions", "all"]:
            raise HTTPException(status_code=400, detail="유효하지 않은 서비스 타입입니다.")
        
        if service_type == "all":
            # 모든 서비스 타입의 캐시 무효화
            service_types = ["feedback", "sentiment", "summary", "keywords", "questions"]
            total_deleted = 0
            
            for st in service_types:
                deleted_count = await cache_service.invalidate_cache(st, pattern)
                total_deleted += deleted_count
            
            deleted_count = total_deleted
        else:
            deleted_count = await cache_service.invalidate_cache(service_type, pattern)
        
        return {
            "success": True,
            "message": f"{deleted_count}개의 캐시 항목이 삭제되었습니다.",
            "deleted_count": deleted_count
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error("캐시 무효화 실패", service_type=service_type, error=str(e))
        raise HTTPException(status_code=500, detail="캐시 무효화에 실패했습니다.")


@router.get("/performance/summary")
async def get_performance_summary():
    """
    전체 성능 요약 조회
    """
    try:
        # 시스템 건강 상태
        health_data = await analytics_service.get_system_health()
        
        # 캐시 통계
        cache_info = await cache_service.get_cache_info()
        
        # 실시간 메트릭
        realtime_data = await analytics_service.get_realtime_metrics()
        
        # 최근 7일 트렌드
        daily_data = await analytics_service.get_daily_analytics(7)
        
        # 주요 지표 계산
        total_calls_7d = sum(day.get("total_calls", 0) for day in daily_data)
        avg_calls_per_day = total_calls_7d / len(daily_data) if daily_data else 0
        
        summary = {
            "system_status": health_data.get("status", "unknown"),
            "current_success_rate": health_data.get("success_rate_24h", 0),
            "cache_hit_rate": cache_info.get("overall", {}).get("hit_rate_percentage", 0),
            "avg_calls_per_day": round(avg_calls_per_day, 1),
            "current_load": realtime_data.get("current_hour", {}),
            "trend_7d": {
                "total_calls": total_calls_7d,
                "daily_average": round(avg_calls_per_day, 1)
            },
            "top_endpoints": daily_data[-1].get("top_endpoints", {}) if daily_data else {},
            "timestamp": realtime_data.get("timestamp")
        }
        
        return {
            "success": True,
            "data": summary
        }
        
    except Exception as e:
        logger.error("성능 요약 조회 실패", error=str(e))
        raise HTTPException(status_code=500, detail="성능 요약을 조회할 수 없습니다.")


@router.get("/monitoring/alerts")
async def get_monitoring_alerts():
    """
    모니터링 알림 조회
    """
    try:
        health_data = await analytics_service.get_system_health()
        cache_info = await cache_service.get_cache_info()
        
        alerts = []
        
        # 성공률 체크
        success_rate = health_data.get("success_rate_24h", 100)
        if success_rate < 95:
            alerts.append({
                "level": "warning" if success_rate >= 90 else "critical",
                "message": f"성공률이 낮습니다: {success_rate}%",
                "metric": "success_rate",
                "value": success_rate,
                "threshold": 95
            })
        
        # 캐시 히트율 체크
        hit_rate = cache_info.get("overall", {}).get("hit_rate_percentage", 0)
        if hit_rate < 70:
            alerts.append({
                "level": "warning",
                "message": f"캐시 히트율이 낮습니다: {hit_rate}%",
                "metric": "cache_hit_rate",
                "value": hit_rate,
                "threshold": 70
            })
        
        # 실시간 로드 체크
        realtime_data = await analytics_service.get_realtime_metrics()
        calls_per_minute = realtime_data.get("current_minute", {}).get("calls", 0)
        
        if calls_per_minute > 100:  # 분당 100회 이상 호출 시 알림
            alerts.append({
                "level": "info",
                "message": f"높은 트래픽이 감지되었습니다: 분당 {calls_per_minute}회 호출",
                "metric": "calls_per_minute",
                "value": calls_per_minute,
                "threshold": 100
            })
        
        return {
            "success": True,
            "data": {
                "alert_count": len(alerts),
                "alerts": alerts,
                "timestamp": realtime_data.get("timestamp")
            }
        }
        
    except Exception as e:
        logger.error("모니터링 알림 조회 실패", error=str(e))
        raise HTTPException(status_code=500, detail="모니터링 알림을 조회할 수 없습니다.")