const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1';

export interface CustomerProfile {
  customerId: string;
  name: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  state?: string | null;
  pincode?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CustomerAuthResponse {
  profile: CustomerProfile;
  customerId: string;
  apiToken: string;
}

interface CustomerSession {
  customerId: string;
  apiToken: string;
  profile?: CustomerProfile;
}

let activeSession: CustomerSession | null = null;

export function setCustomerSession(session: CustomerSession | null) {
  activeSession = session;
}

export function getCustomerHeaders(): HeadersInit {
  if (!activeSession) {
    return {};
  }
  return {
    'X-Customer-Id': activeSession.customerId,
    'X-Customer-Api-Key': activeSession.apiToken,
  };
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, init);
  if (!response.ok) {
    const text = await response.text();
    let message = text;
    try {
      const json = JSON.parse(text) as { message?: string; error?: string };
      message = json.message ?? json.error ?? text;
    } catch {
      /* use raw text */
    }
    throw new Error(message || `Request failed (${response.status})`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export interface RestaurantSummary {
  id: string;
  name: string;
  city: string;
  rating: number;
  isOpen: boolean;
  estimatedWaitMins: number;
  cuisines: string[];
}

export interface RestaurantPage {
  content: RestaurantSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface MenuItem {
  id: string;
  name: string;
  price: number;
  available: boolean;
  category?: string;
}

export interface RestaurantMenu {
  restaurantId: string;
  name: string;
  city?: string;
  isOpen: boolean;
  estimatedWaitMins: number;
  menuItems: MenuItem[];
}

export interface OrderItemRequest {
  menuItemId: string;
  quantity: number;
}

export interface CreateOrderRequest {
  restaurantId: string;
  deliveryAddress: string;
  city: string;
  paymentMode: string;
  items: OrderItemRequest[];
}

export interface OrderResponse {
  orderId: string;
  status: string;
  paymentStatus: string;
  totalAmount?: number;
  deliveryAddressLine1?: string;
  city?: string;
  driverId?: string;
  createdAt?: string;
}

export interface OrderSummary {
  orderId: string;
  restaurantId: string;
  restaurantName: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
  deliveryAddressLine1: string;
  city: string;
  createdAt?: string;
}

export interface TrackingSnapshot {
  orderId: string;
  driverId?: string;
  latitude?: number;
  longitude?: number;
  heading?: number;
  timestamp?: string;
}

export function registerCustomer(body: {
  name: string;
  email: string;
  phone: string;
  password: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  pincode?: string;
}) {
  return apiFetch<CustomerAuthResponse>('/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export function loginCustomer(body: { loginId: string; password: string }) {
  return apiFetch<CustomerAuthResponse>('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export function getProfile() {
  return apiFetch<CustomerProfile>('/auth/me', {
    headers: getCustomerHeaders(),
  });
}

export function updateProfile(body: {
  name: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  pincode?: string;
}) {
  return apiFetch<CustomerProfile>('/auth/profile', {
    method: 'PATCH',
    headers: {
      ...getCustomerHeaders(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });
}

export function listRestaurants(params: Record<string, string | number | boolean>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== undefined) {
      query.set(key, String(value));
    }
  });
  return apiFetch<RestaurantPage>(`/restaurants?${query}`);
}

export function getMenu(restaurantId: string) {
  return apiFetch<RestaurantMenu>(`/restaurants/${restaurantId}/menu`);
}

export function createOrder(body: CreateOrderRequest, idempotencyKey: string) {
  return apiFetch<OrderResponse>('/orders', {
    method: 'POST',
    headers: {
      ...getCustomerHeaders(),
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(body),
  });
}

export function listMyOrders(scope: 'active' | 'history' | 'all' = 'all') {
  return apiFetch<OrderSummary[]>(`/orders?scope=${scope}`, {
    headers: getCustomerHeaders(),
  });
}

export function getOrder(orderId: string) {
  return apiFetch<OrderResponse>(`/orders/${orderId}`, {
    headers: getCustomerHeaders(),
  });
}

export function payOrder(orderId: string) {
  return apiFetch<OrderResponse>(`/orders/${orderId}/pay`, {
    method: 'POST',
    headers: getCustomerHeaders(),
  });
}

export function getTracking(orderId: string) {
  return apiFetch<TrackingSnapshot>(`/orders/${orderId}/tracking`, {
    headers: getCustomerHeaders(),
  });
}
