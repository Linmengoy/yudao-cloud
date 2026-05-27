import Link from "next/link";
import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyInlineNotice } from "@/features/safety/safety-ui";
import { normalizeSafetyStatus, normalizeSafetyStatusFromError } from "@/features/safety/safety-status";
import type { AigcTask } from "../task-types";

function formatJson(value?: string) {
  if (!value) return "";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

export function TaskResult({ task }: { task: AigcTask }) {
  const safetyStatus = normalizeSafetyStatus(task.safetyStatus ?? task.auditStatus ?? (task.status === "AUDITING" ? "reviewing" : null));
  const failedSafetyStatus = task.status === "FAILED" ? normalizeSafetyStatusFromError(task.auditReason ?? task.failReason) : "idle";
  const safety = getSafetyCopy(safetyStatus !== "idle" ? safetyStatus : failedSafetyStatus, "task");

  if (safety.status !== "idle") {
    return <SafetyInlineNotice state={safety} />;
  }

  if (task.status === "FAILED") {
    return (
      <div className="rounded-lg border border-border-warm bg-background p-4 text-sm text-destructive">
        {task.failReason || "生成失败，请调整参数后重试。"}
      </div>
    );
  }

  if (task.status === "CANCELLED") {
    return <div className="rounded-lg border border-border-warm bg-background p-4 text-sm text-muted-gray">任务已取消。</div>;
  }

  if (task.status === "REFUNDING") {
    return <div className="rounded-lg border border-border-warm bg-background p-4 text-sm text-charcoal">退款处理中或冻结积分释放中。</div>;
  }

  if (task.status === "REFUNDED") {
    return <div className="rounded-lg border border-border-warm bg-background p-4 text-sm text-charcoal">积分已退回或冻结已释放。</div>;
  }

  if (task.status !== "SUCCESS") {
    return <div className="rounded-lg border border-border-warm bg-background p-4 text-sm text-muted-gray">生成结果将在任务完成后展示。</div>;
  }

  const outputData = formatJson(task.outputData);

  return (
    <div className="flex flex-col gap-4">
      {task.outputAssetId && (
        <div className="rounded-lg border border-border-warm bg-background p-4">
          <p className="text-sm font-medium text-charcoal">文件型结果</p>
          <p className="mt-1 text-xs text-muted-gray">资产类型：{task.outputAssetType || "未知"}</p>
          <Link
            href={`/assets/${task.outputAssetId}`}
            className="mt-4 inline-flex rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80"
          >
            查看资产
          </Link>
        </div>
      )}
      {task.outputText && (
        <div className="rounded-lg border border-border-warm bg-background p-4">
          <p className="text-sm font-medium text-charcoal">文本结果</p>
          <p className="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-charcoal">{task.outputText}</p>
        </div>
      )}
      {outputData && (
        <div className="rounded-lg border border-border-warm bg-background p-4">
          <p className="text-sm font-medium text-charcoal">结构化结果</p>
          <pre className="mt-3 overflow-auto rounded-md bg-muted p-3 text-xs leading-relaxed text-charcoal">{outputData}</pre>
        </div>
      )}
      {!task.outputAssetId && !task.outputText && !outputData && (
        <div className="rounded-lg border border-border-warm bg-background p-4 text-sm text-muted-gray">任务已完成，暂无可展示结果。</div>
      )}
    </div>
  );
}
