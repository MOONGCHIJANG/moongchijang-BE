export const DEFAULT_THRESHOLDS = {
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<1000', 'p(99)<2000'],
};
