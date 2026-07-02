@echo off
title Melodify Web Launcher
echo.
echo ===================================================
echo   MELODIFY WEB STARTUP
echo ===================================================
echo.

:: Check for Node.js
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is NOT installed!
    echo Please install it from https://nodejs.org/
    pause
    exit /b
)

:: Set path carefully (handle spaces)
set "TARGET_DIR=%~dp0website\v2"
echo [1/3] Navigating to: "%TARGET_DIR%"

if not exist "%TARGET_DIR%" (
    echo [ERROR] Website folder not found at: "%TARGET_DIR%"
    pause
    exit /b
)

cd /d "%TARGET_DIR%"

:: Port Cleanup
echo [2/3] Cleaning up Port 3000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3000 ^| findstr LISTENING') do (
    taskkill /f /pid %%a >nul 2>&1
)

:: Install / Run
echo [3/3] Starting Server (React 19 Premium Mode)...
echo.
echo NOTE: If this is the first time, it may take 1-2 minutes to update.
echo.

:: We use 'call' before npm to ensure the script continues
call npm install --legacy-peer-deps && npm run dev

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Something went wrong.
    echo Try running these commands manually in cmd:
    echo cd website/v2
    echo npm install
    echo npm run dev
    pause
)

pause
