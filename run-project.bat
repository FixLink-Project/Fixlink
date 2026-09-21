@echo off
chcp 65001 > nul
echo ===============================================================================
echo                       FIXLINK - KHỞI CHẠY MÔI TRƯỜNG PHÁT TRIỂN
echo ===============================================================================
echo.
echo   Script này chỉ chạy backend. Giao diện chạy riêng bằng lệnh:
echo       cd frontend ^&^& npm install ^&^& npm run dev
echo.
echo   * Giao diện (Vite dev server):  http://localhost:5173
echo   * Swagger API Docs:             http://localhost:8080/swagger-ui.html
echo   * H2 Database Console:          http://localhost:8080/h2-console
echo                                   (JDBC: jdbc:h2:mem:fixlink_db, user: sa, mật khẩu để trống)
echo.
echo   Đang biên dịch và khởi chạy backend... Nhấn Ctrl+C để dừng.
echo ===============================================================================
set MAVEN_OPTS=-Xmx1024m
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
pause
