import { Route, Routes } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import CustomerDashboardPage from './pages/CustomerDashboardPage';
import CustomerRegisterPage from './pages/CustomerRegisterPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import TechnicianDashboardPage from './pages/TechnicianDashboardPage';
import TechnicianRegisterPage from './pages/TechnicianRegisterPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/dang-ky" element={<CustomerRegisterPage />} />
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
        path="/tho"
        element={
          <RequireAuth roles={['TECHNICIAN']}>
            <TechnicianDashboardPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
