"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import {
  Home,
  PlusCircle,
  Folder,
  ListChecks,
  Wallet,
  HelpCircle,
  Zap,
  Settings,
  BookOpen,
  MessageCircle,
  Globe,
  LogOut,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/auth/auth-store";
import { ThemeToggle } from "@/features/theme/ThemeToggle";
import { NotificationBell } from "@/features/notifications/components/notification-bell";

const sidebarLinks = [
  { href: "/app", icon: Home, label: "首页" },
  { href: "/projects", icon: PlusCircle, label: "项目" },
  { href: "/assets", icon: Folder, label: "资产" },
  { href: "/tasks", icon: ListChecks, label: "任务" },
];

const popoverMenuItems = [
  { href: "/profile", icon: Settings, label: "账户管理" },
  { href: "/wallet", icon: Wallet, label: "钱包 / 用量" },
  { href: "#", icon: BookOpen, label: "使用指南" },
  { href: "#", icon: MessageCircle, label: "联系我们" },
  { href: "#", icon: Globe, label: "简体中文" },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { loggedIn, loading, wallet, user, logout, openModal, authReason } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [popoverOpen, setPopoverOpen] = useState(false);
  const popoverRef = useRef<HTMLDivElement>(null);

  // Defer auth-dependent rendering until after hydration to avoid SSR/client mismatch
  const mounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );

  useEffect(() => {
    if (loading) return;
    if (!loggedIn) {
      router.push("/");
      if (authReason !== "manual-logout") {
        setTimeout(() => openModal("email", pathname, "required"), 100);
      }
    }
  }, [authReason, loggedIn, loading, openModal, pathname, router]);

  // Close popover on click outside
  useEffect(() => {
    if (!popoverOpen) return;
    function handleClick(e: MouseEvent) {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setPopoverOpen(false);
      }
    }
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setPopoverOpen(false);
    }
    document.addEventListener("mousedown", handleClick);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClick);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [popoverOpen]);

  if (loading || !mounted) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-sm text-muted-gray">加载中...</div>
      </div>
    );
  }

  if (!loggedIn) return null;

  const credits = wallet ? wallet.balance : 0;
  const isCanvasRoute = pathname.startsWith("/canvas");

  return (
    <div className="flex h-screen">
      {/* Sidebar */}
      {!isCanvasRoute && <aside className="relative z-[160] hidden w-[72px] shrink-0 flex-col items-center border-r border-border-warm bg-background py-4 md:flex">
        <div className="mb-4 flex size-10 items-center justify-center rounded-lg bg-charcoal text-sm font-semibold text-off-white">
          C
        </div>

        <nav className="flex flex-1 flex-col items-center gap-1">
          {sidebarLinks.map((link) => {
            const active =
              link.href === "/app"
                ? pathname === "/app"
                : link.href === "/projects"
                  ? pathname.startsWith("/projects") || pathname.startsWith("/create")
                : pathname.startsWith(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                title={link.label}
                className={cn(
                  "flex size-10 items-center justify-center rounded-lg transition-colors",
                  active
                    ? "bg-muted text-charcoal"
                    : "text-muted-gray hover:bg-muted hover:text-charcoal"
                )}
              >
                <link.icon className="size-5" />
              </Link>
            );
          })}
        </nav>

        {/* Bottom actions */}
        <div className="flex flex-col items-center gap-1">
          <Link
            href="#"
            title="帮助"
            className="flex size-10 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
          >
            <HelpCircle className="size-5" />
          </Link>

          <NotificationBell />

          <ThemeToggle className="size-10 rounded-lg p-0" />

          <Link
            href="/wallet"
            title="余额 / 用量"
            className="mx-1.5 flex items-center justify-center gap-1 rounded-lg border border-border-warm px-2 py-1.5 text-xs font-medium text-muted-gray transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal"
          >
            <Zap className="size-3.5" />
            {credits}
          </Link>

          {/* Avatar + popover */}
          <div className="relative" ref={popoverRef}>
            <button
              onClick={() => setPopoverOpen((v) => !v)}
              className="flex size-10 items-center justify-center rounded-lg transition-colors hover:bg-muted"
              title={user?.nickname ?? "用户"}
            >
              <div className="flex size-7 items-center justify-center rounded-full bg-charcoal text-xs font-medium text-off-white">
                {user?.nickname?.[0] ?? "U"}
              </div>
            </button>

            {/* Popover */}
            {popoverOpen && (
              <div className="absolute bottom-0 left-full z-[220] ml-2 w-[340px] rounded-xl border border-border-warm bg-background shadow-lg">
                {/* User info header */}
                <div className="flex items-center gap-3 border-b border-border-warm px-4 py-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-charcoal text-sm font-medium text-off-white">
                    {user?.nickname?.[0] ?? "U"}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-charcoal">
                      {user?.nickname ?? "用户"}
                    </p>
                    {user?.mobile && (
                      <p className="truncate text-xs text-muted-gray">
                        {user.mobile.replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2")}
                      </p>
                    )}
                  </div>
                </div>

                {/* Plan & credits */}
                <div className="flex items-center justify-between border-b border-border-warm px-4 py-3">
                  <div>
                    <p className="text-xs text-muted-gray">Free 计划</p>
                    <p className="text-sm font-medium text-charcoal">
                      {credits} 积分
                    </p>
                  </div>
                  <Link
                    href="/pricing"
                    onClick={() => setPopoverOpen(false)}
                    className="inline-flex items-center gap-1 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-1.5 text-xs font-medium text-charcoal active:opacity-80"
                  >
                    升级
                    <ChevronRight className="size-3" />
                  </Link>
                </div>

                {/* Menu items */}
                <div className="py-1">
                  {popoverMenuItems.map((item) => (
                    <Link
                      key={item.label}
                      href={item.href}
                      onClick={() => setPopoverOpen(false)}
                      className="flex items-center gap-3 px-4 py-2.5 text-sm text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
                    >
                      <item.icon className="size-4" />
                      {item.label}
                    </Link>
                  ))}
                </div>

                {/* Logout */}
                <div className="border-t border-border-warm py-1">
                  <button
                    onClick={async () => {
                      if (!window.confirm("确定要退出登录吗？")) return;
                      setPopoverOpen(false);
                      await logout();
                    }}
                    className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
                  >
                    <LogOut className="size-4" />
                    退出登录
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </aside>}

      {/* Main area */}
      <div className="relative z-0 flex flex-1 flex-col overflow-hidden">
        {/* Page content */}
        <div className="flex-1 overflow-auto">{children}</div>
      </div>
    </div>
  );
}
