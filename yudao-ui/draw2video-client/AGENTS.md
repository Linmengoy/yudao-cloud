<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# Copse App Agent Guide

## Product Direction

Copse is a ToC AI image creation app. The admin/backend system is Yudao, but this frontend is a standalone Next.js app focused on user-facing creation workflows.

The MVP is not a generic admin panel and should not copy the Yudao Vue admin UI. It should feel like a lightweight creative workspace:

- Marketing pages for unauthenticated users.
- Auth modal instead of a hard route transition for login.
- After login, use the left workspace sidebar as the primary navigation.
- `/create/image` is the core product surface.
- Video creation is now in MVP on the same canvas. Keep it lightweight: draft/generated `VideoNode`, image-to-video references, async task polling, and direct node replacement. Defer advanced video workflows such as storyboards, timeline editing, multi-shot planning, masks, or batch queues.

## Tech Stack

- Next.js App Router.
- TypeScript.
- Tailwind CSS.
- Lucide icons.
- React Flow via `@xyflow/react` for the canvas.
- Motion for React via `motion` for local UI animation.
- Yudao backend APIs will eventually be called through `/app-api`.

Use existing project helpers and components before adding new abstractions.

## Important Files

- `src/app/(marketing)/layout.tsx`: public marketing shell.
- `src/app/(app)/layout.tsx`: authenticated workspace shell.
- `src/app/(app)/create/image/page.tsx`: image creation canvas.
- `src/features/canvas/*`: React Flow nodes, canvas storage, upload, clipboard, context menu.
- `src/features/canvas/ImageNode.tsx`: unified image node for uploads, draft placeholders, and generated image results.
- `src/features/canvas/TextNode.tsx`: editable/resizable text node and mocked text generation composer.
- `src/features/canvas/VideoNode.tsx`: video draft/generated node and async Seedance task polling UI.
- `src/features/canvas/image-upload.ts`: local image/video upload validation and metadata extraction.
- `src/features/canvas/image-store.ts`: IndexedDB cache for uploaded image and video data.
- `src/features/image-generation/*`: image generation params, model config, size handling.
- `src/app/app-api/ai/generation/task/create/route.ts`: server-side image provider proxy.
- `src/app/app-api/ai/video/generation/task/create/route.ts`: server-side video task creation proxy.
- `src/app/app-api/ai/video/generation/task/[taskId]/route.ts`: server-side video task polling proxy.
- `src/app/app-api/ai/video/wan/task/*`: server-side Wan 2.2 task creation, polling, and protected video streaming proxy.
- `src/features/auth/*`: current mock auth layer.
- `src/features/auth/VerificationCodeField.tsx`: shared email/SMS verification-code sending UI for auth and profile email binding.
- `src/features/theme/*`: theme provider/toggle for light and dark mode.
- `src/features/wallet/mock-wallet.ts`: current mock balance layer.
- `src/lib/api-client.ts`: future Yudao app API client integration.

## Design Rules

- Keep the UI quiet, warm, and utilitarian.
- Avoid marketing-style cards inside the authenticated workspace.
- The logged-in workspace should rely on the left sidebar, not a top tab bar.
- Top-right wallet/avatar controls should live in the sidebar bottom area where possible.
- Cards should not be overly rounded. Keep controls compact and readable.
- Use Lucide icons for icon buttons.
- Every icon-only control needs an accessible label or tooltip.
- Floating menus and popovers must close on outside click, Escape, and after executing an action.

## Auth And Backend Assumptions

Current auth may be mocked while product logic is built.

Do not assume Yudao admin users can log in through `/app-api/member/auth/login`. Admin users and member users are separate domains in Yudao.

Future backend integration should use:

- Member auth through `/app-api/member/auth/*`.
- Wallet/balance through app/member wallet APIs.
- File upload through Yudao file upload APIs.
- Generation task creation/polling through new `/app-api/ai/generation/*` endpoints.
- Video task creation/polling through `/app-api/ai/video/generation/*` endpoints.

Do not put provider API keys or model secrets in the frontend.

## Canvas Product Model

Project library:

