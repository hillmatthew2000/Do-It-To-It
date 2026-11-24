@echo off
rem Starts the web application (http://localhost:8080 by default). Extra arguments are passed on,
rem e.g. run.bat --server.port=9090
call "%~dp0launch.cmd" --spring %*
set "RC=%ERRORLEVEL%"
rem Keep the window open on failure so the message can be read when started by double-click.
if not "%RC%"=="0" pause
exit /b %RC%
