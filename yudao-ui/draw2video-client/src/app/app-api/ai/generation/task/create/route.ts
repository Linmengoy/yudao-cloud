import { NextResponse } from "next/server";

type ImageTaskParams = {
  size?: string;
  quality?: "auto" | "low" | "medium" | "high";
  output_format?: "png" | "jpeg" | "webp";
  output_compression?: number | null;
  moderation?: "auto" | "low";
  n?: number;
};

type CreateTaskBody = {
  type?: "image";
  mode?: "generate" | "edit";
  prompt?: string;
  params?: ImageTaskParams;
  inputImages?: Array<{
    dataUrl: string;
    fileName?: string;
    mimeType?: string;
  }>;
};

type ImagesApiItem = {
  b64_json?: string;
  url?: string;
  revised_prompt?: string;
};

type ImagesApiResponse = {
  data?: ImagesApiItem[];
  error?: {
    message?: string;
  };
  message?: string;
  msg?: string;
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

const MIME_BY_FORMAT = {
  png: "image/png",
  jpeg: "image/jpeg",
  webp: "image/webp",
} satisfies Record<NonNullable<ImageTaskParams["output_format"]>, string>;

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

async function imageInputToBlob(dataUrl: string): Promise<Blob> {
  if (/^https?:\/\//.test(dataUrl)) {
    const response = await fetch(dataUrl, { cache: "no-store" });
    if (!response.ok) throw new Error(`Failed to fetch input image: ${response.status}`);
    return response.blob();
  }

  const match = dataUrl.match(/^data:([^;,]+)?(;base64)?,(.*)$/);
  if (!match) throw new Error("Invalid image data URL");

  const mimeType = match[1] || "application/octet-stream";
  const isBase64 = Boolean(match[2]);
  const payload = match[3] || "";
  const binary = isBase64 ? atob(payload) : decodeURIComponent(payload);
  const bytes = new Uint8Array(binary.length);

  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }

  return new Blob([bytes], { type: mimeType });
}

function normalizeImageFromApi(item: ImagesApiItem, fallbackMime: string): string | null {
  if (item.b64_json) {
    return `data:${fallbackMime};base64,${item.b64_json}`;
  }
  if (item.url) return item.url;
  return null;
}

async function readApiError(response: Response) {
  const text = await response.text();
  if (!text) return response.statusText || "Upstream request failed";

  try {
    const body = JSON.parse(text) as ImagesApiResponse;
    return body.error?.message || body.message || body.msg || text;
  } catch {
    return text;
  }
}

export async function POST(request: Request) {
  const startedAt = Date.now();
  const apiKey = process.env.COPSE_IMAGE_API_KEY;
  const baseUrl = normalizeBaseUrl(process.env.COPSE_IMAGE_API_BASE_URL ?? "https://img.copse.top/v1");
  const model = process.env.COPSE_IMAGE_MODEL ?? "gpt-image-2";

  if (!apiKey) {
    return NextResponse.json(
      { code: 500, msg: "Missing COPSE_IMAGE_API_KEY", data: null },
      { status: 500 }
    );
  }

  try {
    const body = (await request.json()) as CreateTaskBody;
    const prompt = body.prompt?.trim();
    if (!prompt) {
      return NextResponse.json(
        { code: 400, msg: "Prompt is required", data: null },
        { status: 400 }
      );
    }

    const params = body.params ?? {};
    const outputFormat = params.output_format ?? "png";
    const mime = MIME_BY_FORMAT[outputFormat] ?? "image/png";
    const inputImages = body.inputImages?.filter((img) => img.dataUrl) ?? [];
    const isEdit = body.mode === "edit" || inputImages.length > 0;

    let response: Response;
    let upstreamUrl: string;

    if (isEdit) {
      const formData = new FormData();
      formData.append("model", model);
      formData.append("prompt", prompt);
      formData.append("size", params.size ?? "auto");
      formData.append("quality", params.quality ?? "auto");
      formData.append("output_format", outputFormat);
      formData.append("moderation", params.moderation ?? "auto");

      if (params.output_compression != null && outputFormat !== "png") {
        formData.append("output_compression", String(params.output_compression));
      }
      if ((params.n ?? 1) > 1) {
        formData.append("n", String(params.n));
      }

      for (let index = 0; index < inputImages.length; index += 1) {
        const image = inputImages[index];
        const blob = await imageInputToBlob(image.dataUrl);
        const ext = blob.type.split("/")[1] || "png";
        formData.append("image", blob, image.fileName || `input-${index + 1}.${ext}`);
      }

      upstreamUrl = `${baseUrl}/images/edits`;
      response = await fetch(upstreamUrl, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
        },
        cache: "no-store",
        body: formData,
      });
    } else {
      const requestBody: Record<string, unknown> = {
        model,
        prompt,
        size: params.size ?? "auto",
        quality: params.quality ?? "auto",
        output_format: outputFormat,
        moderation: params.moderation ?? "auto",
      };

      if (params.output_compression != null && outputFormat !== "png") {
        requestBody.output_compression = params.output_compression;
      }
      if ((params.n ?? 1) > 1) {
        requestBody.n = params.n;
      }

      upstreamUrl = `${baseUrl}/images/generations`;
      response = await fetch(upstreamUrl, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        cache: "no-store",
        body: JSON.stringify(requestBody),
      });
    }

    if (!response.ok) {
      const detail = await readApiError(response);
      console.error("[generation/task/create] upstream error", {
        upstreamUrl,
        status: response.status,
        detail,
      });
      throw new UpstreamApiError(response.status, detail);
    }

    const payload = (await response.json()) as ImagesApiResponse;
    const imageUrls = (payload.data ?? [])
      .map((item) => normalizeImageFromApi(item, mime))
      .filter((url): url is string => Boolean(url));

    if (imageUrls.length === 0) {
      throw new Error("接口没有返回可识别的图片数据");
    }

    return NextResponse.json({
      code: 0,
      msg: "success",
      data: {
        taskId: null,
        status: "complete",
        imageUrls,
        revisedPrompts: (payload.data ?? []).map((item) => item.revised_prompt).filter(Boolean),
        elapsedMs: Date.now() - startedAt,
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Generation failed";
    const upstreamStatus = error instanceof UpstreamApiError ? error.status : undefined;
    const upstreamDetail = error instanceof UpstreamApiError ? error.detail : undefined;
    return NextResponse.json(
      {
        code: 500,
        msg: message,
        data: {
          taskId: null,
          status: "failed",
          imageUrls: [],
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
