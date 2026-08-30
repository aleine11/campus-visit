<template>
  <div class="dashboard-page">
    <!-- ===== 4 个数字卡片（对标 frontend-prototype.md A1） ===== -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="26" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <!-- ===== 近 7 天预约趋势折线图 ===== -->
      <el-col :xs="24" :sm="14">
        <el-card shadow="never">
          <template #header>近 7 天预约趋势</template>
          <div ref="chartRef" class="trend-chart" />
        </el-card>
      </el-col>

      <!-- ===== 最近 5 条待审核订单（快捷审核入口） ===== -->
      <el-col :xs="24" :sm="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>最近待审核</span>
              <el-button text type="primary" size="small" @click="router.push('/admin/reservation')">
                全部订单 &gt;
              </el-button>
            </div>
          </template>
          <div v-loading="loading">
            <template v-if="dash.recentPending?.length">
              <div v-for="row in dash.recentPending" :key="row.id" class="pending-item">
                <div class="pending-main">
                  <span class="pending-name">{{ row.realName || '访客' }}</span>
                  <span class="pending-meta">{{ formatDate(row.visitDate) }} {{ row.timeSlot }} · {{ row.peopleCount }} 人</span>
                </div>
                <div class="pending-side">
                  <span class="pending-time">{{ formatDateTime(row.submitTime) }}</span>
                  <el-button type="primary" size="small" @click="router.push('/admin/reservation')">去审核</el-button>
                </div>
              </div>
            </template>
            <EmptyState v-else-if="!loading" description="暂无待审核订单，全部处理完毕" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 后台首页看板（对标 frontend-prototype.md A1 + architecture.md 10.1 DashboardVO）
 *
 * 一次请求拿全 6 块数据（后端聚合好的，前端不发多个请求）：
 *   todayReservationCount / pendingAuditCount / visitorTotal / chatTotalCount
 *   weeklyTrend（[{date, count}]，已补零 7 个点连续）
 *   recentPending（最近 5 条待审核，复用预约列表 VO）
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Calendar, Bell, User, ChatDotRound } from '@element-plus/icons-vue'
import { dashboard } from '@/api/stats'
import { formatDate, formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const dash = reactive({
  todayReservationCount: 0,
  pendingAuditCount: 0,
  visitorTotal: 0,
  chatTotalCount: 0,
  weeklyTrend: [],
  recentPending: [],
})

/** 卡片配置：value 用 computed 跟随 dash 响应式更新 */
const cards = computed(() => [
  { label: '今日新增预约', value: dash.todayReservationCount, icon: Calendar, bg: '#409eff' },
  { label: '待审核订单', value: dash.pendingAuditCount, icon: Bell, bg: '#e6a23c' },
  { label: '访客总数', value: dash.visitorTotal, icon: User, bg: '#67c23a' },
  { label: 'AI 问答总次数', value: dash.chatTotalCount, icon: ChatDotRound, bg: '#909399' },
])

/* ===== ECharts：初始化 / 更新 / 销毁（窗口缩放自适应） ===== */
const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const dates = dash.weeklyTrend.map((d) => formatDate(d.date))
  const counts = dash.weeklyTrend.map((d) => d.count)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 }, // 人数是整数，不允许出现 0.5 这种刻度
    series: [
      {
        name: '预约数',
        type: 'line',
        smooth: true,
        data: counts,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#409eff' },
      },
    ],
  })
}

function handleResize() {
  chart?.resize()
}

onMounted(async () => {
  loading.value = true
  try {
    const vo = await dashboard()
    Object.assign(dash, vo)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
  renderChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 组件销毁要清监听 + 销毁实例，否则内存泄漏（SPA 页面来回切会越积越多）
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}
.mt16 {
  margin-top: 16px;
}
.trend-chart {
  height: 300px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pending-item {
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.pending-item:last-child {
  border-bottom: none;
}
.pending-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.pending-name {
  font-weight: 600;
  color: #303133;
}
.pending-meta {
  font-size: 12px;
  color: #909399;
}
.pending-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.pending-time {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
