import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import {
  createOrder,
  getMenu,
  MenuItem,
  RestaurantMenu,
} from '../api/client';

interface CartLine {
  item: MenuItem;
  quantity: number;
}

export default function RestaurantPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, session } = useAuth();
  const [menu, setMenu] = useState<RestaurantMenu | null>(null);
  const [cart, setCart] = useState<Record<string, CartLine>>({});
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('Pune');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (session?.profile) {
      setAddress(session.profile.addressLine1);
      setCity(session.profile.city);
    }
  }, [session]);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getMenu(id)
      .then((data) => {
        setMenu(data);
        setCity(data.city ?? 'Pune');
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  const cartLines = useMemo(() => Object.values(cart), [cart]);
  const cartTotal = cartLines.reduce((sum, line) => sum + line.item.price * line.quantity, 0);

  function addToCart(item: MenuItem) {
    setCart((prev) => {
      const existing = prev[item.id];
      return {
        ...prev,
        [item.id]: {
          item,
          quantity: (existing?.quantity ?? 0) + 1,
        },
      };
    });
  }

  function updateQty(itemId: string, delta: number) {
    setCart((prev) => {
      const line = prev[itemId];
      if (!line) return prev;
      const nextQty = line.quantity + delta;
      if (nextQty <= 0) {
        const { [itemId]: _, ...rest } = prev;
        return rest;
      }
      return { ...prev, [itemId]: { ...line, quantity: nextQty } };
    });
  }

  async function placeOrder() {
    if (!id || cartLines.length === 0) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/restaurants/${id}` } });
      return;
    }
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const order = await createOrder(
        {
          restaurantId: id,
          deliveryAddress: address,
          city,
          paymentMode: 'UPI',
          items: cartLines.map((l) => ({ menuItemId: l.item.id, quantity: l.quantity })),
        },
        `ui-${Date.now()}`,
      );
      setSuccess(`Order placed! ID: ${order.orderId}`);
      setTimeout(() => navigate(`/track/${order.orderId}`), 1200);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Order failed');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <p className="muted">Loading menu…</p>;
  if (!menu) return <div className="error-banner">{error ?? 'Restaurant not found'}</div>;

  return (
    <>
      <Link to="/" className="muted" style={{ display: 'inline-block', marginBottom: '1rem' }}>
        ← Back to restaurants
      </Link>
      <h1 className="page-title">{menu.name}</h1>
      <p className="page-subtitle">
        <span className={`badge ${menu.isOpen ? 'open' : 'closed'}`}>
          {menu.isOpen ? 'Open' : 'Closed'}
        </span>{' '}
        · Est. wait {menu.estimatedWaitMins ?? '—'} min
      </p>

      {error && <div className="error-banner">{error}</div>}
      {success && <div className="success-banner">{success}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '1.5rem', alignItems: 'start' }}>
        <div>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Menu</h2>
          <div className="card-grid" style={{ gridTemplateColumns: '1fr' }}>
            {menu.menuItems.map((item) => (
              <div key={item.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem' }}>
                <div>
                  <strong>{item.name}</strong>
                  {item.category && <span className="muted"> · {item.category}</span>}
                  <p className="muted" style={{ margin: '0.35rem 0' }}>₹{item.price.toFixed(2)}</p>
                  {!item.available && <span className="badge closed">Unavailable</span>}
                </div>
                <button disabled={!item.available || !menu.isOpen} onClick={() => addToCart(item)}>
                  Add
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="card" style={{ position: 'sticky', top: '1rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginTop: 0 }}>Your cart</h2>
          {cartLines.length === 0 && <p className="muted">Add items to continue</p>}
          {cartLines.map((line) => (
            <div key={line.item.id} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
              <span>
                {line.item.name} × {line.quantity}
              </span>
              <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                <button className="secondary" style={{ padding: '0.25rem 0.5rem' }} onClick={() => updateQty(line.item.id, -1)}>
                  −
                </button>
                <button className="secondary" style={{ padding: '0.25rem 0.5rem' }} onClick={() => updateQty(line.item.id, 1)}>
                  +
                </button>
              </div>
            </div>
          ))}
          {cartLines.length > 0 && (
            <>
              {!isAuthenticated && (
                <p className="muted" style={{ marginBottom: '0.75rem' }}>
                  <Link to="/login" state={{ from: `/restaurants/${id}` }}>
                    Sign in
                  </Link>{' '}
                  to place your order.
                </p>
              )}
              <p style={{ fontWeight: 600, borderTop: '1px solid var(--border)', paddingTop: '0.75rem' }}>
                Total: ₹{cartTotal.toFixed(2)}
              </p>
              <div className="form-row">
                <label htmlFor="address">Delivery address</label>
                <input id="address" value={address} onChange={(e) => setAddress(e.target.value)} />
              </div>
              <div className="form-row">
                <label htmlFor="order-city">City</label>
                <input id="order-city" value={city} onChange={(e) => setCity(e.target.value)} />
              </div>
              <button
                disabled={submitting || !menu.isOpen || !isAuthenticated}
                onClick={placeOrder}
                style={{ width: '100%' }}
              >
                {submitting ? 'Placing order…' : isAuthenticated ? 'Place order' : 'Sign in to order'}
              </button>
            </>
          )}
        </div>
      </div>
    </>
  );
}
