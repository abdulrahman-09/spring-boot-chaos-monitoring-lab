import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 2xx and 400 count as "expected". Anything else (404, 5xx) shows up as a failed request.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 400));

export const options = {
  scenarios: {
    orders: {
      executor: 'constant-arrival-rate',
      rate: 10,           // iterations per second
      timeUnit: '1s',
      duration: '30m',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
};

const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  // ~5% of requests are invalid on purpose, to produce HTTP 400 responses.
  const invalid = Math.random() < 0.05;

  const payload = JSON.stringify({
    customerId: `c-${Math.floor(Math.random() * 1000)}`,
    amount: invalid ? -5 : Number((Math.random() * 200 + 1).toFixed(2)),
  });

  const created = http.post(`${BASE_URL}/orders`, payload, params);

  if (created.status === 201) {
    const orderId = created.json('id');
    const fetched = http.get(`${BASE_URL}/orders/${orderId}`);
    check(fetched, { 'GET /orders/{id} is 200': (r) => r.status === 200 });
  }
}
