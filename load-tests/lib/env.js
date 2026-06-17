export function requireEnv(name) {
  const value = __ENV[name];
  if (!value) {
    throw new Error(`Missing required env: ${name}`);
  }
  return value;
}

export function optionalEnv(name, fallback = '') {
  return __ENV[name] || fallback;
}

export function buildBaseUrl() {
  return requireEnv('MCJ_BASE_URL').replace(/\/$/, '');
}

let cachedAuthHeaders = null;

export function buildAuthHeaders() {
  if (cachedAuthHeaders === null) {
    const accessToken = optionalEnv('MCJ_ACCESS_TOKEN');
    cachedAuthHeaders = accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
  }

  return cachedAuthHeaders;
}
