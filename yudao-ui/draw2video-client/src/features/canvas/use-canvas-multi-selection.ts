import { useCallback, useMemo, useRef, useState } from "react";

import type { AppNode } from "@/features/canvas/types";
import type { SelectionRectSnapshot } from "@/features/canvas/CanvasChrome";

export type MultiSelectionAction = "group" | "merge";

type UseCanvasMultiSelectionOptions = {
  getNodes: () => AppNode[];
  readBounds: () => SelectionRectSnapshot | null;
  resolveAction: (selectedNodes: AppNode[], currentNodes: AppNode[]) => MultiSelectionAction | null;
};

export function useCanvasMultiSelection({
  getNodes,
  readBounds,
  resolveAction,
}: UseCanvasMultiSelectionOptions) {
  const [bounds, setBounds] = useState<SelectionRectSnapshot | null>(null);
  const [action, setAction] = useState<MultiSelectionAction | null>(null);
  const optionsRef = useRef({ getNodes, readBounds, resolveAction });
  optionsRef.current = { getNodes, readBounds, resolveAction };

  const clear = useCallback(() => {
    setBounds(null);
    setAction(null);
  }, []);

  const refresh = useCallback((sourceNodes?: AppNode[]) => {
    const { getNodes: readNodes, readBounds: readCurrentBounds, resolveAction: resolveCurrentAction } = optionsRef.current;
    const currentNodes = sourceNodes ?? readNodes();
    const selectedNodes = currentNodes.filter((node) => node.selected);
    const nextAction = resolveCurrentAction(selectedNodes, currentNodes);

    window.requestAnimationFrame(() => {
      const nextBounds = readCurrentBounds();
      setAction(nextAction);
      setBounds(nextBounds && selectedNodes.length > 1 ? nextBounds : null);
    });
  }, []);

  return useMemo(() => ({
    bounds,
    action,
    clear,
    refresh,
  }), [action, bounds, clear, refresh]);
}
