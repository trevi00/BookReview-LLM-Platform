@echo off
echo ========================================
echo BookReview 전체 시스템 중지 (Docker)
echo ========================================

echo.
echo 전체 시스템을 중지하고 있습니다...
docker-compose -f docker-compose.dev.yml down

echo.
echo 시스템이 중지되었습니다.
pause