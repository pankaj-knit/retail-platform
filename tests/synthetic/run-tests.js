const http = require("http");

const BASE_URL =
  process.env.BASE_URL ||
  "http://istio-ingressgateway.istio-system.svc.cluster.local";
const PUSHGATEWAY_URL =
  process.env.PUSHGATEWAY_URL ||
  "http://pushgateway.retail-observe.svc.cluster.local:9091";
const JOB_NAME = "synthetic_tests";

const TESTS = [
  {
    name: "homepage_loads",
    path: "/",
    expect: { status: 200, bodyContains: "RetailHub" },
  },
  {
    name: "login_page_loads",
    path: "/login",
    expect: { status: 200, bodyContains: "RetailHub" },
  },
  {
    name: "register_page_loads",
    path: "/register",
    expect: { status: 200 },
  },
  {
    name: "cart_page_loads",
    path: "/cart",
    expect: { status: 200 },
  },
  {
    name: "products_api_responds",
    path: "/api/products",
    expect: { status: 200 },
  },
];

function httpGet(urlStr, timeoutMs = 10_000) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlStr);
    const req = http.get(
      { hostname: url.hostname, port: url.port || 80, path: url.pathname, timeout: timeoutMs },
      (res) => {
        let body = "";
        res.on("data", (chunk) => (body += chunk));
        res.on("end", () => resolve({ status: res.statusCode, body }));
      }
    );
    req.on("timeout", () => { req.destroy(); reject(new Error("timeout")); });
    req.on("error", reject);
  });
}

async function runTest(test) {
  const start = Date.now();
  try {
    const { status, body } = await httpGet(`${BASE_URL}${test.path}`);
    const duration = (Date.now() - start) / 1000;

    let passed = status === test.expect.status;
    if (passed && test.expect.bodyContains) {
      passed = body.includes(test.expect.bodyContains);
    }

    console.log(`  ${passed ? "PASS" : "FAIL"} ${test.name} (${status}, ${duration.toFixed(2)}s)`);
    return { name: test.name, passed, duration };
  } catch (err) {
    const duration = (Date.now() - start) / 1000;
    console.log(`  FAIL ${test.name} (${err.message}, ${duration.toFixed(2)}s)`);
    return { name: test.name, passed: false, duration };
  }
}

function pushMetrics(results) {
  const now = Math.floor(Date.now() / 1000);
  const lines = results.map((r) =>
    [
      `synthetic_test_success{test="${r.name}"} ${r.passed ? 1 : 0}`,
      `synthetic_test_duration_seconds{test="${r.name}"} ${r.duration.toFixed(3)}`,
      `synthetic_test_last_run_timestamp{test="${r.name}"} ${now}`,
    ].join("\n")
  );
  const body = lines.join("\n") + "\n";
  const url = new URL(`${PUSHGATEWAY_URL}/metrics/job/${JOB_NAME}`);

  return new Promise((resolve, reject) => {
    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: "POST",
        headers: { "Content-Type": "text/plain", "Content-Length": Buffer.byteLength(body) },
      },
      (res) => {
        let data = "";
        res.on("data", (c) => (data += c));
        res.on("end", () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            console.log(`\nPushed ${results.length} test metrics to Pushgateway (HTTP ${res.statusCode})`);
            resolve();
          } else {
            console.error(`Pushgateway HTTP ${res.statusCode}: ${data}`);
            reject(new Error(`HTTP ${res.statusCode}`));
          }
        });
      }
    );
    req.on("error", reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  console.log(`Synthetic tests against ${BASE_URL}\n`);
  const results = [];
  for (const t of TESTS) {
    results.push(await runTest(t));
  }

  const passed = results.filter((r) => r.passed).length;
  console.log(`\n${passed}/${results.length} tests passed`);

  await pushMetrics(results);

  process.exit(passed === results.length ? 0 : 1);
}

main().catch((err) => {
  console.error("Fatal:", err.message);
  process.exit(1);
});
