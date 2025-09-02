# 프론트엔드 개발 서버용 Dockerfile
FROM nginx:alpine

# 필요한 패키지 설치
RUN apk add --no-cache bash curl

# Nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/nginx.conf

# HTML 파일들 복사
COPY . /usr/share/nginx/html

# 포트 노출
EXPOSE 3000

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]