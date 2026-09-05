import { useState } from 'react';
import { importAnalyticsData } from '../api/client';

export default function DataPage() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleImport() {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const result = await importAnalyticsData();
      setMessage(
        typeof result === 'object' && result !== null && 'message' in result
          ? String((result as { message: string }).message)
          : 'Sample CSV import completed. Open the Analytics Dashboard to run queries.',
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Analytics data</h1>
      <p className="page-subtitle">
        Import the Assignment 2 sample dataset from third-assignment-sample-data-set into the analytics schema.
      </p>
      {error && <div className="error-banner">{error}</div>}
      {message && <div className="success-banner">{message}</div>}
      <div className="card">
        <p className="muted" style={{ marginTop: 0 }}>
          Requires the API container with SWIFTEATS_DATASET_PATH mounted (default in docker-compose).
          Import is idempotent with a lock — safe to retry.
        </p>
        <button onClick={handleImport} disabled={loading}>
          {loading ? 'Importing…' : 'Import sample CSV data'}
        </button>
      </div>
    </>
  );
}
