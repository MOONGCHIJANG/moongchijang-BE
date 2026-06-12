import { check, sleep } from 'k6';
import { createDefaultOptions } from '../config/options.js';
import { buildBaseUrl } from '../lib/env.js';
import { getJson } from '../lib/http.js';

export const options = createDefaultOptions({
  tags: {
    scenario: 'template',
  },
});

export default function () {
  const baseUrl = buildBaseUrl();
  const response = getJson(`${baseUrl}/health`);

  check(response, {
    'status is 200': (res) => res.status === 200,
  });

  sleep(1);
}
