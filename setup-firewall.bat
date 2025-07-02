@echo off
echo ======================================
echo  EGLA Database Server Firewall Setup
echo ======================================
echo.

echo Checking if running as Administrator...
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: This script must be run as Administrator!
    echo.
    echo Right-click this file and select "Run as Administrator"
    echo.
    pause
    exit /b 1
)

echo ✓ Running as Administrator
echo.

echo Setting up firewall rules for EGLA Database servers...
echo.

echo Adding rule for HTTP API Server (Port 3000)...
netsh advfirewall firewall add rule name="EGLA HTTP API Server" dir=in action=allow protocol=TCP localport=3000
if %errorLevel% equ 0 (
    echo ✓ HTTP API Server rule added successfully
) else (
    echo ❌ Failed to add HTTP API Server rule
)

echo.
echo Adding rule for Convex Database Server (Port 3210)...
netsh advfirewall firewall add rule name="EGLA Convex Database" dir=in action=allow protocol=TCP localport=3210
if %errorLevel% equ 0 (
    echo ✓ Convex Database rule added successfully
) else (
    echo ❌ Failed to add Convex Database rule
)

echo.
echo Adding rule for Node.js (for development)...
netsh advfirewall firewall add rule name="Node.js for EGLA" dir=in action=allow program="C:\Program Files\nodejs\node.exe"
if %errorLevel% equ 0 (
    echo ✓ Node.js rule added successfully
) else (
    echo ⚠️ Node.js rule failed (may be in different location)
)

echo.
echo ======================================
echo Firewall setup complete!
echo ======================================
echo.
echo Your Android device should now be able to connect to:
echo   • HTTP API: http://10.100.0.2:3000
echo   • Database: http://10.100.0.2:3210
echo.
echo Note: Make sure both servers are running:
echo   1. Convex server: npx convex dev
echo   2. HTTP API server: node server.js
echo.
pause 