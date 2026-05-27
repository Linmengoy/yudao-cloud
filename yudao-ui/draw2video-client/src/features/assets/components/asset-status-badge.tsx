import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyStatusPill } from "@/features/safety/safety-ui";
import { getAssetAuditStatusLabel, getAssetVisibilityLabel } from "../asset-dictionaries";

export function AssetAuditBadge({ status }: { status?: string }) {
  const safety = getSafetyCopy(status, "asset");
  if (safety.status !== "idle") return <SafetyStatusPill state={{ ...safety, title: getAssetAuditStatusLabel(status) }} />;
  return <span className="inline-flex rounded-full border border-border-warm bg-background px-2 py-1 text-xs text-muted-gray">{getAssetAuditStatusLabel(status)}</span>;
}

export function AssetVisibilityBadge({ visibility }: { visibility?: string }) {
  return (
    <span className="inline-flex rounded-full border border-border-warm bg-muted px-2 py-1 text-xs text-muted-gray">
      {getAssetVisibilityLabel(visibility)}
    </span>
  );
}
