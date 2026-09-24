# Acceptance & Verification Report: Jira RC-38
**Project:** FixLink - Home Appliance Repair Service Platform  
**Sprint:** Sprint 2  
**Story ID:** `RC-38: Technician Search Requests by Service Area`  
**Parent Story:** `RC-37: Technician Discovery and Quoting`  
**Status:** `DONE` (100% Acceptance Criteria Met)

---

## 1. Executive Summary & Definition of Done

* **Goal:** Enable verified technicians (`APPROVED` KYC) to browse, filter, and search open repair requests (`BIDDING_OPEN`) by specific Service Areas (Districts), with optional case-insensitive keyword search (`title`, `description`, `address`).
* **Definition of Done Checklist:**
  - [x] Backend API endpoint supports filtering by `areaId` and keyword `search`.
  - [x] Security enforced: Unverified (`PENDING`) technicians are blocked (`403 Forbidden`).
  - [x] Responsive Frontend Filter Toolbar with area dropdown, active tags, and empty state.
  - [x] 100% automated test coverage with 0 regressions (75/75 tests passed).
  - [x] Swagger OpenAPI documentation updated.

---

## 2. Acceptance Criteria Verification Matrix

| AC # | Acceptance Criteria | Implementation Detail | Status |
| :--- | :--- | :--- | :---: |
| **AC 1** | Verified technician can filter matching requests by a specific Service Area (`areaId`). | Backend filters `r.area.id = :areaId` for requests with `BIDDING_OPEN`. Frontend provides Service Area selector. | **PASSED** |
| **AC 2** | When no area is selected, system defaults to all service areas registered in technician's profile. | Default fallback queries `profile.getServiceAreas()`. | **PASSED** |
| **AC 3** | Technician can combine area filter with keyword search (matching title, description, or address). | JPQL `LOWER(title/description/address) LIKE :search` combined with `areaId`. | **PASSED** |
| **AC 4** | System displays a user-friendly Empty State when no requests match the selected area. | Backend returns `totalItems: 0`. UI displays clean empty state with a "Reset Filter" action button. | **PASSED** |
| **AC 5** | Strict role and KYC verification: Unverified technicians or customer accounts cannot access matching requests. | Throws `AppException(ErrorCode.UNVERIFIED_TECHNICIAN, HttpStatus.FORBIDDEN)`. | **PASSED** |

---

## 3. Architecture & File Changes

```
┌─────────────────────────────────────────────────────────────┐
│ Frontend (React 18 + TypeScript + Tailwind CSS)             │
│ - TechnicianDashboardPage.tsx (Filter toolbar & tags)       │
└──────────────────────────┬──────────────────────────────────┘
                           │ GET /api/v1/repair-requests/matching?areaId=...&search=...
┌──────────────────────────▼──────────────────────────────────┐
│ Backend (Spring Boot 3.3.4 - Hexagonal Architecture)         │
│ - Web: RepairRequestController.java                         │
│ - Application: RepairRequestService.java & UseCase.java     │
│ - Persistence: SpringDataRepairRequestRepository.java       │
└─────────────────────────────────────────────────────────────┘
```

* **Backend Changes:**
  * [`RepairRequestController.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/in/web/controller/RepairRequestController.java): Added `@RequestParam(required = false) Long areaId`.
  * [`RepairRequestService.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/service/RepairRequestService.java): Added area validation (`findByIdAndIsActiveTrue`), KYC check, and query delegation.
  * [`SpringDataRepairRequestRepository.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/out/persistence/repository/SpringDataRepairRequestRepository.java): Added `findMatchingForTechnicianByArea` JPQL query.
* **Frontend Changes:**
  * [`TechnicianDashboardPage.tsx`](file:///d:/đồ%20án%20spring2/frontend/src/pages/TechnicianDashboardPage.tsx): Integrated Service Area select dropdown, search input, quick-clear tags, and empty state.

---

## 4. Test Results & Verification

### 4.1 Automated Integration Tests
* Dedicated test suite: [`RepairRequestAreaSearchIntegrationTest.java`](file:///d:/đồ%20án%20spring2/backend/src/test/java/com/fixlink/adapter/in/web/controller/RepairRequestAreaSearchIntegrationTest.java) (6 test cases).
* **Test Suite Result:**
```text
[INFO] Results:
[INFO] Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4.2 Frontend Verification
* `npm run typecheck` $\rightarrow$ **0 errors**.
* `npm run build` $\rightarrow$ **Success** (Production bundle built in 2.21s).

---

## 5. Screenshot Evidence (Placeholders for Your Submission)

> *Place your captured screenshots in this folder and link them below:*

### Screenshot 1: Technician Dashboard with Service Area Filter Toolbar
*Shows the default matching requests list with the Service Area dropdown and Search box.*
```markdown
![Technician Dashboard Filter Toolbar](./screenshot_01_dashboard.png)
```

### Screenshot 2: Filtered by Specific Service Area
*Shows requests filtered by selecting a specific district (e.g., "Quận 1") with the active filter tag `📍 Quận 1 ✕`.*
```markdown
![Filter by Service Area](./screenshot_02_area_filter.png)
```

### Screenshot 3: Combined Area & Keyword Filter
*Shows dual filter tags (e.g., `📍 Quận 1 ✕` and `Keyword: "thoát sàn" ✕`) and matching result.*
```markdown
![Combined Area and Keyword Filter](./screenshot_03_combined_filter.png)
```

### Screenshot 4: Swagger UI API Specification
*Shows `GET /api/v1/repair-requests/matching` with parameters `areaId` and `search`.*
```markdown
![Swagger API Documentation](./screenshot_04_swagger.png)
```

---

## 6. How to Test & Demo Directly

| Environment | URL | Credentials |
| :--- | :--- | :--- |
| **Frontend Web** | [http://localhost:5173](http://localhost:5173) | `tech01` / `Password@123` |
| **Backend API** | [http://localhost:8080](http://localhost:8080) | — |
| **Swagger UI** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | — |
| **H2 Database** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | JDBC: `jdbc:h2:mem:fixlinkdb`, User: `sa`, Pass: *(empty)* |
