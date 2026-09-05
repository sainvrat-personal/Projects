export function statusBadgeClass(status: string): string {
  return `badge status-${status.toLowerCase().replace(/_/g, '-')}`;
}

export function formatStatus(status: string): string {
  return status.replace(/_/g, ' ');
}

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: 'Awaiting payment',
  CONFIRMED: 'Confirmed',
  PREPARING: 'Preparing',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
  DELAYED: 'Delayed',
  FAILED: 'Failed',
  RETURNED: 'Returned',
};

export function formatOrderStatus(status: string): string {
  return ORDER_STATUS_LABELS[status] ?? formatStatus(status);
}

export function formatPaymentStatus(status: string): string {
  if (status === 'PENDING') return 'Processing…';
  if (status === 'SUCCESS') return 'Successful';
  if (status === 'FAILED') return 'Failed';
  return status;
}

export function isAwaitingPayment(status: string): boolean {
  return status === 'PENDING_PAYMENT' || status === 'PAYMENT_FAILED';
}

export function formatOrderDate(value?: string): string {
  if (!value) return '—';
  return new Date(value).toLocaleString();
}
