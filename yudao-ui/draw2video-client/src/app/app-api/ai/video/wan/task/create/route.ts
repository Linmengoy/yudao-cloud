import { NextResponse } from "next/server";

type WanCreateBody = {
  prompt?: string;
  referenceImage?: string;
  referenceImages?: string[];
  size?: "1280*704" | "704*1280";
  frame_num?: number;
  sample_steps?: number;
  sample_shift?: number;
  sample_guide_scale?: number;
  seed?: number;
};

type WanJobPayload = {
  job_id?: string;
  status?: string;
  task?: string;
  created_at?: string;
  updated_at?: string;
  prompt?: string;
  video_url?: string | null;
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
const DEFAULT_SIZE = "1280*704";

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
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

function dataUrlToBlob(dataUrl: string): { blob: Blob; fileName: string } {
  const [meta, base64] = dataUrl.split(",");
  if (!meta || !base64) throw new Error("Invalid reference image dataUrl");
  const mimeMatch = meta.match(/^data:([^;]+);base64$/);
  const mimeType = mimeMatch?.[1] || "image/png";
  const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
  const ext = mimeType.includes("jpeg") ? "jpg" : mimeType.includes("webp") ? "webp" : "png";
  return { blob: new Blob([bytes], { type: mimeType }), fileName: `reference.${ext}` };
}

async function imageSourceToBlob(source: string): Promise<{ blob: Blob; fileName: string }> {
  if (source.startsWith("data:")) return dataUrlToBlob(source);

  const response = await fetch(source, { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`Reference image fetch failed: ${response.status}`);
  }
  const blob = await response.blob();
  return { blob, fileName: "reference.png" };
}

function normalizeSize(value: unknown): "1280*704" | "704*1280" {
  return value === "704*1280" ? "704*1280" : DEFAULT_SIZE;
}

export async function POST(request: Request) {
  const startedAt = Date.now();
  const apiKey = process.env.COPSE_WAN_VIDEO_API_KEY;
  const baseUrl = normalizeBaseUrl(process.env.COPSE_WAN_VIDEO_API_BASE_URL ?? DEFAULT_BASE_URL);

  if (!apiKey) {
    return NextResponse.json(
      { code: 500, msg: "Missing COPSE_WAN_VIDEO_API_KEY", data: null },
      { status: 500 }
    );
  }

  try {
    const body = (await request.json().catch(() => ({}))) as WanCreateBody;
    const prompt = body.prompt?.trim();
    if (!prompt) {
      return NextResponse.json(
        { code: 400, msg: "Missing prompt", data: null },
        { status: 400 }
      );
    }

    const referenceImage = body.referenceImage || body.referenceImages?.find(Boolean);
    const size = normalizeSize(body.size);
    const form = new FormData();
    form.set("prompt", prompt);
    form.set("size", size);
    form.set("frame_num", String(body.frame_num ?? 121));
    form.set("sample_steps", String(body.sample_steps ?? 20));
    form.set("sample_shift", String(body.sample_shift ?? 5));
    form.set("sample_guide_scale", String(body.sample_guide_scale ?? 5));
    form.set("seed", String(body.seed ?? -1));

    const endpoint = referenceImage ? "/v1/i2v" : "/v1/t2v";
    if (referenceImage) {
      const { blob, fileName } = await imageSourceToBlob(referenceImage);
      form.set("image", blob, fileName);
    }

    const upstreamUrl = `${baseUrl}${endpoint}`;
    const response = await fetch(upstreamUrl, {
      method: "POST",
      headers: { "X-API-Key": apiKey },
      body: form,
      cache: "no-store",
    });

    if (!response.ok) {
      const detail = await readWanError(response);
      console.error("[video/wan/task/create] upstream error", {
        upstreamUrl,
        status: response.status,
        detail,
      });
      throw new WanApiError(response.status, detail);
    }

    const payload = (await response.json()) as WanJobPayload;
    const jobId = payload.job_id;

    return NextResponse.json({
      code: 0,
      msg: "success",
      data: {
        taskId: jobId ? `wan:${jobId}` : null,
        jobId,
        status: payload.status ?? "queued",
        model: "wan2.2-ti2v-5b",
        task: payload.task ?? (referenceImage ? "i2v" : "t2v"),
        size,
        elapsedMs: Date.now() - startedAt,
        upstream: payload,
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Wan video task creation failed";
    const upstreamStatus = error instanceof WanApiError ? error.status : undefined;
    const upstreamDetail = error instanceof WanApiError ? error.detail : undefined;

    return NextResponse.json(
      {
        code: 500,
        msg: message,
        data: {
          taskId: null,
          status: "failed",
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

