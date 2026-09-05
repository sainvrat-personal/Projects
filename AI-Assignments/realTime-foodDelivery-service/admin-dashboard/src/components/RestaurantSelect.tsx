import { AdminRestaurantSummary } from '../api/client';

interface RestaurantSelectProps {
  label: string;
  value: string;
  onChange: (id: string) => void;
  restaurants: AdminRestaurantSummary[];
  placeholder?: string;
  filter?: (restaurant: AdminRestaurantSummary) => boolean;
  required?: boolean;
}

export default function RestaurantSelect({
  label,
  value,
  onChange,
  restaurants,
  placeholder = 'Select a restaurant',
  filter,
  required,
}: RestaurantSelectProps) {
  const options = filter ? restaurants.filter(filter) : restaurants;

  return (
    <div className="form-row">
      <label>{label}</label>
      <select value={value} onChange={(e) => onChange(e.target.value)} required={required}>
        <option value="">{placeholder}</option>
        {options.map((r) => (
          <option key={r.id} value={r.id}>
            {r.name} · {r.city} ({r.status})
          </option>
        ))}
      </select>
      {value && (
        <p className="muted" style={{ margin: 0, fontSize: '0.8rem', fontFamily: 'monospace' }}>
          ID: {value}
        </p>
      )}
    </div>
  );
}
