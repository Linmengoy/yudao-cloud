import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import type { AigcModel } from "@/features/generation/model-api";
import {
  QUICK_GENERATION_MODE_LABELS,
  communityPostCoverUrl,
  getCompatibleModels,
  getQuickGenerationMode,
  getModelMaxReferenceImages,
  getSelectedTabModels,
  pickDefaultModelId,
  workspaceCommunityCacheKey,
} from "./workspace-page-contracts";

function model(id: number, type: number, defaultModel = false): AigcModel {
  return {
    id,
    type,
    defaultModel,
    code: `model-${id}`,
    name: `Model ${id}`,
    model: `provider-model-${id}`,
    publicVisible: true,
    sort: id,
    status: 1,
    providerId: 1,
  };
}

const workspacePage = readFileSync(new URL("./page.tsx", import.meta.url), "utf8");
const providerForm = readFileSync(
  resolve(process.cwd(), "../draw2video-admin/src/views/aigc/model/provider/ProviderForm.vue"),
  "utf8",
);

describe("workspace quick generation contracts", () => {
  it("filters the model dropdown to the active image or video tab", () => {
    const models = [model(1, 1, true), model(2, 2), model(3, 3), model(4, 2, true), model(5, 3, true)];

    expect(getSelectedTabModels(models, "image").map((item) => item.id)).toEqual([2, 4]);
    expect(getSelectedTabModels(models, "video").map((item) => item.id)).toEqual([3, 5]);
    expect(pickDefaultModelId(models, "image")).toBe(4);
    expect(pickDefaultModelId(models, "video")).toBe(5);
    expect(pickDefaultModelId([model(10, 2)], "video")).toBeNull();
  });

  it("derives the visible generation badge from tab and reference images", () => {
    expect(QUICK_GENERATION_MODE_LABELS[getQuickGenerationMode("image", false)]).toBe("文生图");
    expect(QUICK_GENERATION_MODE_LABELS[getQuickGenerationMode("image", true)]).toBe("图生图");
    expect(QUICK_GENERATION_MODE_LABELS[getQuickGenerationMode("video", false)]).toBe("文生视频");
    expect(QUICK_GENERATION_MODE_LABELS[getQuickGenerationMode("video", true)]).toBe("图生视频");
  });

  it("switches default video model away from first-frame-only models for multi-reference input", () => {
    const firstFrameOnly = {
      ...model(11, 3, true),
      name: "首图视频模型",
      capabilities: ["IMAGE_TO_VIDEO"],
      remark: "最多 1 张参考图",
    };
    const multiReference = {
      ...model(12, 3),
      name: "多参考视频模型",
      capabilities: ["IMAGE_TO_VIDEO"],
      remark: "最多 3 张参考图",
    };
    const textOnlyVideo = {
      ...model(13, 3),
      capabilities: ["TEXT_TO_VIDEO"],
    };
    const models = [firstFrameOnly, multiReference, textOnlyVideo];

    expect(getModelMaxReferenceImages(firstFrameOnly)).toBe(1);
    expect(getModelMaxReferenceImages(multiReference)).toBe(3);
    expect(getCompatibleModels(models, "video", "IMAGE_TO_VIDEO", 3).map((item) => item.id)).toEqual([12]);
    expect(pickDefaultModelId(models, "video", "IMAGE_TO_VIDEO", 3)).toBe(12);
  });
});

describe("workspace homepage acceptance contracts", () => {
  it("keeps the four shortcut entries and new-project empty-state path", () => {
    expect(workspacePage).toContain("新建画布");
    expect(workspacePage).toContain('href="/templates"');
    expect(workspacePage).toContain('href="/community"');
    expect(workspacePage).toContain('href="/assets"');
    expect(workspacePage).toContain("新建项目");
    expect(workspacePage).toContain("还没有项目，从这里开始吧");
    expect(workspacePage).toContain("border-dashed");
    expect(workspacePage).toContain("canvasApi.createProject({ name: \"未命名项目\" })");
  });

  it("loads and renders the community inspiration strip as a quiet optional section", () => {
    expect(workspaceCommunityCacheKey(7)).toBe("workspace:community-hot:7");
    expect(communityPostCoverUrl({ id: 1, authorUserId: 1, title: "cover", coverUrl: "cover.png", fileUrl: "file.png" })).toBe("cover.png");
    expect(communityPostCoverUrl({ id: 2, authorUserId: 1, title: "file", fileUrl: "file.png" })).toBe("file.png");

    expect(workspacePage).toContain('getCommunityPosts({ pageNo: 1, pageSize: 8, sort: "hot" })');
    expect(workspacePage).toContain("Array.from({ length: 8 })");
    expect(workspacePage).toContain("setCommunityHidden(posts.length === 0)");
    expect(workspacePage).toContain("if (!ignore && !hasVisiblePosts) setCommunityHidden(true)");
    expect(workspacePage).toContain("scrollbar-hide mt-5 flex gap-3 overflow-x-auto");
    expect(workspacePage).toContain('className="group w-36 shrink-0"');
    expect(workspacePage).toContain("rounded-[10px]");
    expect(workspacePage).toContain("group-hover:scale-[1.02]");
    expect(workspacePage).toContain("truncate text-xs font-medium text-charcoal");
    expect(workspacePage).toContain("`/community/${encodeURIComponent(String(post.id))}`");
  });
});

describe("admin provider proxy form contracts", () => {
  it("uses proxyId as the only visible proxy configuration and preserves legacy fields by omission", () => {
    const template = providerForm.slice(0, providerForm.indexOf("<script setup"));

    expect(template).toContain(':label="t(\'aigc.model.fields.proxy\')" prop="proxyEnabled"');
    expect(template).toContain(':label="t(\'aigc.model.fields.proxy\')" prop="proxyId"');
    expect(template).toContain('v-model="formData.proxyId"');
    expect(template).toContain("openProxyManage");
    expect(template).not.toContain("proxyProtocol");
    expect(template).not.toContain("proxyHost");
    expect(template).not.toContain("proxyPort");
    expect(template).not.toContain("proxyUsername");
    expect(template).not.toContain("proxyPassword");

    expect(providerForm).toContain("delete data.proxyProtocol");
    expect(providerForm).toContain("delete data.proxyHost");
    expect(providerForm).toContain("delete data.proxyPort");
    expect(providerForm).toContain("delete data.proxyUsername");
    expect(providerForm).toContain("delete data.proxyPassword");
    expect(providerForm).toContain("if (!data.proxyEnabled)");
    expect(providerForm).toContain("data.proxyId = undefined");
    expect(providerForm).toContain("window.open('/aigc/model/proxy', '_blank', 'noopener,noreferrer')");
  });
});
