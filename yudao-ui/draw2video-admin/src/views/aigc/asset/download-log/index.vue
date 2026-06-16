<template>
  <ContentWrap>
    <el-empty :description="t('aigc.asset.downloadLog.empty')" />
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'AigcAssetDownloadLog' })

const { t } = useI18n()
</script>
