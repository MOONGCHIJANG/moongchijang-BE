import { check, sleep } from 'k6';
import http from 'k6/http';
import { createDefaultOptions } from '../config/options.js';
import { buildBaseUrl, optionalEnv, requireEnv } from '../lib/env.js';

const DEFAULT_PAGE_SIZE = 20;
const DEFAULT_REPORT_YEAR = 2026;
const DEFAULT_REPORT_MONTH = 6;

export const options = createDefaultOptions({
  tags: {
    scenario: 'admin-read',
    domain: 'admin',
    flow: 'dashboard-settlement-refund',
  },
});

export default function () {
  const adminAccessToken = requireEnv('MCJ_ADMIN_ACCESS_TOKEN');
  const baseUrl = buildBaseUrl();
  const reportYear = Number(optionalEnv('MCJ_REPORT_YEAR', DEFAULT_REPORT_YEAR));
  const reportMonth = Number(optionalEnv('MCJ_REPORT_MONTH', DEFAULT_REPORT_MONTH));

  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${adminAccessToken}`,
  };

  assertSuccess(
    http.get(`${baseUrl}/api/v1/admin/summary`, withHeaders(headers, 'admin-summary')),
    'admin summary',
  );

  assertSuccess(
    http.get(
      `${baseUrl}/api/v1/admin/dashboard/unconfirmed-orders?page=0&size=${DEFAULT_PAGE_SIZE}`,
      withHeaders(headers, 'admin-unconfirmed-orders'),
    ),
    'admin unconfirmed orders',
  );

  assertSuccess(
    http.get(
      `${baseUrl}/api/v1/admin/dashboard/urgent-refunds?page=0&size=${DEFAULT_PAGE_SIZE}`,
      withHeaders(headers, 'admin-urgent-refunds'),
    ),
    'admin urgent refunds',
  );

  assertSuccess(
    http.get(
      `${baseUrl}/api/v1/admin/settlements/dashboard?year=${reportYear}&month=${reportMonth}`,
      withHeaders(headers, 'admin-settlements-dashboard'),
    ),
    'admin settlements dashboard',
  );

  assertSuccess(
    http.get(
      `${baseUrl}/api/v1/admin/settlements?year=${reportYear}&month=${reportMonth}&status=ALL&page=0&size=${DEFAULT_PAGE_SIZE}`,
      withHeaders(headers, 'admin-settlements'),
    ),
    'admin settlements',
  );

  assertSuccess(
    http.get(
      `${baseUrl}/api/v1/admin/refund-requests?tab=ALL&caseFilter=ALL&page=0&size=${DEFAULT_PAGE_SIZE}`,
      withHeaders(headers, 'admin-refund-requests'),
    ),
    'admin refund requests',
  );

  sleep(1);
}

function withHeaders(headers, endpoint) {
  return {
    headers,
    tags: { endpoint },
  };
}

function assertSuccess(response, label) {
  check(response, {
    [`${label} status is 200`]: (res) => res.status === 200,
    [`${label} has success payload`]: (res) => {
      const body = safeJsonParse(res);
      return body?.success === true;
    },
  });
}

function safeJsonParse(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}
