---
sidebar_position: 1
title: 使用指南入口
slug: /
---

# 使用指南入口

Draw2Video 使用指南采用 Docusaurus 独立文档站承载，管理后台通过 `/guide/` 入口嵌入或跳转访问。

当前原型聚焦三件事：

- 验证 Docusaurus 与现有 Vue 管理端可以通过静态路径集成。
- 约定指南内容从数据库同步为构建快照，而不是让文档站运行时直接查库。
- 为后续指南浏览、内容管理、发布和搜索能力保留边界。

## 验收路径

```bash
cd yudao-ui/draw2video-guide
pnpm install
pnpm build
```

构建产物位于 `build/`，测试环境可发布到静态资源目录 `/guide/`。
