@echo off
chcp 65001 > nul
echo ===============================================================================
echo                       FIXLINK - KHỞI CHẠY MÔI TRƯỜNG PHÁT TRIỂN
echo ===============================================================================
echo.
echo   Script này khởi chạy backend. Giao diện chạy riêng bằng lệnh:
echo       cd frontend ^&^& npm run dev
echo.
echo   * Giao diện (Vite dev server):  http://localhost:5173
echo   * Swagger API Docs:             http://localhost:8080/swagger-ui.html
echo   * H2 Database Console:          http://localhost:8080/h2-console
echo                                   (JDBC: jdbc:h2:mem:fixlink_db, user: sa, mật khẩu để trống)
echo.
echo   Đang khởi chạy backend FixLink... Nhấn Ctrl+C để dừng.
echo ===============================================================================
cd /d "%~dp0backend"
if not exist "target\fixlink-backend-0.0.1-SNAPSHOT.jar" (
    echo Đang đóng gói backend...
    call mvnw.cmd package -DskipTests
)
java -Xmx1024m -Dspring.profiles.active=local -jar "target\fixlink-backend-0.0.1-SNAPSHOT.jar"
pause
