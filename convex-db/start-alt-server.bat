@echo off
echo ==========================================
echo  Starting EGLA Alternative Server (8080)
echo ==========================================
echo.

echo This server uses port 8080 instead of 3000
echo to bypass potential router/firewall restrictions.
echo.

echo Starting server...
node server-alt.js

pause 