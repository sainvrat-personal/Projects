import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { listMyOrders, OrderSummary } from '../api/client';
import { formatOrderDate, formatOrderStatus, formatPaymentStatus, isAwaitingPayment, statusBadgeClass } from '../utils/orderStatus';

function OrderCard({ order }: { order: OrderSummary }) {
  const orderId = String(order.orderId);

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', gap: '0.75rem', flexWrap: 'wrap' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: '1.05rem' }}>{order.restaurantName}</h2>
          <p className="muted" style={{ margin: '0.35rem 0 0' }}>
            {order.city} · {order.deliveryAddressLine1}
          </p>
        </div>
        <span className={statusBadgeClass(order.status)}>{formatOrderStatus(order.status)}</span>
      </div>

      <p style={{ margin: '0.75rem 0 0' }}>
        Total <strong>₹{Number(order.totalAmount).toFixed(2)}</strong>
        {order.paymentStatus && (
          <>
            {' '}
            · Payment <strong>{formatPaymentStatus(order.paymentStatus)}</strong>
          </>
        )}
      </p>

      <p className="muted" style={{ margin: '0.5rem 0 0', fontSize: '0.85rem' }}>
        Placed {formatOrderDate(order.createdAt)}
      </p>

      <p style={{ margin: '0.75rem 0 0', fontFamily: 'monospace', fontSize: '0.8rem', wordBreak: 'break-all' }}>
        {orderId}
      </p>

      <Link
        to={`/track/${orderId}`}
        style={{ display: 'inline-block', marginTop: '0.75rem', color: 'var(--accent)', fontWeight: 600 }}
      >
        {isAwaitingPayment(order.status) ? 'Complete payment →' : 'Track order →'}
      </Link>
    </div>
  );
}

function OrderSection({ title, orders, emptyMessage }: { title: string; orders: OrderSummary[]; emptyMessage: string }) {
  return (
    <section style={{ marginBottom: '2rem' }}>
      <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>{title}</h2>
      {orders.length === 0 ? (
        <p className="muted">{emptyMessage}</p>
      ) : (
        <div className="card-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))' }}>
          {orders.map((order) => (
            <OrderCard key={String(order.orderId)} order={order} />
          ))}
        </div>
      )}
    </section>
  );
}

export default function MyOrdersPage() {
  const { isAuthenticated } = useAuth();
  const [activeOrders, setActiveOrders] = useState<OrderSummary[]>([]);
  const [historyOrders, setHistoryOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    Promise.all([listMyOrders('active'), listMyOrders('history')])
      .then(([active, history]) => {
        if (!cancelled) {
          setActiveOrders(active);
          setHistoryOrders(history);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated]);

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: '/orders' }} replace />;
  }

  return (
    <>
      <h1 className="page-title">My orders</h1>
      <p className="page-subtitle">
        Active deliveries appear first. Copy an order ID or jump straight to live tracking.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p className="muted">Loading your orders…</p>}

      {!loading && !error && (
        <>
          <OrderSection
            title="Active orders"
            orders={activeOrders}
            emptyMessage="No active orders. Browse restaurants and place your first order."
          />
          <OrderSection
            title="History"
            orders={historyOrders}
            emptyMessage="Completed and cancelled orders will show up here."
          />
        </>
      )}

      {!loading && activeOrders.length === 0 && historyOrders.length === 0 && !error && (
        <Link to="/" className="card" style={{ display: 'inline-block', marginTop: '0.5rem' }}>
          Browse restaurants →
        </Link>
      )}
    </>
  );
}
