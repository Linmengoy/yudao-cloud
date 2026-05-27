import { NextResponse } from "next/server";

type WanJobPayload = {
  job_id?: string;
  status?: string;
  task?: string;
  created_at?: string;
  updated_at?: string;
  prompt?: string;
  video_url?: string | null;
  video_path?: string | null;
  output_dir?: string | null;
  error?: string | null;
  logs?: string | null;
  detail?: string;
};

class WanApiError extends Error {
  status: number;
  detail: string;

  constructor(status: number, detail: string) {
    super(`Wan upstream ${status}: ${detail}`);
    this.name = "WanApiError";
    this.status = status;
    this.detail = detail;
  }
}

const DEFAULT_BASE_URL = "http://7cfae580bc4c4e3eaf90694ecaf32fb8.qhdcloud.lanyun.net:17860";

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

function normalizeTaskId(taskId: string) {
  return taskId.startsWith("wan:") ? taskId.slice(4) : taskId;
}

function mapStatus(status?: string) {
  if (status === "succeeded") return "complete";
  if (status === "failed") return "failed";
  if (status === "queued") return "queued";
  if (status === "running") return "running";
  return status || "running";
}

async function readWanError(response: Response) {
  const text = await response.text();
  if (!text) return response.statusText || "Wan request failed";
  try {
    const parsed = JSON.parse(text) as WanJobPayload;
    return parsed.detail || parsed.error || text;
  } catch {
    return text;
  }
}

export async function GET(
  request: Request,
  context: { params: Promise<{ taskId: string }> }
) {
  const startedAt = Date.now();
  const apiKey = process.env.COPSE_WAN_VIDEO_API_KEY;
  const baseUrl = normalizeBaseUrl(process.env.COPSE_WAN_VIDEO_API_BASE_URL ?? DEFAULT_BASE_URL);
  const { taskId } = await context.params;
  const jobId = normalizeTaskId(taskId);

  if (!apiKey) {
    return NextResponse.json(
      { code: 500, msg: "Missing COPSE_WAN_VIDEO_API_KEY", data: null },
      { status: 500 }
    );
  }

  try {
    const upstreamUrl = `${baseUrl}/v1/jobs/${encodeURIComponent(jobId)}`;
    const response = await fetch(upstreamUrl, {
      method: "GET",
      headers: { "X-API-Key": apiKey },
      cache: "no-store",
    });

    if (!response.ok) {
      const detail = await readWanError(response);
      console.error("[video/wan/task/get] upstream error", {
        upstreamUrl,
        status: response.status,
        detail,
      });
      throw new WanApiError(response.status, detail);
    }

    const payload = (await response.json()) as WanJobPayload;
    const status = mapStatus(payload.status);
    const origin = new URL(request.url).origin;
    const videoUrl = payload.video_url
      ? `${origin}/app-api/ai/video/wan/task/${encodeURIComponent(jobId)}/video`
      : null;

    return NextResponse.json({
      code: 0,
      msg: "success",
      data: {
        taskId: `wan:${payload.job_id ?? jobId}`,
        jobId: payload.job_id ?? jobId,
        status,
        videoUrl,
        elapsedMs: Date.now() - startedAt,
        errorMessage: payload.error ?? null,
        upstream: payload,
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Wan video task query failed";
    const upstreamStatus = error instanceof WanApiError ? error.status : undefined;
    const upstreamDetail = error instanceof WanApiError ? error.detail : undefined;

    return NextResponse.json(
      {
        code: 500,
        msg: message,
        data: {
          taskId: `wan:${jobId}`,
          jobId,
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
