"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Menu, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/features/auth/auth-store";
import { formatPoints } from "@/features/wallet/wallet-api";

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

export function Header() {
  const pathname = usePathname();
  const { loggedIn, wallet, user, openModal, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const links = loggedIn ? authLinks : guestLinks;

  return (
    <header className="sticky top-0 z-50 border-b border-border-warm bg-background">
      <div className="mx-auto flex h-14 max-w-[1200px] items-center justify-between px-4">
        <Link href="/" className="text-lg font-semibold text-charcoal">
          Copse
        </Link>

        {/* Desktop nav */}
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

        {/* Desktop actions */}
        <div className="hidden items-center gap-3 md:flex">
          {loggedIn && wallet ? (
            <Link
              href="/wallet"
              className="flex items-center gap-1.5 rounded-md px-2.5 py-1 text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
            >
              {formatPoints(wallet.balance)}
            </Link>
          ) : null}
          {loggedIn ? (
            <div className="relative group">
              <button className="flex size-8 items-center justify-center rounded-full bg-muted text-xs font-medium text-charcoal">
                {user?.nickname?.[0] ?? "U"}
              </button>
              {/* Dropdown */}
              <div className="invisible absolute right-0 top-full mt-1 min-w-[140px] rounded-lg border border-border-warm bg-background py-1 opacity-0 shadow-sm transition-all group-hover:visible group-hover:opacity-100">
                <Link
                  href="/profile"
                  className="block px-3 py-2 text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
                >
                  个人设置
                </Link>
                <Link
                  href="/wallet"
                  className="block px-3 py-2 text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
                >
                  钱包
                </Link>
                <button
                  onClick={() => logout()}
                  className="w-full px-3 py-2 text-left text-sm text-muted-gray hover:bg-muted hover:text-charcoal"
                >
                  退出登录
                </button>
              </div>
            </div>
          ) : (
            <button
              onClick={() => openModal()}
              className="inline-flex items-center rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80"
            >
              开始体验
            </button>
          )}
        </div>

        {/* Mobile toggle */}
        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="inline-flex items-center justify-center rounded-md p-2 text-charcoal hover:bg-muted md:hidden"
        >
          {mobileOpen ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>

      {/* Mobile menu */}
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
            {loggedIn && wallet && (
              <Link
                href="/wallet"
                onClick={() => setMobileOpen(false)}
                className="text-sm text-muted-gray"
              >
                余额：{formatPoints(wallet.balance)}
              </Link>
            )}
            {loggedIn ? (
              <button
                onClick={() => {
                  setMobileOpen(false);
                  void logout();
                }}
                className="text-left text-sm text-muted-gray"
              >
                退出登录
              </button>
            ) : (
              <button
                onClick={() => {
                  setMobileOpen(false);
                  openModal();
                }}
                className="inline-flex w-fit items-center rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]"
              >
                开始体验
              </button>
            )}
          </div>
        </nav>
      )}
    </header>
  );
}
