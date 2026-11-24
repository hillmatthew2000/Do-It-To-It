@echo off
rem Runs the packaged Do-It-To-It jar. Used by run.bat and console.bat; any arguments are passed on
rem to the application (for example --spring, --server.port=9090 or --todo.data-dir=D:\tasks).
setlocal

set "APP_DIR=%~dp0"

if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)
if defined JAVA_HOME if not exist "%JAVA_CMD%" (
    echo JAVA_HOME is set to "%JAVA_HOME%", but "%JAVA_CMD%" does not exist. 1>&2
    exit /b 1
)
if not defined JAVA_HOME (
    where java >nul 2>&1 || (
        echo Java 21 or newer is required. Install a JDK and either set JAVA_HOME or put java on the PATH. 1>&2
        exit /b 1
    )
)

set "JAR="
for %%F in ("%APP_DIR%target\do-it-to-it-*.jar") do (
    if /i not "%%~xF"==".original" set "JAR=%%~fF"
)
if not defined JAR (
    echo No application jar found in "%APP_DIR%target". 1>&2
    echo Build it first with:  cd /d "%APP_DIR%" ^&^& mvnw.cmd package 1>&2
    exit /b 1
)

"%JAVA_CMD%" -jar "%JAR%" %*
exit /b %ERRORLEVEL%
