# Copse App

Copse is a Next.js App Router prototype for a ToC AI creative workspace. The current core surface is `/canvas`, a React Flow canvas where users create, connect, and generate image, text, and video nodes.

## Stack

- Next.js App Router
- TypeScript
- Tailwind CSS
- Lucide icons
- React Flow via `@xyflow/react`
- Motion for React via `motion`
- IndexedDB for local image payload cache
- `/app-api` route handlers for backend/provider-facing calls

## Getting Started

```bash
npm install
npm run dev
```

Open `http://localhost:3000/canvas`.

## Environment

Image generation is proxied through the server route so provider keys do not live in frontend code.

Create `.env.local`:

```bash
COPSE_IMAGE_API_BASE_URL=https://img.copse.top/v1
COPSE_IMAGE_API_KEY=your_key_here
COPSE_IMAGE_MODEL=gpt-image-2

COPSE_VIDEO_API_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
COPSE_VIDEO_API_KEY=your_video_key_here
COPSE_VIDEO_MODEL=doubao-seedance-2-0-260128
```

The image route is:

```text
POST /app-api/ai/generation/task/create
```

It supports text-to-image and image-to-image/edit mode. The route returns a Yudao-style response:

```json
{ "code": 0, "msg": "success", "data": { "taskId": "...", "status": "complete", "imageUrls": [] } }
```

The video routes are:

```text
POST /app-api/ai/video/generation/task/create
GET  /app-api/ai/video/generation/task/[taskId]
```

Video generation is asynchronous. The create route returns a `taskId`; the node keeps polling the query route while the upstream status is `running` and replaces the video placeholder when a `videoUrl` is returned.

Wan 2.2 local server proxy:

```txt
POST /app-api/ai/video/wan/task/create
GET  /app-api/ai/video/wan/task/[taskId]
GET  /app-api/ai/video/wan/task/[taskId]/video
```

Wan 2.2 supports text-to-video when no reference image is connected and image-to-video when one image is connected. The frontend only exposes the two supported sizes, `1280*704` and `704*1280`; other inference params use server defaults. Wan queue timing treats `queued` as waiting time, starts the visible generation timer only after `running`, and freezes the elapsed time at `succeeded` or `failed`.

## Canvas Model

The canvas is moving toward a node-native creative workspace:

- `ImageNode`: uploaded images, draft image placeholders, and generated images.
- `TextNode`: editable/resizable text cards with a selected-state text composer.
- `VideoNode`: draft video placeholders and generated video results, backed by the Ark Seedance task API.
- `PromptNode` and `ResultNode`: legacy-compatible components still present while the canvas migrates to direct node replacement.

Project model:

- `/app` shows recent projects and a new-project entry.
- `/projects` is the project library. The sidebar project icon should land here before entering a canvas.
- `/assets` is the asset library. The sidebar asset icon should land here and show generated image/video assets in a compact masonry wall.
- `/canvas?projectId=...` opens one project's canvas. `/create/image` is kept only as a compatibility redirect.
- Canvas drafts are scoped by `projectId`; creating or opening a project updates its last-opened time and lightweight summary.
- Project cards expose a cover-image icon next to delete. It opens a two-column paged picker: project image assets on the left and all user image assets on the right. Saving updates `coverAssetId` only; signed cover URLs remain runtime display data.

Asset library:

- Generated images and videos are loaded from the backend asset page API incrementally instead of fetching the entire asset set at once.
- Uploaded image assets are listed separately from generated image assets by `sourceType`.
- Asset category counts come from `GET /aigc/asset/my-category-counts` with the same search filters as the list. Do not recalculate cross-category totals from the current loaded page.
- The asset wall uses a compact Muuri layout: media keeps its natural ratio, wide items may span multiple columns, and column count adapts to the container width.
- Scrolling near the bottom loads the next page; when there are no more records the page shows an “已加载全部” state.
- Asset search is debounced and should be sent through paged backend filtering rather than filtering a preloaded full dataset.
- Each asset item links to its asset detail or source project canvas when only a local fallback exists.
- The `/app` plus button opens an image asset picker. Selected generated or uploaded images become quick-generate reference images.
- Private OSS/S3 asset URLs are treated as runtime access tickets. UI state may hold `fileUrl`, `thumbnailUrl`, `previewUrl`, `videoUrl`, or `files[].accessUrl` for display, but persisted canvas/project data should keep stable `assetId` / `outputAssetId` values and refresh URLs from the asset detail/access-url APIs when the page opens or a URL is close to expiring. Canvas should prefer the batch `POST /aigc/asset/access-urls` API when refreshing many node previews.

