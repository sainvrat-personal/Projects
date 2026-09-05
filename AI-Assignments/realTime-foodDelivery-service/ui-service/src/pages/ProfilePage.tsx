import { FormEvent, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { updateProfile } from '../api/client';

export default function ProfilePage() {
  const { session, isAuthenticated, refreshProfile, logout } = useAuth();
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    pincode: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (session?.profile) {
      const profile = session.profile;
      setForm({
        name: profile.name,
        email: profile.email,
        phone: profile.phone,
        addressLine1: profile.addressLine1,
        addressLine2: profile.addressLine2 ?? '',
        city: profile.city,
        state: profile.state ?? 'Maharashtra',
        pincode: profile.pincode ?? '',
      });
    }
  }, [session]);

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: '/profile' }} replace />;
  }

  function updateField(field: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      await updateProfile(form);
      await refreshProfile();
      setSuccess('Profile updated.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Your profile</h1>
      <p className="page-subtitle">Manage delivery details used when you place orders.</p>

      <form className="card" style={{ maxWidth: 560 }} onSubmit={handleSubmit}>
        {error && <div className="error-banner">{error}</div>}
        {success && <div className="success-banner">{success}</div>}

        <div className="form-row">
          <label htmlFor="profile-name">Full name</label>
          <input id="profile-name" value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
        </div>
        <div className="form-row">
          <label htmlFor="profile-email">Email</label>
          <input
            id="profile-email"
            type="email"
            value={form.email}
            onChange={(e) => updateField('email', e.target.value)}
            required
          />
        </div>
        <div className="form-row">
          <label htmlFor="profile-phone">Phone</label>
          <input
            id="profile-phone"
            value={form.phone}
            onChange={(e) => updateField('phone', e.target.value)}
            required
          />
        </div>
        <div className="form-row">
          <label htmlFor="profile-address">Delivery address</label>
          <input
            id="profile-address"
            value={form.addressLine1}
            onChange={(e) => updateField('addressLine1', e.target.value)}
            required
          />
        </div>
        <div className="form-row">
          <label htmlFor="profile-address2">Apartment / landmark</label>
          <input
            id="profile-address2"
            value={form.addressLine2}
            onChange={(e) => updateField('addressLine2', e.target.value)}
          />
        </div>
        <div className="toolbar" style={{ marginBottom: 0 }}>
          <div className="form-row" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="profile-city">City</label>
            <input id="profile-city" value={form.city} onChange={(e) => updateField('city', e.target.value)} required />
          </div>
          <div className="form-row" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="profile-state">State</label>
            <input id="profile-state" value={form.state} onChange={(e) => updateField('state', e.target.value)} />
          </div>
          <div className="form-row" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="profile-pincode">Pincode</label>
            <input id="profile-pincode" value={form.pincode} onChange={(e) => updateField('pincode', e.target.value)} />
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
          <button type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save changes'}
          </button>
          <button type="button" className="secondary" onClick={logout}>
            Sign out
          </button>
        </div>
      </form>

      <p className="muted" style={{ marginTop: '1rem' }}>
        <Link to="/">← Back to restaurants</Link>
      </p>
    </>
  );
}