- `/app` should show recent projects and an entry for creating a new project.
- `/projects` is the project library/list page. The sidebar project icon should navigate here first.
- `/assets` is the asset library/list page. The sidebar asset icon should navigate here and show generated image/video categories.
- Opening a project should navigate to `/create/image?projectId=<id>`.
- Canvas drafts must be scoped by project ID. Do not mix all projects into one global canvas draft.
- Project metadata should stay lightweight: name, kind, timestamps, node count, asset count, and optional thumbnail metadata. Do not store large media data in project metadata.

Asset library:

- List generated images and generated videos separately.
- Do not list uploaded reference media as generated assets.
- Asset rows should link back to their source project canvas.
- Until the backend asset service exists, local asset discovery may scan saved per-project canvas drafts.

The `/create/image` canvas should model creation as connected, directly editable nodes:

- `ImageNode`: uploaded images, draft image placeholders, generated image results, and reference images.
- `TextNode`: editable/resizable text cards, with generation results written back into the same node.
- `VideoNode`: draft video placeholders, generated video results, and future uploaded video media.
- `PromptNode` and `ResultNode`: legacy-compatible components may remain during migration, but new image generation should prefer the unified `ImageNode` flow.

Current image generation flow:

- Creating an image prompt creates a draft `ImageNode` placeholder, not a separate prompt card.
- Selecting an image node opens a composer below the image.
- Reference images are linked by entering reference-pick mode from the composer `+` button, then clicking another image node.
- Reference thumbnails appear in the composer toolbar on the same row as the `+` button.
- Removing one thumbnail removes only that edge.
- Generation detects incoming image edges and switches mode from `generate` to `edit`.
- Generation writes the result back into the same draft image node instead of creating a `ResultNode`.
- Uploaded images are media-only nodes; selecting one should not open the prompt composer.
- Draft images with no `dataUrl` render inside a fixed preview slot. Changing image size params changes only the placeholder card ratio inside that slot; the selected-node composer should not jump.
- Real images should display at their natural aspect ratio, scaled down to a bounded max size without cropping.

Current text node flow:

- Double-clicking blank canvas opens a create menu with `Text`, `Image`, and `Video`.
- `TextNode` supports direct double-click editing.
- `TextNode` supports bottom-right resize.
- Selecting a text node opens a composer below it.
- Text generation is currently mocked and writes the result into the same text node.

Current video node flow:

- Creating a video prompt creates a draft `VideoNode` placeholder.
- Selecting a draft/generated video node opens a composer below a fixed preview slot.
- Changing video aspect ratio changes only the placeholder card ratio inside that slot; the composer should not jump.
- Video MVP uses the real Seedance 2.0 task API through server routes.
- Wan 2.2 uses a custom server proxy. If no image is connected, create a text-to-video job; if one image is connected, create an image-to-video job. Do not expose the Wan API key to the frontend.
- Wan 2.2 currently exposes only `1280*704` and `704*1280` size choices in the UI. Keep frame/sample params at provider defaults unless explicitly requested.
- Video generation is async: submit creates a task, then the node polls until a video URL is returned.
- For video task timing, `queued` means waiting and must not count toward generation time. Start elapsed timing when upstream status becomes `running`; freeze it at `succeeded` or `failed`.
- Generated video results should replace the current draft video node in place.
- Uploaded video nodes are media-only nodes; selecting one should not open the prompt composer.
- Uploaded videos should validate against the current Copse MVP limits: MP4/MOV, 2 seconds to 3 minutes, and no more than 200MB per file.
- Current MVP only supports `image -> video` references. Do not add `video -> video`, `text -> video`, audio, timeline, or storyboard flows unless explicitly requested.

Canvas history:

- Support `Cmd+Z` / `Ctrl+Z` undo and `Cmd+Shift+Z` / `Cmd+Y` redo for graph state changes.
- Undo/redo should restore nodes and edges together.
- Avoid recording hydration as a user history step.
- Avoid recording high-frequency system-only updates when practical, especially polling ticks.

Allowed MVP connections:

- `image -> image`: reference image input for edit/image-to-image generation.
- `image -> text`: image context for future caption/OCR/description workflows.
- `text -> image`: text prompt for image generation.
- `text -> text`: future rewrite/summarize/continue workflows.
- `image -> video`: image reference input for video generation.

