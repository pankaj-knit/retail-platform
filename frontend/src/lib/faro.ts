import {
  getWebInstrumentations,
  initializeFaro,
  type Faro,
} from "@grafana/faro-web-sdk";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";

let faro: Faro | null = null;

export function getFaro(): Faro | null {
  return faro;
}

export function initFaro(): Faro | null {
  if (faro) return faro;
  if (typeof window === "undefined") return null;

  const collectorUrl =
    process.env.NEXT_PUBLIC_FARO_COLLECTOR_URL ||
    `${window.location.origin}/collect`;

  faro = initializeFaro({
    url: collectorUrl,
    app: {
      name: "frontend",
      version: process.env.NEXT_PUBLIC_APP_VERSION || "0.1.0",
      environment: process.env.NODE_ENV || "production",
    },
    instrumentations: [
      ...getWebInstrumentations({ captureConsole: true }),
      new TracingInstrumentation(),
    ],
  });

  return faro;
}
