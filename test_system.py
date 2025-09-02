#!/usr/bin/env python3
"""
BookReview-LLM-Platform 시스템 테스트 스크립트
"""

import requests
import json
import time
from datetime import datetime

class SystemTester:
    def __init__(self):
        self.backend_url = "http://localhost:8080/api"
        self.ai_url = "http://localhost:8001/api/v1"
        self.test_results = []
        
    def log_test(self, name, success, message=""):
        """테스트 결과 기록"""
        status = "✅ PASS" if success else "❌ FAIL"
        result = f"{status} {name}"
        if message:
            result += f" - {message}"
        print(result)
        self.test_results.append({
            "name": name,
            "success": success,
            "message": message,
            "timestamp": datetime.now().isoformat()
        })
    
    def test_backend_health(self):
        """백엔드 헬스체크"""
        try:
            response = requests.get(f"{self.backend_url}/health", timeout=5)
            success = response.status_code == 200
            message = f"Status: {response.status_code}"
        except Exception as e:
            success = False
            message = str(e)
        
        self.log_test("Backend Health Check", success, message)
        return success
    
    def test_ai_service_health(self):
        """AI 서비스 헬스체크"""
        try:
            response = requests.get(f"{self.ai_url}/health", timeout=5)
            success = response.status_code == 200
            message = f"Status: {response.status_code}"
        except Exception as e:
            success = False
            message = str(e)
        
        self.log_test("AI Service Health Check", success, message)
        return success
    
    def test_database_connection(self):
        """데이터베이스 연결 테스트"""
        try:
            # Actuator를 통한 DB 헬스체크
            response = requests.get("http://localhost:8080/actuator/health", timeout=10)
            if response.status_code == 200:
                health_data = response.json()
                db_status = health_data.get("components", {}).get("db", {}).get("status")
                success = db_status == "UP"
                message = f"DB Status: {db_status}"
            else:
                success = False
                message = f"Actuator not available: {response.status_code}"
        except Exception as e:
            success = False
            message = str(e)
        
        self.log_test("Database Connection", success, message)
        return success
    
    def test_redis_connection(self):
        """Redis 연결 테스트"""
        try:
            response = requests.get("http://localhost:8080/actuator/health", timeout=10)
            if response.status_code == 200:
                health_data = response.json()
                redis_status = health_data.get("components", {}).get("redis", {}).get("status")
                success = redis_status == "UP"
                message = f"Redis Status: {redis_status}"
            else:
                success = False
                message = f"Actuator not available: {response.status_code}"
        except Exception as e:
            success = False
            message = str(e)
        
        self.log_test("Redis Connection", success, message)
        return success
    
    def test_user_registration(self):
        """사용자 회원가입 테스트"""
        try:
            test_user = {
                "username": f"testuser_{int(time.time())}",
                "email": f"test_{int(time.time())}@example.com",
                "password": "TestPassword123!",
                "confirmPassword": "TestPassword123!"
            }
            
            response = requests.post(
                f"{self.backend_url}/auth/register",
                json=test_user,
                timeout=10
            )
            
            success = response.status_code in [200, 201]
            message = f"Status: {response.status_code}"
            
            if success:
                self.test_user = test_user
                
        except Exception as e:
            success = False
            message = str(e)
        
        self.log_test("User Registration", success, message)
        return success
    
    def test_ai_analytics(self):
        """AI 서비스 분석 기능 테스트"""
        try:
            response = requests.get(f"{self.ai_url}/analytics/system/health", timeout=10)
            success = response.status_code == 200
            
            if success:
                data = response.json()
                status = data.get("data", {}).get("status", "unknown")
                message = f"System Status: {status}"
            else:
                message = f"Status: {response.status_code}"
                
        except Exception as e:
            success = False
            message = str(e)
        
        self.log_test("AI Analytics", success, message)
        return success
    
    def run_all_tests(self):
        """모든 테스트 실행"""
        print("🧪 BookReview-LLM-Platform 시스템 테스트 시작")
        print("=" * 50)
        
        # 인프라 테스트
        print("\n📊 인프라 서비스 테스트:")
        self.test_database_connection()
        self.test_redis_connection()
        
        # 서비스 헬스체크
        print("\n🏥 서비스 헬스체크:")
        backend_ok = self.test_backend_health()
        ai_ok = self.test_ai_service_health()
        
        # 기능 테스트 (서비스가 실행중일 때만)
        if backend_ok:
            print("\n🔐 백엔드 기능 테스트:")
            self.test_user_registration()
        
        if ai_ok:
            print("\n🤖 AI 서비스 기능 테스트:")
            self.test_ai_analytics()
        
        # 결과 요약
        print("\n" + "=" * 50)
        print("📋 테스트 결과 요약:")
        
        passed = sum(1 for result in self.test_results if result["success"])
        total = len(self.test_results)
        
        print(f"✅ 성공: {passed}/{total}")
        print(f"❌ 실패: {total - passed}/{total}")
        
        if passed == total:
            print("\n🎉 모든 테스트 통과! 시스템이 정상 작동 중입니다.")
        else:
            print(f"\n⚠️  {total - passed}개의 테스트가 실패했습니다. 로그를 확인해주세요.")
        
        return passed, total

def main():
    print("BookReview-LLM-Platform 시스템 테스트")
    print("실행하기 전에 다음을 확인해주세요:")
    print("1. Docker Compose가 실행중인지 (MySQL, Redis)")
    print("2. 백엔드 서비스가 실행중인지 (포트 8080)")
    print("3. AI 서비스가 실행중인지 (포트 8001)")
    print()
    
    input("계속하려면 Enter를 누르세요...")
    
    tester = SystemTester()
    passed, total = tester.run_all_tests()
    
    # 결과를 파일로 저장
    with open("test_results.json", "w", encoding="utf-8") as f:
        json.dump({
            "timestamp": datetime.now().isoformat(),
            "passed": passed,
            "total": total,
            "results": tester.test_results
        }, f, ensure_ascii=False, indent=2)
    
    print(f"\n📄 상세 결과가 test_results.json에 저장되었습니다.")
    input("\n종료하려면 Enter를 누르세요...")

if __name__ == "__main__":
    main()