Current image generation behavior:

- `/app` quick generation supports pasted, uploaded, and asset-picked reference images. References are shown immediately, cached in `localStorage`, and restored when the user returns to the page. The cache is keyed by stable asset metadata; signed preview URLs are refreshed from the asset API instead of being trusted as permanent links.
- Clipboard images can be pasted directly with `Ctrl+V` / `Cmd+V` on `/app`; pasted files are uploaded/assetized as reference images and previewed immediately.
- The `/app` plus icon opens an image asset picker. The picker can choose generated images or uploaded images, but selection only affects reference inputs and does not change the source type of generated output assets.
- Multiple quick-generate reference images are supported. The first image is still sent through legacy single-reference fields for compatibility, while the complete list is sent through array fields.
- Quick generation filters models by the current input capability (`TEXT_TO_IMAGE` / `IMAGE_TO_IMAGE` / `TEXT_TO_VIDEO` / `IMAGE_TO_VIDEO`), opens a `DynamicParamForm` parameter popover from the sliders button, and sends the selected params with template defaults filled in.
- When `/app` creates a canvas project from text plus reference images, each reference image should become a canvas image node and be connected to the target generation node. Multi-image requests use all references for node creation and request arrays; single-image providers or legacy fields use the first reference image.
- Quick generation must not submit until the prompt, selected model, required params, and reference compatibility rules are valid. Missing parameter configuration should be solved by showing the model parameter popover, not by silently disabling submit.
- New image generation creates an `ImageNode` draft placeholder.
- Image models, model parameters, and price display come from the AIGC model APIs. Backend SELECT options/default values are normalized so saved JSON-array values render as plain values such as `1:1`.
- Existing image nodes persist `aigcModelId`; refreshing a project should restore the node's selected model instead of falling back to the default model.
- Selecting the image opens a composer below the image.
- Reference images are connected to the selected image node and shown as thumbnails in the composer toolbar.
- Generation updates the same image node in place instead of creating a separate result node.
- Server-backed node runs apply the final `TASK_STATUS_PATCH` from `operation.operationJson.payload.patch` immediately after `run/sync` succeeds. Do not rely only on realtime/sync echo for the node that initiated the run.
- Image and video generation patches persist output asset IDs, not signed output URLs. Runtime keys such as `previewUrl`, `outputPreviewUrl`, `videoUrl`, and `assetUrlExpireTime` are stripped from snapshots, operation payloads, and local project persistence, then repopulated from current asset responses for display.
- Uploaded images are media-only nodes; selecting one does not open a prompt composer.
- Empty draft images render inside a fixed preview slot, and the placeholder card changes aspect ratio when image size params change.
- Real images and sketch previews display at their natural aspect ratio, scaled down to a bounded max size with the full image visible. They should not show an extra card border or black letterbox around the media.
- Image card titles are part of the zoomed canvas content. The selected media toolbar, selected-node composer, and side create handles keep a stable screen size while zoom changes.
- Server-backed image nodes persist the returned `taskId` immediately after task creation. If the page is refreshed while the node is pending, the node resumes polling from the stored `taskId`.

Current text behavior:

- Double-click blank canvas to open a create menu.
- Create `Text`, `Image`, or `Video` nodes.
- Text cards can be double-clicked for direct editing.
- Text cards can be resized from the bottom-right handle.
- Text card titles are part of the zoomed canvas content; the selected text composer keeps a stable screen size while zoom changes.
- Server-backed text nodes persist the returned `taskId` immediately after task creation and resume pending polling after refresh.
- Text generation is currently mocked and writes the result back into the same text node.

Current sketch behavior:

- Sketch nodes use a freeform tldraw canvas instead of a fixed frame.
- Saving a sketch exports the natural bounds of visible sketch content. Empty sketches save a blank preview.
- Existing sketches that still contain the legacy locked frame remove that frame on editor mount.
- The sketch editor supports PNG, JPG, WEBP, and GIF uploads into the tldraw scene.
- While the sketch editor is open, React Flow keyboard shortcuts are disabled so sketch deletion, undo, redo, and context-menu interactions stay inside tldraw.
- Sketch card titles are part of the zoomed canvas content; selected-node toolbar and side create handles keep stable screen size while zoom changes.

Current video behavior:

- `/app` quick video generation uses the same reference-image cache and multi-image payload. Providers that only support one image use the first reference image.
- New video nodes are draft placeholders.
- Video generation should use AIGC model capability lists and parameter templates for `TEXT_TO_VIDEO`, `IMAGE_TO_VIDEO`, `FIRST_LAST_FRAME_VIDEO`, and `MULTI_REF_VIDEO` instead of relying on hardcoded frontend model buttons.
- Seedance video models expose persistent mode controls in the composer. The visible modes are derived from the selected model capabilities; connected image references determine which modes are currently selectable.
- Selecting a draft/generated video opens a composer below the fixed preview slot.
- The video placeholder changes aspect ratio when video params change.
- Uploaded video nodes are media-only nodes and do not open a prompt composer.
- Video upload accepts MP4/MOV clips, validates 2 seconds to 3 minutes, and rejects files over 200MB.
- Video references support `image -> video`, first/last frame, and multi-reference payloads when the selected model advertises those capabilities.
- `Seedance 2.0` models use the backend AIGC model/channel configuration and HKCopp OpenAPI generation flow.
- `Wan 2.2` uses the custom Wan server proxy and supports no-reference text-to-video plus single-reference image-to-video.
- Successful video task creation keeps the node in a loading state and polls until a video URL is available. Queued video jobs show `排队中` and do not start elapsed timing until the upstream job becomes `running`.
- Server-backed video nodes persist the returned `taskId` immediately after task creation and resume pending polling after refresh.
- Server-backed video runs apply the final operation patch from `run/sync`, then refresh the current playable URL from `assetId` / `outputAssetId` before clearing the user's visible result. A successful run must not leave the node blank until the next page refresh.
- Video card titles are part of the zoomed canvas content; selected media toolbar, selected-node composer, and side create handles keep stable screen size while zoom changes.

Canvas history:

- `Cmd+Z` / `Ctrl+Z`: undo.
- `Cmd+Shift+Z` / `Cmd+Y`: redo.
- History currently tracks graph state changes such as node creation, deletion, movement, edges, params, and prompt changes.

Canvas navigation:

- Trackpad two-finger scroll pans the canvas.
- Trackpad pinch zooms the canvas.
- Double-clicking the blank canvas opens the create menu instead of zooming.
- The canvas uses a low-contrast dotted positioning background in both light and dark themes.
- Node preview cards use opaque background surfaces so the positioning dots do not show through cards.
- Selected node toolbars, selected-node composers, and side create handles keep a stable screen size while the React Flow canvas zoom changes. This keeps prompt entry usable even when the canvas is zoomed out.
- Node titles stay in canvas coordinates, so they scale visually with the canvas zoom and remain aligned to their preview content.
- The canvas skips history/snapshot churn while nodes are actively dragged and saves the final position after drag stop.
- Image nodes subscribe only to the React Flow state they actually need, so unrelated canvas changes do not force every image node through model/parameter recomputation.
- Canvas edges keep the normal React Flow line style. Signal/highlight animation may be layered on top for a light sweep effect, but it should not replace the basic path shape or break selection/connection behavior.
- Realtime messages must be filtered by the active server project ID before applying presence, member updates, operation acks, or rejection events. The `projectId` field may arrive as a number or string, so compare using a normalized string.

Canvas motion:

- Motion is used for local UI transitions only.
- Node preview cards fade/scale in without touching React Flow's canvas transform.
- Selected-node composers, model pickers, parameter popovers, context menus, create menus, drag overlays, and reference-picker banners animate in/out.
- Image and video placeholder cards animate smoothly when aspect ratio params change.
- Image generation loading uses an in-node horizontal highlight sweep with status and elapsed time centered over the media.
- Parameter segmented controls use a sliding selected-state indicator for size, ratio, quality, format, moderation, video ratio, resolution, duration, and audio switches.

## Local Persistence

Canvas persistence is split:

- `localStorage`: lightweight canvas graph, node metadata, edges, viewport.
- `localStorage`: project metadata and per-project canvas drafts keyed by `projectId`.
- IndexedDB: image data keyed by `imageId`.
- IndexedDB: uploaded video data keyed by `videoId`.

Large image `dataUrl` values are stripped before saving the canvas graph to `localStorage`.
Uploaded video `data:` URLs are also stripped before saving; generated remote video URLs can stay in the graph.
Opening `/canvas` without a saved snapshot should show an empty canvas. The app should not auto-create a default image node unless the user explicitly creates, pastes, uploads, drags, or quick-generates content.

Server persistence target:

- MySQL stores `canvas_project`, `canvas_snapshot` metadata, `canvas_operation_log`, and `canvas_asset_ref`. Frontend payloads should keep stable IDs and lightweight node data so these tables stay queryable.
- OSS / MinIO stores large snapshot JSON bodies and historical snapshot packages. The frontend must not assume a snapshot body always comes from MySQL inline JSON.
- Snapshot storage is threshold based: after runtime URLs and large local media are stripped, bodies up to 512KB with up to 200 nodes, 500 edges, and 64KB per node data may stay inline in MySQL; anything larger should be stored in OSS / MinIO with MySQL metadata only. Bodies at or above 2MB are always object-store snapshots.
- Redis stores collaboration room hot state, pending operation state, and presence. A successful realtime accept may mean the operation is accepted into the hot path, not necessarily persisted to MySQL yet.
- Runtime asset URLs from private OSS/S3 expire. Canvas, project covers, and asset pickers should refresh previews from asset APIs by `assetId` / `outputAssetId`, not reuse saved signed URLs.
- Project cards display `nodeCount`, `assetCount`, `coverAssetId`, and runtime cover URL from backend project data. If a backend response lacks `coverAssetId`, the server may repair it from snapshot/asset refs; the frontend should not persist signed cover URLs as the project identity.

## Auth, Theme, And Email Code UI

The public and authenticated shells support light/dark theme switching through `ThemeProvider` and `ThemeToggle`.

Auth is handled by `src/features/auth/*`:

- `AuthModal` owns the animated modal surface.
- `AuthPanel` keeps the default flow focused on email/password login.
- Secondary flows are available for email-code login, phone-code login, registration, and password reset.
- `VerificationCodeField` is the shared email/SMS code sending UI used by auth and profile email binding.
- Successful login with no explicit redirect lands on `/app`.

Email verification content is not hardcoded in the client. The member email code service uses Yudao mail templates:

- Template codes live in `yudao-module-member/.../MemberEmailCodeSceneEnum.java`.
- Initial template content lives in `yudao-module-member/yudao-module-member-server/src/main/resources/member-email-register.sql`.
- Existing databases may need an admin mail-template update or a migration because changing the SQL initializer only affects newly initialized environments.

## Useful Commands

```bash
npm run lint
npm run build
```

Run both before handing off canvas changes. Docker image builds execute `npm run build`, so TypeScript-only issues such as realtime message union types will fail deployment even when local dev mode appears fine.

## Important Files

- `src/app/(app)/canvas/page.tsx`: canvas orchestration.
- `src/app/(app)/create/image/page.tsx`: compatibility redirect to `/canvas`.
- `src/features/canvas/ImageNode.tsx`: unified image/draft/generated image node.
- `src/features/canvas/TextNode.tsx`: editable/resizable text node.
- `src/features/canvas/VideoNode.tsx`: video draft/generated node and Seedance task polling UI.
- `src/features/canvas/use-generation.ts`: image generation request helpers.
- `src/app/app-api/ai/generation/task/create/route.ts`: image provider proxy route.
- `src/app/app-api/ai/video/generation/task/create/route.ts`: video task creation proxy route.
- `src/app/app-api/ai/video/generation/task/[taskId]/route.ts`: video task polling proxy route.
- `src/features/image-generation/*`: image model and parameter UI.
- `src/features/canvas/use-canvas-storage.ts`: localStorage persistence.
- `src/features/canvas/image-store.ts`: IndexedDB image cache.
- `src/features/auth/VerificationCodeField.tsx`: shared email/SMS verification-code send control.
- `src/features/theme/*`: local theme provider and toggle.
