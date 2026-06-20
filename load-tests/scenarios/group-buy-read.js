import { check, sleep } from 'k6';
import { createDefaultOptions } from '../config/options.js';
import { buildBaseUrl, optionalEnv } from '../lib/env.js';
import { getJson } from '../lib/http.js';

const DEFAULT_GROUP_BUY_ID = 960005;
const DEFAULT_PAGE_SIZE = 20;
const baseUrl = buildBaseUrl();
const groupBuyId = Number(optionalEnv('MCJ_GROUP_BUY_ID', DEFAULT_GROUP_BUY_ID));

export const options = createDefaultOptions({
  tags: {
    scenario: 'group-buy-read',
    domain: 'group-buy',
    flow: 'feed-detail-progress',
  },
});

export default function () {
  const feedResponse = getJson(
    `${baseUrl}/api/v1/group-buys?filter=ALL&page=0&size=${DEFAULT_PAGE_SIZE}`,
    { tags: { endpoint: 'group-buy-feed' } },
  );

  check(feedResponse, {
    'feed status is 200': (res) => res.status === 200,
    'feed has success payload': (res) => {
      const body = safeJsonParse(res);
      return body?.success === true;
    },
  });

  const detailResponse = getJson(
    `${baseUrl}/api/v1/group-buys/${groupBuyId}`,
    { tags: { endpoint: 'group-buy-detail' } },
  );

  check(detailResponse, {
    'detail status is 200': (res) => res.status === 200,
    'detail has success payload': (res) => {
      const body = safeJsonParse(res);
      return body?.success === true;
    },
  });

  const progressResponse = getJson(
    `${baseUrl}/api/v1/group-buys/${groupBuyId}/progress`,
    { tags: { endpoint: 'group-buy-progress' } },
  );

  check(progressResponse, {
    'progress status is 200': (res) => res.status === 200,
    'progress has success payload': (res) => {
      const body = safeJsonParse(res);
      return body?.success === true;
    },
  });

  const batchProgressResponse = getJson(
    `${baseUrl}/api/v1/group-buys/progress?ids=${groupBuyId}`,
    { tags: { endpoint: 'group-buy-progress-batch' } },
  );

  check(batchProgressResponse, {
    'batch progress status is 200': (res) => res.status === 200,
    'batch progress has success payload': (res) => {
      const body = safeJsonParse(res);
      return body?.success === true;
    },
  });

  sleep(1);
}

function safeJsonParse(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}
