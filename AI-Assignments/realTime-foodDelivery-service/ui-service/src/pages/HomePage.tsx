import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listRestaurants, RestaurantSummary } from '../api/client';

export default function HomePage() {
  const [city, setCity] = useState('Pune');
  const [name, setName] = useState('');
  const [restaurants, setRestaurants] = useState<RestaurantSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    listRestaurants({ city, name, page: 0, size: 20 })
      .then((page) => {
        if (!cancelled) {
          setRestaurants(page.content);
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
  }, [city, name]);

  return (
    <>
      <h1 className="page-title">Find food near you</h1>
      <p className="page-subtitle">Browse restaurants across Maharashtra — cached for fast menu loads.</p>

      <div className="toolbar">
        <div className="form-row">
          <label htmlFor="city">City</label>
          <select id="city" value={city} onChange={(e) => setCity(e.target.value)}>
            <option value="Pune">Pune</option>
            <option value="Mumbai">Mumbai</option>
            <option value="Nagpur">Nagpur</option>
          </select>
        </div>
        <div className="form-row">
          <label htmlFor="name">Search name</label>
          <input
            id="name"
            placeholder="e.g. Misal"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p className="muted">Loading restaurants…</p>}

      {!loading && !error && restaurants.length === 0 && (
        <p className="muted">No restaurants found. Try another city.</p>
      )}

      <div className="card-grid">
        {restaurants.map((r) => (
          <Link key={r.id} to={`/restaurants/${r.id}`} className="card" style={{ display: 'block' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', gap: '0.5rem' }}>
              <h2 style={{ margin: 0, fontSize: '1.15rem' }}>{r.name}</h2>
              <span className={`badge ${r.isOpen ? 'open' : 'closed'}`}>
                {r.isOpen ? 'Open' : 'Closed'}
              </span>
            </div>
            <p className="muted" style={{ margin: '0.5rem 0' }}>
              {r.city} · ★ {r.rating.toFixed(1)} · ~{r.estimatedWaitMins} min
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
              {r.cuisines?.map((c) => (
                <span key={c} className="badge">
                  {c}
                </span>
              ))}
            </div>
          </Link>
        ))}
      </div>
    </>
  );
}