Disallow self-connections and unrelated node type combinations.

## React Flow Rules

- Do not use `type: "bezier"` for edges. React Flow does not have a built-in `"bezier"` type.
- Use `type: "default"` or omit `type` for curved default edges.
- Do not use `smoothstep` unless the product explicitly wants right-angle/stepped lines.
- Migrate old saved edge types:
  - `"smoothstep"` -> `"default"`
  - `"bezier"` -> `"default"`
- Define `nodeTypes` outside React components, or memoize them safely, to avoid React Flow warnings.
- Keep `nodrag` only on controls that need their own pointer behavior:
  - buttons
  - inputs
  - textareas
  - selects
  - popovers
- Do not put `nodrag` on image preview areas. Image cards should be draggable from the image body.
- Keep `draggable={false}` on `<img>` elements to prevent native browser image dragging.
- Set `zoomOnDoubleClick={false}` when double-click is used for canvas creation menus.
- Set `zoomOnScroll={false}`, `panOnScroll`, and `zoomOnPinch` so trackpad two-finger scroll pans the canvas while pinch gestures zoom.
- Hide selected-node composers while a node is being dragged; only the node card should move during drag.
- Fixed preview slots should be used for draft image/video placeholders so parameter changes do not move the selected-node composer.
- Keep the dotted canvas positioning background low-contrast and sparse in both light and dark themes.
- Node preview cards must use opaque surfaces so canvas dots do not show through the card body.
- Selected-node composers, small node headers/toolbars, and side create handles should visually keep a stable screen size across React Flow zoom levels.
- Do not animate React Flow's outer node transform or canvas transform. Motion should only animate node internals, floating menus, banners, popovers, and preview-card width/height inside the fixed preview slot.

## Motion Rules

- Use `motion/react` for React UI animation.
- Keep motion short and utilitarian, generally `0.12s` to `0.22s` for opacity/scale transitions.
- Use spring transitions for selected segmented-control indicators and placeholder aspect-ratio changes.
- Parameter segmented controls should use a moving selected-state indicator instead of abrupt background class swaps when practical.
- Floating UI should animate in/out but still obey outside click, Escape, and action-completion close behavior.
- Respect React Flow drag behavior: selected-node composers should not appear while `dragging` is true, and animation wrappers must not block node dragging from preview bodies.

## Auth, Email, And Theme Rules

- Default auth entry should stay simple: email/password first, with email-code login, phone-code login, registration, and reset as secondary flows.
- Auth modal transitions should use `motion/react` and remain short and utilitarian.
- Avoid browser-native focus rings on auth inputs; use the project `input-base` focus treatment.
- Theme state should go through `ThemeProvider` and `ThemeToggle`, not one-off local toggles.
- Use `VerificationCodeField` for email/SMS verification-code sending UI instead of duplicating send buttons and countdown logic.
- Member email verification content is backed by Yudao mail templates, not client UI. Template codes are defined in `yudao-module-member/.../MemberEmailCodeSceneEnum.java`; initializer content is in `yudao-module-member/yudao-module-member-server/src/main/resources/member-email-register.sql`.
- If an existing database already has old `system_mail_template` rows, update them through the admin mail-template page or add a migration. Editing the initializer only affects fresh environments.

## Canvas Persistence

Do not store large image `dataUrl` values directly in localStorage.

Use this split:

- localStorage: lightweight canvas structure.
  - nodes
  - edges
  - viewport
  - image metadata without `dataUrl`
- localStorage: lightweight project metadata and per-project canvas drafts keyed by project ID.
- IndexedDB: image binary/dataUrl cache keyed by `imageId`.

Required behavior:

- Uploading or pasting an image must save image data to IndexedDB.
- Saving canvas must strip `ImageNode.data.dataUrl` before writing localStorage.
- Hydrating canvas must restore `dataUrl` from IndexedDB.
- Hydration must complete before auto-save starts.
- Do not overwrite a saved draft with default/incomplete state during initial hydration.
- If an image cache entry is missing, prefer showing a missing-preview state over silently deleting the node and edges.
- Clearing the canvas should clear both localStorage and the canvas image cache.
- Text node content and dimensions can be stored in localStorage because they are lightweight.
- Video node metadata, task IDs, and generated remote URLs can be stored in localStorage. Uploaded video `data:` URLs should be stored in IndexedDB keyed by `videoId`, then stripped before saving the canvas graph.

