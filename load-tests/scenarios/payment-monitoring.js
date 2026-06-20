import http from 'k6/http';
import { check, sleep } from 'k6';
import { createDefaultOptions } from '../config/options.js';
import { buildAuthHeaders, optionalEnv } from '../lib/env.js';

const BASE_URL = resolveBaseUrl();
const AUTH_TOKEN = resolveAccessToken();
const GROUP_BUY_ID = optionalEnv('MCJ_GROUP_BUY_ID', optionalEnv('GROUP_BUY_ID', '960005'));
const RUN_CREATE_ORDER = parseBoolean(optionalEnv('RUN_CREATE_ORDER', 'false'));
const ALLOW_STATE_CHANGE = parseBoolean(optionalEnv('ALLOW_STATE_CHANGE', 'false'));
const RUN_COMPLETE_FAILURE = parseBoolean(optionalEnv('RUN_COMPLETE_FAILURE', 'true'));
const RUN_WEBHOOK_INVALID = parseBoolean(optionalEnv('RUN_WEBHOOK_INVALID', 'false'));

export const options = createDefaultOptions({
  scenarios: {
    payment_monitoring_smoke: {
      executor: 'constant-vus',
      vus: Number(optionalEnv('VUS', '3')),
      duration: optionalEnv('DURATION', '1m'),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.30'],
    http_req_duration: ['p(95)<1500'],
  },
  tags: {
    scenario: 'payment-monitoring',
    domain: 'payment',
    flow: 'monitoring-smoke',
  },
});

export function setup() {
  validateSelectedFlows();
  return {
    baseUrl: BASE_URL,
    runCreateOrder: RUN_CREATE_ORDER,
    runCompleteFailure: RUN_COMPLETE_FAILURE,
    runWebhookInvalid: RUN_WEBHOOK_INVALID,
  };
}

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
  sleep(Number(optionalEnv('SLEEP_SECONDS', '1')));
}

function authHeaders() {
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };

  if (AUTH_TOKEN) {
    headers.Authorization = `Bearer ${AUTH_TOKEN}`;
  }

  return headers;
}

function createPaymentOrder() {
  const payload = JSON.stringify({
    quantity: Number(optionalEnv('ORDER_QUANTITY', '1')),
    agreedNoCancelAfterGoal: true,
    agreedRefundBeforeGoal: true,
    agreedNoRefundAfterNoShow: true,
    agreedNoWithdrawal: true,
  });

  const response = http.post(
    `${BASE_URL}/api/v1/group-buys/${GROUP_BUY_ID}/payment-orders`,
    payload,
    { headers: authHeaders(), tags: { payment_flow: 'create_order' } },
  );

  check(response, {
    'create order returned handled status': (res) => [200, 400, 401, 403, 404, 409].includes(res.status),
  });
}

function validateSelectedFlows() {
  if (!RUN_CREATE_ORDER && !RUN_COMPLETE_FAILURE && !RUN_WEBHOOK_INVALID) {
    throw new Error(
      'At least one payment flow must be enabled. Set one of RUN_CREATE_ORDER, RUN_COMPLETE_FAILURE, RUN_WEBHOOK_INVALID.',
    );
  }

  if (RUN_CREATE_ORDER && !ALLOW_STATE_CHANGE) {
    throw new Error(
      'RUN_CREATE_ORDER=true requires ALLOW_STATE_CHANGE=true. This guard prevents accidental state-changing requests.',
    );
  }

  if (RUN_CREATE_ORDER && !AUTH_TOKEN) {
    throw new Error('RUN_CREATE_ORDER=true requires MCJ_ACCESS_TOKEN or AUTH_TOKEN.');
  }

  if (RUN_CREATE_ORDER && !GROUP_BUY_ID) {
    throw new Error('RUN_CREATE_ORDER=true requires MCJ_GROUP_BUY_ID or GROUP_BUY_ID.');
  }
}

function completePaymentWithMissingOrder() {
  const payload = JSON.stringify({
    paymentId: `MCJ-loadtest-${__VU}-${__ITER}`,
    amount: Number(optionalEnv('COMPLETE_AMOUNT', '1000')),
  });

  const response = http.post(
    `${BASE_URL}/api/v1/payments/portone/complete`,
    payload,
    { headers: authHeaders(), tags: { payment_flow: 'complete_failure' } },
  );

  check(response, {
    'complete failure returned handled status': (res) => [400, 401, 403, 404, 409].includes(res.status),
  });
}

function sendInvalidWebhook() {
  const payload = JSON.stringify({
    type: 'Transaction.Paid',
    storeId: optionalEnv('WEBHOOK_STORE_ID', 'load-test-store'),
    paymentId: `MCJ-loadtest-webhook-${__VU}-${__ITER}`,
  });

  const response = http.post(
    `${BASE_URL}/api/v1/payments/portone/webhook`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      tags: { payment_flow: 'webhook_invalid' },
    },
  );

  check(response, {
    'webhook invalid returned handled status': (res) => [200, 400, 401, 403].includes(res.status),
  });
}

function resolveBaseUrl() {
  const mcjBaseUrl = optionalEnv('MCJ_BASE_URL');
  if (mcjBaseUrl) {
    return mcjBaseUrl.replace(/\/$/, '');
  }

  return optionalEnv('BASE_URL', 'http://localhost:8080').replace(/\/$/, '');
}

function resolveAccessToken() {
  const mcjAccessToken = optionalEnv('MCJ_ACCESS_TOKEN');
  if (mcjAccessToken) {
    return mcjAccessToken;
  }

  const authToken = optionalEnv('AUTH_TOKEN');
  if (authToken) {
    return authToken;
  }

  const authHeaders = buildAuthHeaders();
  return authHeaders.Authorization?.replace(/^Bearer\s+/, '') || '';
}

function parseBoolean(value) {
  return value.toLowerCase() === 'true';
}
