import { FileText, Music, PlaySquare } from "lucide-react";
import type { AigcAsset } from "../asset-types";
import { getAssetPreviewUrl, getAssetTypeLabel } from "../asset-dictionaries";

export function AssetPreview({ asset, large = false }: { asset: AigcAsset; large?: boolean }) {
  const type = asset.assetType;
  const previewUrl = getAssetPreviewUrl(asset);
  const sizeClass = large ? "min-h-[360px]" : "h-24";

  if (type === "IMAGE" && previewUrl) {
    return (
      <div
        className={`${sizeClass} rounded-xl border border-border-warm bg-muted bg-contain bg-center bg-no-repeat`}
        style={{ backgroundImage: `url(${previewUrl})` }}
        role="img"
        aria-label={asset.title || "图片资产预览"}
      />
    );
  }

  if (type === "VIDEO") {
    if (large && asset.fileUrl) {
      return <video src={asset.fileUrl} poster={asset.coverUrl || asset.thumbnailUrl} controls className="max-h-[520px] w-full rounded-xl border border-border-warm bg-muted" />;
    }
    return (
      <div className={`${sizeClass} flex items-center justify-center rounded-xl border border-border-warm bg-muted text-muted-gray`}>
        <PlaySquare className={large ? "size-10" : "size-6"} />
      </div>
    );
  }

  if (type === "AUDIO" && large && asset.fileUrl) {
    return (
      <div className="rounded-xl border border-border-warm bg-background p-6">
        <audio src={asset.fileUrl} controls className="w-full" />
      </div>
    );
  }

  const Icon = type === "AUDIO" ? Music : FileText;
  return (
    <div className={`${sizeClass} flex flex-col items-center justify-center rounded-xl border border-border-warm bg-muted text-muted-gray`}>
      <Icon className={large ? "size-10" : "size-6"} />
      {large && <span className="mt-3 text-sm">{getAssetTypeLabel(type)}</span>}
    </div>
  );
}
