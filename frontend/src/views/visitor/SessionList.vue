<template>
  <div class="session-list-page">
    <h2 class="page-title">预约参观</h2>

    <div v-loading="loading">
      <el-row v-if="list.length" :gutter="20">
        <el-col v-for="s in list" :key="s.id" :span="8">
          <el-card class="session-card" shadow="hover">
            <div class="session-date">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(s.visitDate) }}
            </div>
            <div class="session-slot">
              <el-icon><AlarmClock /></el-icon>
              {{ s.timeSlot }}
            </div>
            <el-progress
              :percentage="Math.round((s.usedPeople / s.maxPeople) * 100)"
              :color="progressColor(s)"
              :stroke-width="10"
            />
            <div class="session-meta">
              <span>已预约 {{ s.usedPeople }} / {{ s.maxPeople }}</span>
              <span :class="s.remaining > 0 ? 'remain-ok' : 'remain-zero'">剩余 {{ s.remaining }}</span>
            </div>
            <el-button
              type="primary"
              class="book-btn"
              :disabled="s.remaining <= 0"
              @click="toBook(s)"
            >
              {{ s.remaining > 0 ? '立即预约' : '已满员' }}
            </el-button>
          </el-card>
        </el-col>
      </el-row>
      <EmptyState v-else-if="!loading" description="暂无可预约场次" />
    </div>

    <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
  </div>
</template>

<script setup>
/**
 * 场次列表页（对标 F4：可预约场次，名额进度条可视化）
 * 后端已自动过滤：下架场次 + 过期场次（visitDate < 今天），前端无需再筛
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, AlarmClock } from '@element-plus/icons-vue'
import { pageAvailable } from '@/api/session'
import { useUserStore } from '@/store/user'
import { formatDate } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 9 }) // 9 = 3列×3行 整齐

function progressColor(s) {
  const pct = (s.usedPeople / s.maxPeople) * 100
  if (pct > 50) return '#67C23A'
  if (pct > 20) return '#E6A23C'
  return '#F56C6C'
}

/** 点预约：未登录 → 去登录并记住回跳；已登录 → 去填预约表单 */
function toBook(s) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再预约')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (userStore.isAdmin) {
    ElMessage.info('管理员账号无需预约，请用访客账号操作')
    return
  }
  router.push(`/visitor/reservation/submit/${s.id}`)
}

async function load() {
  loading.value = true
  try {
    const page = await pageAvailable(query)
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
}
.session-card {
  margin-bottom: 20px;
}
.session-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 6px;
}
.session-slot {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
  margin-bottom: 12px;
}
.session-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  margin: 10px 0 14px;
}
.remain-ok {
  color: #67c23a;
  font-weight: 600;
}
.remain-zero {
  color: #f56c6c;
  font-weight: 600;
}
.book-btn {
  width: 100%;
}
</style>
