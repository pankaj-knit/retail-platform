"use client";

import { useEffect } from "react";
import { initFaro } from "@/lib/faro";

// Initialize Faro as early as possible so fetch is patched with traceparent before any api.get().
if (typeof window !== "undefined") {
  initFaro();
}

export function FaroInit() {
  useEffect(() => {
    initFaro();
  }, []);

  return null;
}
