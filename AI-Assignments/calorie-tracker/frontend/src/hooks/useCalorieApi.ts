import { useAuth } from '../context/useAuth';

export function useCalorieApi(token: string) {
  const { logout } = useAuth();

  const apiFetch = async (url: string, options: RequestInit = {}) => {
    const headers = {
      ...(options.headers || {}),
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
    const res = await fetch(url, { ...options, headers });
    if (res.status === 401) {
      logout();
      throw new Error('Unauthorized');
    }
    return res;
  };

  const get = async (url: string) => {
    const res = await apiFetch(url, { method: 'GET' });
    return res.json();
  };

  const post = async (url: string, body: unknown) => {
    const res = await apiFetch(url, {
      method: 'POST',
      body: JSON.stringify(body),
    });
    return res.json();
  };

  const put = async (url: string, body: unknown) => {
    const res = await apiFetch(url, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
    return res.json();
  };

  const del = async (url: string) => {
    const res = await apiFetch(url, { method: 'DELETE' });
    return res;
  };

  return { get, post, put, del };
}
