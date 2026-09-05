import { FormEvent, useCallback, useEffect, useState } from 'react';
import RestaurantSelect from '../components/RestaurantSelect';
import {
  addMenuItem,
  AdminRestaurantSummary,
  approveRestaurant,
  createRestaurant,
  getAdminRestaurant,
  listAdminRestaurants,
  softDeleteRestaurant,
  updateRestaurant,
} from '../api/client';

const SECTIONS = [
  { id: 'create', label: 'Create' },
  { id: 'approve', label: 'Approve' },
  { id: 'add-item', label: 'Add item' },
  { id: 'update', label: 'Update' },
  { id: 'delete', label: 'Delete' },
] as const;

export default function RestaurantsPage() {
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [restaurants, setRestaurants] = useState<AdminRestaurantSummary[]>([]);

  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('Pune');
  const [cuisines, setCuisines] = useState('Maharashtrian');
  const [email, setEmail] = useState('owner@example.com');
  const [opening, setOpening] = useState('08:00');
  const [closing, setClosing] = useState('22:00');

  const [approveId, setApproveId] = useState('');
  const [menuRestaurantId, setMenuRestaurantId] = useState('');
  const [itemName, setItemName] = useState('');
  const [itemCategory, setItemCategory] = useState('Main');
  const [itemPrice, setItemPrice] = useState('199');

  const [updateId, setUpdateId] = useState('');
  const [updateName, setUpdateName] = useState('');
  const [updateAddress, setUpdateAddress] = useState('');
  const [updateCity, setUpdateCity] = useState('');
  const [updateCuisines, setUpdateCuisines] = useState('');
  const [updateEmail, setUpdateEmail] = useState('');
  const [updateOpening, setUpdateOpening] = useState('08:00');
  const [updateClosing, setUpdateClosing] = useState('22:00');
  const [updateIsOpen, setUpdateIsOpen] = useState(true);
  const [updateWaitMins, setUpdateWaitMins] = useState('30');

  const [deleteId, setDeleteId] = useState('');

  const reloadRestaurants = useCallback(() => {
    listAdminRestaurants()
      .then(setRestaurants)
      .catch(() => setRestaurants([]));
  }, []);

  useEffect(() => {
    reloadRestaurants();
  }, [reloadRestaurants, success]);

  useEffect(() => {
    const hash = window.location.hash.replace('#', '');
    if (!hash) return;
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  useEffect(() => {
    if (!updateId) return;
    getAdminRestaurant(updateId)
      .then((r) => {
        setUpdateName(r.name);
        setUpdateAddress(r.addressLine1);
        setUpdateCity(r.city);
        setUpdateCuisines((r.cuisines ?? []).join(', '));
        setUpdateEmail(r.contactEmail ?? '');
        setUpdateOpening(r.openingTime?.slice(0, 5) ?? '08:00');
        setUpdateClosing(r.closingTime?.slice(0, 5) ?? '22:00');
        setUpdateIsOpen(r.isOpen);
        setUpdateWaitMins(String(r.estimatedWaitMins ?? 30));
      })
      .catch((err: Error) => setError(err.message));
  }, [updateId]);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      const created = await createRestaurant({
        name,
        address,
        city,
        cuisines: cuisines.split(',').map((c) => c.trim()).filter(Boolean),
        contactEmail: email,
        openingTime: opening,
        closingTime: closing,
      });
      setSuccess(`Created ${created.name} (${created.id}) — status ${created.status}`);
      setApproveId(created.id);
      setMenuRestaurantId(created.id);
      setUpdateId(created.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed');
    }
  }

  async function handleApprove() {
    if (!approveId) return;
    setError(null);
    setSuccess(null);
    try {
      const r = await approveRestaurant(approveId);
      setSuccess(`Approved ${r.name} — now ${r.status}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Approve failed');
    }
  }

  async function handleAddMenu(e: FormEvent) {
    e.preventDefault();
    if (!menuRestaurantId) return;
    setError(null);
    setSuccess(null);
    try {
      await addMenuItem(menuRestaurantId, {
        name: itemName,
        category: itemCategory,
        price: Number(itemPrice),
        available: true,
      });
      setSuccess(`Added menu item "${itemName}" to restaurant`);
      setItemName('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Menu add failed');
    }
  }

  async function handleUpdate(e: FormEvent) {
    e.preventDefault();
    if (!updateId) return;
    setError(null);
    setSuccess(null);
    try {
      const r = await updateRestaurant(updateId, {
        name: updateName,
        address: updateAddress,
        city: updateCity,
        cuisines: updateCuisines.split(',').map((c) => c.trim()).filter(Boolean),
        contactEmail: updateEmail,
        openingTime: updateOpening,
        closingTime: updateClosing,
        isOpen: updateIsOpen,
        estimatedWaitMins: Number(updateWaitMins),
      });
      setSuccess(`Updated ${r.name}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    }
  }

  async function handleSoftDelete() {
    if (!deleteId) return;
    if (!window.confirm('Suspend this restaurant? It will be hidden from customers (soft delete).')) {
      return;
    }
    setError(null);
    setSuccess(null);
    try {
      const r = await softDeleteRestaurant(deleteId);
      setSuccess(`Suspended ${r.name} — status ${r.status}`);
      setDeleteId('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  }

  return (
    <>
      <h1 className="page-title">Restaurants</h1>
      <p className="page-subtitle">Onboard partners, approve listings, manage menus, and suspend restaurants.</p>

      {error && <div className="error-banner">{error}</div>}
      {success && <div className="success-banner">{success}</div>}

      <nav className="subsection-nav" aria-label="Restaurant actions">
        {SECTIONS.map((section) => (
          <a key={section.id} href={`#${section.id}`}>
            {section.label}
          </a>
        ))}
      </nav>

      <div className="card">
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>All restaurants ({restaurants.length})</h2>
        {restaurants.length === 0 ? (
          <p className="muted">No restaurants found. Create one below.</p>
        ) : (
          <ul style={{ margin: 0, paddingLeft: '1.2rem' }}>
            {restaurants.map((r) => (
              <li key={r.id} style={{ marginBottom: '0.35rem' }}>
                {r.name} · {r.city}{' '}
                <span className={`badge status-${r.status.toLowerCase()}`}>{r.status}</span>
                <span className="muted" style={{ marginLeft: '0.35rem' }}>
                  {r.id.slice(0, 8)}…
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      <form id="create" className="card section-anchor" onSubmit={handleCreate}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Create restaurant</h2>
        <div className="grid-2">
          <div className="form-row"><label>Name</label><input value={name} onChange={(e) => setName(e.target.value)} required /></div>
          <div className="form-row"><label>City</label><input value={city} onChange={(e) => setCity(e.target.value)} required /></div>
          <div className="form-row"><label>Address</label><input value={address} onChange={(e) => setAddress(e.target.value)} required /></div>
          <div className="form-row"><label>Cuisines (comma-separated)</label><input value={cuisines} onChange={(e) => setCuisines(e.target.value)} /></div>
          <div className="form-row"><label>Contact email</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} /></div>
          <div className="form-row"><label>Hours</label><div style={{ display: 'flex', gap: '0.5rem' }}><input value={opening} onChange={(e) => setOpening(e.target.value)} /><input value={closing} onChange={(e) => setClosing(e.target.value)} /></div></div>
        </div>
        <button type="submit">Create restaurant</button>
      </form>

      <div id="approve" className="card section-anchor">
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Approve restaurant</h2>
        <RestaurantSelect
          label="Restaurant"
          value={approveId}
          onChange={setApproveId}
          restaurants={restaurants}
          placeholder="Select pending restaurant"
          filter={(r) => r.status === 'PENDING'}
        />
        <button type="button" onClick={handleApprove} disabled={!approveId}>
          Approve selected
        </button>
      </div>

      <form id="add-item" className="card section-anchor" onSubmit={handleAddMenu}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Add menu item</h2>
        <RestaurantSelect
          label="Restaurant"
          value={menuRestaurantId}
          onChange={setMenuRestaurantId}
          restaurants={restaurants}
          filter={(r) => r.status !== 'SUSPENDED'}
          required
        />
        <div className="grid-2">
          <div className="form-row"><label>Item name</label><input value={itemName} onChange={(e) => setItemName(e.target.value)} required /></div>
          <div className="form-row"><label>Category</label><input value={itemCategory} onChange={(e) => setItemCategory(e.target.value)} /></div>
          <div className="form-row"><label>Price (₹)</label><input type="number" min="0.01" step="0.01" value={itemPrice} onChange={(e) => setItemPrice(e.target.value)} required /></div>
        </div>
        <button type="submit" disabled={!menuRestaurantId}>Add item</button>
      </form>

      <form id="update" className="card section-anchor" onSubmit={handleUpdate}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Update restaurant</h2>
        <RestaurantSelect
          label="Restaurant"
          value={updateId}
          onChange={setUpdateId}
          restaurants={restaurants}
          filter={(r) => r.status !== 'SUSPENDED'}
          required
        />
        {updateId && (
          <div className="grid-2">
            <div className="form-row"><label>Name</label><input value={updateName} onChange={(e) => setUpdateName(e.target.value)} required /></div>
            <div className="form-row"><label>City</label><input value={updateCity} onChange={(e) => setUpdateCity(e.target.value)} required /></div>
            <div className="form-row"><label>Address</label><input value={updateAddress} onChange={(e) => setUpdateAddress(e.target.value)} required /></div>
            <div className="form-row"><label>Cuisines</label><input value={updateCuisines} onChange={(e) => setUpdateCuisines(e.target.value)} /></div>
            <div className="form-row"><label>Contact email</label><input type="email" value={updateEmail} onChange={(e) => setUpdateEmail(e.target.value)} /></div>
            <div className="form-row"><label>Est. wait (mins)</label><input type="number" min="1" value={updateWaitMins} onChange={(e) => setUpdateWaitMins(e.target.value)} /></div>
            <div className="form-row"><label>Opening</label><input value={updateOpening} onChange={(e) => setUpdateOpening(e.target.value)} /></div>
            <div className="form-row"><label>Closing</label><input value={updateClosing} onChange={(e) => setUpdateClosing(e.target.value)} /></div>
            <div className="form-row">
              <label>Open for orders</label>
              <select value={updateIsOpen ? 'true' : 'false'} onChange={(e) => setUpdateIsOpen(e.target.value === 'true')}>
                <option value="true">Open</option>
                <option value="false">Closed</option>
              </select>
            </div>
          </div>
        )}
        <button type="submit" disabled={!updateId}>Save changes</button>
      </form>

      <div id="delete" className="card section-anchor">
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Delete restaurant (soft)</h2>
        <p className="muted" style={{ marginTop: 0 }}>
          Sets status to <strong>SUSPENDED</strong> and closes the restaurant. Data is retained for audit.
        </p>
        <RestaurantSelect
          label="Restaurant"
          value={deleteId}
          onChange={setDeleteId}
          restaurants={restaurants}
          placeholder="Select restaurant to suspend"
          filter={(r) => r.status !== 'SUSPENDED'}
        />
        <button type="button" className="danger" onClick={handleSoftDelete} disabled={!deleteId}>
          Suspend restaurant
        </button>
      </div>
    </>
  );
}
