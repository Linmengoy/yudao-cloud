"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useRef, useState, useEffect } from "react";
import { Menu, X, ChevronRight, LogOut, Settings, Wallet } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/auth/auth-store";
import { formatPoints } from "@/features/wallet/wallet-api";
import { ThemeToggle } from "@/features/theme/ThemeToggle";

const guestLinks = [
  { href: "/", label: "首页" },
  { href: "/pricing", label: "价格" },
];

const authLinks = [
  { href: "/", label: "首页" },
  { href: "/pricing", label: "价格" },
  { href: "/app", label: "工作台" },
  { href: "/tasks", label: "我的作品" },
];

function maskAccount(value?: string) {
  if (!value) return "已登录";
  if (value.includes("@")) return value.replace(/^(.).+(@.+)$/, "$1***$2");
  return value.replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2");
}

export default function MarketingLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const { loggedIn, user, wallet, openModal, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const accountRef = useRef<HTMLDivElement>(null);
  const links = loggedIn ? authLinks : guestLinks;

  useEffect(() => {
    if (!accountOpen) return;
    function handleClick(event: MouseEvent) {
      if (accountRef.current && !accountRef.current.contains(event.target as Node)) {
        setAccountOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setAccountOpen(false);
    }
    document.addEventListener("mousedown", handleClick);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClick);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [accountOpen]);

  async function handleLogout() {
    if (!window.confirm("确定要退出登录吗？")) return;
    setAccountOpen(false);
    setMobileOpen(false);
    await logout();
  }

  return (
    <>
      <header className="sticky top-0 z-50 border-b border-border-warm bg-background/95">
        <div className="mx-auto flex h-14 max-w-[1200px] items-center justify-between px-4">
          <Link href="/" className="text-lg font-semibold text-charcoal">
            Copse
          </Link>

          <nav className="hidden items-center gap-6 md:flex">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  "text-sm text-muted-gray transition-colors hover:text-charcoal",
                  pathname === link.href && "text-charcoal"
                )}
              >
                {link.label}
              </Link>
            ))}
          </nav>

          <div className="hidden items-center gap-3 md:flex">
            <ThemeToggle />
            {loggedIn ? (
              <>
                <Link href="/app" className="primary-button inline-flex items-center gap-1">
                  进入工作台
                  <ChevronRight className="size-4" />
                </Link>
                <div className="relative" ref={accountRef}>
                  <button
                    onClick={() => setAccountOpen((open) => !open)}
                    className="flex size-9 items-center justify-center rounded-full bg-muted text-xs font-medium text-charcoal transition-colors hover:bg-border-warm focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
                    aria-label="打开账户菜单"
                  >
                    {user?.nickname?.[0] ?? "U"}
                  </button>
                  {accountOpen && (
                    <div className="absolute right-0 top-full mt-2 w-64 rounded-xl border border-border-warm bg-background py-2 shadow-[rgba(0,0,0,0.1)_0px_4px_12px]">
                      <div className="border-b border-border-warm px-4 pb-3 pt-2">
                        <p className="truncate text-sm font-medium text-charcoal">{user?.nickname ?? "用户"}</p>
                        <p className="mt-1 truncate text-xs text-muted-gray">{maskAccount(user?.email ?? user?.mobile)}</p>
                        <p className="mt-2 text-xs text-muted-gray">{formatPoints(wallet?.balance)}</p>
                      </div>
                      <Link
                        href="/profile"
                        onClick={() => setAccountOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
                      >
                        <Settings className="size-4" />
                        个人中心
                      </Link>
                      <Link
                        href="/wallet"
                        onClick={() => setAccountOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
                      >
                        <Wallet className="size-4" />
                        钱包 / 用量
                      </Link>
                      <button
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
                      >
                        <LogOut className="size-4" />
                        退出登录
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <button
                onClick={() => openModal("email")}
                className="primary-button inline-flex items-center"
              >
                登录 / 注册
              </button>
            )}
          </div>

          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="inline-flex items-center justify-center rounded-md p-2 text-charcoal hover:bg-muted focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px] md:hidden"
            aria-label="打开导航菜单"
          >
            {mobileOpen ? <X className="size-5" /> : <Menu className="size-5" />}
          </button>
        </div>

        {mobileOpen && (
          <nav className="border-t border-border-warm bg-background px-4 py-3 md:hidden">
            <div className="flex flex-col gap-3">
              {links.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setMobileOpen(false)}
                  className={cn(
                    "text-sm text-muted-gray transition-colors hover:text-charcoal",
                    pathname === link.href && "text-charcoal"
                  )}
                >
                  {link.label}
                </Link>
              ))}
              {loggedIn ? (
                <>
                  <div className="rounded-lg border border-border-warm px-3 py-2">
                    <p className="text-sm font-medium text-charcoal">{user?.nickname ?? "用户"}</p>
                    <p className="text-xs text-muted-gray">{maskAccount(user?.email ?? user?.mobile)}</p>
                    <p className="mt-1 text-xs text-muted-gray">{formatPoints(wallet?.balance)}</p>
                  </div>
                  <button onClick={handleLogout} className="text-left text-sm text-muted-gray">
                    退出登录
                  </button>
                </>
              ) : (
                <button
                  onClick={() => {
                    setMobileOpen(false);
                    openModal("email");
                  }}
                  className="primary-button inline-flex w-fit items-center"
                >
                  登录 / 注册
                </button>
              )}
              <ThemeToggle showLabel className="w-fit px-0 py-0 hover:bg-transparent" />
            </div>
          </nav>
        )}
      </header>
      <main className="flex-1">{children}</main>
    </>
  );
}
