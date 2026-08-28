@echo off
title MindMatrix - 100% Free Live Public Server
color 0A
cls
echo ================================================================
echo        MINDMATRIX - 100%% FREE PUBLIC LIVE HOSTING
echo ================================================================
echo.

:: Set Java and Tomcat Environment Variables
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "JRE_HOME=C:\Program Files\Java\jdk-17"
set "CATALINA_HOME=C:\xampp\tomcat"
set "CATALINA_BASE=C:\xampp\tomcat"

echo [1/3] Checking MySQL database...
netstat -ano | findstr :3306 >nul
if %errorlevel% neq 0 (
    echo [i] Starting MySQL...
    if exist "C:\xampp\mysql_start.bat" (
        start "" /min "C:\xampp\mysql_start.bat"
        timeout /t 3 >nul
    ) else (
        echo [!] Please make sure MySQL is started in XAMPP Control Panel.
    )
) else (
    echo [OK] MySQL is active on port 3306.
)

echo.
echo [2/3] Checking Tomcat web server...
netstat -ano | findstr :8080 >nul
if %errorlevel% neq 0 (
    echo [i] Starting Apache Tomcat on port 8080...
    pushd "C:\xampp\tomcat\bin"
    start "Tomcat Server" "startup.bat"
    popd
    echo     Waiting 6 seconds for Tomcat to initialize...
    timeout /t 6 >nul
) else (
    echo [OK] Tomcat is already running on port 8080.
)

echo.
echo [3/3] Starting Cloudflare Free Secure Public Tunnel...
echo ================================================================
echo  Your live shareable link will appear below!
echo  Look for the link ending in .trycloudflare.com
echo  (Example: https://something.trycloudflare.com)
echo ================================================================
echo.
echo  Keep this window OPEN while playing or testing the game.
echo.

"%~dp0cloudflared.exe" tunnel --url http://localhost:8080
pause
