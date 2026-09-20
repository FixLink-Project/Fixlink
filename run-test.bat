@echo off
chcp 65001 > nul
echo ===============================================================================
echo            FIXLINK AUTOMATED TEST RUNNER (31 INTEGRATION TESTS)
echo ===============================================================================
echo.
set MAVEN_OPTS=-Xmx1024m
cd /d "%~dp0backend"
call mvnw.cmd test
pause
