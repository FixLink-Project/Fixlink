@echo off
rem Chay cong LiteLLM cho Tro ly FixLink tai http://localhost:4000 (can Docker Desktop).
rem Doc GROQ_API_KEY, LITELLM_MASTER_KEY tu file .env o thu muc goc du an.
cd /d "%~dp0.."
if not exist ".env" (
    echo Chua co file .env. Hay copy .env.example thanh .env va dien key.
    pause
    exit /b 1
)
docker compose up litellm
