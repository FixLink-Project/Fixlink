# FixLink - Branch Protection Rules & Merge Policies

Tài liệu quy định và hướng dẫn cấu hình **Branch Protection Rules** trên nhánh `main` của GitHub Repository, đảm bảo mọi đoạn mã trước khi được tích hợp đều phải vượt qua toàn bộ quy trình kiểm thử tự động (CI Pipeline).

---

## 1. Mục Tiêu Chính Sách

- **Ngăn chặn code lỗi**: Không cho phép code chưa pass test được merge vào nhánh chính.
- **Bắt buộc Peer Review**: Mọi thay đổi đều phải thông qua Pull Request có ít nhất 1 kỹ sư duyệt.
- **Bảo toàn tính ổn định của Staging**: Vì mỗi lần merge vào `main` sẽ tự động trigger deploy sang Staging, code trên `main` bắt buộc phải luôn ở trạng thái sẵn sàng chạy production.

---

## 2. Hướng Dẫn Cấu Hình trên GitHub Repository

Vào mục **Settings > Branches > Add branch ruleset** (hoặc **Branch protection rules**):

### Cấu hình cơ bản:
1. **Branch name pattern**: `main`
2. **Protect matching branches**: Bật (Checked)

### Các thiết lập bắt buộc (Mandatory Rules):

| Thiết lập trên GitHub UI | Giá trị khuyến nghị | Mục đích kỹ thuật |
| :--- | :--- | :--- |
| **Require a pull request before merging** | ✅ Bật | Chặn push trực tiếp vào `main`, bắt buộc tạo PR |
| **Required approvals** | `1` reviewer | Yêu cầu ít nhất 1 thành viên review code |
| **Dismiss stale pull request approvals when new commits are pushed** | ✅ Bật | Nếu có commit mới được push, bắt buộc review lại |
| **Require status checks to pass before merging** | ✅ Bật | Chặn merge nếu bất kỳ job CI nào bị fail |
| **Status checks that are required** | Chọn 3 jobs sau:<br>• `Backend Build, Lint & Test`<br>• `Frontend Build, Lint & Test`<br>• `Verify Container Image Builds` | Bảo đảm cả Backend, Frontend và Docker build đều thành công 100% |
| **Require branches to be up to date before merging** | ✅ Bật | Nhánh PR phải được rebase/merge với `main` mới nhất |
| **Require conversation resolution before merging** | ✅ Bật | Toàn bộ comment thảo luận trên PR phải được giải quyết |
| **Do not allow bypassing the above settings** | ✅ Bật | Áp dụng chính sách này cho cả Quản trị viên (Admin) |
| **Allow force pushes** | ❌ Tắt (Disabled) | Tuyệt đối cấm force push (`git push -f`) vào `main` |
| **Allow deletions** | ❌ Tắt (Disabled) | Chống xóa nhầm nhánh `main` |

---

## 3. Quy Trình Làm Việc Tiêu Chuẩn (Git Flow)

```
[ Developer Branch: feature/fix-xyz ]
                │
                ▼ (1. Push commits)
[ Pull Request to 'main' ]
                │
                ├───► Trigger GitHub Action: FixLink Continuous Integration (CI)
                │     ├── Job: Backend Build, Lint & Test (PASS)
                │     ├── Job: Frontend Build, Lint & Test (PASS)
                │     └── Job: Verify Container Image Builds (PASS)
                │
                ▼ (2. Peer Review Approval)
[ PR Approved & Status Checks Green ]
                │
                ▼ (3. Merge to 'main')
[ Branch: 'main' ]
                │
                ▼ Trigger GitHub Action: FixLink CD - Deploy to Staging
                └───► Build Release Images -> GHCR -> Deploy Staging Server
```
