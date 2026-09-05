import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAdminKey, setAdminKey } from '../api/client';

interface Props {
  overview?: boolean;
}

export default function SettingsPage({ overview }: Props) {
  const [key, setKey] = useState(getAdminKey());
  const [saved, setSaved] = useState(false);

  function handleSave(e: FormEvent) {
    e.preventDefault();
    setAdminKey(key.trim());
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  }

  if (overview) {
    return (
      <>
        <h1 className="page-title">Operations dashboard</h1>
        <p className="page-subtitle">
          Manage restaurant onboarding, kitchen order states, and analytics data import.
        </p>
        <div className="grid-2">
          <Link to="/restaurants" className="card">
            <strong>Restaurants</strong>
            <p className="muted">Onboard, approve, and manage menus</p>
          </Link>
          <Link to="/orders" className="card">
            <strong>Orders</strong>
            <p className="muted">Advance kitchen and delivery states</p>
          </Link>
          <Link to="/customers" className="card">
            <strong>Customers</strong>
            <p className="muted">Activate or deactivate user accounts</p>
          </Link>
          <Link to="/data" className="card">
            <strong>Analytics data</strong>
            <p className="muted">Import Assignment 2 sample CSV</p>
          </Link>
          <Link to="/settings" className="card">
            <strong>Settings</strong>
            <p className="muted">Configure admin API key</p>
          </Link>
        </div>
      </>
    );
  }

  return (
    <>
      <h1 className="page-title">Settings</h1>
      <p className="page-subtitle">Admin API key is sent as header X-Admin-Api-Key on all admin routes.</p>
      {saved && <div className="success-banner">Saved to browser localStorage.</div>}
      <form className="card" onSubmit={handleSave} style={{ maxWidth: 480 }}>
        <div className="form-row">
          <label htmlFor="adminKey">Admin API key</label>
          <input id="adminKey" value={key} onChange={(e) => setKey(e.target.value)} />
        </div>
        <button type="submit">Save key</button>
      </form>
    </>
  );
}
