import React, { useMemo, useState } from 'react'
import Layout from '@theme/Layout'
import Link from '@docusaurus/Link'

type GuideSearchItem = {
  title: string
  module: string
  summary: string
  path: string
  body: string
}

const publishedGuides: GuideSearchItem[] = [
  {
    title: '使用指南入口',
    module: '指南首页',
    summary: '说明 Draw2Video 使用指南入口、静态发布路径和构建验证方式。',
    path: '/',
    body: 'Draw2Video 使用指南 Docusaurus 管理后台 /guide 构建 静态站点 发布'
  },
  {
    title: '全业务模块内容模板与维护流程',
    module: '内容维护',
    summary: '定义模块目录、内容模板、维护责任和首批录入清单。',
    path: '/content-template',
    body: '业务模块 模板 适用对象 操作入口 步骤 截图 注意事项 常见问题 维护流程 首批录入'
  },
  {
    title: 'Docusaurus 集成方案',
    module: '平台集成',
    summary: '说明独立文档站与管理端入口、静态路径和部署方式。',
    path: '/docusaurus-integration',
    body: 'Docusaurus 集成 管理端 入口 静态资源 /guide Nginx 部署'
  },
  {
    title: '数据库内容源',
    module: '内容同步',
    summary: '说明指南内容从数据库导出为 Markdown、MDX 或 JSON 快照。',
    path: '/database-content-source',
    body: '数据库 内容 指南 快照 Markdown MDX JSON 构建 同步'
  }
]

function searchGuides(keyword: string) {
  const query = keyword.trim().toLowerCase()
  if (!query) return publishedGuides
  return publishedGuides.filter((item) =>
    `${item.title} ${item.module} ${item.summary} ${item.body}`.toLowerCase().includes(query)
  )
}

export default function SearchPage() {
  const [keyword, setKeyword] = useState('')
  const results = useMemo(() => searchGuides(keyword), [keyword])

  return (
    <Layout title="指南搜索" description="搜索已发布的 Draw2Video 使用指南">
      <main className="guide-search-page">
        <section className="guide-search-header">
          <h1>指南搜索</h1>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="输入关键词搜索已发布指南"
            aria-label="搜索指南内容"
            autoFocus
          />
        </section>

        <section className="guide-search-results">
          {results.length > 0 ? (
            results.map((item) => (
              <Link className="guide-search-result" key={item.path} to={item.path}>
                <div className="guide-search-module">{item.module}</div>
                <h2>{item.title}</h2>
                <p>{item.summary}</p>
              </Link>
            ))
          ) : (
            <div className="guide-search-empty">没有匹配的已发布指南</div>
          )}
        </section>
      </main>
    </Layout>
  )
}
