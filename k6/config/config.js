// ============================================================
// config/config.js — centralised environment configuration
// ============================================================

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const THRESHOLDS = {
  http_req_failed:   [{ threshold: 'rate<0.05',  abortOnFail: false }],
  http_req_duration: [{ threshold: 'p(95)<2000', abortOnFail: false }],
};

// Shared VU options per scenario
export const SMOKE_OPTIONS = {
  vus: 1,
  iterations: 1,
};

export const FUNCTIONAL_OPTIONS = {
  vus: 1,
  iterations: 1,
};

export const LOAD_OPTIONS = {
  stages: [
    { duration: '10s', target: 5  },
    { duration: '20s', target: 10 },
    { duration: '10s', target: 0  },
  ],
};
