import { check, sleep } from 'k6';
import http from 'k6/http';
import { createDefaultOptions } from '../config/options.js';
import { buildBaseUrl, optionalEnv, requireEnv } from '../lib/env.js';
import { getJson } from '../lib/http.js';

const DEFAULT_GROUP_BUY_ID = 960005;
const DEFAULT_PAGE_SIZE = 20;

export const options = createDefaultOptions({
  tags: {
    scenario: 'favorite-stateful-read',
    domain: 'favorite',
    flow: 'wishlist-progress-heartbeat',
  },
});

export default function () {
  requireEnv('MCJ_ACCESS_TOKEN');
  const baseUrl = buildBaseUrl();
  const groupBuyId = Number(optionalEnv('MCJ_GROUP_BUY_ID', DEFAULT_GROUP_BUY_ID));

  const wishlistResponse = getJson(
    `${baseUrl}/api/v1/wishlists?filter=ALL&excludeClosed=false&sort=LATEST&page=0&size=${DEFAULT_PAGE_SIZE}`,
    { tags: { endpoint: 'wishlist-list' } },
  );
  assertSuccess(wishlistResponse, 'wishlist list');

  const singleProgressResponse = getJson(
    `${baseUrl}/api/v1/group-buys/${groupBuyId}/progress`,
    { tags: { endpoint: 'group-buy-progress-single' } },
  );
  assertSuccess(singleProgressResponse, 'group buy progress single');

  const batchProgressResponse = getJson(
    `${baseUrl}/api/v1/group-buys/progress?ids=${groupBuyId}`,
    { tags: { endpoint: 'group-buy-progress-batch' } },
  );
  assertSuccess(batchProgressResponse, 'group buy progress batch');

  const heartbeatResponse = http.post(
    `${baseUrl}/api/v1/group-buys/${groupBuyId}/viewers/heartbeat`,
    null,
    {
      headers: {
        Accept: 'application/json',
      },
      tags: { endpoint: 'group-buy-viewers-heartbeat' },
    },
  );
  assertSuccess(heartbeatResponse, 'group buy viewers heartbeat');

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
