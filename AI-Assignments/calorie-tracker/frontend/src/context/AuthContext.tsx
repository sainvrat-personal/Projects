
import React, { useCallback } from 'react';
import { AuthContext } from './AuthContextValue';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const logout = useCallback(() => {
    sessionStorage.removeItem('accessToken');
    window.location.href = '/';
  }, []);

  return (
    <AuthContext.Provider value={{ logout }}>
      {children}
    </AuthContext.Provider>
  );
};
