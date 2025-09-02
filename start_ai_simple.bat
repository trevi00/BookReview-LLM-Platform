@echo off
echo Starting BookReview AI Service...
echo ====================================

cd /d C:\Users\rudtn\BookReview-LLM-Platform\ai-service

echo Installing required packages...
pip install pydantic-settings 2>nul

echo.
echo Starting AI Service on http://localhost:8001
echo Press Ctrl+C to stop
echo.

python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload