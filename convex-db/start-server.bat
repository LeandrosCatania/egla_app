@echo off
echo Starting Convex Location Database Server...

REM Set up Node.js environment
set PATH=%LOCALAPPDATA%\nvs\node\22.17.0\x64;%PATH%

REM Set Convex URL
set CONVEX_URL=http://localhost:3210

echo.
echo Starting Convex dev server...
start "Convex Dev Server" /min cmd /c "npx convex dev"

echo Waiting for Convex dev server to start...
timeout /t 5 /nobreak >nul

echo.
echo Starting HTTP API server...
start "HTTP API Server" /min cmd /c "node server.js"

echo.
echo ✅ Servers started successfully!
echo.
echo 🌐 Convex Database: http://localhost:3210
echo 🚀 HTTP API Server: http://localhost:3000
echo.
echo Available API endpoints:
echo   POST http://localhost:3000/api/location - Insert location record
echo   GET  http://localhost:3000/api/location/recent - Get recent records
echo   GET  http://localhost:3000/health - Health check
echo.
echo To stop the servers, close the opened terminal windows.
echo.
pause 