# AIGC admin i18n hardcoded Chinese scan

Scan time: 2026-06-16 19:50 Asia/Shanghai

Scope:

- `src/views/aigc/`
- `src/api/aigc/`

Completed in this round:

- Asset pages already migrated to `aigc.asset.*` locale keys in `src/locales/zh-CN.ts` and `src/locales/en.ts`.
- Model option constants have centralized option definitions that can be reused by later model page migrations.

Remaining user-visible Chinese by area:

| Area | Representative files | Follow-up |
| --- | --- | --- |
| Model base pages | `model/model/index.vue`, `model/model/ModelForm.vue`, `model/model/ModelDetailPage.vue` | Covered by #212 for base list/form; detail page still needs a dedicated pass. |
| Model price/route/proxy/param pages | `model/price/*`, `model/route/*`, `model/proxy/*`, `model/param/*` | Covered by #213 for price/route/proxy/param list/form pages. |
| Model tenant and usage pages | `model/tenant/*`, `model/usage/index.vue` | Create a follow-up i18n task; usage contains chart titles and detail dialog labels. |
| Billing pages | `billing/recharge/*`, `billing/recharge-package/*`, `billing/utils.ts` | Create a follow-up i18n task after billing state copy is finalized. |
| Safety pages | `safety/sensitive-word/*` | Small follow-up migration; labels and validation messages remain Chinese. |
| Release notes | `release-note/*` | Small follow-up migration; form/list labels remain Chinese. |

Recommended next split:

1. `model/usage` charts and detail dialog.
2. `model/tenant` tenant policy pages.
3. `billing` recharge/recharge-package pages.
4. `safety` and `release-note` small pages.

Validation command used for this inventory:

```powershell
rg -n "[\u4e00-\u9fff]" yudao-ui\draw2video-admin\src\views\aigc yudao-ui\draw2video-admin\src\api\aigc -g "*.vue" -g "*.ts"
```
