export async function register() {
  if (process.env.NEXT_RUNTIME === "nodejs") {
    const { NodeSDK } = await import("@opentelemetry/sdk-node");
    const { OTLPTraceExporter } = await import(
      "@opentelemetry/exporter-trace-otlp-http"
    );
    const { Resource } = await import("@opentelemetry/resources");
    const {
      ATTR_SERVICE_NAME,
      ATTR_SERVICE_VERSION,
    } = await import("@opentelemetry/semantic-conventions");
    const { BatchSpanProcessor } = await import(
      "@opentelemetry/sdk-trace-node"
    );

    const endpoint =
      process.env.OTEL_EXPORTER_OTLP_ENDPOINT ||
      "http://otel-collector.retail-observe.svc.cluster.local:4318";

    const traceExporter = new OTLPTraceExporter({
      url: `${endpoint}/v1/traces`,
    });

    const sdk = new NodeSDK({
      resource: new Resource({
        [ATTR_SERVICE_NAME]: "frontend",
        [ATTR_SERVICE_VERSION]: process.env.npm_package_version || "0.1.0",
      }),
      spanProcessors: [new BatchSpanProcessor(traceExporter)],
    });

    sdk.start();
  }
}
