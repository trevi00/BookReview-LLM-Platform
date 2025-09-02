@echo off
echo BookReview AI Service 시작
echo =========================

echo Conda 가상환경 활성화...
call conda activate bookreview-ai

echo AI Service 시작...
cd /d C:\Users\rudtn\BookReview-LLM-Platform\ai-service

echo 서비스가 시작됩니다...
echo - API 문서: http://localhost:8001/docs
echo - Health Check: http://localhost:8001/health
echo - Analytics: http://localhost:8001/api/v1/analytics/system/health

uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload