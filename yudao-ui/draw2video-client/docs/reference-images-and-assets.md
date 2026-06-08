# 参考图与资产选择说明

本文记录 `/app` 快速生成入口的参考图能力，以及 `/assets` 资产库按来源拆分后的联调约定。

## 工作台入口

入口页面：`src/app/(app)/app/page.tsx`。

- `Ctrl+V` / 粘贴剪贴板图片时，会把图片上传为 `IMAGE` 资产并加入参考图列表。
- 纸夹按钮支持选择一张或多张本地图片文件。
- 加号按钮打开资产选择弹窗，弹窗从当前用户资产库中加载图片资产。
- 粘贴、本地上传、资产选择得到的图片都会追加到同一个参考图列表。
- 第一张参考图作为主预览图展示，并继续用于旧版单图字段兼容。
- 多余参考图在缩略图条中展示，可以逐张移除。
- 参考图列表缓存到 `localStorage` 的 `copse:workspace:reference-images`，切走后再回到 `/app` 会恢复当前参考图。

## 快速生成请求

快速生成请求保留旧版单图字段，同时新增多图数组字段。

顶层字段示例：

```json
{
  "referenceAssetId": 101,
  "referenceAssetIds": [101, 102],
  "referencePreviewUrl": "https://example.com/first.png",
  "referencePreviewUrls": [
    "https://example.com/first.png",
    "https://example.com/second.png"
  ]
}
```

`inputParams` 会写入当前模型参数模板的默认值，并补齐多种参考图别名，兼容不同供应商客户端：

```json
{
  "providerModel": "provider-model-name",
  "referenceAssetIds": [101, 102],
  "referenceImageIds": ["101", "102"],
  "referenceImages": [
    "https://example.com/first.png",
    "https://example.com/second.png"
  ],
  "referencePreviewUrls": [
    "https://example.com/first.png",
    "https://example.com/second.png"
  ],
  "inputImageIds": ["101", "102"],
  "inputImageUrls": [
    "https://example.com/first.png",
    "https://example.com/second.png"
  ],
  "inputImages": [
    {
      "imageId": "101",
      "fileName": "first.png",
      "dataUrl": "https://example.com/first.png",
      "mimeType": "image/png"
    }
  ]
}
```

生成模式按模型类型和参考图数量自动选择：

- 图片模型且无参考图：`TEXT_TO_IMAGE`
- 图片模型且有参考图：`IMAGE_TO_IMAGE`
- 视频模型且无参考图：`TEXT_TO_VIDEO`
- 视频模型且有参考图：`IMAGE_TO_VIDEO`

模型参数通过 `getAigcModelParamList(modelId, generationMode)` 获取。启用状态的参数模板会优先使用后端默认值，其次使用可选项默认值，最后再使用常见图片/视频参数的保守兜底值，避免快速生成入口因为缺少模型参数而无法发起请求。

## 后端兼容规则

快速生成 VO 同时支持以下字段：

- `referenceAssetId`
- `referenceAssetIds`
- `referencePreviewUrl`
- `referencePreviewUrls`

`AigcCanvasProjectServiceImpl` 归一化时优先使用数组字段；数组为空时回退到单图字段。视频快速生成会把所有参考图资产绑定到首个节点，项目封面仍使用第一张参考图，保持旧逻辑兼容。

`runReqVO.inputParams` 接收完整归一化参数，用于真正发起生成任务。节点展示用的 `params` 会剥离 `referenceImages`、`referenceAssetIds`、`referenceImageIds`、`inputImages`、`inputImageUrls`、`inputImageIds`，避免把请求专用字段重复塞入节点参数。

供应商客户端兼容方式：

- GPT / Gemini 图片客户端读取 `inputImages` 和 `inputImageUrls`。
- Grok / 兼容视频客户端读取 `referenceImages`，只支持单图时取第一张。

## 资产库来源拆分

资产页：`src/app/(app)/assets/page.tsx`。

图片资产不再把上传图和生成图混在一起，而是按 `sourceType` 拆分：

- 生成图片：`assetType=IMAGE&sourceType=GENERATE`
- 上传图片：`assetType=IMAGE&sourceType=UPLOAD`
- 生成视频：`assetType=VIDEO`
- 其它文件：客户端兜底分组，展示非图片、非视频资产

`AigcAssetPageParams` 增加 `sourceType`，共享资产分页 API 会把它作为 query 参数传给 `/aigc/asset/my-page`。

## 验证记录

本次改动已执行：

```bash
npm run lint
npx tsc --noEmit
mvn -pl yudao-module-aigc-workflow/yudao-module-aigc-workflow-server -am -DskipTests compile
git diff --check
```

验证说明：

- `npm run lint` 通过，仅有 Next.js `<img>` warning。
- `npx tsc --noEmit` 通过。
- 工作流服务端 Maven compile 通过。
- `git diff --check` 无空白错误，仅有 Git 的 CRLF 提示。
- `npm run build` 曾尝试执行，但本地环境中超时，不能记录为通过项。
- 本地 `/app` 浏览器验证需要登录态；没有 token 时，应用布局会跳回公开首页并打开登录弹窗，这是当前鉴权守卫的预期行为。
