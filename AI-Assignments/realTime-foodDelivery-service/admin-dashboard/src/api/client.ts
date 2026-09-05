const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1';
const STORAGE_KEY = 'swifteats_admin_api_key';

export function getAdminKey(): string {
  return localStorage.getItem(STORAGE_KEY) ?? import.meta.env.VITE_ADMIN_API_KEY ?? '';
}

export function setAdminKey(key: string) {
  localStorage.setItem(STORAGE_KEY, key);
}

function adminHeaders(): HeadersInit {
  return {
    'X-Admin-Api-Key': getAdminKey(),
    'Content-Type': 'application/json',
  };
}

export async function adminFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { ...adminHeaders(), ...init?.headers },
  });
  if (!response.ok) {
    const text = await response.text();
    let message = text;
    try {
      const json = JSON.parse(text) as { message?: string };
      message = json.message ?? text;
    } catch {
      /* raw */
    }
    throw new Error(message || `Request failed (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export interface CreateRestaurantRequest {
  name: string;
  address: string;
  city: string;
  contactEmail: string;
  openingTime: string;
  closingTime: string;
  cuisines: string[];
}

export interface AdminRestaurantSummary {
  id: string;
  name: string;
  city: string;
  status: string;
  isOpen: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface RestaurantDetail {
  id: string;
  name: string;
  addressLine1: string;
  city: string;
  status: string;
  isOpen: boolean;
  cuisines: string[];
  contactEmail?: string;
  openingTime?: string;
  closingTime?: string;
  estimatedWaitMins?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateRestaurantRequest {
  name?: string;
  address?: string;
  city?: string;
  contactEmail?: string;
  openingTime?: string;
  closingTime?: string;
  cuisines?: string[];
  isOpen?: boolean;
  estimatedWaitMins?: number;
}

export interface CreateMenuItemRequest {
  name: string;
  category: string;
  price: number;
  available: boolean;
}

export interface TransitionOrderRequest {
  status: string;
  changedBy?: string;
  reason?: string;
}

export interface OrderResponse {
  orderId: string;
  status: string;
  paymentStatus: string;
  createdAt?: string;
  updatedAt?: string;
  paymentProcessedAt?: string;
}

export interface AdminCustomerSummary {
  id: string;
  name: string;
  email: string;
  phone: string;
  city?: string;
  status: 'ACTIVE' | 'SUSPENDED';
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminCustomerDetail extends AdminCustomerSummary {
  addressLine1?: string;
  addressLine2?: string;
  state?: string;
  pincode?: string;
}

export function listAdminRestaurants(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return adminFetch<AdminRestaurantSummary[]>(`/admin/restaurants${query}`);
}

export function getAdminRestaurant(id: string) {
  return adminFetch<RestaurantDetail>(`/admin/restaurants/${id}`);
}

export function createRestaurant(body: CreateRestaurantRequest) {
  return adminFetch<RestaurantDetail>('/admin/restaurants', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateRestaurant(id: string, body: UpdateRestaurantRequest) {
  return adminFetch<RestaurantDetail>(`/admin/restaurants/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function softDeleteRestaurant(id: string) {
  return adminFetch<RestaurantDetail>(`/admin/restaurants/${id}`, { method: 'DELETE' });
}

export function approveRestaurant(id: string) {
  return adminFetch<RestaurantDetail>(`/admin/restaurants/${id}/approve`, { method: 'PATCH' });
}

export function addMenuItem(restaurantId: string, body: CreateMenuItemRequest) {
  return adminFetch(`/admin/restaurants/${restaurantId}/menu-items`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function transitionOrder(orderId: string, body: TransitionOrderRequest) {
  return adminFetch<OrderResponse>(`/admin/orders/${orderId}/state`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function importAnalyticsData() {
  return adminFetch<{ message?: string; rowsImported?: number }>('/admin/analytics/import', {
    method: 'POST',
    headers: { 'X-Admin-Api-Key': getAdminKey() },
  });
}

export function listAdminCustomers(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return adminFetch<AdminCustomerSummary[]>(`/admin/customers${query}`);
}

export function getAdminCustomer(id: string) {
  return adminFetch<AdminCustomerDetail>(`/admin/customers/${id}`);
}

export function updateCustomerStatus(id: string, status: 'ACTIVE' | 'SUSPENDED') {
  return adminFetch<AdminCustomerDetail>(`/admin/customers/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}
