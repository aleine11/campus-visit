<template>
  <div class="notice-detail-page" v-loading="loading">
    <template v-if="detail">
      <el-card>
        <h1 class="detail-title">{{ detail.title }}</h1>
        <div class="detail-meta">
          <el-icon><Clock /></el-icon>
          发布时间：{{ formatDateTime(detail.publishTime) }}
        </div>
        <el-divider />
        <!-- 正文：pre-wrap 保留后端存的换行与缩进 -->
        <div class="detail-content">{{ detail.content }}</div>
        <el-divider />
        <!-- 上一篇/下一篇导航（后端 prevId/nextId，草稿不参与） -->
        <div class="detail-nav">
          <el-button text :disabled="!detail.prevId" @click="goOther(detail.prevId)">
            &lt; 上一篇
          </el-button>
          <el-button text :disabled="!detail.nextId" @click="goOther(detail.nextId)">
            下一篇 &gt;
          </el-button>
        </div>
      </el-card>
      <el-button class="back-btn" text @click="router.push('/visitor/notice')">
        <el-icon><Back /></el-icon> 返回列表
      </el-button>
    </template>
    <EmptyState v-else-if="!loading" description="公告不存在或已下架" />
  </div>
</template>

<script setup>
/**
 * 公告详情页（对标 F3：正文 + 上一篇/下一篇）
 * 切换上下篇：replaceCurrent 避免历史记录堆栈越积越深
 */
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDetail } from '@/api/notice'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import { Back, Clock } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)

async function load(id) {
  if (!id) return
  loading.value = true
  try {
    detail.value = await getDetail(id)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 跳上/下一篇：替换当前路由并重新加载 */
function goOther(id) {
  if (!id) return
  router.replace(`/visitor/notice/${id}`)
}

// 监听路由参数变化（点上一篇/下一篇时 URL 变了但组件复用，必须手动重载）
watch(
  () => route.params.id,
  (id) => load(id),
  { immediate: true }
)
</script>

<style scoped>
.detail-title {
  font-size: 24px;
  color: #303133;
  line-height: 1.4;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  margin-top: 10px;
}
.detail-content {
  font-size: 15px;
  line-height: 1.9;
  color: #303133;
  white-space: pre-wrap; /* 保留换行 */
  min-height: 120px;
}
.detail-nav {
  display: flex;
  justify-content: space-between;
}
.back-btn {
  margin-top: 16px;
}
</style>
