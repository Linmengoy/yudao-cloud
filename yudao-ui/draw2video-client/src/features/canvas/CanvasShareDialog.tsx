"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { Check, Copy, Link2, Trash2, UserPlus, X } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasMember, CanvasProjectRole } from "@/features/canvas/types";

interface CanvasShareDialogProps {
  open: boolean;
  projectId: string;
  projectRole: CanvasProjectRole | null;
  members: CanvasMember[];
  onOpenChange: (open: boolean) => void;
  onMembersChange: (members: CanvasMember[]) => void;
}

type EditableRole = Exclude<CanvasProjectRole, "owner">;

function isEditableRole(value: string): value is EditableRole {
  return value === "editor" || value === "viewer";
}

export function CanvasShareDialog({
  open,
  projectId,
  projectRole,
  members,
  onOpenChange,
  onMembersChange,
}: CanvasShareDialogProps) {
  const [inviteUserId, setInviteUserId] = useState("");
  const [inviteRole, setInviteRole] = useState<EditableRole>("editor");
  const [statusText, setStatusText] = useState("");
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const canManageMembers = projectRole === "owner";
  const shareUrl = useMemo(() => {
    if (typeof window === "undefined") return `/canvas?projectId=${projectId}`;
    return `${window.location.origin}/canvas?projectId=${projectId}`;
  }, [projectId]);

  const refreshMembers = useCallback(async () => {
    const nextMembers = await canvasApi.getProjectMembers(projectId);
    onMembersChange(nextMembers);
  }, [onMembersChange, projectId]);

  useEffect(() => {
    if (!open) return;
    refreshMembers().catch(() => setStatusText("成员列表加载失败"));
  }, [open, refreshMembers]);

  const openChanged = useCallback((nextOpen: boolean) => {
    if (nextOpen) {
      setStatusText("");
      setCopied(false);
    }
    onOpenChange(nextOpen);
  }, [onOpenChange]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") openChanged(false);
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, openChanged]);

  async function copyShareUrl() {
    await navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setStatusText("协作链接已复制，只有项目成员可以访问");
    window.setTimeout(() => setCopied(false), 1600);
  }

  async function inviteMember() {
    const userId = Number(inviteUserId);
    if (!Number.isFinite(userId) || userId <= 0) {
      setStatusText("请输入有效的用户 ID");
      return;
    }
    setLoading(true);
    setStatusText("");
    try {
      await canvasApi.inviteProjectMember(projectId, { userId, role: inviteRole });
      setInviteUserId("");
      await refreshMembers();
      setStatusText("成员已邀请");
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "邀请失败");
    } finally {
      setLoading(false);
    }
  }

  async function updateMemberRole(member: CanvasMember, role: string) {
    if (!isEditableRole(role) || member.role === role) return;
    setLoading(true);
    setStatusText("");
    try {
      await canvasApi.updateProjectMemberRole(projectId, member.id, { role });
      await refreshMembers();
      setStatusText("成员角色已更新");
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "角色更新失败");
    } finally {
      setLoading(false);
    }
  }

  async function removeMember(member: CanvasMember) {
    setLoading(true);
    setStatusText("");
    try {
      await canvasApi.removeProjectMember(projectId, member.id);
      await refreshMembers();
      setStatusText("成员已移除");
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "移除失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-[180] flex items-center justify-center bg-charcoal/25 p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.14 }}
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) openChanged(false);
          }}
        >
          <motion.div
            className="w-full max-w-[520px] rounded-2xl border border-border-warm bg-background p-4 shadow-[0_24px_80px_rgba(28,28,28,0.18)]"
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.16, ease: "easeOut" }}
          >
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h2 className="text-sm font-semibold text-charcoal">共享画布</h2>
                <p className="mt-1 text-xs text-muted-gray">复制链接或邀请成员加入当前协作项目。</p>
              </div>
              <button
                type="button"
                onClick={() => openChanged(false)}
                className="rounded-lg p-1.5 text-muted-gray hover:bg-muted hover:text-charcoal"
                aria-label="关闭共享弹窗"
              >
                <X className="size-4" />
              </button>
            </div>

            <div className="space-y-4">
              <section>
                <div className="mb-2 flex items-center gap-1.5 text-xs font-medium text-charcoal">
                  <Link2 className="size-3.5" />
                  协作链接
                </div>
                <div className="flex gap-2">
                  <input
                    value={shareUrl}
                    readOnly
                    className="input-base h-9 flex-1 truncate px-3 text-xs"
                  />
                  <button
                    type="button"
                    onClick={copyShareUrl}
                    className="flex h-9 items-center gap-1.5 rounded-md border border-border-warm px-3 text-xs text-charcoal hover:bg-muted"
                  >
                    {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
                    {copied ? "已复制" : "复制"}
                  </button>
                </div>
                <p className="mt-1.5 text-[11px] text-muted-gray">链接不会自动授权，非项目成员打开后仍会被后端拦截。</p>
              </section>

              <section className={!canManageMembers ? "opacity-60" : ""}>
                <div className="mb-2 flex items-center gap-1.5 text-xs font-medium text-charcoal">
                  <UserPlus className="size-3.5" />
                  邀请成员
                </div>
                <div className="flex gap-2">
                  <input
                    value={inviteUserId}
                    onChange={(event) => setInviteUserId(event.target.value.replace(/\D/g, ""))}
                    disabled={!canManageMembers || loading}
                    placeholder="输入用户 ID"
                    className="input-base h-9 flex-1 px-3 text-xs"
                  />
                  <select
                    value={inviteRole}
                    onChange={(event) => setInviteRole(event.target.value as EditableRole)}
                    disabled={!canManageMembers || loading}
                    className="h-9 rounded-md border border-border-warm bg-background px-2 text-xs text-charcoal outline-none disabled:opacity-50"
                  >
                    <option value="editor">editor</option>
                    <option value="viewer">viewer</option>
                  </select>
                  <button
                    type="button"
                    onClick={inviteMember}
                    disabled={!canManageMembers || loading}
                    className="h-9 rounded-md bg-charcoal px-3 text-xs text-off-white disabled:opacity-50"
                  >
                    邀请
                  </button>
                </div>
                {!canManageMembers && <p className="mt-1.5 text-[11px] text-muted-gray">只有 owner 可以邀请和管理成员。</p>}
              </section>

              <section>
                <div className="mb-2 text-xs font-medium text-charcoal">当前成员</div>
                <div className="max-h-64 overflow-auto rounded-xl border border-border-warm">
                  {members.length === 0 ? (
                    <div className="px-3 py-6 text-center text-xs text-muted-gray">暂无成员</div>
                  ) : (
                    members.map((member) => {
                      const locked = member.role === "owner" || !canManageMembers;
                      return (
                        <div key={member.id} className="flex items-center gap-3 border-b border-border-warm px-3 py-2 last:border-b-0">
                          <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-medium text-charcoal">
                            {String(member.userId).slice(-2)}
                          </div>
                          <div className="min-w-0 flex-1">
                            <div className="truncate text-xs font-medium text-charcoal">用户 {member.userId}</div>
                            <div className="text-[11px] text-muted-gray">{member.joinedTime ? `加入于 ${member.joinedTime}` : "项目成员"}</div>
                          </div>
                          <select
                            value={member.role}
                            disabled={locked || loading}
                            onChange={(event) => updateMemberRole(member, event.target.value)}
                            className="h-8 rounded-md border border-border-warm bg-background px-2 text-xs text-charcoal outline-none disabled:opacity-60"
                          >
                            <option value="owner">owner</option>
                            <option value="editor">editor</option>
                            <option value="viewer">viewer</option>
                          </select>
                          <button
                            type="button"
                            onClick={() => removeMember(member)}
                            disabled={locked || loading}
                            className="rounded-md p-1.5 text-muted-gray hover:bg-muted hover:text-destructive disabled:opacity-40"
                            aria-label={`移除用户 ${member.userId}`}
                          >
                            <Trash2 className="size-3.5" />
                          </button>
                        </div>
                      );
                    })
                  )}
                </div>
              </section>
            </div>

            {statusText && <div className="mt-3 rounded-lg bg-muted px-3 py-2 text-xs text-charcoal/75">{statusText}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
