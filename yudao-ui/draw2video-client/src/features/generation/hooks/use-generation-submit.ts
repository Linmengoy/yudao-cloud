"use client";

import { useCallback, useState } from "react";
import { generationApi } from "../generation-api";
import { createClientRequestId } from "../generation-client-request";
import type { GenerateSubmitRequest, GenerateSubmitResponse } from "../generation-types";

type SubmitOptions = {
  withClientRequestId?: boolean;
};

export function useGenerationSubmit() {
  const [data, setData] = useState<GenerateSubmitResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = useCallback(async (request: GenerateSubmitRequest, options: SubmitOptions = {}) => {
    setLoading(true);
    setError("");
    try {
      const payload = {
        ...request,
        clientRequestId: request.clientRequestId ?? (options.withClientRequestId === false ? undefined : createClientRequestId()),
      };
      const result = await generationApi.submit(payload);
      setData(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : "生成提交失败";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { data, loading, error, submit };
}
