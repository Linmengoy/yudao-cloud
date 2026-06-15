---
sidebar_position: 2
title: Docusaurus 集成方案
---

# Docusaurus 集成方案

## 推荐结论

采用 Docusaurus 3 的 classic 模板，作为独立静态文档站接入 Draw2Video。

## 集成边界

| 项目 | 方案 |
| --- | --- |
| 前端工程 | `yudao-ui/draw2video-guide` |
| 管理端入口 | `yudao-ui/draw2video-admin/src/views/aigc/guide/index.vue` |
| 默认访问路径 | `/guide/` |
| 构建命令 | `pnpm build` |
| 发布产物 | `build/` |
| 菜单权限 | `aigc:guide:query` |

## 部署影响

管理端不直接打包 Docusaurus。Docusaurus 单独构建后，将静态产物挂载到前端站点的 `/guide/` 路径。

推荐 Nginx 形态：

```nginx
location /guide/ {
  alias /srv/draw2video-guide/;
  try_files $uri $uri/ /guide/index.html;
}
```

## 后续约束

- 指南站不持有后台登录态。
- 需要权限控制的编辑流程仍放在管理端。
- 文档发布流程通过构建快照完成，避免文档站频繁查询业务库。
- 全文搜索后续可接入 Docusaurus 本地索引或独立搜索服务。
