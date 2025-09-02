@echo off
echo BookReview 시스템 시작 (Docker 없이)
echo ================================

echo ⚠️  주의: Docker 없이 실행하는 간단한 테스트 모드입니다.
echo 완전한 기능을 위해서는 Docker를 사용하세요.
echo.

echo 1. AI 서비스만 먼저 테스트해보겠습니다...
echo.

echo 2. Conda 가상환경 활성화...
call conda activate bookreview-ai

echo 3. AI 서비스 디렉토리로 이동...
cd /d C:\Users\rudtn\BookReview-LLM-Platform\ai-service

echo 4. 필요한 패키지 설치...
pip install -r requirements.txt

echo 5. API 키 테스트...
cd ..
python test_api_key.py

echo 6. AI 서비스 시작 준비 완료!
echo.
echo 🚀 다음 명령으로 AI 서비스를 시작하세요:
echo    cd ai-service
echo    uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
echo.
echo 그러면 http://localhost:8001/docs 에서 API 문서를 확인할 수 있습니다.

pause