"""
간단한 FastAPI 테스트 서버
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Create FastAPI app
app = FastAPI(
    title="BookReview AI Service",
    description="AI-powered feedback service for book reviews",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {
        "message": "BookReview AI Service is running!",
        "version": "1.0.0",
        "endpoints": {
            "health": "/health",
            "docs": "/docs",
            "openai_test": "/test/openai",
            "redis_test": "/test/redis"
        }
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "BookReview AI Service"
    }

@app.get("/test/openai")
async def test_openai():
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key or api_key == "YOUR_NEW_API_KEY_HERE":
        raise HTTPException(status_code=500, detail="OpenAI API key not configured")
    
    return {
        "status": "success",
        "message": "OpenAI API key configured",
        "key_preview": f"{api_key[:20]}..."
    }

@app.get("/test/redis")
async def test_redis():
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0)
        r.ping()
        return {"status": "success", "message": "Redis connection OK"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Redis connection failed: {str(e)}")

@app.post("/api/v1/feedback/simple")
async def simple_feedback(content: dict):
    """간단한 피드백 테스트 엔드포인트"""
    
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key or api_key == "YOUR_NEW_API_KEY_HERE":
        raise HTTPException(status_code=500, detail="OpenAI API key not configured")
    
    try:
        import openai
        
        client = openai.OpenAI(api_key=api_key)
        
        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "당신은 독서 피드백 전문가입니다. 간단하고 도움이 되는 피드백을 제공하세요."},
                {"role": "user", "content": f"다음 독서 기록에 대한 피드백을 주세요: {content.get('text', '')}"}
            ],
            max_tokens=200
        )
        
        return {
            "status": "success",
            "feedback": response.choices[0].message.content,
            "model": "gpt-3.5-turbo",
            "tokens_used": response.usage.total_tokens
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI feedback generation failed: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)