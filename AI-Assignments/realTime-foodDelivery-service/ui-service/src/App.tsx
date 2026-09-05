import { NavLink, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';
import MyOrdersPage from './pages/MyOrdersPage';
import RestaurantPage from './pages/RestaurantPage';
import TrackOrderPage from './pages/TrackOrderPage';

export default function App() {
  const { session, isAuthenticated, logout } = useAuth();

  return (
    <div className="layout">
      <header className="header">
        <NavLink to="/" className="logo">
          <span className="logo-icon">🍽</span>
          SwiftEats
        </NavLink>
        <nav className="nav-links">
          <NavLink to="/" end>
            Restaurants
          </NavLink>
          <NavLink to="/track">Track Order</NavLink>
          {isAuthenticated && <NavLink to="/orders">My Orders</NavLink>}
          {isAuthenticated ? (
            <>
              <NavLink to="/profile">{session?.profile.name ?? 'Profile'}</NavLink>
              <button type="button" className="secondary" onClick={logout} style={{ padding: '0.4rem 0.85rem' }}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login">Sign in</NavLink>
              <NavLink to="/register">Register</NavLink>
            </>
          )}
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/orders" element={<MyOrdersPage />} />
        <Route path="/restaurants/:id" element={<RestaurantPage />} />
        <Route path="/track" element={<TrackOrderPage />} />
        <Route path="/track/:orderId" element={<TrackOrderPage />} />
      </Routes>
    </div>
  );
}
