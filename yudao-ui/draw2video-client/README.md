# Copse App

Copse is a Next.js App Router prototype for a ToC AI creative workspace. The current core surface is `/create/image`, a React Flow canvas where users create, connect, and generate image, text, and video nodes.

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

Open `http://localhost:3000/create/image`.

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
- `/assets` is the asset library. The sidebar asset icon should land here and show generated image/video tables.
- `/create/image?projectId=...` opens one project's canvas.
- Canvas drafts are scoped by `projectId`; creating or opening a project updates its last-opened time and lightweight summary.

Asset library:

- Generated images and videos are collected from saved project canvases.
- Uploaded media is not listed as a generated asset.
- Each asset row links back to the source project canvas.

Current image generation behavior:

- New image generation creates an `ImageNode` draft placeholder.
- Selecting the image opens a composer below the image.
- Reference images are connected to the selected image node and shown as thumbnails in the composer toolbar.
- Generation updates the same image node in place instead of creating a separate result node.
- Server-backed node runs apply the final `TASK_STATUS_PATCH` from `operation.operationJson.payload.patch` immediately after `run/sync` succeeds. Do not rely only on realtime/sync echo for the node that initiated the run.
- Uploaded images are media-only nodes; selecting one does not open a prompt composer.
- Empty draft images render inside a fixed preview slot, and the placeholder card changes aspect ratio when image size params change.
- Real images and sketch previews display at their natural aspect ratio, scaled down to a bounded max size with the full image visible. They should not show an extra card border or black letterbox around the media.

Current text behavior:

- Double-click blank canvas to open a create menu.
- Create `Text`, `Image`, or `Video` nodes.
- Text cards can be double-clicked for direct editing.
- Text cards can be resized from the bottom-right handle.
- Text generation is currently mocked and writes the result back into the same text node.

Current video behavior:

- New video nodes are draft placeholders.
- Selecting a draft/generated video opens a composer below the fixed preview slot.
- The video placeholder changes aspect ratio when video params change.
- Uploaded video nodes are media-only nodes and do not open a prompt composer.
- Video upload accepts MP4/MOV clips, validates 2 seconds to 3 minutes, and rejects files over 200MB.
- MVP video references only support `image -> video`.
- `Seedance 2.0` uses the Ark async task API.
- `Wan 2.2` uses the custom Wan server proxy and supports no-reference text-to-video plus single-reference image-to-video.
- Successful video task creation keeps the node in a loading state and polls until a video URL is available. Queued video jobs show `排队中` and do not start elapsed timing until the upstream job becomes `running`.

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

Run both before handing off canvas changes.

## Important Files

- `src/app/(app)/create/image/page.tsx`: canvas orchestration.
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
