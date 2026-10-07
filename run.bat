@echo off
setlocal
echo ===================================================
echo   ExamNexus - Online Examination Portal (Java 21)
echo ===================================================
echo.

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Users\dhars\.tools\apache-maven-3.9.9\bin;%PATH%"

echo [1/3] Verifying Java environment...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java 21 was not found. Please verify installation.
    pause
    exit /b 1
)

echo.
echo [2/3] Starting ExamNexus Spring Boot Server...
start "" http://localhost:8080
call mvnw.cmd spring-boot:run

pause
