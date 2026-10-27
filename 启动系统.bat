@echo off
setlocal
title Stock Trading System
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" -OpenBrowser %*
set "LAUNCH_EXIT_CODE=%ERRORLEVEL%"
if not "%LAUNCH_EXIT_CODE%"=="0" (
    echo.
    echo Failed to start. Read the error above, then press any key to close.
    pause >nul
)
exit /b %LAUNCH_EXIT_CODE%
