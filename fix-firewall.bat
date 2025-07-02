@echo off
echo ==========================================
echo  EGLA Database Firewall Fix Script
echo ==========================================
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

echo Fixing firewall rules with correct port assignments...
echo.

echo Removing existing incorrect rules...
netsh advfirewall firewall delete rule name="EGLA HTTP API Server" >nul 2>&1
netsh advfirewall firewall delete rule name="EGLA Convex Database" >nul 2>&1
echo ✓ Cleaned up existing rules

echo.
echo Creating correct firewall rules...

echo Adding rule for HTTP API Server (Port 3000)...
netsh advfirewall firewall add rule name="EGLA HTTP API Server" dir=in action=allow protocol=TCP localport=3000
if %errorLevel% equ 0 (
    echo ✅ HTTP API Server rule (port 3000) added successfully
) else (
    echo ❌ Failed to add HTTP API Server rule
)

echo.
echo Adding rule for Convex Database Server (Port 3210)...
netsh advfirewall firewall add rule name="EGLA Convex Database" dir=in action=allow protocol=TCP localport=3210
if %errorLevel% equ 0 (
    echo ✅ Convex Database rule (port 3210) added successfully
) else (
    echo ❌ Failed to add Convex Database rule
)

echo.
echo Verifying rules...
echo.
echo HTTP API Server rule:
netsh advfirewall firewall show rule name="EGLA HTTP API Server" | findstr "LocalPort"

echo.
echo Convex Database rule:
netsh advfirewall firewall show rule name="EGLA Convex Database" | findstr "LocalPort"

echo.
echo ==========================================
echo Firewall fix complete!
echo ==========================================
echo.
echo Your Android device should now connect to:
echo   • HTTP API: http://10.100.0.2:3000
echo   • Database: http://10.100.0.2:3210
echo.
echo Test connectivity with: test-connectivity.bat
echo.
pause 