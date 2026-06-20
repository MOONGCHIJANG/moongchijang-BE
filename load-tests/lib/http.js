import http from 'k6/http';
import { buildAuthHeaders } from './env.js';

export function getJson(url, params = {}) {
  return http.get(url, {
    ...params,
    headers: {
      Accept: 'application/json',
      ...buildAuthHeaders(),
      ...(params.headers || {}),
    },
  });
}

export function postJson(url, body, params = {}) {
  return http.post(url, JSON.stringify(body), {
    ...params,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...buildAuthHeaders(),
      ...(params.headers || {}),
    },
  });
}
