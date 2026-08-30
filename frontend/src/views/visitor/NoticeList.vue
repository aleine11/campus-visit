<template>
  <div class="notice-list-page">
    <h2 class="page-title">校园公告</h2>

    <div v-loading="loading">
      <el-card
        v-for="n in list"
        :key="n.id"
        class="notice-item"
        shadow="hover"
        @click="router.push(`/visitor/notice/${n.id}`)"
      >
        <div class="notice-head">
          <h3 class="notice-title">{{ n.title }}</h3>
          <span class="notice-time">
            <el-icon><Clock /></el-icon>
            {{ formatDateTime(n.publishTime) }}
          </span>
        </div>
        <p class="notice-summary">{{ n.summary }}</p>
      </el-card>

      <EmptyState v-if="!loading && list.length === 0" description="暂无公告" />
    </div>

    <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
  </div>
</template>

<script setup>
/**
 * 公告列表页（对标 F2：分页浏览已发布公告）
 * 点击卡片 → 详情页 F3
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pagePublished } from '@/api/notice'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const page = await pagePublished(query)
    list.value = page.records || []
    total.value = Number(page.total) || 0
  } catch {
    /* 静默 */
  } finally {
    loading.value = false
  }
}

function onPageChange({ current, size }) {
  query.current = current
  query.size = size
  load()
}

onMounted(load)
</script>

<style scoped>
.page-title {
  margin-bottom: 18px;
  color: #303133;
}
.notice-item {
  cursor: pointer;
  margin-bottom: 14px;
}
.notice-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.notice-title {
  font-size: 16px;
  color: #303133;
}
.notice-title:hover {
  color: #409eff;
}
.notice-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #c0c4cc;
  flex-shrink: 0;
}
.notice-summary {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}
</style>
