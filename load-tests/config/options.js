import { DEFAULT_THRESHOLDS } from '../lib/thresholds.js';

export const DEFAULT_STAGES = [
  { duration: '1m', target: 10 },
  { duration: '3m', target: 30 },
  { duration: '1m', target: 0 },
];

export function createDefaultOptions(overrides = {}) {
  return {
    stages: overrides.stages ?? DEFAULT_STAGES,
    thresholds: overrides.thresholds ?? DEFAULT_THRESHOLDS,
    tags: {
      service: 'moongchijang-be',
      env: __ENV.MCJ_ENV_NAME || 'dev',
      scenario: __ENV.MCJ_SCENARIO_NAME || 'unknown',
      ...overrides.tags,
    },
    ...overrides,
  };
}
