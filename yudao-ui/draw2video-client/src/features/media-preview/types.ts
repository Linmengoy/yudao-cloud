export type MediaPreviewKind = "image" | "video";

export type MediaPreviewInfoValue = string | number | boolean | null | undefined;

export type MediaPreviewInfoItem = {
  label: string;
  value: MediaPreviewInfoValue;
};

export type MediaPreviewItem = {
  kind: MediaPreviewKind;
  url: string;
  title?: string;
  prompt?: string;
  fileName?: string;
  createdAt?: string;
  projectName?: string;
  information?: MediaPreviewInfoItem[];
};
