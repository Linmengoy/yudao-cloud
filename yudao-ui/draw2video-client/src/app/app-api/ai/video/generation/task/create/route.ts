import { NextResponse } from "next/server";

type VideoContentItem =
  | {
      type: "text";
      text: string;
    }
  | {
      type: "image_url";
      image_url: { url: string };
      role?: "reference_image" | string;
    }
  | {
      type: "video_url";
      video_url: { url: string };
      role?: "reference_video" | string;
    }
  | {
      type: "audio_url";
      audio_url: { url: string };
      role?: "reference_audio" | string;
    };

type CreateVideoTaskBody = {
  model?: string;
  content?: VideoContentItem[];
  prompt?: string;
  referenceImages?: string[];
  referenceVideos?: string[];
  referenceAudios?: string[];
  generate_audio?: boolean;
  generateAudio?: boolean;
  ratio?: string;
  duration?: number;
  watermark?: boolean;
};

type ArkResponse = {
  id?: string;
  task_id?: string;
  status?: string;
  model?: string;
  created_at?: number;
  error?: {
    message?: string;
    code?: string;
  };
  message?: string;
  msg?: string;
  data?: {
    id?: string;
    task_id?: string;
    status?: string;
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

const DEFAULT_CONTENT: VideoContentItem[] = [
  {
    type: "text",
    text: "全程使用视频1的第一视角构图，全程使用音频1作为背景音乐。第一人称视角果茶宣传广告，seedance牌「苹苹安安」苹果果茶限定款；首帧为图片1，你的手摘下一颗带晨露的阿克苏红苹果，轻脆的苹果碰撞声；2-4 秒：快速切镜，你的手将苹果块投入雪克杯，加入冰块与茶底，用力摇晃，冰块碰撞声与摇晃声卡点轻快鼓点，背景音：「鲜切现摇」；4-6 秒：第一人称成品特写，分层果茶倒入透明杯，你的手轻挤奶盖在顶部铺展，在杯身贴上粉红包标，镜头拉近看奶盖与果茶的分层纹理；6-8 秒：第一人称手持举杯，你将图片2中的果茶举到镜头前（模拟递到观众面前的视角），杯身标签清晰可见，背景音「来一口鲜爽」，尾帧定格为图片2。背景声音统一为女生音色。",
  },
  {
    type: "image_url",
    image_url: { url: "https://ark-project.tos-cn-beijing.volces.com/doc_image/r2v_tea_pic1.jpg" },
    role: "reference_image",
  },
  {
    type: "image_url",
    image_url: { url: "https://ark-project.tos-cn-beijing.volces.com/doc_image/r2v_tea_pic2.jpg" },
    role: "reference_image",
  },
  {
    type: "video_url",
    video_url: { url: "https://ark-project.tos-cn-beijing.volces.com/doc_video/r2v_tea_video1.mp4" },
    role: "reference_video",
  },
  {
    type: "audio_url",
    audio_url: { url: "https://ark-project.tos-cn-beijing.volces.com/doc_audio/r2v_tea_audio1.mp3" },
    role: "reference_audio",
  },
];

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

function buildContent(body: CreateVideoTaskBody): VideoContentItem[] {
  if (Array.isArray(body.content) && body.content.length > 0) {
    return body.content;
  }

  const content: VideoContentItem[] = [];
  const prompt = body.prompt?.trim();
  if (prompt) {
    content.push({ type: "text", text: prompt });
  }

  for (const url of body.referenceImages ?? []) {
    if (!url) continue;
    content.push({ type: "image_url", image_url: { url }, role: "reference_image" });
  }
  for (const url of body.referenceVideos ?? []) {
    if (!url) continue;
    content.push({ type: "video_url", video_url: { url }, role: "reference_video" });
  }
  for (const url of body.referenceAudios ?? []) {
    if (!url) continue;
    content.push({ type: "audio_url", audio_url: { url }, role: "reference_audio" });
  }

  return content.length > 0 ? content : DEFAULT_CONTENT;
}

async function readApiError(response: Response) {
  const text = await response.text();
  if (!text) return response.statusText || "Upstream request failed";

  try {
    const body = JSON.parse(text) as ArkResponse;
    return body.error?.message || body.message || body.msg || text;
  } catch {
    return text;
  }
}

function getTaskId(payload: ArkResponse) {
  return payload.id || payload.task_id || payload.data?.id || payload.data?.task_id || null;
}

function getTaskStatus(payload: ArkResponse) {
  return payload.status || payload.data?.status || "submitted";
}

export async function POST(request: Request) {
  const startedAt = Date.now();
  const apiKey = process.env.COPSE_VIDEO_API_KEY;
  const baseUrl = normalizeBaseUrl(
    process.env.COPSE_VIDEO_API_BASE_URL ?? "https://ark.cn-beijing.volces.com/api/v3"
  );
  const model = process.env.COPSE_VIDEO_MODEL ?? "doubao-seedance-2-0-260128";

  if (!apiKey) {
    return NextResponse.json(
      { code: 500, msg: "Missing COPSE_VIDEO_API_KEY", data: null },
      { status: 500 }
    );
  }

  try {
    const body = (await request.json().catch(() => ({}))) as CreateVideoTaskBody;
    const content = buildContent(body);

    const requestBody = {
      model: body.model ?? model,
      content,
      generate_audio: body.generate_audio ?? body.generateAudio ?? true,
      ratio: body.ratio ?? "16:9",
      duration: body.duration ?? 11,
      watermark: body.watermark ?? false,
    };

    const upstreamUrl = `${baseUrl}/contents/generations/tasks`;
    const response = await fetch(upstreamUrl, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      cache: "no-store",
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      const detail = await readApiError(response);
      console.error("[video/generation/task/create] upstream error", {
        upstreamUrl,
        status: response.status,
        detail,
      });
      throw new UpstreamApiError(response.status, detail);
    }

    const payload = (await response.json()) as ArkResponse;
    const taskId = getTaskId(payload);

    return NextResponse.json({
      code: 0,
      msg: "success",
      data: {
        taskId,
        status: getTaskStatus(payload),
        model: requestBody.model,
        ratio: requestBody.ratio,
        duration: requestBody.duration,
        generateAudio: requestBody.generate_audio,
        watermark: requestBody.watermark,
        elapsedMs: Date.now() - startedAt,
        upstream: payload,
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Video task creation failed";
    const upstreamStatus = error instanceof UpstreamApiError ? error.status : undefined;
    const upstreamDetail = error instanceof UpstreamApiError ? error.detail : undefined;

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
