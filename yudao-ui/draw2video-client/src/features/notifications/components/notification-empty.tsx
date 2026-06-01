import { BellOff } from "lucide-react";

export function NotificationEmpty({ title = "暂无通知", description = "新的系统与业务消息会显示在这里。" }) {
  return (
    <div className="mt-16 flex flex-col items-center text-center text-muted-gray">
      <div className="flex size-11 items-center justify-center rounded-lg border border-border-warm bg-background">
        <BellOff className="size-5" />
      </div>
      <p className="mt-4 text-sm font-medium text-charcoal">{title}</p>
      <p className="mt-1 text-sm">{description}</p>
    </div>
  );
}
