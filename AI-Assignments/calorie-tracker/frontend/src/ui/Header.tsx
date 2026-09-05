import React from 'react';
import { ThemeToggle } from './index';
import TestDataButton from '../components/TestDataButton';
import './Header.css';

interface HeaderProps {
  token: string | null;
  onLogout: () => void;
}

const Header: React.FC<HeaderProps> = ({ token, onLogout }) => (
  <header className="header">
    <div className="header__brand">
      <img
        src="/calorie-logo.svg"
        alt="Calorie Tracker Logo"
        className="header__logo"
      />
      <span className="header__title">Calorie Tracker</span>
    </div>
    <div className="header__actions">
      {token && <TestDataButton token={token} />}
      <ThemeToggle />
      {token && (
        <button
          className="logout-btn"
          onClick={onLogout}
        >
          Logout
        </button>
      )}
    </div>
  </header>
);

export default Header;
