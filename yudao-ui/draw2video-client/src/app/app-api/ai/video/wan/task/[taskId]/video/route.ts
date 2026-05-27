import { NextResponse } from "next/server";

const DEFAULT_BASE_URL = "http://7cfae580bc4c4e3eaf90694ecaf32fb8.qhdcloud.lanyun.net:17860";

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

function normalizeTaskId(taskId: string) {
  return taskId.startsWith("wan:") ? taskId.slice(4) : taskId;
}

async function readError(response: Response) {
  const text = await response.text();
  return text || response.statusText || "Wan video download failed";
}

export async function GET(
  _request: Request,
  context: { params: Promise<{ taskId: string }> }
) {
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

  const upstreamUrl = `${baseUrl}/v1/jobs/${encodeURIComponent(jobId)}/video`;
  const response = await fetch(upstreamUrl, {
    method: "GET",
    headers: { "X-API-Key": apiKey },
    cache: "no-store",
  });

  if (!response.ok) {
    return NextResponse.json(
      { code: response.status, msg: await readError(response), data: null },
      { status: response.status }
    );
  }

  return new Response(response.body, {
    status: 200,
    headers: {
      "Content-Type": response.headers.get("Content-Type") ?? "video/mp4",
      "Cache-Control": "no-store",
    },
  });
}

