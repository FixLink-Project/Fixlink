@echo off
set SPRING_PROFILES_ACTIVE=local
if exist "..\.env" (
    for /f "usebackq tokens=1* delims==" %%i in ("..\.env") do (
        set "line=%%i"
        if not "!line:~0,1!"=="#" (
            set "%%i=%%j"
        )
    )
)
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
