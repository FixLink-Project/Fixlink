import { Route, Routes } from 'react-router-dom';
import CustomerRegisterPage from './pages/CustomerRegisterPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/dang-ky" element={<CustomerRegisterPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
