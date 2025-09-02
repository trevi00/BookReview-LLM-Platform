@echo off
echo BookReview AI Service 환경 설정 스크립트
echo =====================================

echo 1. Conda 가상환경 활성화...
call conda activate bookreview-ai

echo 2. 필요한 패키지 설치...
cd /d C:\Users\rudtn\BookReview-LLM-Platform\ai-service
pip install -r requirements.txt

echo 3. 환경변수 파일 확인...
if not exist ai-service\.env (
    echo 🚨 중요: .env 파일이 없습니다!
    echo AI 서비스 디렉토리에 .env 파일이 이미 생성되어 있어야 합니다.
    echo.
    echo 다음 단계를 따라주세요:
    echo 1. https://platform.openai.com/api-keys 에서 새로운 API 키 생성
    echo 2. ai-service\.env 파일에서 OPENAI_API_KEY 값 수정
    echo 3. 기존에 노출된 API 키는 즉시 삭제하세요!
    echo.
    pause
) else (
    echo ✅ .env 파일이 존재합니다.
    echo ⚠️  OPENAI_API_KEY가 올바르게 설정되어 있는지 확인해주세요!
)

echo 4. AI 서비스 실행 준비 완료!
echo.
echo 다음 명령으로 서비스를 시작할 수 있습니다:
echo   conda activate bookreview-ai
echo   cd C:\Users\rudtn\BookReview-LLM-Platform\ai-service
echo   uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
echo.
echo 또는 Docker Compose를 사용하세요:
echo   cd C:\Users\rudtn\BookReview-LLM-Platform
echo   docker-compose up -d
echo.
echo 중요: .env 파일의 OPENAI_API_KEY를 새로 발급받은 키로 수정해주세요!

pause