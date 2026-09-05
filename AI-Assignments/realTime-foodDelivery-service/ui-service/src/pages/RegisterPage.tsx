import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    addressLine1: '',
    addressLine2: '',
    city: 'Pune',
    state: 'Maharashtra',
    pincode: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  function updateField(field: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await register(form);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Create account</h1>
      <p className="page-subtitle">Register to browse, order, and track deliveries.</p>

      <form className="card" style={{ maxWidth: 560 }} onSubmit={handleSubmit}>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="name">Full name</label>
          <input id="name" value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
        </div>

        <div className="form-row">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={(e) => updateField('email', e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="phone">Phone (10 digits)</label>
          <input
            id="phone"
            inputMode="numeric"
            pattern="[0-9]{10}"
            value={form.phone}
            onChange={(e) => updateField('phone', e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="register-password">Password (min 8 characters)</label>
          <input
            id="register-password"
            type="password"
            autoComplete="new-password"
            minLength={8}
            value={form.password}
            onChange={(e) => updateField('password', e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="addressLine1">Delivery address</label>
          <input
            id="addressLine1"
            value={form.addressLine1}
            onChange={(e) => updateField('addressLine1', e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="addressLine2">Apartment / landmark (optional)</label>
          <input
            id="addressLine2"
            value={form.addressLine2}
            onChange={(e) => updateField('addressLine2', e.target.value)}
          />
        </div>

        <div className="toolbar" style={{ marginBottom: 0 }}>
          <div className="form-row" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="city">City</label>
            <input id="city" value={form.city} onChange={(e) => updateField('city', e.target.value)} required />
          </div>
          <div className="form-row" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="state">State</label>
            <input id="state" value={form.state} onChange={(e) => updateField('state', e.target.value)} />
          </div>
          <div className="form-row" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="pincode">Pincode</label>
            <input id="pincode" value={form.pincode} onChange={(e) => updateField('pincode', e.target.value)} />
          </div>
        </div>

        <button type="submit" disabled={submitting} style={{ width: '100%', marginTop: '1rem' }}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>

        <p className="muted" style={{ marginTop: '1rem', marginBottom: 0 }}>
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </>
  );
}
