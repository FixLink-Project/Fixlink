# Acceptance & Verification Report: Jira RC-39
**Project:** FixLink - Home Appliance Repair Service Platform  
**Sprint:** Sprint 2  
**Story ID:** `RC-39: Technician View Request Detail`  
**Parent Story:** `RC-37: Technician Discovery and Quoting`  
**Status:** `DONE` (100% Acceptance Criteria Met)

---

## 1. Executive Summary & Definition of Done

* **Goal:** Enable verified technicians (`APPROVED` KYC) to open and inspect comprehensive repair request details (incident description, photos/videos gallery with modal zoom, category, service area, address, budget, and bidding deadline), while providing interactive quotation actions (submit quote, view/edit/withdraw quote) and strictly preserving quotation privacy.
* **Definition of Done Checklist:**
  - [x] Backend endpoint `GET /api/v1/repair-requests/{id}` authorized for verified technicians to inspect open requests (`BIDDING_OPEN`), assigned requests, or requests they quoted on.
  - [x] Quotation privacy enforced: Technicians only see their own quotation; competitor prices remain confidential.
  - [x] Security enforced: Unverified (`PENDING`) technicians are blocked (`403 Forbidden`).
  - [x] Dedicated Frontend page (`/tho/yeu-cau/:id`) with photo gallery zoom modal, interactive quotation form, quote status cards, and work progress timeline.
  - [x] 100% automated integration test coverage (81/81 tests passed with 0 failures).
  - [x] TypeScript clean check (`tsc --noEmit`) and production bundle build succeeded.

---

## 2. Acceptance Criteria Verification Matrix

| AC # | Acceptance Criteria | Implementation Detail | Status |
| :--- | :--- | :--- | :---: |
| **AC 1** | Verified technician can view full details of an open bidding request (`BIDDING_OPEN`) before quoting. | Backend permits verified technician access to `BIDDING_OPEN` requests. Returns title, description, category, area, address, budget, and media. | **PASSED** |
| **AC 2** | Technician only sees their own quotation; competitor quotations are strictly hidden. | Filtered in `RepairRequestService`: `quotationRepo.findByRequestIdAndTechnicianId(requestId, userId)`. | **PASSED** |
| **AC 3** | Interactive quotation actions: Technician can submit quote, edit quote, or withdraw quote on detail page. | Frontend integrates quotation submission form, existing quote status card, edit mode, and withdraw button. | **PASSED** |
| **AC 4** | Assigned technician can view progress timeline and customer details on assigned jobs. | Backend authorizes assigned technician (`technicianId == userId`). Timeline rendered via `workProgress`. | **PASSED** |
| **AC 5** | Strict role & KYC verification: Unverified technicians (`PENDING`) are blocked (`403 UNVERIFIED_TECHNICIAN`). | Backend validates `profile.getVerificationStatus() == APPROVED`. | **PASSED** |
| **AC 6** | Unauthorized requests: Technicians cannot view private customer drafts (`DRAFT`) or non-existent requests. | Backend returns `403 ACCESS_DENIED` for drafts and `404 NOT_FOUND` for invalid IDs. | **PASSED** |

---

## 3. Architecture & File Changes

```
┌─────────────────────────────────────────────────────────────┐
│ Frontend (React 18 + TypeScript + Tailwind CSS)             │
│ - TechnicianRequestDetailPage.tsx (Dedicated Detail View)   │
│ - TechnicianDashboardPage.tsx ("Xem chi tiết" Link & Button)│
│ - App.tsx (Registered route /tho/yeu-cau/:id)               │
└──────────────────────────┬──────────────────────────────────┘
                           │ GET /api/v1/repair-requests/{id}
┌──────────────────────────▼──────────────────────────────────┐
│ Backend (Spring Boot 3.3.4 - Hexagonal Architecture)         │
│ - Web: RepairRequestController.java                         │
│ - Application: RepairRequestService.java (getDetail logic)  │
│ - Persistence: SpringDataQuotationRepository.java           │
│ - Tests: RepairRequestTechnicianDetailIntegrationTest.java  │
└─────────────────────────────────────────────────────────────┘
```

* **Backend Changes:**
  * [`RepairRequestService.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/service/RepairRequestService.java):
    * Expanded `getDetail` to allow verified technicians to inspect `BIDDING_OPEN` requests, assigned requests, and their own quotations.
    * Enforced KYC check (`UNVERIFIED_TECHNICIAN`).
    * Filtered quotations list so technicians only see their own quote.
  * [`RepairRequestTechnicianDetailIntegrationTest.java`](file:///d:/đồ%20án%20spring2/backend/src/test/java/com/fixlink/adapter/in/web/controller/RepairRequestTechnicianDetailIntegrationTest.java):
    * 6 integration test cases covering AC 1 through AC 6.
* **Frontend Changes:**
  * [`TechnicianRequestDetailPage.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/pages/TechnicianRequestDetailPage.tsx) **[NEW]**:
    * Full detail view with incident description, photo gallery with zoom modal, quotation submission/management form, and progress timeline.
  * [`App.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/App.tsx):
    * Registered `/tho/yeu-cau/:id` route protected by `<RequireAuth roles={['TECHNICIAN']}>`.
  * [`TechnicianDashboardPage.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/pages/TechnicianDashboardPage.tsx):
    * Added "Xem chi tiết" button and clickable links on request cards to navigate to `/tho/yeu-cau/:id`.

---

## 4. Test Results & Verification

### 4.1 Automated Integration Tests
* Dedicated test suite: `RepairRequestTechnicianDetailIntegrationTest.java` (6 test cases).
* **Test Suite Result (Entire Backend):**
```text
[INFO] Results:
[INFO] Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4.2 Frontend Verification
* `npm run typecheck` $\rightarrow$ **0 errors**.
* `npm run build` $\rightarrow$ **Success** (Production bundle built in 2.17s).

---

## 5. Screenshot Evidence (Placeholders for Submission)

> *Place your captured screenshots in this folder and link them below:*

### Screenshot 1: Request Detail View for Technician
*Shows the full request detail page (`/tho/yeu-cau/:id`) with title, requestCode, incident description, and photo gallery.*
```markdown
![Technician Request Detail View](./screenshot_01_request_detail.png)
```

### Screenshot 2: Interactive Quotation Form
*Shows the quotation submission form where technician enters proposed solution, labor fee, and materials fee.*
```markdown
![Technician Submit Quotation Form](./screenshot_02_quote_form.png)
```

### Screenshot 3: Active Quotation Status & Management
*Shows the submitted quotation card with status `PENDING`, price breakdown, and "Chỉnh sửa" / "Rút báo giá" buttons.*
```markdown
![Technician Manage Quotation](./screenshot_03_manage_quote.png)
```

### Screenshot 4: Photo Gallery Modal Zoom
*Shows the enlarged photo preview modal when clicking on an incident picture.*
```markdown
![Incident Photo Zoom Modal](./screenshot_04_image_modal.png)
```

---

## 6. How to Test & Demo Directly

| Environment | URL | Credentials |
| :--- | :--- | :--- |
| **Frontend Web** | [http://localhost:5173](http://localhost:5173) | `tech01` / `Password@123` |
| **Backend API** | [http://localhost:8080](http://localhost:8080) | — |
| **Swagger UI** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | — |
| **H2 Database** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | JDBC: `jdbc:h2:mem:fixlinkdb`, User: `sa`, Pass: *(empty)* |
