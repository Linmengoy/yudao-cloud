"use client";

import { useId } from "react";
import { BaseEdge, getBezierPath, useStore, type EdgeProps } from "@xyflow/react";

export function CanvasSignalEdge({
  source,
  sourceX,
  sourceY,
  target,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  selected,
}: EdgeProps) {
  const gradientId = useId().replace(/:/g, "");
  const isConnectedToSelectedNode = useStore((store) =>
    store.nodes.some((node) => node.selected && (node.id === source || node.id === target))
  );
  const shouldAnimate = selected || isConnectedToSelectedNode;
  const gradientDx = targetX - sourceX;
  const gradientDy = targetY - sourceY;
  const [edgePath] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
    curvature: 0.42,
  });

  return (
    <>
      <defs>
        <linearGradient
          id={gradientId}
          gradientUnits="userSpaceOnUse"
          x1={sourceX}
          y1={sourceY}
          x2={targetX}
          y2={targetY}
        >
          <stop offset="0%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="36%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="50%" stopColor="var(--canvas-edge-signal)" stopOpacity="0.78" />
          <stop offset="64%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="100%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <animate
            attributeName="x1"
            values={`${sourceX - gradientDx};${targetX}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="y1"
            values={`${sourceY - gradientDy};${targetY}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="x2"
            values={`${sourceX};${targetX + gradientDx}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="y2"
            values={`${sourceY};${targetY + gradientDy}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
        </linearGradient>
      </defs>
      <BaseEdge
        path={edgePath}
        style={{
          stroke: selected ? "var(--canvas-edge-selected)" : "var(--canvas-edge-base)",
          strokeWidth: selected ? 2.4 : 1.8,
        }}
      />
      {shouldAnimate && <path className="copse-signal-edge-gradient" d={edgePath} stroke={`url(#${gradientId})`} />}
    </>
  );
}
