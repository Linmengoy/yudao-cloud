import { getTaskStatusMeta } from "../task-status";

export function TaskStatusBadge({ status }: { status?: string }) {
  const meta = getTaskStatusMeta(status);
  return (
    <span className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-1 text-xs ${meta.className}`}>
      <span className={`size-1.5 rounded-full ${meta.dotClassName}`} />
      {meta.label}
    </span>
  );
}
