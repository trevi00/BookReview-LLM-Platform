#!/usr/bin/env python3
"""
OpenAI API 키 테스트 스크립트
사용법: python test_api_key.py
"""

import os
import sys
from dotenv import load_dotenv
import openai

def test_api_key():
    """API 키가 올바르게 작동하는지 테스트"""
    
    # .env 파일 로드
    load_dotenv()
    
    # API 키 확인
    api_key = os.getenv('OPENAI_API_KEY')
    
    if not api_key:
        print("❌ ERROR: OPENAI_API_KEY가 설정되지 않았습니다!")
        print("   .env 파일을 확인해주세요.")
        return False
    
    if api_key == "your-new-api-key-here":
        print("❌ ERROR: API 키가 아직 설정되지 않았습니다!")
        print("   .env 파일에서 OPENAI_API_KEY를 실제 키로 교체해주세요.")
        return False
    
    print(f"✅ API 키 발견: {api_key[:20]}...")
    
    try:
        # OpenAI 클라이언트 설정
        openai.api_key = api_key
        
        # 간단한 테스트 요청
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "user", "content": "안녕하세요! 간단한 테스트입니다."}
            ],
            max_tokens=50
        )
        
        print("✅ API 키 테스트 성공!")
        print(f"   응답: {response.choices[0].message.content}")
        return True
        
    except openai.AuthenticationError:
        print("❌ ERROR: API 키가 유효하지 않습니다!")
        print("   새로운 키를 발급받아 다시 설정해주세요.")
        return False
        
    except Exception as e:
        print(f"❌ ERROR: API 호출 실패: {str(e)}")
        return False

if __name__ == "__main__":
    print("🔑 OpenAI API 키 테스트 시작...")
    print("=" * 40)
    
    success = test_api_key()
    
    print("=" * 40)
    if success:
        print("✅ 모든 테스트 통과! AI 서비스를 시작할 수 있습니다.")
        print("\n🚀 다음 명령으로 AI 서비스를 시작하세요:")
        print("   conda activate bookreview-ai")
        print("   cd C:\\Users\\rudtn\\BookReview-LLM-Platform\\ai-service")
        print("   uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload")
    else:
        print("❌ 테스트 실패! API 키를 확인하고 다시 시도해주세요.")
    
    input("\n계속하려면 Enter를 누르세요...")