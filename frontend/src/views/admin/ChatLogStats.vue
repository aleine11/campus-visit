<template>
  <div class="chatlog-page">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ===== Tab1：问答记录分页（对标 A7-1） ===== -->
      <el-tab-pane label="问答记录" name="logs">
        <el-card shadow="never">
          <el-form inline @submit.prevent>
            <el-form-item label="关键词">
              <el-input
                v-model="logQuery.keyword"
                placeholder="搜问题或回答"
                clearable
                style="width: 160px"
                @keyup.enter="searchLogs"
                @clear="searchLogs"
              />
            </el-form-item>
            <el-form-item label="访客">
              <el-select
                v-model="logQuery.visitorId"
                placeholder="全部访客"
                clearable
                filterable
                style="width: 150px"
                @change="searchLogs"
              >
                <el-option v-for="v in visitors" :key="v.id" :label="v.realName || v.username" :value="v.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker
                v-model="logDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="截止"
                value-format="YYYY-MM-DD"
                style="width: 260px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchLogs">查询</el-button>
            </el-form-item>
          </el-form>

          <div v-loading="logsLoading">
            <el-table v-if="logs.length" :data="logs" stripe>
              <el-table-column prop="visitorName" label="访客" width="100" />
              <el-table-column prop="question" label="问题" min-width="180" show-overflow-tooltip />
              <el-table-column prop="answer" label="AI 回答" min-width="260" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.answer }}
                  <el-tag v-if="row.referDocName" size="small" type="success" class="refer-tag">
                    引用：{{ row.referDocName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="回答时间" width="160">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
            <EmptyState v-else-if="!logsLoading" description="暂无问答记录" />
          </div>
          <PaginationBar :current="logQuery.current" :size="logQuery.size" :total="logsTotal" @change="onLogPageChange" />
        </el-card>
      </el-tab-pane>

      <!-- ===== Tab2：高频问题统计（对标 A7-2：Top10 柱状图 + 词标签云） ===== -->
      <el-tab-pane label="高频统计" name="stats">
        <el-card shadow="never">
          <div class="stats-head">
            <span class="stats-title">近</span>
            <el-input-number v-model="days" :min="1" :max="365" size="small" @change="loadStats" />
            <span class="stats-title">天访客提问热词</span>
          </div>

          <el-row :gutter="16">
            <!-- Top10 柱状图 -->
            <el-col :xs="24" :md="12">
              <div ref="barRef" class="chart-box" />
            </el-col>

            <!-- 方案 A：CSS 标签云（频次越高字越大色越深） -->
            <el-col :xs="24" :md="12">
              <div class="tag-cloud">
                <el-empty v-if="!stats.wordCloud?.length" description="暂无提问数据" :image-size="90" />
                <el-tooltip
                  v-for="w in stats.wordCloud"
                  :key="w.keyword"
                  :content="`「${w.keyword}」出现 ${w.count} 次`"
                  placement="top"
                >
                  <span
                    class="cloud-tag"
                    :style="tagStyle(w)"
                  >{{ w.keyword }}</span>
                </el-tooltip>
              </div>
            </el-col>
          </el-row>

          <el-alert
            class="stats-tip"
            type="info"
            :closable="false"
            show-icon
            title="统计口径：对近 N 天访客提问逐字符统计，过滤标点和「的了吗呢」等停用字后取频次 Top100（单字简化方案，生产可升级 IK 分词出完整词语）"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
/**
 * 问答日志统计页（对标 frontend-prototype.md A7 + architecture.md 模块 10 三接口）
 *
 * Tab1 问答记录：GET /admin/stats/chat-log/page（keyword/visitorId/startDate/endDate 分页）
 * Tab2 高频统计：GET /admin/stats/hot-keywords?days=N → topKeywords(前10) + wordCloud(前100)
 *
 * 词云用方案 A（CSS 标签云）：零新依赖，字号 14~34px 按频次线性映射，颜色按频次分档
 */
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { pageChatLogs, hotKeywords } from '@/api/stats'
import { pageForAdmin as pageVisitors } from '@/api/visitor'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const activeTab = ref('logs')

/* ===== Tab1 问答记录 ===== */
const logsLoading = ref(false)
const logs = ref([])
const logsTotal = ref(0)
const logDateRange = ref(null)
const visitors = ref([]) // 访客下拉（进入页面拉一次全量分页第一页够用）
const logQuery = reactive({ keyword: '', visitorId: null, current: 1, size: 10 })

async function loadLogs() {
  logsLoading.value = true
  try {
    const params = { ...logQuery }
    if (logDateRange.value?.length === 2) {
      params.startDate = logDateRange.value[0]
      params.endDate = logDateRange.value[1]
    }
    const p = await pageChatLogs(params)
    logs.value = p.records || []
    logsTotal.value = Number(p.total) || 0
  } catch {
    /* 拦截器已提示 */
  } finally {
    logsLoading.value = false
  }
}

function searchLogs() {
  logQuery.current = 1
  loadLogs()
}

function onLogPageChange({ current, size }) {
  logQuery.current = current
  logQuery.size = size
  loadLogs()
}

/** 访客下拉数据（取第一页 50 个，毕设数据量足够） */
async function loadVisitors() {
  try {
    const p = await pageVisitors({ current: 1, size: 50 })
    visitors.value = p.records || []
  } catch {
    /* 拦截器已提示 */
  }
}

/* ===== Tab2 高频统计 ===== */
const days = ref(30)
const stats = reactive({ topKeywords: [], wordCloud: [] })
const barRef = ref(null)
let barChart = null

async function loadStats() {
  try {
    const vo = await hotKeywords(days.value)
    Object.assign(stats, vo)
    renderBar()
  } catch {
    /* 拦截器已提示 */
  }
}

function renderBar() {
  if (!barRef.value) return
  if (!barChart) barChart = echarts.init(barRef.value)
  // ⭐ 容器可能刚从"隐藏 tab"变为可见（宽度 0 → 正常），
  // resize 让画布按当前容器尺寸重绘，否则柱状图会是 0 宽白板
  barChart.resize()
  const top = stats.topKeywords || []
  barChart.setOption({
    title: { text: 'Top 10 高频字', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {},
    grid: { left: 40, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: top.map((k) => k.keyword) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        data: top.map((k) => k.count),
        barWidth: '55%',
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top' },
      },
    ],
  })
}

/** 切换到"高频统计" tab 时才渲染图表：
 *  页面加载时 Tab2 处于 display:none，容器宽度为 0，
 *  若此时 echarts.init 会得到 0 宽画布（白板）→ 必须等 tab 可见后再画 */
async function onTabChange(name) {
  if (name !== 'stats') return
  await nextTick()
  renderBar()
}

/** 标签云样式：频次 → 字号线性映射（min 14px，max 34px），颜色按频次分三档 */
function tagStyle(w) {
  const list = stats.wordCloud || []
  const max = list[0]?.count || 1
  const min = list[list.length - 1]?.count || 1
  const ratio = max === min ? 1 : (w.count - min) / (max - min)
  const fontSize = 14 + Math.round(ratio * 20)
  const color = ratio > 0.66 ? '#409eff' : ratio > 0.33 ? '#79bbff' : '#a0cfff'
  return { fontSize: fontSize + 'px', color }
}

function handleResize() {
  barChart?.resize()
}

onMounted(() => {
  loadLogs()
  loadVisitors()
  // ECharts 容器在 Tab2 里，v-if 渲染后才有尺寸 → 等 DOM 更新再初始化
  nextTick(() => {
    loadStats()
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  barChart = null
})
</script>

<style scoped>
.refer-tag {
  margin-left: 6px;
}
.stats-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.stats-title {
  font-size: 14px;
  color: #606266;
}
.chart-box {
  height: 320px;
}
.tag-cloud {
  height: 320px;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 10px 14px;
  padding: 20px;
  border-left: 1px dashed #ebeef5;
  overflow-y: auto;
}
.cloud-tag {
  cursor: default;
  line-height: 1.4;
  user-select: none; /* 纯展示数据，禁止选中更顺滑 */
}
.stats-tip {
  margin-top: 10px;
}
</style>
