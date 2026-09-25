# CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
### Độc lập - Tự do - Hạnh phúc
---

# BÁO CÁO NGHIỆM THU KỸ THUẬT & CHỨC NĂNG HỆ THỐNG
## HẠNG MỤC: TÍNH NĂNG RC-47 - TẠO VÀ QUẢN LÝ LỊCH HẸN KHẢO SÁT & SỬA CHỮA
### (Customer & Technician Appointment Management - Ticket RC-47)
**Dự án:** Sàn Thương mại Dịch vụ Sửa chữa & Cứu hộ Kỹ thuật FixLink  
**Mã phiếu công việc (Task / Story ID):** `RC-47` (`RC47`)  
**Ngày nghiệm thu:** 25/09/2026  
**Địa điểm thực hiện:** Phòng Quản lý Chất lượng & Đảm bảo Phần mềm Dự án FixLink  

---

## I. THÔNG TIN CHUNG VỀ HẠNG MỤC NGHIỆM THU

1. **Tên phân hệ:** Quản lý Lịch hẹn & Điều phối Khảo sát / Thi công (Appointments & Scheduling Engine).
2. **Mã tính năng & Mã phiếu công việc:** `RC-47` (Tạo và quản lý lịch hẹn khảo sát / sửa chữa giữa Khách hàng và Kỹ thuật viên).
3. **Mục tiêu tính năng:**
   - Cho phép Khách hàng hoặc Kỹ thuật viên thiết lập lịch hẹn trực tiếp cho đơn sửa chữa đã chốt báo giá hoặc cần khảo sát.
   - Thẩm định chặt chẽ thời gian hẹn: ngày và giờ hẹn bắt buộc phải ở thời điểm tương lai (`validateDateTimeInFuture`).
   - Ngăn chặn xung đột lịch làm việc: không cho phép tạo lịch hẹn chồng chéo khung giờ của cùng một kỹ thuật viên.
   - Quản lý trạng thái vòng đời lịch hẹn: `CONFIRMED` (Đã xác nhận), `RESCHEDULED` (Đã dời lịch), `COMPLETED` (Đã hoàn thành), `CANCELLED` (Đã hủy).
   - Bảo mật nghiêm ngặt chống can thiệp trái phép (Anti-IDOR): Chỉ các bên tham gia (Khách hàng tạo đơn, Kỹ thuật viên phụ trách) hoặc Quản trị viên (ADMIN) mới có quyền truy cập hoặc thay đổi lịch hẹn.
4. **Kiến trúc kỹ thuật:**
   - **Backend:** Java 21, Spring Boot 3.3.4, Spring Data JPA, Hibernate, Flyway Migration (`V7__create_appointments_table.sql`).
   - **Bảo mật:** Spring Security JWT Stateless RBAC (Role-Based Access Control).
   - **Cơ sở dữ liệu:** PostgreSQL / H2 Database test profile.

---

## II. KẾT QUẢ TRIỂN KHAI VÀ NGHIỆM THU MÃ NGUỒN

### 1. Danh sách các API Endpoints đã hoàn thành

| Phương thức | Đường dẫn API (Endpoint) | Quyền hạn (RBAC) | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/appointments` | Khách hàng / KTV / Admin | Tạo lịch hẹn mới (Trạng thái khởi tạo: `CONFIRMED`) |
| `GET` | `/api/v1/appointments/{id}` | Khách hàng / KTV / Admin | Xem thông tin chi tiết một lịch hẹn (bảo vệ Anti-IDOR) |
| `GET` | `/api/v1/repair-requests/{requestId}/appointments` | Các bên tham gia | Danh sách tất cả lịch hẹn thuộc về một yêu cầu sửa chữa |
| `GET` | `/api/v1/appointments/my` | Authenticated Users | Danh sách toàn bộ lịch hẹn của người dùng đang đăng nhập |
| `PATCH` | `/api/v1/appointments/{id}/reschedule` | Khách hàng / KTV / Admin | Dời lịch hẹn sang ngày giờ mới (`RESCHEDULED`) |
| `PATCH` | `/api/v1/appointments/{id}/complete` | Kỹ thuật viên / Admin | Xác nhận hoàn thành công việc tại chỗ (`COMPLETED`) |
| `PATCH` | `/api/v1/appointments/{id}/cancel` | Khách hàng / KTV / Admin | Hủy lịch hẹn kèm lý do bắt buộc (`CANCELLED`) |

### 2. Kết quả kiểm thử tự động (Automated Test Execution)

Toàn bộ các ca kiểm thử đơn vị (Unit Tests) và kiểm thử tích hợp (Integration Tests) cho phân hệ Lịch hẹn đều đạt tỷ lệ pass **100% (0 Failure, 0 Error)**:

* `AppointmentTest.java`: 16/16 ca kiểm thử miền (Domain model validation, cancellation reasons, state transitions) **PASSED**.
* `AppointmentServiceTest.java`: 10/10 ca kiểm thử tầng ứng dụng (Business rules, date validation, anti-IDOR, state machine) **PASSED**.
* `AppointmentLifecycleIntegrationTest.java`: 8/8 ca kiểm thử tích hợp vòng đời trọn vẹn qua MockMvc & JWT **PASSED**.

---

## III. KẾT LUẬN & XÁC NHẬN BÀN GIAO

* Tính năng **RC-47 (Lịch hẹn)** đã hoàn thiện trọn vẹn, đáp ứng 100% tiêu chí nghiệm thu đề ra.
* Mã nguồn sạch sẽ, không có xung đột, tương thích hoàn toàn với kiến trúc hệ sinh thái FixLink.
* Đã sẵn sàng tích hợp và triển khai phục vụ dự án.
