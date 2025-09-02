# BookReview-LLM-Platform 세션 계속 가이드

## 🚀 다음 세션에서 해야 할 일

### 1. 관리자 권한으로 명령 프롬프트 실행
```cmd
# Windows 키 + R → "cmd" 입력 → Ctrl+Shift+Enter (관리자 권한)
# 또는 시작 메뉴에서 "명령 프롬프트" 우클릭 → "관리자 권한으로 실행"
```

### 2. 프로젝트 디렉토리로 이동
```cmd
cd C:\Users\rudtn\BookReview-LLM-Platform\backend
```

### 3. Docker MySQL 컨테이너 확인 및 시작
```cmd
# 컨테이너 상태 확인
docker ps -a

# MySQL 컨테이너가 있다면 시작
docker start bookreview-mysql

# MySQL 컨테이너가 없다면 새로 생성
docker run -d ^
  --name bookreview-mysql ^
  -e MYSQL_ROOT_PASSWORD=root_password ^
  -e MYSQL_DATABASE=bookreview ^
  -e MYSQL_USER=bookreview_user ^
  -e MYSQL_PASSWORD=bookreview_password ^
  -p 3307:3306 ^
  mysql:8.0
```

### 4. Claude Code 재시작
```cmd
# 현재 디렉토리에서 Claude Code 실행
claude-code

# 또는 새 터미널에서
cd C:\Users\rudtn\BookReview-LLM-Platform\backend
claude-code
```

### 5. Claude에게 알려줄 내용
"관리자 권한으로 cmd를 열고 클로드 코드를 다시 시작했어. MySQL Docker 컨테이너 문제부터 이어서 해결하자."

## 🔧 현재 상태 요약

### ✅ 완료된 사항
- **Spring Boot 백엔드**: 100% 컴파일 성공
- **React Native 모바일 앱**: 모든 주요 화면 구현 완료
- **FastAPI AI 서비스**: 구현 완료
- **단위 테스트**: 19개 중 19개 모두 통과
- **AuthService 테스트**: Mockito 이슈 해결
- **JwtUtil 테스트**: ExpiredJwtException 해결

### ⏳ 남은 작업
- **MySQL Docker 컨테이너**: 3307 포트에서 실행 필요
- **백엔드 서버 시작**: 데이터베이스 연결 후 정상 구동 확인
- **통합 테스트**: 전체 시스템 연동 테스트

### 🎯 예상 소요 시간
- MySQL 컨테이너 시작: 2-3분
- 백엔드 서버 구동 확인: 1-2분
- 전체 시스템 테스트: 5-10분

## 📋 체크리스트

### MySQL 컨테이너 시작 확인
- [ ] `docker ps` 명령으로 bookreview-mysql 컨테이너 RUNNING 상태 확인
- [ ] `netstat -an | findstr 3307` 명령으로 포트 3307 리스닝 확인

### 백엔드 서버 시작 확인  
- [ ] `./gradlew bootRun` 명령 실행
- [ ] "Started BookReviewApplication" 메시지 확인
- [ ] localhost:8080 포트에서 서버 응답 확인

### 최종 검증
- [ ] `./gradlew test` - 모든 테스트 통과 확인
- [ ] Postman/curl로 API 엔드포인트 테스트
- [ ] React Native 앱에서 백엔드 API 연동 테스트

## 🛠️ 문제 해결 팁

### Docker 관련 문제
```cmd
# Docker Desktop이 실행 중인지 확인
docker version

# Docker 서비스 재시작 (필요시)
net stop com.docker.service
net start com.docker.service
```

### MySQL 연결 문제
```cmd
# MySQL 컨테이너 로그 확인
docker logs bookreview-mysql

# MySQL 컨테이너 내부 접속 (테스트용)
docker exec -it bookreview-mysql mysql -u bookreview_user -p
```

### 포트 충돌 문제
```cmd
# 3307 포트 사용 중인 프로세스 확인
netstat -ano | findstr 3307

# 프로세스 종료 (PID 확인 후)
taskkill /PID [PID번호] /F
```

## 📁 중요 파일 위치

### 설정 파일
- `C:\Users\rudtn\BookReview-LLM-Platform\backend\src\main\resources\application-dev.yml`
- `C:\Users\rudtn\BookReview-LLM-Platform\backend\build.gradle`

### 주요 소스 코드
- `C:\Users\rudtn\BookReview-LLM-Platform\backend\src\main\java\com\bookreview\`
- `C:\Users\rudtn\BookReview-LLM-Platform\mobile\BookReviewApp\src\`
- `C:\Users\rudtn\BookReview-LLM-Platform\ai-service\`

### 문서
- `C:\Users\rudtn\BookReview-LLM-Platform\docs\`

## 🎯 최종 목표
1. **완전한 시스템 구동**: Spring Boot + FastAPI + React Native + MySQL + Redis
2. **모든 테스트 통과**: 단위 테스트 + 통합 테스트
3. **API 엔드포인트 검증**: 실제 데이터 CRUD 동작 확인
4. **모바일 앱 연동**: React Native에서 백엔드 API 호출 성공

---

**다음 세션 시작 멘트**: "관리자 권한으로 cmd를 열고 클로드 코드를 다시 시작했어. MySQL Docker 컨테이너 문제부터 이어서 해결하자."