import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { getOrder, getTracking, OrderResponse, payOrder, TrackingSnapshot } from '../api/client';
import { formatOrderStatus, formatPaymentStatus, isAwaitingPayment, statusBadgeClass } from '../utils/orderStatus';

const POLL_MS = 4000;

export default function TrackOrderPage() {
  const { orderId: routeOrderId } = useParams<{ orderId?: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [orderIdInput, setOrderIdInput] = useState(routeOrderId ?? '');
  const [orderId, setOrderId] = useState(routeOrderId ?? '');
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [tracking, setTracking] = useState<TrackingSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [payError, setPayError] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);
  const [polling, setPolling] = useState(false);

  useEffect(() => {
    if (routeOrderId) {
      setOrderId(routeOrderId);
      setOrderIdInput(routeOrderId);
    }
  }, [routeOrderId]);

  useEffect(() => {
    if (!orderId || !isAuthenticated) return;

    let cancelled = false;
    let timer: ReturnType<typeof setInterval> | undefined;

    async function refresh() {
      try {
        const [orderData, trackingData] = await Promise.all([
          getOrder(orderId),
          getTracking(orderId).catch(() => null),
        ]);
        if (!cancelled) {
          setOrder(orderData);
          if (trackingData) setTracking(trackingData);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load order');
        }
      }
    }

    refresh();
    setPolling(true);
    timer = setInterval(refresh, POLL_MS);

    return () => {
      cancelled = true;
      setPolling(false);
      if (timer) clearInterval(timer);
    };
  }, [orderId, isAuthenticated]);

  async function handlePayNow() {
    if (!orderId || paying) return;
    setPaying(true);
    setPayError(null);
    try {
      const updated = await payOrder(orderId);
      setOrder(updated);
    } catch (err) {
      setPayError(err instanceof Error ? err.message : 'Payment failed');
    } finally {
      setPaying(false);
    }
  }

  function handleLookup(e: FormEvent) {
    e.preventDefault();
    const trimmed = orderIdInput.trim();
    if (!trimmed) return;
    setOrderId(trimmed);
    navigate(`/track/${trimmed}`);
  }

  const awaitingPayment = order ? isAwaitingPayment(order.status) : false;

  return (
    <>
      <h1 className="page-title">Track your order</h1>
      <p className="page-subtitle">
        Live driver location refreshes every {POLL_MS / 1000}s once the order is out for delivery.
      </p>

      <form className="toolbar" onSubmit={handleLookup}>
        <div className="form-row" style={{ flex: 2 }}>
          <label htmlFor="orderId">Order ID</label>
          <input
            id="orderId"
            placeholder="Paste order UUID"
            value={orderIdInput}
            onChange={(e) => setOrderIdInput(e.target.value)}
          />
        </div>
        <button type="submit">Track</button>
      </form>

      {error && <div className="error-banner">{error}</div>}

      {!isAuthenticated && (
        <div className="error-banner" style={{ borderColor: 'var(--accent)', color: 'var(--text)' }}>
          <Link to="/login" state={{ from: orderId ? `/track/${orderId}` : '/track' }}>
            Sign in
          </Link>{' '}
          to view your order status and live driver tracking.
        </div>
      )}

      {order && (
        <div className="card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div>
              <h2 style={{ margin: 0, fontSize: '1.1rem' }}>Order {order.orderId}</h2>
              <p className="muted" style={{ margin: '0.35rem 0' }}>
                {order.city} · {order.deliveryAddressLine1}
              </p>
            </div>
            <span className={statusBadgeClass(order.status)}>{formatOrderStatus(order.status)}</span>
          </div>
          <p style={{ margin: '0.75rem 0 0' }}>
            Payment status: <strong>{formatPaymentStatus(order.paymentStatus)}</strong>
            {order.totalAmount != null && <> · Total ₹{Number(order.totalAmount).toFixed(2)}</>}
          </p>

          {awaitingPayment && (
            <div style={{ marginTop: '1rem', padding: '0.75rem', borderRadius: 8, background: 'var(--surface-hover)', border: '1px solid var(--border)' }}>
              {order.status === 'PENDING_PAYMENT' && (
                <p className="muted" style={{ margin: '0 0 0.75rem' }}>
                  Payment is processed automatically after checkout. If it stays pending, use Pay now.
                </p>
              )}
              {order.status === 'PAYMENT_FAILED' && (
                <p className="muted" style={{ margin: '0 0 0.75rem' }}>
                  The last payment attempt failed. You can retry below.
                </p>
              )}
              {payError && <div className="error-banner" style={{ marginBottom: '0.75rem' }}>{payError}</div>}
              <button type="button" onClick={handlePayNow} disabled={paying}>
                {paying ? 'Processing payment…' : `Pay now${order.totalAmount != null ? ` · ₹${Number(order.totalAmount).toFixed(2)}` : ''}`}
              </button>
            </div>
          )}

          {order.status === 'CONFIRMED' && order.paymentStatus === 'SUCCESS' && (
            <p className="muted" style={{ marginTop: '0.75rem' }}>
              Payment is complete and the order is confirmed. An admin will move it to{' '}
              <strong>Preparing</strong> when the kitchen starts.
            </p>
          )}

          {polling && (
            <p className="muted" style={{ marginTop: '0.5rem' }}>
              Auto-refreshing…
            </p>
          )}
        </div>
      )}

      {tracking && tracking.latitude != null && (
        <div className="card">
          <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Driver location</h2>
          <p>
            Lat {Number(tracking.latitude).toFixed(5)}, Lng {Number(tracking.longitude).toFixed(5)}
            {tracking.heading != null && <> · Heading {Number(tracking.heading).toFixed(0)}°</>}
          </p>
          <div
            style={{
              marginTop: '1rem',
              height: 180,
              borderRadius: 8,
              background: `radial-gradient(circle at ${50 + (Number(tracking.longitude) % 10) * 3}% ${50 + (Number(tracking.latitude) % 10) * 3}%, var(--accent-soft), var(--surface-hover))`,
              border: '1px solid var(--border)',
              display: 'grid',
              placeItems: 'center',
            }}
          >
            <span style={{ fontSize: '2rem' }}>🛵</span>
          </div>
          {tracking.timestamp && (
            <p className="muted" style={{ marginTop: '0.75rem' }}>
              Updated {new Date(tracking.timestamp).toLocaleTimeString()}
            </p>
          )}
        </div>
      )}

      {order && order.status !== 'OUT_FOR_DELIVERY' && order.status !== 'DELIVERED' && (
        <p className="muted" style={{ marginTop: '1rem' }}>
          Driver map appears after admin moves the order to OUT_FOR_DELIVERY.
        </p>
      )}
    </>
  );
}
