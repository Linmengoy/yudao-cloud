import { NextResponse } from "next/server";

type ArkTaskPayload = {
  id?: string;
  task_id?: string;
  status?: string;
  error?: {
    message?: string;
    code?: string;
  };
  message?: string;
  msg?: string;
  content?: unknown;
  result?: unknown;
  data?: {
    id?: string;
    task_id?: string;
    status?: string;
    content?: unknown;
    result?: unknown;
    [key: string]: unknown;
  };
  [key: string]: unknown;
};

class UpstreamApiError extends Error {
  status: number;
  detail: string;

  constructor(status: number, detail: string) {
    super(`Upstream ${status}: ${detail}`);
    this.name = "UpstreamApiError";
    this.status = status;
    this.detail = detail;
  }
}

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

async function readApiError(response: Response) {
  const text = await response.text();
  if (!text) return response.statusText || "Upstream request failed";

  try {
    const body = JSON.parse(text) as ArkTaskPayload;
    return body.error?.message || body.message || body.msg || text;
  } catch {
    return text;
  }
}

function getTaskId(payload: ArkTaskPayload, fallback: string) {
  return payload.id || payload.task_id || payload.data?.id || payload.data?.task_id || fallback;
}

function getTaskStatus(payload: ArkTaskPayload) {
  return payload.status || payload.data?.status || "processing";
}

function findVideoUrl(value: unknown): string | null {
  if (!value) return null;
  if (typeof value === "string") {
    return /^https?:\/\//.test(value) ? value : null;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findVideoUrl(item);
      if (found) return found;
    }
    return null;
  }
  if (typeof value !== "object") return null;

  const record = value as Record<string, unknown>;
  for (const key of ["url", "video_url", "videoUrl", "output_url", "outputUrl"]) {
    const found = findVideoUrl(record[key]);
    if (found) return found;
  }
  for (const key of ["content", "result", "data", "video", "videos", "output", "outputs"]) {
    const found = findVideoUrl(record[key]);
    if (found) return found;
  }
  return null;
}

export async function GET(
  _request: Request,
  context: { params: Promise<{ taskId: string }> }
) {
  const startedAt = Date.now();
  const apiKey = process.env.COPSE_VIDEO_API_KEY;
  const baseUrl = normalizeBaseUrl(
    process.env.COPSE_VIDEO_API_BASE_URL ?? "https://ark.cn-beijing.volces.com/api/v3"
  );
  const { taskId } = await context.params;

  if (!apiKey) {
    return NextResponse.json(
      { code: 500, msg: "Missing COPSE_VIDEO_API_KEY", data: null },
      { status: 500 }
    );
  }

  try {
    const upstreamUrl = `${baseUrl}/contents/generations/tasks/${encodeURIComponent(taskId)}`;
    const response = await fetch(upstreamUrl, {
      method: "GET",
      headers: { Authorization: `Bearer ${apiKey}` },
      cache: "no-store",
    });

    if (!response.ok) {
      const detail = await readApiError(response);
      console.error("[video/generation/task/get] upstream error", {
        upstreamUrl,
        status: response.status,
        detail,
      });
      throw new UpstreamApiError(response.status, detail);
    }

    const payload = (await response.json()) as ArkTaskPayload;
    const status = getTaskStatus(payload);
    const videoUrl = findVideoUrl(payload);

    return NextResponse.json({
      code: 0,
      msg: "success",
      data: {
        taskId: getTaskId(payload, taskId),
        status,
        videoUrl,
        elapsedMs: Date.now() - startedAt,
        errorMessage: payload.error?.message ?? null,
        upstream: payload,
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Video task query failed";
    const upstreamStatus = error instanceof UpstreamApiError ? error.status : undefined;
    const upstreamDetail = error instanceof UpstreamApiError ? error.detail : undefined;

    return NextResponse.json(
      {
        code: 500,
        msg: message,
        data: {
          taskId,
          status: "failed",
          videoUrl: null,
          elapsedMs: Date.now() - startedAt,
          errorMessage: message,
          upstreamStatus,
          upstreamDetail,
        },
      },
      { status: 500 }
    );
  }
}
