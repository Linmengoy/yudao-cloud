import type { ImageNodeData, VideoNodeData } from "./types";

const DB_NAME = "copse_canvas";
const IMAGE_STORE_NAME = "images";
const VIDEO_STORE_NAME = "videos";
const DB_VERSION = 2;

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

export async function saveImage(image: ImageNodeData): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readwrite");
    tx.objectStore(IMAGE_STORE_NAME).put(image);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function loadImage(imageId: string): Promise<ImageNodeData | null> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readonly");
    const req = tx.objectStore(IMAGE_STORE_NAME).get(imageId);
    req.onsuccess = () => resolve(req.result ?? null);
    req.onerror = () => reject(req.error);
  });
}

export async function deleteImage(imageId: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IMAGE_STORE_NAME, "readwrite");
    tx.objectStore(IMAGE_STORE_NAME).delete(imageId);
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
  data: Omit<VideoNodeData, "videoUrl"> & { videoUrl?: string | null };
  blob?: Blob;
}

export async function saveVideo(video: VideoNodeData, blob?: Blob): Promise<void> {
  if (!video.videoId) throw new Error("Video node is missing videoId");
  const data = { ...video };
  delete data.videoUrl;
  const record: VideoStoreRecord = { videoId: video.videoId, data, blob };
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VIDEO_STORE_NAME, "readwrite");
    tx.objectStore(VIDEO_STORE_NAME).put(record);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function loadVideo(videoId: string): Promise<VideoNodeData | null> {
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
        const data = record.data;
        const blob = record.blob instanceof Blob ? record.blob : undefined;
        resolve({
          ...data,
          videoUrl: blob ? URL.createObjectURL(blob) : data.videoUrl ?? null,
        } as VideoNodeData);
        return;
      }
      resolve(result as VideoNodeData);
    };
    req.onerror = () => reject(req.error);
  });
}

export async function deleteVideo(videoId: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VIDEO_STORE_NAME, "readwrite");
    tx.objectStore(VIDEO_STORE_NAME).delete(videoId);
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
