export type MediaPreviewKind = "image" | "video";

export type MediaPreviewInfoValue = string | number | boolean | null | undefined;

export type MediaPreviewInfoItem = {
  label: string;
  value: MediaPreviewInfoValue;
};

export type MediaPreviewEditableAsset = {
  title: string;
  description: string;
  tags: string;
  visibility?: string;
  auditStatus?: string;
  status?: string;
  auditReason?: string;
  taskId?: number;
  saving?: boolean;
  canEdit?: boolean;
  canDownload?: boolean;
  canDelete?: boolean;
  canPublish?: boolean;
  onChange: (patch: Partial<Pick<MediaPreviewEditableAsset, "title" | "description" | "tags">>) => void;
  onSave: () => void | Promise<void>;
  onVisibilityChange?: (visibility: string) => void | Promise<void>;
  onDownload?: () => void | Promise<void>;
  onDelete?: () => void | Promise<void>;
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
  editableAsset?: MediaPreviewEditableAsset;
};
