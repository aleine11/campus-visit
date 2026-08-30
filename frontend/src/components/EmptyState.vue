<template>
  <!-- 空数据占位（对标 frontend-prototype.md 8.2） -->
  <el-empty :description="description" :image-size="120" />
</template>

<script setup>
/**
 * 空数据占位组件
 * 用途：列表无数据 / 搜索无结果时显示，避免"白屏让人以为是坏了"
 */
defineProps({
  description: { type: String, default: '暂无数据' },
})
</script>
