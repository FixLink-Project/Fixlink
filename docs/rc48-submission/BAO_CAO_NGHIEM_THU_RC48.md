# Acceptance & Verification Report: Jira RC-48
**Project:** FixLink - Home Appliance Repair Service Platform  
**Sprint:** Sprint 2  
**Story ID:** `RC-48: Appointment Status Lifecycle`  
**Parent Story:** `RC-43: Quote Approval and Appointment`  
**Status:** `DONE` (100% Acceptance Criteria Met)

---

## 1. Executive Summary & Definition of Done

* **Goal:** Implement a comprehensive Appointment entity and Status Lifecycle State Machine between Customers and Technicians following quotation acceptance. The lifecycle manages:
  1. **Appointment Creation** (`CONFIRMED`) with future date and time validation.
  2. **Rescheduling** (`RESCHEDULED`) allowing participants to postpone with new date/time and notes.
  3. **Completion** (`COMPLETED`) marking work/inspection done on-site.
  4. **Cancellation** (`CANCELLED`) with mandatory cancellation reason.
  5. **Terminal State Integrity**: Rejects state mutation once an appointment reaches `COMPLETED` or `CANCELLED`.
  6. **Authorization & Privacy (IDOR)**: Ensures only involved customers, assigned technicians, or administrators can view or mutate appointments.
* **Definition of Done Checklist:**
  - [x] Database migration: Flyway `V7__create_appointments_table.sql` created with constraints and foreign keys.
  - [x] Domain model: `AppointmentStatus.java` state machine with `canTransitionTo()` and `isTerminal()`.
  - [x] Persistence layer: `AppointmentJpaEntity.java` and `SpringDataAppointmentRepository.java`.
  - [x] Application layer: `AppointmentUseCase.java` and `AppointmentService.java` with business rule validations.
  - [x] Web controller: `AppointmentController.java` with RESTful lifecycle endpoints (`POST`, `GET`, `PATCH /reschedule`, `PATCH /complete`, `PATCH /cancel`).
  - [x] Frontend UI: `AppointmentCard.tsx` with dynamic status badges, reschedule modal, complete prompt, and cancel modal. Integrated into both `RepairRequestDetailPage.tsx` and `TechnicianRequestDetailPage.tsx`.
  - [x] 100% automated integration test coverage (89/89 total backend tests passed).
  - [x] TypeScript clean check (`tsc --noEmit`) and production bundle build succeeded.

---

## 2. State Machine Diagram

```
        ┌──────────────────────────────────────────────────┐
        │  Create Appointment (Customer / Technician)     │
        └─────────────────────────┬────────────────────────┘
                                  │
                                  ▼
                        ┌───────────────────┐
            ┌───────────┤     CONFIRMED     ├───────────┐
            │           └─────────┬─────────┘           │
            │                     │                     │
            │ [Reschedule]        │ [Complete]          │ [Cancel + Reason]
            ▼                     ▼                     ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│    RESCHEDULED    ├───┤     COMPLETED     │   │     CANCELLED     │
└─────────┬─────────┘   └───────────────────┘   └───────────────────┘
          │ (Terminal)        (Terminal)
          │ [Cancel + Reason]
          └─────────────────────────────────────────────▲
```

---

## 3. Acceptance Criteria Verification Matrix

| AC # | Acceptance Criteria | Implementation Detail | Status |
| :--- | :--- | :--- | :---: |
| **AC 1** | Initial appointment creation defaults to `CONFIRMED`. | Enforced in `AppointmentService` & DB default column. | **PASSED** |
| **AC 2** | Appointment date and time must be present and in the future. | Validated in `AppointmentService.validateDateTimeInFuture`. Past dates return `400 Bad Request`. | **PASSED** |
| **AC 3** | Rescheduling transitions status from `CONFIRMED`/`RESCHEDULED` to `RESCHEDULED`. | Enforced via `status.canTransitionTo(RESCHEDULED)` with future date check. | **PASSED** |
| **AC 4** | Technicians/Customers can mark appointment as `COMPLETED`. | Transitions to terminal `COMPLETED` state; notes recorded. | **PASSED** |
| **AC 5** | Either participant can cancel with mandatory reason. | Transitions to terminal `CANCELLED` state; reason validated (`NotBlank`). | **PASSED** |
| **AC 6** | Terminal state protection: Cannot alter `COMPLETED` or `CANCELLED` appointments. | State machine checks `isTerminal()`; returns `400 Bad Request`. | **PASSED** |
| **AC 7** | Access control & IDOR prevention: Unrelated users cannot view or alter appointments. | Validates ownership against `customerId` & `technicianId`; returns `403 Forbidden`. | **PASSED** |
| **AC 8** | Interactive Frontend UI integration for both Customer and Technician views. | `AppointmentCard.tsx` with action buttons, modals, and real-time updates. | **PASSED** |

