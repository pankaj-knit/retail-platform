"use client";

import { useEffect } from "react";

export function FaroInit() {
  useEffect(() => {
    import("@/lib/faro").then(({ initFaro }) => initFaro());
  }, []);

  return null;
}
