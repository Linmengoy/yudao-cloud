export function createPoller<T>(
  fn: () => Promise<T>,
  predicate: (result: T) => boolean,
  interval = 2000,
  maxAttempts = 60
): { start: () => Promise<T>; stop: () => void } {
  let stopped = false;

  return {
    async start() {
      for (let i = 0; i < maxAttempts; i++) {
        if (stopped) throw new Error("Polling stopped");
        const result = await fn();
        if (!predicate(result)) return result;
        await new Promise((r) => setTimeout(r, interval));
      }
      throw new Error("Polling timeout");
    },
    stop() {
      stopped = true;
    },
  };
}
