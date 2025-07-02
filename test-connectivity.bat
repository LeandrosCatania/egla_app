@echo off
echo ==========================================
echo  EGLA Database Connectivity Test
echo ==========================================
echo.

echo Testing server connectivity...
echo.

echo 1. Testing localhost connection...
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:3000/health
if %errorLevel% equ 0 (
    echo ✓ Localhost connection working
) else (
    echo ❌ Localhost connection failed
)

echo.
echo 2. Testing network IP (10.100.0.2)...
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://10.100.0.2:3000/health
if %errorLevel% equ 0 (
    echo ✓ Network IP connection working
) else (
    echo ❌ Network IP connection failed - check firewall
)

echo.
echo 3. Getting full health check response...
echo.
curl http://10.100.0.2:3000/health
echo.

echo.
echo 4. Testing with detailed output...
echo.
curl -v http://10.100.0.2:3000/health

echo.
echo ==========================================
echo Test complete!
echo ==========================================
echo.
echo If network IP test failed:
echo   1. Run setup-firewall.bat as Administrator
echo   2. Check Windows Firewall settings
echo   3. Ensure antivirus isn't blocking connections
echo.
pause 