"use client";

import { useCallback, useRef, useState, type CSSProperties, type MouseEvent } from "react";
import { Pause, Play, Volume2, VolumeX } from "lucide-react";
import { cn } from "@/lib/utils";

type PreviewVideoPlayerProps = {
  src: string;
  poster?: string;
  className?: string;
  videoClassName?: string;
  onLoadedMetadata?: (video: HTMLVideoElement) => void;
  playOnHover?: boolean;
  clickToToggle?: boolean;
  controlsInteractive?: boolean;
};

function formatVideoTime(seconds: number | null | undefined) {
  if (!seconds || !Number.isFinite(seconds) || seconds < 0) return "0:00";
  const total = Math.floor(seconds);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
}

function getRangeProgress(current: number, duration: number) {
  if (!Number.isFinite(current) || !Number.isFinite(duration) || duration <= 0) return "0%";
  return `${Math.min(100, Math.max(0, (current / duration) * 100))}%`;
}

function stopParentClick(event: MouseEvent) {
  event.preventDefault();
  event.stopPropagation();
}

export function PreviewVideoPlayer({
  src,
  poster,
  className,
  videoClassName,
  onLoadedMetadata,
  playOnHover = false,
  clickToToggle = true,
  controlsInteractive = true,
}: PreviewVideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [durationSec, setDurationSec] = useState(0);
  const [volume, setVolume] = useState(1);

  const handleTogglePlayback = useCallback((event?: MouseEvent) => {
    if (event) stopParentClick(event);
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      video.play().catch(() => undefined);
    } else {
      video.pause();
    }
  }, []);

  const handleMouseEnter = useCallback(() => {
    if (!playOnHover) return;
    videoRef.current?.play().catch(() => undefined);
  }, [playOnHover]);

  const handleMouseLeave = useCallback(() => {
    if (!playOnHover) return;
    const video = videoRef.current;
    if (!video) return;
    video.pause();
    video.currentTime = 0;
    setCurrentTime(0);
  }, [playOnHover]);

  const handleSeek = useCallback((value: string) => {
    const video = videoRef.current;
    if (!video) return;
    const nextTime = Number(value);
    if (!Number.isFinite(nextTime)) return;
    video.currentTime = nextTime;
    setCurrentTime(nextTime);
  }, []);

  const handleVolumeChange = useCallback((value: string) => {
    const video = videoRef.current;
    const nextVolume = Math.min(1, Math.max(0, Number(value)));
    if (!Number.isFinite(nextVolume)) return;
    setVolume(nextVolume);
    if (video) {
      video.volume = nextVolume;
      video.muted = nextVolume === 0;
    }
  }, []);

  const handleToggleMute = useCallback((event: MouseEvent) => {
    stopParentClick(event);
    const video = videoRef.current;
    if (!video) return;
    if (video.muted || video.volume === 0) {
      const nextVolume = volume > 0 ? volume : 1;
      video.muted = false;
      video.volume = nextVolume;
      setVolume(nextVolume);
      return;
    }
    video.muted = true;
    setVolume(0);
  }, [volume]);

  return (
    <div
      className={cn("group relative flex size-full items-center justify-center overflow-hidden", className)}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <video
        ref={videoRef}
        src={src}
        poster={poster}
        className={cn("max-h-full max-w-full object-contain", videoClassName)}
        playsInline
        onClick={clickToToggle ? handleTogglePlayback : undefined}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onTimeUpdate={(event) => setCurrentTime(event.currentTarget.currentTime)}
        onLoadedMetadata={(event) => {
          const video = event.currentTarget;
          setDurationSec(Number.isFinite(video.duration) ? video.duration : 0);
          onLoadedMetadata?.(video);
        }}
      />
      <div
        className={cn(
          "absolute inset-x-0 bottom-0 flex items-center gap-3 bg-gradient-to-t from-[#1c1c1c]/80 via-[#1c1c1c]/45 to-transparent px-4 pb-3 pt-8 text-[#fcfbf8] opacity-0 transition-opacity group-hover:opacity-100",
          !controlsInteractive && "pointer-events-none"
        )}
        onClick={controlsInteractive ? stopParentClick : undefined}
      >
        <button
          type="button"
          onClick={handleTogglePlayback}
          className="flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-[#fcfbf8]/12 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#fcfbf8]"
          aria-label={isPlaying ? "暂停" : "播放"}
        >
          {isPlaying ? <Pause className="size-5" /> : <Play className="size-5 fill-current" />}
        </button>
        <span className="w-10 text-xs tabular-nums">{formatVideoTime(currentTime)}</span>
        <input
          type="range"
          min={0}
          max={Math.max(0.01, durationSec)}
          step={0.01}
          value={Math.min(currentTime, Math.max(0.01, durationSec))}
          onClick={stopParentClick}
          onChange={(event) => handleSeek(event.target.value)}
          className="video-control-range h-1 min-w-0 flex-1"
          style={{ "--video-range-progress": getRangeProgress(currentTime, durationSec) } as CSSProperties}
          aria-label="视频进度"
        />
        <span className="w-10 text-right text-xs tabular-nums">{formatVideoTime(durationSec)}</span>
        <div className="group/volume relative flex size-8 items-center justify-center">
          <button
            type="button"
            onClick={handleToggleMute}
            className="flex size-8 items-center justify-center rounded-full hover:bg-[#fcfbf8]/12 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#fcfbf8]"
            aria-label={volume === 0 ? "取消静音" : "静音"}
          >
            {volume === 0 ? <VolumeX className="size-4" /> : <Volume2 className="size-4" />}
          </button>
          <div className="pointer-events-none absolute bottom-full left-1/2 mb-2 flex h-24 w-8 -translate-x-1/2 items-center justify-center rounded-full bg-[#1c1c1c]/90 opacity-0 shadow-xl transition-opacity group-hover/volume:pointer-events-auto group-hover/volume:opacity-100">
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={volume}
              onClick={stopParentClick}
              onChange={(event) => handleVolumeChange(event.target.value)}
              className="video-control-range h-20 w-1 [writing-mode:vertical-rl]"
              aria-label="音量"
              style={{ direction: "rtl" }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
