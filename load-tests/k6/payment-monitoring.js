import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const GROUP_BUY_ID = __ENV.GROUP_BUY_ID || '1';
const RUN_CREATE_ORDER = (__ENV.RUN_CREATE_ORDER || 'false').toLowerCase() === 'true';
const RUN_COMPLETE_FAILURE = (__ENV.RUN_COMPLETE_FAILURE || 'true').toLowerCase() === 'true';
const RUN_WEBHOOK_INVALID = (__ENV.RUN_WEBHOOK_INVALID || 'false').toLowerCase() === 'true';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

export const options = {
  scenarios: {
    payment_monitoring_smoke: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 3),
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.30'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  if (RUN_CREATE_ORDER) {
    createPaymentOrder();
  }
  if (RUN_COMPLETE_FAILURE) {
    completePaymentWithMissingOrder();
  }
  if (RUN_WEBHOOK_INVALID) {
    sendInvalidWebhook();
  }
  sleep(Number(__ENV.SLEEP_SECONDS || 1));
}

function authHeaders() {
  const headers = { 'Content-Type': 'application/json' };
  if (AUTH_TOKEN) {
    headers.Authorization = `Bearer ${AUTH_TOKEN}`;
  }
  return headers;
}

function createPaymentOrder() {
  const payload = JSON.stringify({
    quantity: Number(__ENV.ORDER_QUANTITY || 1),
    agreedNoCancelAfterGoal: true,
    agreedRefundBeforeGoal: true,
    agreedNoRefundAfterNoShow: true,
    agreedNoWithdrawal: true,
  });
  const res = http.post(
    `${BASE_URL}/api/v1/group-buys/${GROUP_BUY_ID}/payment-orders`,
    payload,
    { headers: authHeaders(), tags: { payment_flow: 'create_order' } },
  );
  check(res, {
    'create order returned handled status': (r) => [200, 400, 401, 403, 404, 409].includes(r.status),
  });
}

function completePaymentWithMissingOrder() {
  const payload = JSON.stringify({
    paymentId: `MCJ-loadtest-${__VU}-${__ITER}`,
    amount: Number(__ENV.COMPLETE_AMOUNT || 1000),
  });
  const res = http.post(
    `${BASE_URL}/api/v1/payments/portone/complete`,
    payload,
    { headers: authHeaders(), tags: { payment_flow: 'complete_failure' } },
  );
  check(res, {
    'complete failure returned handled status': (r) => [400, 401, 403, 404, 409].includes(r.status),
  });
}

function sendInvalidWebhook() {
  const payload = JSON.stringify({
    type: 'Transaction.Paid',
    storeId: __ENV.WEBHOOK_STORE_ID || 'load-test-store',
    paymentId: `MCJ-loadtest-webhook-${__VU}-${__ITER}`,
  });
  const res = http.post(
    `${BASE_URL}/api/v1/payments/portone/webhook`,
    payload,
    { headers: { 'Content-Type': 'application/json' }, tags: { payment_flow: 'webhook_invalid' } },
  );
  check(res, {
    'webhook invalid returned handled status': (r) => [200, 400, 401, 403].includes(r.status),
  });
}
