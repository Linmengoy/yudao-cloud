<template>
  <ContentWrap>
    <div class="guide-toolbar">
      <div>
        <div class="guide-title">使用指南</div>
        <div class="guide-subtitle">Docusaurus 原型入口，测试环境默认发布到 /guide/。</div>
      </div>
      <div class="guide-actions">
        <el-button @click="openGuide">
          <Icon icon="ep:link" class="mr-5px" />打开站点
        </el-button>
        <el-button type="primary" @click="openSource">
          <Icon icon="ep:document" class="mr-5px" />查看工程
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-row :gutter="16">
      <el-col v-for="item in decisions" :key="item.title" :xs="24" :lg="8" class="mb-16px">
        <el-card shadow="never">
          <div class="decision-title">{{ item.title }}</div>
          <div class="decision-body">{{ item.body }}</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <IFrame :src="guideUrl" />
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'AigcGuide' })

const guideUrl = computed(() => import.meta.env.VITE_APP_GUIDE_URL || '/guide/')

const decisions = [
  {
    title: '技术选型',
    body: '采用 Docusaurus 3 classic 模板，作为独立静态文档站维护。'
  },
  {
    title: '部署边界',
    body: '文档站单独构建，产物由前端静态服务发布，不侵入管理端构建链路。'
  },
  {
    title: '内容源',
    body: '数据库内容先导出为 Markdown 或 JSON 快照，文档站构建时读取快照。'
  }
]

const openGuide = () => {
  window.open(guideUrl.value)
}

const openSource = () => {
  window.open('https://docusaurus.io/docs/installation')
}
</script>

<style scoped>
.guide-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.guide-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.guide-subtitle,
.decision-body {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.guide-actions {
  display: flex;
  gap: 8px;
}

.decision-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

@media (max-width: 768px) {
  .guide-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .guide-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
