# 🔒 보안 가이드

## 중요한 보안 알림

### ⚠️ API 키 관리
- **절대 Git에 API 키를 커밋하지 마세요**
- `.env` 파일은 `.gitignore`에 포함되어 있습니다
- API 키가 노출된 경우 즉시 새로 발급받으세요

### 🔑 환경변수 설정

#### 1. OpenAI API 키 설정
```bash
# .env 파일에 새로운 API 키 설정
OPENAI_API_KEY=sk-proj-your-new-api-key-here
```

#### 2. 프로덕션 환경 설정
```bash
# 프로덕션에서는 더 강력한 SECRET_KEY 사용
SECRET_KEY=your-very-long-and-secure-secret-key-here
DEBUG=False
```

### 🛡️ 보안 기능

#### 백엔드 (Spring Boot)
- ✅ JWT 기반 인증
- ✅ 비밀번호 암호화 (BCrypt)
- ✅ XSS/SQL Injection 방지
- ✅ Rate Limiting
- ✅ 보안 헤더 설정
- ✅ CORS 설정
- ✅ 입력 검증

#### AI 서비스 (FastAPI)
- ✅ API 키 보호
- ✅ Rate Limiting
- ✅ 요청 검증
- ✅ 에러 로깅
- ✅ 보안 감사

### 🔄 API 키 교체 절차

1. **기존 키 비활성화**
   - https://platform.openai.com/api-keys 접속
   - 기존 키 삭제

2. **새 키 발급**
   - "Create new secret key" 클릭
   - 키 복사 (한 번만 표시됨!)

3. **환경변수 업데이트**
   ```bash
   # .env 파일 수정
   OPENAI_API_KEY=sk-proj-your-new-key-here
   ```

4. **서비스 재시작**
   ```bash
   # AI 서비스 재시작
   conda activate bookreview-ai
   uvicorn app.main:app --reload
   ```

### 📋 보안 체크리스트

- [ ] API 키가 Git에 커밋되지 않았는지 확인
- [ ] `.env` 파일이 `.gitignore`에 포함되었는지 확인
- [ ] 프로덕션 환경에서 DEBUG=False 설정
- [ ] 강력한 SECRET_KEY 사용
- [ ] HTTPS 사용 (프로덕션)
- [ ] 정기적인 API 키 순환
- [ ] 로그 모니터링

### 🚨 보안 사고 대응

1. **API 키 노출 시**
   - 즉시 기존 키 비활성화
   - 새 키 발급 및 적용
   - 사용량 모니터링

2. **의심스러운 활동 감지 시**
   - 로그 분석
   - IP 차단
   - 보안팀 연락

### 📞 연락처
- 보안 문제 발견 시: security@bookreview.com
- 긴급 상황: +82-10-xxxx-xxxx