"use client";

import { useId } from "react";
import { BaseEdge, getBezierPath, useReactFlow, useStore, type EdgeProps } from "@xyflow/react";
import type { EdgeDeleteEventDetail } from "./types";

export function CanvasSignalEdge({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  source,
  target,
}: EdgeProps) {
  const gradientId = useId().replace(/:/g, "");
  const { setEdges } = useReactFlow();
  const isConnectedCardSelected = useStore((store) =>
    store.nodes.some((node) => (node.id === source || node.id === target) && node.selected)
  );
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
          <stop offset="35%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="50%" stopColor="var(--canvas-edge-signal)" stopOpacity="0.92" />
          <stop offset="65%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="100%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <animate
            attributeName="x1"
            values={`${sourceX - gradientDx};${targetX}`}
            dur="1.8s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="y1"
            values={`${sourceY - gradientDy};${targetY}`}
            dur="1.8s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="x2"
            values={`${sourceX};${targetX + gradientDx}`}
            dur="1.8s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="y2"
            values={`${sourceY};${targetY + gradientDy}`}
            dur="1.8s"
            repeatCount="indefinite"
          />
        </linearGradient>
      </defs>
      <BaseEdge path={edgePath} />
      {isConnectedCardSelected ? (
        <path className="copse-signal-edge-gradient" d={edgePath} stroke={`url(#${gradientId})`} />
      ) : null}
      <path
        d={edgePath}
        fill="none"
        stroke="transparent"
        strokeWidth={18}
        className="cursor-pointer"
        onClick={(event) => {
          event.stopPropagation();
          setEdges((eds) => eds.map((edge) => ({ ...edge, selected: edge.id === id })));
        }}
        onDoubleClick={(event) => {
          event.stopPropagation();
          window.dispatchEvent(new CustomEvent<EdgeDeleteEventDetail>("copse:edge-delete", {
            detail: { edgeId: id },
          }));
        }}
      />
    </>
  );
}
