#!/usr/bin/env python3

import os
import sys

def main():
    print("BookReview AI Service Quick Test")
    print("=" * 35)
    
    # Check .env file
    env_file = "ai-service/.env" 
    if os.path.exists(env_file):
        print("OK: .env file exists")
        
        # Read and check API key
        with open(env_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'OPENAI_API_KEY=sk-proj-' in content:
                print("OK: API key is configured")
            else:
                print("ERROR: API key not found or invalid")
                return False
    else:
        print("ERROR: .env file not found")
        return False
    
    # Test imports
    print("\nTesting imports...")
    try:
        import fastapi
        import uvicorn
        import openai
        import redis
        print("OK: All packages imported successfully")
    except ImportError as e:
        print(f"ERROR: Import failed - {e}")
        return False
    
    # Test Redis
    print("\nTesting Redis connection...")
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0)
        r.ping()
        print("OK: Redis connection successful")
    except Exception as e:
        print(f"ERROR: Redis connection failed - {e}")
        return False
    
    print("\n" + "=" * 35)
    print("All tests passed!")
    print("Ready to start AI service.")
    print("\nNext steps:")
    print("1. cd ai-service")
    print("2. uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload")
    return True

if __name__ == "__main__":
    try:
        success = main()
        if not success:
            sys.exit(1)
    except Exception as e:
        print(f"Unexpected error: {e}")
        sys.exit(1)