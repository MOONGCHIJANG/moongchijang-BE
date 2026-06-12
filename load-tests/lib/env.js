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

export function buildAuthHeaders() {
  const accessToken = optionalEnv('MCJ_ACCESS_TOKEN');
  if (!accessToken) {
    return {};
  }

  return {
    Authorization: `Bearer ${accessToken}`,
  };
}
