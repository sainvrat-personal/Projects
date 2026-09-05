import { Routes, Route } from 'react-router-dom';
import { LandingPage } from './pages/LandingPage';
import Login from './auth/Login';
import ResetPassword from './auth/ResetPassword';
import Signup from './auth/Signup';
import PaymentAcceptance from './pages/PaymentAcceptance';

export const App = () => {
  return (
      <div>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/resetPassword" element={<ResetPassword />} />
          <Route path="/updatePaymentStatus" element={<PaymentAcceptance />} />
        </Routes>
      </div>
  );
};

export default App;