import { createContext } from 'react';

interface AuthContextType {
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType>({
  logout: () => {},
});
