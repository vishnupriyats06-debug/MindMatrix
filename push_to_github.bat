@echo off
title Push MindMatrix to GitHub & Auto-Deploy
color 0B
cls
echo ================================================================
echo         MINDMATRIX - 1-CLICK REDEPLOY TO GITHUB & RENDER
echo ================================================================
echo.

set "GIT_EXE=%TEMP%\mingit\cmd\git.exe"
if not exist "%GIT_EXE%" (
    where git >nul 2>nul
    if %errorlevel% equ 0 (
        set "GIT_EXE=git"
    ) else (
        echo [ERROR] Git was not found!
        pause
        exit /b 1
    )
)

echo [1/3] Staging all modified files...
"%GIT_EXE%" add .

echo [2/3] Committing changes...
set "COMMIT_MSG=Updated project - %date% %time%"
"%GIT_EXE%" commit -m "%COMMIT_MSG%"

echo [3/3] Pushing to GitHub...
"%GIT_EXE%" push origin main

echo.
echo ================================================================
if %errorlevel% equ 0 (
    echo  [SUCCESS] Changes pushed to GitHub!
    echo  - Your live hosting (Render) will now auto-detect this push
    echo    and redeploy your site in 1 to 2 minutes.
    echo  - Your live URL stays the EXACT SAME!
) else (
    echo  [NOTE] If push failed, please check your network or credentials.
)
echo ================================================================
echo.
pause

