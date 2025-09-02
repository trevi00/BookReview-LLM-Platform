@echo off
echo BookReview-LLM-Platform 시스템 시작
echo ======================================

echo 1. Docker 서비스 상태 확인...
docker --version
if %errorlevel% neq 0 (
    echo ❌ Docker가 설치되지 않았습니다!
    echo Docker Desktop을 설치하고 다시 실행해주세요.
    pause
    exit
)

echo 2. Docker Compose로 인프라 서비스 시작...
docker-compose up -d mysql redis

echo 3. 백엔드 서비스 상태 확인...
echo Spring Boot 백엔드는 IntelliJ에서 수동으로 실행해주세요:
echo   - BookreviewApplication.java 실행
echo   - 포트: 8080
echo   - Swagger UI: http://localhost:8080/swagger-ui.html

echo 4. AI 서비스 상태 확인...
echo AI 서비스는 별도 터미널에서 실행:
echo   conda activate bookreview-ai
echo   cd ai-service
echo   uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload

echo.
echo 🌟 시스템 접속 정보:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo 📊 백엔드 API:     http://localhost:8080
echo 📖 Swagger 문서:  http://localhost:8080/swagger-ui.html
echo 🤖 AI 서비스:     http://localhost:8001
echo 📚 AI API 문서:   http://localhost:8001/docs
echo 💾 MySQL:         localhost:3306 (user: root, pass: password)
echo 🔴 Redis:         localhost:6379
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

pause