---

## 4. Architecture & Key Files

| Layer | File Path | Description |
| :--- | :--- | :--- |
| **Migration** | [`V7__create_appointments_table.sql`](file:///d:/đồ%20án%20spring2/backend/src/main/resources/db/migration/V7__create_appointments_table.sql) | DDL for `appointments` table with FKs & indexes |
| **Domain** | [`AppointmentStatus.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/domain/model/AppointmentStatus.java) | State machine enum with transition rules |
| **Entity** | [`AppointmentJpaEntity.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/out/persistence/entity/AppointmentJpaEntity.java) | JPA entity mapped to `appointments` table |
| **Repository** | [`SpringDataAppointmentRepository.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/out/persistence/repository/SpringDataAppointmentRepository.java) | Query methods by request, customer, and technician |
| **Port** | [`AppointmentUseCase.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/port/in/AppointmentUseCase.java) | Application port interface and command DTOs |
| **Service** | [`AppointmentService.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/service/AppointmentService.java) | Core business logic, validation, IDOR checks |
| **Web** | [`AppointmentController.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/in/web/controller/AppointmentController.java) | REST endpoints for appointment lifecycle operations |
| **Integration Test** | [`AppointmentLifecycleIntegrationTest.java`](file:///d:/đồ%20án%20spring2/backend/src/test/java/com/fixlink/adapter/in/web/controller/AppointmentLifecycleIntegrationTest.java) | 8 comprehensive automated integration tests |
| **Frontend Component** | [`AppointmentCard.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/components/AppointmentCard.tsx) | Status badges, reschedule, complete, cancel modals |
| **Customer Detail Page** | [`RepairRequestDetailPage.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/pages/RepairRequestDetailPage.tsx) | Customer view with appointment booking & card |
| **Technician Detail Page** | [`TechnicianRequestDetailPage.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/pages/TechnicianRequestDetailPage.tsx) | Technician view with appointment card & lifecycle actions |

---

## 5. Test Results & Verification

### 5.1 Automated Integration Tests
* Dedicated test class: `AppointmentLifecycleIntegrationTest.java` (8/8 passed).
* **Full Backend Test Suite Execution:**
```text
[INFO] Results:
[INFO] Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 21.743 s
```

### 5.2 Frontend Static Analysis & Build
* `npm run typecheck` $\rightarrow$ **0 errors**.
* `npm run build` $\rightarrow$ **Success** (Production bundle built in 8.78s).

---

## 6. Screenshot Evidence (Placeholders for Submission)

> *Place your captured screenshots in this folder and link them below:*

### Screenshot 1: Appointment Created with Initial Status (`CONFIRMED`)
*Shows the appointment card with the blue "Đã xác nhận" status badge, scheduled date/time, and participant information.*
```markdown
![Appointment Confirmed](./screenshot_01_appointment_confirmed.png)
```

### Screenshot 2: Rescheduling Modal & Updated Status (`RESCHEDULED`)
*Shows the reschedule modal with date/time picker, followed by the amber "Đã dời lịch" status badge.*
```markdown
![Appointment Rescheduled](./screenshot_02_appointment_rescheduled.png)
```

### Screenshot 3: Appointment Completed (`COMPLETED`)
*Shows the green "Đã hoàn thành" status badge and locked lifecycle actions.*
```markdown
![Appointment Completed](./screenshot_03_appointment_completed.png)
```

### Screenshot 4: Appointment Cancelled with Mandatory Reason (`CANCELLED`)
*Shows the red "Đã hủy" status badge with the highlighted cancellation reason box.*
```markdown
![Appointment Cancelled](./screenshot_04_appointment_cancelled.png)
```

### Screenshot 5: Automated Integration Test Execution (100% Pass)
*Shows terminal execution of `mvnw test` passing 89/89 test cases.*
```markdown
![Integration Test Results](./screenshot_05_test_results.png)
```

---

## 7. Test Accounts & Navigation

| Environment | URL | Credentials | Role |
| :--- | :--- | :--- | :--- |
| **Customer Portal** | [http://localhost:5173/login](http://localhost:5173/login) | `customer01` / `Password@123` | Customer |
| **Technician Portal 1** | [http://localhost:5173/login](http://localhost:5173/login) | `tech01` / `Password@123` | Approved Technician (Request #1, #8, #9, #11) |
| **Technician Portal 2** | [http://localhost:5173/login](http://localhost:5173/login) | `tho_dien_lanh_01` / `Password@123` | Active Technician (Request #2, #4, #6, #10) |
| **Admin Portal** | [http://localhost:5173/login](http://localhost:5173/login) | `admin` / `Admin@123` | Administrator |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Bearer JWT Token | API Documentation |
