@echo off
rem Starts the interactive console. Extra arguments are passed on, e.g. console.bat --todo.data-dir=D:\tasks
call "%~dp0launch.cmd" %*
set "RC=%ERRORLEVEL%"
rem Keep the window open on failure so the message can be read when started by double-click.
if not "%RC%"=="0" pause
exit /b %RC%
