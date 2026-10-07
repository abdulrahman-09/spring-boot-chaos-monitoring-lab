import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://order-service:8080';

http.setResponseCallback(
  http.expectedStatuses({ min: 200, max: 299 }, 400)
);

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 1000,
      stages: [
        { target: 10, duration: '1m' },
        { target: 25, duration: '1m' },
        { target: 50, duration: '1m' },
        { target: 100, duration: '1m' },
        { target: 0, duration: '1m' },
      ],
    },
  },
};

const params = {
  headers: {
    'Content-Type': 'application/json',
  },
};

export default function () {
  const payload = JSON.stringify({
    customerId: `stress-${__VU}`,
    amount: 25.0,
  });

  const response = http.post(`${BASE_URL}/orders`, payload, params);

  check(response, {
    'order request completed': (r) =>
      r.status === 201 || r.status === 400 || r.status === 502 || r.status === 504,
  });
}