## Clipboard And Context Menu

The canvas should support:

- Uploading image files.
- Dragging image files onto the canvas.
- Pasting screenshots/images with `Cmd+V` or `Ctrl+V`.
- Right-click custom context menu.

Paste rules:

- If focus is inside input, textarea, select, or contenteditable, do not intercept paste.
- Keyboard paste should read from `event.clipboardData`.
- Context-menu paste may use `navigator.clipboard.read()` when available.
- If browser permission blocks clipboard read, show a lightweight message and suggest keyboard paste.

Context menu rules:

- Right-click opens one menu at the cursor.
- Left-click outside closes it.
- Escape closes it.
- Canvas pan/zoom/drag/connect should close it.
- Clicking a menu item closes it after the action.

Expected basic menu items:

- Paste.
- Zoom in.
- Zoom out.
- Fit view/show all elements.
- Zoom to 100%.

Image node right-click menu currently supports:

- Copy.
- Paste (disabled until node-level paste is implemented).
- Duplicate.
- Delete.
- Copy to clipboard.

Floating tooltips and node-level context menus should be portaled to `document.body` when using viewport coordinates. React Flow transforms can otherwise offset or scale `fixed` elements.

## Image Generation Parameters

Image generation params should follow the existing `features/image-generation` model, adapted from `gpt_image_playground`:

- `size`: `auto` or normalized `WIDTHxHEIGHT`.
- `quality`: `auto | low | medium | high`.
- `output_format`: `png | jpeg | webp`.
- `output_compression`: `null` for PNG, numeric for JPEG/WebP.
- `moderation`: `auto | low`.
- `n`: 1-10.

The size picker should support:

- Auto.
- Ratio-based sizing with 1K/2K/4K tiers.
- Custom width/height.
- Validation/normalization to legal model dimensions.

## Floating UI Rules

Every floating UI must implement:

- Outside click close.
- Escape close.
- Action completion close.
- No accidental close when clicking inside the floating UI.

This applies to:

- Canvas context menu.
- Node context menus.
- Image/Text/Video composer model picker.
- Image/Video parameter popovers.
- Quality/format/moderation controls.
- Future account and wallet menus.

## Verification Before Finishing

For canvas changes, verify manually:

- Upload at least two images.
- Drag image cards from the image body.
- Create an image draft and select it.
- Connect both uploaded images to one selected image draft through reference-pick mode.
- Confirm the selected image composer shows reference thumbnails on the top toolbar row.
- Refresh and confirm images, edges, and thumbnails remain.
- Generate an image and confirm the same draft image node is replaced by the generated image.
- Double-click blank canvas and confirm the create menu opens without zooming.
- Create a TextNode, edit it, resize it, and run mocked generation.
- Create a VideoNode and verify the placeholder changes ratio inside a fixed preview slot.
- Submit a video task only when API cost is acceptable; otherwise verify the route contract without spending generation credits.
- Confirm uploaded image nodes do not open the image prompt composer.
- Confirm `Cmd+Z` restores a deleted node and `Cmd+Shift+Z` redoes the change.
- Paste a screenshot into the canvas.
- Right-click menu opens and closes correctly.
- Model picker closes on outside click and Escape.
- Generated/reference edges use curved default edges.

Run:

- `npm run lint`
- `npm run build`

If a command cannot be run, state why in the final response.

## Scope Control

Do not port large chunks from Tapnow Studio or other canvas projects. Use them as interaction references only.

Avoid copying a giant single-file app pattern. Keep Copse modular:

- Canvas page orchestration in `page.tsx`.
- Node components in `src/features/canvas`.
- Param UI in `src/features/image-generation`.
- Storage helpers isolated from UI.

Implement MVP interactions first. Defer advanced workflow features such as storyboard nodes, batch queues, grouping, masking, and complex history panels unless explicitly requested.
