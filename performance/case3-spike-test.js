
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.SANDBOX_API_KEY || 'local-test-api-key';
const TARGET_VUS = Number(__ENV.TARGET_VUS || 10);

const failedChecks = new Counter('sandbox_auth_failed_checks');

export const options = {
  stages: [
    { duration: '1m', target: TARGET_VUS },
    { duration: '3m', target: TARGET_VUS },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.01'],
    sandbox_auth_failed_checks: ['count==0'],
  },
};

export default function () {
  const id = `${__VU}-${__ITER}-${Date.now()}`;

  const payload = JSON.stringify({
    api_key: API_KEY,
    mock_id_number: `MOCK-${id}`,
    device_data: {
      device_id: `DEVICE-${id}`,
      biometric_hash: 'synthetic-load-test-hash',
    },
  });

  const response = http.post(
    `${BASE_URL}/api/v1/sandbox/auth`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      timeout: '10s',
    }
  );

  const passed = check(response, {
    'HTTP 200': r => r.status === 200,
    'auth_token is present': r => {
      try {
        const body = r.json();
        return typeof body.auth_token === 'string' &&
          body.auth_token.length > 0;
      } catch (error) {
        return false;
      }
    },
  });

  if (!passed) {
    failedChecks.add(1);
  }
}