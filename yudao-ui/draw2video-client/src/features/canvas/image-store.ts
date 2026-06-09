import type { ImageNodeData, VideoNodeData } from "./types";

const DB_NAME = "copse_canvas";
const IMAGE_STORE_NAME = "images";
const VIDEO_STORE_NAME = "videos";
const DB_VERSION = 3;

export interface CanvasMediaStoreScope {
  ownerKey?: string | number | null;
  projectId?: string | number | null;
}

type ScopedImageStoreRecord = ImageNodeData & {
  ownerKey?: string | null;
  scopeProjectId?: string | null;
};

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(IMAGE_STORE_NAME)) {
        db.createObjectStore(IMAGE_STORE_NAME, { keyPath: "imageId" });
      }
      if (!db.objectStoreNames.contains(VIDEO_STORE_NAME)) {
        db.createObjectStore(VIDEO_STORE_NAME, { keyPath: "videoId" });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function normalizeScopeValue(value: string | number | null | undefined) {
  return value == null || value === "" ? null : String(value);
}

function scopedRecord<T extends Record<string, unknown>>(record: T, scope?: CanvasMediaStoreScope): T & { ownerKey?: string | null; scopeProjectId?: string | null } {
  return {
    ...record,
    ownerKey: normalizeScopeValue(scope?.ownerKey),
    scopeProjectId: normalizeScopeValue(scope?.projectId),
  };
}

function isScopeMatch(record: { ownerKey?: string | null; scopeProjectId?: string | null } | null | undefined, scope?: CanvasMediaStoreScope) {
  const ownerKey = normalizeScopeValue(scope?.ownerKey);
  const projectId = normalizeScopeValue(scope?.projectId);
  if (!ownerKey && !projectId) return true;
  if (!record?.ownerKey || !record?.scopeProjectId) return false;
  return record.ownerKey === ownerKey && record.scopeProjectId === projectId;
}

function stripImageScope(record: ScopedImageStoreRecord): ImageNodeData {
  const image = { ...record };
  delete image.ownerKey;
  delete image.scopeProjectId;
  return image as ImageNodeData;
}

function stripVideoScope(record: VideoNodeData & { ownerKey?: string | null; scopeProjectId?: string | null }): VideoNodeData {
  const video = { ...record };
  delete video.ownerKey;
  delete video.scopeProjectId;
  return video as VideoNodeData;
}

export async function saveImage(image: ImageNodeData, scope?: CanvasMediaStoreScope): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readwrite");
    tx.objectStore(IMAGE_STORE_NAME).put(scopedRecord(image, scope));
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function loadImage(imageId: string, scope?: CanvasMediaStoreScope): Promise<ImageNodeData | null> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readonly");
    const req = tx.objectStore(IMAGE_STORE_NAME).get(imageId);
    req.onsuccess = () => {
      const result = req.result as ScopedImageStoreRecord | undefined;
      resolve(result && isScopeMatch(result, scope) ? stripImageScope(result) : null);
    };
    req.onerror = () => reject(req.error);
  });
}

export async function deleteImage(imageId: string, scope?: CanvasMediaStoreScope): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readwrite");
    const store = tx.objectStore(IMAGE_STORE_NAME);
    if (!scope?.ownerKey && !scope?.projectId) {
      store.delete(imageId);
    } else {
      const req = store.get(imageId);
      req.onsuccess = () => {
        const result = req.result as ScopedImageStoreRecord | undefined;
        if (isScopeMatch(result, scope)) store.delete(imageId);
      };
    }
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function clearImages(): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readwrite");
    tx.objectStore(IMAGE_STORE_NAME).clear();
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

interface VideoStoreRecord {
  videoId: string;
  ownerKey?: string | null;
  scopeProjectId?: string | null;
  data: Omit<VideoNodeData, "videoUrl"> & { videoUrl?: string | null };
  blob?: Blob;
}

export async function saveVideo(video: VideoNodeData, blob?: Blob, scope?: CanvasMediaStoreScope): Promise<void> {
  if (!video.videoId) throw new Error("Video node is missing videoId");
  const data = { ...video };
  delete data.videoUrl;
  const record: VideoStoreRecord = scopedRecord({ videoId: video.videoId, data, blob }, scope);
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VIDEO_STORE_NAME, "readwrite");
    tx.objectStore(VIDEO_STORE_NAME).put(record);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function loadVideo(videoId: string, scope?: CanvasMediaStoreScope): Promise<VideoNodeData | null> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VIDEO_STORE_NAME, "readonly");
    const req = tx.objectStore(VIDEO_STORE_NAME).get(videoId);
    req.onsuccess = () => {
      const result = req.result as VideoStoreRecord | VideoNodeData | undefined;
      if (!result) {
        resolve(null);
        return;
      }
      if ("data" in result && result.data && typeof result.data === "object") {
        const record = result as VideoStoreRecord;
        if (!isScopeMatch(record, scope)) {
          resolve(null);
          return;
        }
        const data = record.data;
        const blob = record.blob instanceof Blob ? record.blob : undefined;
        resolve({
          ...data,
          videoUrl: blob ? URL.createObjectURL(blob) : data.videoUrl ?? null,
        } as VideoNodeData);
        return;
      }
      const legacyResult = result as VideoNodeData & { ownerKey?: string | null; scopeProjectId?: string | null };
      if (!isScopeMatch(legacyResult, scope)) {
        resolve(null);
        return;
      }
      resolve(stripVideoScope(legacyResult));
    };
    req.onerror = () => reject(req.error);
  });
}

export async function deleteVideo(videoId: string, scope?: CanvasMediaStoreScope): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VIDEO_STORE_NAME, "readwrite");
    const store = tx.objectStore(VIDEO_STORE_NAME);
    if (!scope?.ownerKey && !scope?.projectId) {
      store.delete(videoId);
    } else {
      const req = store.get(videoId);
      req.onsuccess = () => {
        const result = req.result as VideoStoreRecord | undefined;
        if (isScopeMatch(result, scope)) store.delete(videoId);
      };
    }
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function clearVideos(): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VIDEO_STORE_NAME, "readwrite");
    tx.objectStore(VIDEO_STORE_NAME).clear();
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function clearCanvasMediaStore(): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction([IMAGE_STORE_NAME, VIDEO_STORE_NAME], "readwrite");
    tx.objectStore(IMAGE_STORE_NAME).clear();
    tx.objectStore(VIDEO_STORE_NAME).clear();
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}
