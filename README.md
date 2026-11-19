# Case 3 — k6

Run against an authorized dedicated test target:
```bash
BASE_URL=http://your-test-host SANDBOX_API_KEY=your-key k6 run performance/case3-spike-test.js
```
The profile is 5,000 VUs: 1-minute ramp-up, 3-minute hold, 1-minute ramp-down. Thresholds: p95 < 800 ms and HTTP failure rate < 1%. Do not run 5,000 VUs against production or a public service without written permission.
