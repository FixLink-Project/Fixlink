import { Route, Routes } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import AdminCategoriesPage from './pages/AdminCategoriesPage';
import AdminUsersPage from './pages/AdminUsersPage';
import CreateRepairRequestPage from './pages/CreateRepairRequestPage';
import CustomerDashboardPage from './pages/CustomerDashboardPage';
import CustomerRegisterPage from './pages/CustomerRegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import MyRepairRequestsPage from './pages/MyRepairRequestsPage';
import NotFoundPage from './pages/NotFoundPage';
import RepairRequestDetailPage from './pages/RepairRequestDetailPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import TechnicianDashboardPage from './pages/TechnicianDashboardPage';
import TechnicianProfilePage from './pages/TechnicianProfilePage';
import TechnicianRegisterPage from './pages/TechnicianRegisterPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/dang-ky" element={<CustomerRegisterPage />} />
      <Route path="/quen-mat-khau" element={<ForgotPasswordPage />} />
      <Route path="/dat-lai-mat-khau" element={<ResetPasswordPage />} />
      <Route path="/dang-ky-tho" element={<TechnicianRegisterPage />} />
      <Route
        path="/khach-hang"
        element={
          <RequireAuth roles={['CUSTOMER']}>
            <CustomerDashboardPage />
          </RequireAuth>
        }
      />
      <Route
        path="/dang-yeu-cau"
        element={
          <RequireAuth roles={['CUSTOMER']}>
            <CreateRepairRequestPage />
          </RequireAuth>
        }
      />
      <Route
        path="/yeu-cau-cua-toi"
        element={
          <RequireAuth roles={['CUSTOMER']}>
            <MyRepairRequestsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/yeu-cau-cua-toi/:id"
        element={
          <RequireAuth roles={['CUSTOMER']}>
            <RepairRequestDetailPage />
          </RequireAuth>
        }
      />
      <Route
        path="/tho"
        element={
          <RequireAuth roles={['TECHNICIAN']}>
            <TechnicianDashboardPage />
          </RequireAuth>
        }
      />
      <Route
        path="/quan-tri"
        element={
          <RequireAuth roles={['ADMIN', 'STAFF']}>
            <AdminUsersPage />
          </RequireAuth>
        }
      />
      <Route
        path="/quan-tri/danh-muc"
        element={
          <RequireAuth roles={['ADMIN']}>
            <AdminCategoriesPage />
          </RequireAuth>
        }
      />
      <Route
        path="/tho/ho-so"
        element={
          <RequireAuth roles={['TECHNICIAN']}>
            <TechnicianProfilePage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
