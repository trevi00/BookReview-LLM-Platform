@echo off
echo ========================================
echo BookReview 전체 시스템 + 개발도구 시작
echo ========================================

echo.
echo 1. 기존 컨테이너 정리...
docker-compose -f docker-compose.dev.yml down

echo.
echo 2. 전체 시스템 + 개발도구 빌드 및 시작...
docker-compose -f docker-compose.dev.yml --profile tools up --build -d

echo.
echo 3. 서비스 상태 확인...
timeout /t 15 >nul

echo.
echo ========================================
echo 서비스 상태:
echo ========================================
docker-compose -f docker-compose.dev.yml ps

echo.
echo ========================================
echo 접속 URL:
echo ========================================
echo [메인 서비스]
echo 프론트엔드:        http://localhost:3000
echo 백엔드 API:        http://localhost:8080/swagger-ui.html
echo AI 서비스:         http://localhost:8000/docs
echo.
echo [개발 도구]
echo 데이터베이스 관리:  http://localhost:8081 (Adminer)
echo Redis 관리:        http://localhost:8082 (Redis Commander)
echo.
echo [헬스체크]
echo 백엔드 헬스:       http://localhost:8080/actuator/health
echo AI 서비스 헬스:    http://localhost:8000/health
echo 프론트엔드 헬스:   http://localhost:3000/health
echo ========================================

echo.
echo 전체 시스템 + 개발도구가 시작되었습니다!
echo 브라우저에서 http://localhost:3000 으로 접속하여 통합 테스트를 진행하세요.

pause