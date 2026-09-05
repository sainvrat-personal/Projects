import { useCallback, useEffect, useState } from 'react';
import {
  AdminCustomerSummary,
  getAdminCustomer,
  listAdminCustomers,
  updateCustomerStatus,
} from '../api/client';

function formatTimestamp(value?: string): string {
  if (!value) return '—';
  return new Date(value).toLocaleString();
}

export default function CustomersPage() {
  const [customers, setCustomers] = useState<AdminCustomerSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'SUSPENDED'>('ALL');
  const [selectedId, setSelectedId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [actionId, setActionId] = useState<string | null>(null);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    const status = statusFilter === 'ALL' ? undefined : statusFilter;
    listAdminCustomers(status)
      .then(setCustomers)
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [statusFilter]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function handleStatusChange(customerId: string, status: 'ACTIVE' | 'SUSPENDED') {
    setActionId(customerId);
    setError(null);
    setSuccess(null);
    try {
      await updateCustomerStatus(customerId, status);
      setSuccess(status === 'ACTIVE' ? 'Customer activated.' : 'Customer deactivated.');
      if (selectedId === customerId && status === 'SUSPENDED') {
        setSelectedId('');
      }
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setActionId(null);
    }
  }

  async function handleSelect(customerId: string) {
    setSelectedId(customerId);
    setError(null);
    try {
      await getAdminCustomer(customerId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load customer');
    }
  }

  return (
    <>
      <h1 className="page-title">Customers</h1>
      <p className="page-subtitle">
        Activate or deactivate customer accounts. Deactivated users cannot sign in or place orders.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {success && <div className="success-banner">{success}</div>}

      <div className="toolbar">
        <div className="form-row">
          <label htmlFor="statusFilter">Status</label>
          <select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as 'ALL' | 'ACTIVE' | 'SUSPENDED')}
          >
            <option value="ALL">All</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Deactivated</option>
          </select>
        </div>
        <button type="button" onClick={reload}>Refresh</button>
      </div>

      {loading && <p className="muted">Loading customers…</p>}

      {!loading && customers.length === 0 && (
        <p className="muted">No customers found for this filter.</p>
      )}

      {!loading && customers.length > 0 && (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.92rem' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)' }}>
                <th style={{ padding: '0.5rem' }}>Name</th>
                <th style={{ padding: '0.5rem' }}>Email</th>
                <th style={{ padding: '0.5rem' }}>Phone</th>
                <th style={{ padding: '0.5rem' }}>City</th>
                <th style={{ padding: '0.5rem' }}>Status</th>
                <th style={{ padding: '0.5rem' }}>Created</th>
                <th style={{ padding: '0.5rem' }}>Updated</th>
                <th style={{ padding: '0.5rem' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {customers.map((customer) => (
                <tr
                  key={customer.id}
                  style={{
                    borderBottom: '1px solid var(--border)',
                    background: selectedId === customer.id ? 'var(--surface-hover)' : undefined,
                  }}
                >
                  <td style={{ padding: '0.5rem' }}>{customer.name}</td>
                  <td style={{ padding: '0.5rem' }}>{customer.email}</td>
                  <td style={{ padding: '0.5rem' }}>{customer.phone}</td>
                  <td style={{ padding: '0.5rem' }}>{customer.city ?? '—'}</td>
                  <td style={{ padding: '0.5rem' }}>
                    <span className={`badge status-${customer.status.toLowerCase()}`}>{customer.status}</span>
                  </td>
                  <td style={{ padding: '0.5rem' }}>{formatTimestamp(customer.createdAt)}</td>
                  <td style={{ padding: '0.5rem' }}>{formatTimestamp(customer.updatedAt)}</td>
                  <td style={{ padding: '0.5rem', whiteSpace: 'nowrap' }}>
                    <button
                      type="button"
                      style={{ marginRight: '0.35rem' }}
                      onClick={() => handleSelect(customer.id)}
                    >
                      Details
                    </button>
                    {customer.status === 'ACTIVE' ? (
                      <button
                        type="button"
                        disabled={actionId === customer.id}
                        onClick={() => handleStatusChange(customer.id, 'SUSPENDED')}
                      >
                        {actionId === customer.id ? 'Saving…' : 'Deactivate'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={actionId === customer.id}
                        onClick={() => handleStatusChange(customer.id, 'ACTIVE')}
                      >
                        {actionId === customer.id ? 'Saving…' : 'Activate'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedId && (
        <CustomerDetailPanel customerId={selectedId} onClose={() => setSelectedId('')} />
      )}
    </>
  );
}

function CustomerDetailPanel({ customerId, onClose }: { customerId: string; onClose: () => void }) {
  const [detail, setDetail] = useState<Awaited<ReturnType<typeof getAdminCustomer>> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminCustomer(customerId)
      .then(setDetail)
      .catch((err: Error) => setError(err.message));
  }, [customerId]);

  return (
    <div className="card" style={{ marginTop: '1rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0, fontSize: '1.05rem' }}>Customer detail</h2>
        <button type="button" onClick={onClose}>Close</button>
      </div>
      {error && <p className="muted" style={{ marginTop: '0.75rem' }}>{error}</p>}
      {detail && (
        <dl style={{ marginTop: '0.75rem', display: 'grid', gridTemplateColumns: '120px 1fr', gap: '0.35rem 1rem' }}>
          <dt className="muted">ID</dt>
          <dd style={{ margin: 0, fontFamily: 'monospace', fontSize: '0.85rem' }}>{detail.id}</dd>
          <dt className="muted">Name</dt>
          <dd style={{ margin: 0 }}>{detail.name}</dd>
          <dt className="muted">Email</dt>
          <dd style={{ margin: 0 }}>{detail.email}</dd>
          <dt className="muted">Phone</dt>
          <dd style={{ margin: 0 }}>{detail.phone}</dd>
          <dt className="muted">Address</dt>
          <dd style={{ margin: 0 }}>
            {detail.addressLine1}
            {detail.addressLine2 ? `, ${detail.addressLine2}` : ''}
            {detail.city ? ` · ${detail.city}` : ''}
            {detail.state ? `, ${detail.state}` : ''}
            {detail.pincode ? ` ${detail.pincode}` : ''}
          </dd>
          <dt className="muted">Status</dt>
          <dd style={{ margin: 0 }}>{detail.status}</dd>
          <dt className="muted">Created</dt>
          <dd style={{ margin: 0 }}>{formatTimestamp(detail.createdAt)}</dd>
          <dt className="muted">Updated</dt>
          <dd style={{ margin: 0 }}>{formatTimestamp(detail.updatedAt)}</dd>
        </dl>
      )}
    </div>
  );
}
