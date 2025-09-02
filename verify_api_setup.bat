@echo off
echo API 키 설정 확인 스크립트
echo ============================

echo 1. 가상환경 생성 (이미 있으면 스킵됨)...
conda create -n bookreview-ai python=3.11 -y

echo.
echo 2. 가상환경 활성화...
call conda activate bookreview-ai

echo.
echo 3. 필수 패키지 설치...
pip install python-dotenv openai requests

echo.
echo 4. .env 파일 확인...
cd /d C:\Users\rudtn\BookReview-LLM-Platform\ai-service
if exist .env (
    echo ✅ .env 파일이 존재합니다.
    findstr "OPENAI_API_KEY" .env
) else (
    echo ❌ .env 파일이 없습니다!
    exit /b 1
)

echo.
echo 5. API 키 테스트...
cd /d C:\Users\rudtn\BookReview-LLM-Platform
python test_api_key.py

echo.
echo 6. 설정 완료!
echo 다음 단계: start_ai_service.bat 실행
pause