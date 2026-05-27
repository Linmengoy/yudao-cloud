"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Home,
  PlusCircle,
  Folder,
  Image,
  User,
  HelpCircle,
  LogOut,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/auth/auth-store";
import { formatPoints } from "@/features/wallet/wallet-api";

const sidebarLinks = [
  { href: "/app", icon: Home, label: "首页" },
  { href: "/create/image", icon: PlusCircle, label: "创建" },
  { href: "/tasks", icon: Folder, label: "作品" },
  { href: "/create/video", icon: Image, label: "视频" },
  { href: "/profile", icon: User, label: "设置" },
];

export function WorkspaceShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { wallet, user, logout } = useAuth();

  return (
    <div className="flex h-[calc(100vh-57px)]">
      {/* Sidebar */}
      <aside className="hidden w-16 shrink-0 flex-col items-center border-r border-border-warm bg-background py-4 md:flex">
        <nav className="flex flex-1 flex-col items-center gap-1">
          {sidebarLinks.map((link) => {
            const active =
              link.href === "/app"
                ? pathname === "/app"
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

        <div className="flex flex-col items-center gap-1">
          <Link
            href="#"
            title="帮助"
            className="flex size-10 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
          >
            <HelpCircle className="size-5" />
          </Link>
          <button
            onClick={logout}
            title="退出登录"
            className="flex size-10 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
          >
            <LogOut className="size-5" />
          </button>
        </div>
      </aside>

      {/* Main area */}
      <div className="flex flex-1 flex-col overflow-auto">
        {/* Top bar */}
        <div className="flex items-center justify-end border-b border-border-warm px-6 py-3">
          <div className="flex items-center gap-3">
            {wallet && (
              <Link
                href="/wallet"
                className="rounded-md px-2.5 py-1 text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
              >
                {formatPoints(wallet.balance)}
              </Link>
            )}
            <Link
              href="/pricing"
              className="rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-1 text-xs text-charcoal active:opacity-80"
            >
              升级
            </Link>
            <Link
              href="/profile"
              className="flex size-8 items-center justify-center rounded-full bg-muted text-xs font-medium text-charcoal"
            >
              {user?.nickname?.[0] ?? "U"}
            </Link>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto">{children}</div>
      </div>
    </div>
  );
}
