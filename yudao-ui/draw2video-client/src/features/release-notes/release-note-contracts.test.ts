import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const releaseNotesPage = readFileSync(
  resolve(process.cwd(), "src/app/(app)/release-notes/page.tsx"),
  "utf8",
);
const appLayout = readFileSync(resolve(process.cwd(), "src/app/(app)/layout.tsx"), "utf8");
const systemDbSql = readFileSync(
  resolve(process.cwd(), "../../sql/mysql/system/system_db.sql"),
  "utf8",
);
const aigcAdminMenuSql = readFileSync(
  resolve(process.cwd(), "../../sql/mysql/system/aigc_admin_menu.sql"),
  "utf8",
);

describe("release note page contracts", () => {
  it("loads published release notes and keeps an empty-state fallback", () => {
    expect(releaseNotesPage).toContain("getPublishedReleaseNotes(30)");
    expect(releaseNotesPage).toContain("setSelectedId(data[0]?.id ?? null)");
    expect(releaseNotesPage).toContain("暂无更新记录");
    expect(releaseNotesPage).toContain("版本记录暂时不可用");
    expect(releaseNotesPage).toContain("本次更新暂无详细内容。");
    expect(releaseNotesPage).toContain("formatReleaseDate(selectedNote.releaseDate)");
  });
});

describe("release note menu contracts", () => {
  it("places the release note entry after the language item and links to the page", () => {
    const languageIndex = appLayout.indexOf("简体中文");
    const releaseNoteIndex = appLayout.indexOf("版本更新");

    expect(appLayout).toContain("getPublishedReleaseNotes(1)");
    expect(appLayout).toContain("useState(getFallbackVersion())");
    expect(appLayout).toContain('href="/release-notes"');
    expect(releaseNoteIndex).toBeGreaterThan(languageIndex);
  });
});

describe("release note admin sql contracts", () => {
  it("keeps menu and button permissions in initialization sql files", () => {
    for (const sql of [systemDbSql, aigcAdminMenuSql]) {
      expect(sql).toContain("版本更新记录");
      expect(sql).toContain("aigc:release-note:query");
      expect(sql).toContain("aigc:release-note:create");
      expect(sql).toContain("aigc:release-note:update");
      expect(sql).toContain("aigc:release-note:publish");
      expect(sql).toContain("aigc:release-note:delete");
    }
  });
});
