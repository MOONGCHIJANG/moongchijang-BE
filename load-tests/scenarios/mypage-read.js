import { check, sleep } from 'k6';
import { createDefaultOptions } from '../config/options.js';
import { buildBaseUrl, requireEnv } from '../lib/env.js';
import { getJson } from '../lib/http.js';

const DEFAULT_PAGE_SIZE = 20;

export const options = createDefaultOptions({
  tags: {
    scenario: 'mypage-read',
    domain: 'mypage',
    flow: 'summary-participations-refunds',
  },
});

export default function () {
  requireEnv('MCJ_ACCESS_TOKEN');
  const baseUrl = buildBaseUrl();

  const summaryResponse = getJson(
    `${baseUrl}/api/v1/users/me/tabs/counts`,
    { tags: { endpoint: 'mypage-tabs-counts' } },
  );
  assertSuccess(summaryResponse, 'tabs counts');

  const inProgressResponse = getJson(
    `${baseUrl}/api/v1/users/me/participations?status=IN_PROGRESS`,
    { tags: { endpoint: 'mypage-participations-in-progress' } },
  );
  assertSuccess(inProgressResponse, 'participations in progress');

  const pickupWaitingResponse = getJson(
    `${baseUrl}/api/v1/users/me/participations?status=PICKUP_WAITING`,
    { tags: { endpoint: 'mypage-participations-pickup-waiting' } },
  );
  assertSuccess(pickupWaitingResponse, 'participations pickup waiting');

  const inProgressTabResponse = getJson(
    `${baseUrl}/api/v1/users/me/participations/in-progress?page=0&size=${DEFAULT_PAGE_SIZE}`,
    { tags: { endpoint: 'mypage-in-progress-tab' } },
  );
  assertSuccess(inProgressTabResponse, 'in-progress tab');

  const pickupWaitingTabResponse = getJson(
    `${baseUrl}/api/v1/users/me/participations/pickup-waiting?page=0&size=${DEFAULT_PAGE_SIZE}`,
    { tags: { endpoint: 'mypage-pickup-waiting-tab' } },
  );
  assertSuccess(pickupWaitingTabResponse, 'pickup waiting tab');

  const refundsResponse = getJson(
    `${baseUrl}/api/v1/users/me/refunds`,
    { tags: { endpoint: 'mypage-refunds' } },
  );
  assertSuccess(refundsResponse, 'refunds');

  const groupBuyRequestsResponse = getJson(
    `${baseUrl}/api/v1/users/me/group-buy-requests`,
    { tags: { endpoint: 'mypage-group-buy-requests' } },
  );
  assertSuccess(groupBuyRequestsResponse, 'group buy requests');

  sleep(1);
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
