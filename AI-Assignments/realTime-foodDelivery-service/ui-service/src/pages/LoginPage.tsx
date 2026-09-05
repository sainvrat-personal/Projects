import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/';

  if (isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(loginId.trim(), password);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Sign in</h1>
      <p className="page-subtitle">
        Use your SwiftEats account to place and track orders. Demo:{' '}
        <code>demo.customer@example.com</code> / <code>Demo@123</code>
      </p>

      <form className="card" style={{ maxWidth: 480 }} onSubmit={handleSubmit}>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="loginId">Email or phone</label>
          <input
            id="loginId"
            type="text"
            autoComplete="username"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            placeholder="you@example.com or 9876543210"
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <button type="submit" disabled={submitting} style={{ width: '100%' }}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="muted" style={{ marginTop: '1rem', marginBottom: 0 }}>
          New here? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </>
  );
}
