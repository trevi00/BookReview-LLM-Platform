#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
from dotenv import load_dotenv

def test_env_setup():
    print("=== Environment Test ===")
    
    # .env file check
    env_path = "ai-service/.env"
    if os.path.exists(env_path):
        print(f"✓ .env file found: {env_path}")
    else:
        print(f"✗ .env file not found: {env_path}")
        return False
    
    # Load environment variables
    load_dotenv(env_path)
    
    # Check API key
    api_key = os.getenv('OPENAI_API_KEY')
    if api_key and api_key != 'YOUR_NEW_API_KEY_HERE':
        print(f"✓ API key configured: {api_key[:20]}...")
        return True
    else:
        print("✗ API key not properly configured")
        return False

def test_imports():
    print("\n=== Import Test ===")
    try:
        import fastapi
        print(f"✓ FastAPI: {fastapi.__version__}")
        
        import uvicorn
        print(f"✓ Uvicorn: {uvicorn.__version__}")
        
        import openai
        print(f"✓ OpenAI: {openai.__version__}")
        
        import redis
        print(f"✓ Redis: {redis.__version__}")
        
        return True
    except ImportError as e:
        print(f"✗ Import error: {e}")
        return False

def test_redis_connection():
    print("\n=== Redis Connection Test ===")
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
        r.ping()
        print("✓ Redis connection successful")
        return True
    except Exception as e:
        print(f"✗ Redis connection failed: {e}")
        return False

if __name__ == "__main__":
    print("BookReview AI Service Test")
    print("=" * 30)
    
    env_ok = test_env_setup()
    import_ok = test_imports()
    redis_ok = test_redis_connection()
    
    print("\n" + "=" * 30)
    print("Summary:")
    print(f"Environment: {'✓' if env_ok else '✗'}")
    print(f"Imports: {'✓' if import_ok else '✗'}")
    print(f"Redis: {'✓' if redis_ok else '✗'}")
    
    if env_ok and import_ok:
        print("\n🚀 Ready to start AI service!")
        print("Run: cd ai-service && uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload")
    else:
        print("\n❌ Please fix the issues above first.")
    
    input("\nPress Enter to continue...")