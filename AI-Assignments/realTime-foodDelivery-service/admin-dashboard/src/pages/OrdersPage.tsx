import { FormEvent, useState } from 'react';
import { transitionOrder } from '../api/client';

const STATUSES = ['PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DELAYED', 'FAILED', 'CANCELLED'];

export default function OrdersPage() {
  const [orderId, setOrderId] = useState('');
  const [status, setStatus] = useState('PREPARING');
  const [changedBy, setChangedBy] = useState('admin-dashboard');
  const [reason, setReason] = useState('');
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleTransition(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const order = await transitionOrder(orderId.trim(), {
        status,
        changedBy,
        reason: reason || undefined,
      });
      setResult(`Order ${order.orderId} → ${order.status} (payment: ${order.paymentStatus})`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Transition failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Order operations</h1>
      <p className="page-subtitle">
        Advance kitchen and delivery workflow. Paste an order ID from the customer UI or API.
      </p>
      {error && <div className="error-banner">{error}</div>}
      {result && <div className="success-banner">{result}</div>}

      <form className="card" onSubmit={handleTransition} style={{ maxWidth: 520 }}>
        <div className="form-row">
          <label htmlFor="orderId">Order ID</label>
          <input id="orderId" value={orderId} onChange={(e) => setOrderId(e.target.value)} required />
        </div>
        <div className="form-row">
          <label htmlFor="status">Target status</label>
          <select id="status" value={status} onChange={(e) => setStatus(e.target.value)}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
        <div className="form-row">
          <label htmlFor="changedBy">Changed by</label>
          <input id="changedBy" value={changedBy} onChange={(e) => setChangedBy(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="reason">Reason (optional, for FAILED/DELAYED)</label>
          <input id="reason" value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
        <button type="submit" disabled={loading}>{loading ? 'Updating…' : 'Update state'}</button>
      </form>

      <div className="card muted">
        Order lifecycle: customer payment moves the order to <strong>CONFIRMED</strong> automatically.
        Admin then advances: CONFIRMED → <strong>PREPARING</strong> →{' '}
        <strong>OUT_FOR_DELIVERY</strong> (assigns driver) → <strong>DELIVERED</strong>.
        Enable GPS simulator in docker-compose for live tracking on the customer UI.
      </div>
    </>
  );
}
