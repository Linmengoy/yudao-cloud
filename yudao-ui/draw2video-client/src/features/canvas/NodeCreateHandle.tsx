"use client";

import { useState, type KeyboardEvent, type MouseEvent, type PointerEvent } from "react";
import { Handle, Position, useStore } from "@xyflow/react";
import { Plus } from "lucide-react";
import type { NodeCreateMenuEventDetail } from "./types";
import { cn } from "@/lib/utils";

type NodeCreateHandleProps = {
  nodeId: string;
  direction: "incoming" | "outgoing";
  enabled?: boolean;
  selected?: boolean;
  showButton?: boolean;
};

const HANDLE_EVENT = "copse:node-create-menu";
const BUTTON_SIZE = 28;
const HOVER_ZONE_WIDTH = 128;
const HOVER_ZONE_HEIGHT = 168;

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function dispatchCreateMenu(
  nodeId: string,
  direction: NodeCreateHandleProps["direction"],
  point: { x: number; y: number }
) {
  window.dispatchEvent(
    new CustomEvent<NodeCreateMenuEventDetail>(HANDLE_EVENT, {
      detail: {
        nodeId,
        direction,
        clientX: point.x,
        clientY: point.y,
      },
    })
  );
}

export function NodeCreateHandle({ nodeId, direction, enabled = true, selected = false, showButton = true }: NodeCreateHandleProps) {
  const zoom = useStore((s) => s.transform[2] || 1);
  const toFlowUnit = (value: number) => value / zoom;
  const isIncoming = direction === "incoming";
  const label = isIncoming ? "添加上游卡片" : "创建下游卡片";
  const buttonSize = toFlowUnit(BUTTON_SIZE);
  const hoverZoneWidth = toFlowUnit(HOVER_ZONE_WIDTH);
  const hoverZoneHeight = toFlowUnit(HOVER_ZONE_HEIGHT);
  const visualLeft = toFlowUnit(isIncoming ? -52 : 32);
  const zoneLeft = visualLeft + buttonSize / 2 - hoverZoneWidth / 2;
  const [followOffset, setFollowOffset] = useState({ x: 0, y: 0 });
  const [isFollowing, setIsFollowing] = useState(false);

  if (!enabled) return null;

  return (
    <Handle
      type={isIncoming ? "target" : "source"}
      position={isIncoming ? Position.Left : Position.Right}
      isConnectable
      role="button"
      tabIndex={0}
      aria-label={label}
      title={label}
      onClick={(event: MouseEvent<HTMLDivElement>) => {
        if (!showButton) return;
        event.preventDefault();
        event.stopPropagation();
        dispatchCreateMenu(nodeId, direction, { x: event.clientX, y: event.clientY });
      }}
      onKeyDown={(event: KeyboardEvent<HTMLDivElement>) => {
        if (!showButton) return;
        if (event.key !== "Enter" && event.key !== " ") return;
        event.preventDefault();
        event.stopPropagation();
        const rect = event.currentTarget.getBoundingClientRect();
        dispatchCreateMenu(nodeId, direction, {
          x: rect.left + rect.width / 2,
          y: rect.top + rect.height / 2,
        });
      }}
      className={cn(
        "group !z-20 !size-2 !overflow-visible !border-0 !bg-transparent !p-0 !shadow-none !outline-none !pointer-events-auto"
      )}
    >
      {showButton && (
        <span
          className="absolute top-1/2 -translate-y-1/2"
          onPointerMove={(event: PointerEvent<HTMLSpanElement>) => {
            const rect = event.currentTarget.getBoundingClientRect();
            const centerX = rect.left + rect.width / 2;
            const centerY = rect.top + rect.height / 2;
            const maxX = (HOVER_ZONE_WIDTH - BUTTON_SIZE) / 2;
            const maxY = (HOVER_ZONE_HEIGHT - BUTTON_SIZE) / 2;
            setIsFollowing(true);
            setFollowOffset({
              x: clamp(event.clientX - centerX, -maxX, maxX),
              y: clamp(event.clientY - centerY, -maxY, maxY),
            });
          }}
          onPointerLeave={() => {
            setIsFollowing(false);
            setFollowOffset({ x: 0, y: 0 });
          }}
          style={{
            left: zoneLeft,
            width: hoverZoneWidth,
            height: hoverZoneHeight,
          }}
        >
          <span
            className={cn(
              "pointer-events-none absolute flex size-7 items-center justify-center rounded-full border border-border-warm bg-background/95 text-muted-gray shadow-[0_2px_8px_rgba(28,28,28,0.16)] transition-[opacity,border-color,color] duration-150 hover:border-charcoal/40 hover:text-charcoal group-hover:opacity-100 group-focus-visible:opacity-100",
              selected ? "opacity-100" : "opacity-0",
              !isFollowing && "transition-[left,top,opacity,border-color,color] duration-200 ease-out"
            )}
            style={{
              left: hoverZoneWidth / 2 - buttonSize / 2 + toFlowUnit(followOffset.x),
              top: hoverZoneHeight / 2 - buttonSize / 2 + toFlowUnit(followOffset.y),
              width: buttonSize,
              height: buttonSize,
            }}
          >
            <Plus
              className="pointer-events-none"
              style={{ width: toFlowUnit(16), height: toFlowUnit(16) }}
            />
          </span>
        </span>
      )}
    </Handle>
  );
}
