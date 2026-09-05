import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import {
  CustomerAuthResponse,
  CustomerProfile,
  getProfile,
  loginCustomer,
  registerCustomer,
  setCustomerSession,
} from '../api/client';

const STORAGE_KEY = 'swifteats.customer.session';

interface CustomerSession {
  customerId: string;
  apiToken: string;
  profile: CustomerProfile;
}

interface AuthContextValue {
  session: CustomerSession | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (loginId: string, password: string) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

export interface RegisterPayload {
  name: string;
  email: string;
  phone: string;
  password: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  pincode?: string;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function persistSession(session: CustomerSession | null) {
  if (session) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    setCustomerSession(session);
  } else {
    localStorage.removeItem(STORAGE_KEY);
    setCustomerSession(null);
  }
}

function readStoredSession(): CustomerSession | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CustomerSession;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

function toSession(response: CustomerAuthResponse): CustomerSession {
  return {
    customerId: response.customerId,
    apiToken: response.apiToken,
    profile: response.profile,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<CustomerSession | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = readStoredSession();
    setSession(stored);
    setCustomerSession(stored);
    setLoading(false);
  }, []);

  const applySession = useCallback((next: CustomerSession | null) => {
    setSession(next);
    persistSession(next);
  }, []);

  const login = useCallback(async (loginId: string, password: string) => {
    const response = await loginCustomer({ loginId, password });
    applySession(toSession(response));
  }, [applySession]);

  const register = useCallback(async (payload: RegisterPayload) => {
    const response = await registerCustomer(payload);
    applySession(toSession(response));
  }, [applySession]);

  const logout = useCallback(() => {
    applySession(null);
  }, [applySession]);

  const refreshProfile = useCallback(async () => {
    if (!session) return;
    const profile = await getProfile();
    const next = { ...session, profile };
    applySession(next);
  }, [applySession, session]);

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: session != null,
      loading,
      login,
      register,
      logout,
      refreshProfile,
    }),
    [session, loading, login, register, logout, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
