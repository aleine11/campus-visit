<template>
  <div class="session-booking-page">
    <!-- ===== 横幅 ===== -->
    <div class="booking-banner">
      <div class="banner-crumb">首页 / 预约参观</div>
      <h1 class="banner-title">预约参观</h1>
      <p class="banner-sub">CAMPUS TOUR BOOKING · 选择日期与时段，几分钟完成校园参观预约</p>
    </div>

    <div class="booking-card" v-loading="loading">
      <!-- ===== ① 横向日期胶囊条 ===== -->
      <div class="date-bar">
        <div class="date-bar-label">选择日期</div>
        <button class="date-arrow" :disabled="!canScrollLeft" @click="scrollDates(-1)">‹</button>
        <div class="date-scroll" ref="dateScrollRef" @scroll="onScroll">
          <div
            v-for="(day, i) in days"
            :key="day.key"
            class="date-cell"
            :class="{ active: i === selectedIndex, disabled: day.sessions.length === 0 }"
            @click="selectDay(i)"
          >
            <span v-if="i === 0" class="dc-tag">今天</span>
            <span v-else-if="i === 1" class="dc-tag">明天</span>
            <span class="dc-week">{{ WEEK[day.date.getDay()] }}</span>
            <span class="dc-day">{{ day.date.getDate() }}</span>
            <span
              v-if="day.sessions.length"
              class="dc-dot"
              :class="{ full: day.sessions.every((s) => s.remaining <= 0) }"
            ></span>
          </div>
        </div>
        <button class="date-arrow" :disabled="!canScrollRight" @click="scrollDates(1)">›</button>
        <button class="today-btn" @click="backToday">回到今天</button>
      </div>

      <!-- ===== ② 当日标题与统计 ===== -->
      <div class="day-header">
        <span class="d-title">{{ currentDayTitle }}</span>
        <span v-if="currentDay" class="d-week">{{ WEEK[currentDay.date.getDay()] }}</span>
        <span class="d-stat">{{ currentDayStat }}</span>
      </div>

      <!-- ===== ③ 上午/下午分组场次 ===== -->
      <div v-if="currentDay && currentDay.sessions.length">
        <div v-for="g in groups" :key="g.key" class="slot-group" v-show="sessionsOf(g.key).length">
          <div class="group-label">
            <span class="g-icon">{{ g.icon }}</span>{{ g.name }}
            <span class="g-line"></span>
          </div>
          <div class="slot-grid">
            <div
              v-for="s in sessionsOf(g.key)"
              :key="s.id"
              class="slot-card"
              :class="statusOf(s)"
              @click="toBook(s)"
            >
              <div v-if="s.remaining <= 0" class="full-ribbon">已满</div>
              <div class="slot-time">{{ s.timeSlot }}</div>
              <div class="slot-duration">时长约 {{ durationOf(s.timeSlot) }} · {{ g.name }}</div>
              <div class="slot-progress">
                <div class="bar" :style="{ width: percentOf(s) + '%', background: barColor(s) }"></div>
              </div>
              <div class="slot-meta">
                <span>已约 {{ s.usedPeople }}/{{ s.maxPeople }} 人</span>
                <span class="slot-status" :class="'status-' + statusOf(s)">{{ statusText(s) }}</span>
              </div>
              <button class="slot-btn" :class="statusOf(s)" :disabled="s.remaining <= 0">
                {{ s.remaining <= 0 ? '已满员' : s.remaining <= TIGHT_LIMIT ? '立即预约（紧张）' : '立即预约' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 当天无场次 -->
      <div v-else-if="!loading" class="empty-day">
        <div class="e-icon">📅</div>
        <div>这一天还没有安排参观场次，请选择其他日期</div>
      </div>

      <!-- 图例 -->
      <div class="legend">
        <span><i style="background: #67c23a"></i>名额充足</span>
        <span><i style="background: #e6a23c"></i>即将约满（剩余≤{{ TIGHT_LIMIT }}人）</span>
        <span><i style="background: #c0c4cc"></i>已满员</span>
        <span><i class="dot-empty"></i>灰色日期=当天无场次</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 场次选择页（对标 F4，新版"先选日期、再选时段"两步式）
 *
 * 数据来源：GET /session/available?startDate=&endDate=&size=100
 *   后端一次返回未来 14 天全部可预约场次，前端按 visitDate 分组成日期胶囊条；
 *   上午/下午按时段起始小时（<12 点为上午）自动分组。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageAvailable } from '@/api/session'
import { useUserStore } from '@/store/user'

const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const DAYS = 14 // 日期胶囊条展示未来 14 天
const TIGHT_LIMIT = 10 // 剩余名额 ≤10 显示"即将约满"

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dateScrollRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(true)

/** 14 天日期骨架（sessions 加载后按日期填充） */
const days = ref(buildEmptyDays())
const selectedIndex = ref(0)

const groups = [
  { key: 'am', name: '上午场', icon: '🌅' },
  { key: 'pm', name: '下午场', icon: '☀️' },
]

const currentDay = computed(() => days.value[selectedIndex.value])
const currentDayTitle = computed(() => {
  const d = currentDay.value?.date
  return d ? `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日` : ''
})
const currentDayStat = computed(() => {
  const sessions = currentDay.value?.sessions || []
  if (!sessions.length) return '当天暂无可预约场次'
  const open = sessions.filter((s) => s.remaining > 0).length
  return `共 ${sessions.length} 场 · ${open > 0 ? `${open} 场可预约` : '均已约满'}`
})

function sessionsOf(period) {
  return (currentDay.value?.sessions || []).filter((s) => periodOf(s.timeSlot) === period)
}

/* ============ 数据加载 ============ */
async function load() {
  loading.value = true
  try {
    const start = fmtDate(days.value[0].date)
    const end = fmtDate(days.value[DAYS - 1].date)
    const page = await pageAvailable({ startDate: start, endDate: end, current: 1, size: 100 })
    const records = page.records || []

    // 按 visitDate（"2026-09-12"）挂到对应日期骨架上，每天内按起始时间升序
    const byDate = new Map()
    records.forEach((s) => {
      if (!byDate.has(s.visitDate)) byDate.set(s.visitDate, [])
      byDate.get(s.visitDate).push(s)
    })
    days.value.forEach((day) => {
      day.sessions = (byDate.get(day.key) || []).sort((a, b) => a.timeSlot.localeCompare(b.timeSlot))
    })

    // 默认选中：今天有场次选今天，否则选第一个有场次的日期，都没有就停留今天
    const firstWithSession = days.value.findIndex((d) => d.sessions.length > 0)
    selectedIndex.value = days.value[0].sessions.length > 0 ? 0 : firstWithSession >= 0 ? firstWithSession : 0
    await nextTickVisible()
  } catch {
    /* 请求拦截器已统一弹错误提示 */
  } finally {
    loading.value = false
  }
}

/** DOM 更新后刷新箭头可用状态并把选中胶囊滚入视野 */
async function nextTickVisible() {
  await tick()
  updateArrows()
  scrollActiveIntoView('auto')
}
function tick() {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()))
}

/* ============ 日期条交互 ============ */
function selectDay(i) {
  selectedIndex.value = i
  scrollActiveIntoView('smooth')
}
function scrollDates(dir) {
  dateScrollRef.value?.scrollBy({ left: dir * 250, behavior: 'smooth' })
}
function backToday() {
  selectedIndex.value = 0
  dateScrollRef.value?.scrollTo({ left: 0, behavior: 'smooth' })
}
function onScroll() {
  updateArrows()
}
function updateArrows() {
  const el = dateScrollRef.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 2
  canScrollRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 2
}
function scrollActiveIntoView(behavior) {
  const el = dateScrollRef.value
  const cell = el?.querySelector('.date-cell.active')
  cell?.scrollIntoView({ behavior, inline: 'center', block: 'nearest' })
}

/* ============ 场次状态/展示辅助 ============ */
function statusOf(s) {
  if (s.remaining <= 0) return 'full'
  if (s.remaining <= TIGHT_LIMIT) return 'tight'
  return 'ok'
}
function barColor(s) {
  const st = statusOf(s)
  return st === 'full' ? '#c0c4cc' : st === 'tight' ? '#e6a23c' : '#67c23a'
}
function percentOf(s) {
  return Math.round((s.usedPeople / s.maxPeople) * 100)
}
function statusText(s) {
  const st = statusOf(s)
  if (st === 'full') return '已满员'
  if (st === 'tight') return `即将约满 · 剩 ${s.remaining} 名`
  return `名额充足 · 剩 ${s.remaining} 名`
}

/** "09:00-11:00" → "am"；"14:00-16:00" → "pm" */
function periodOf(timeSlot) {
  const startHour = parseInt(String(timeSlot).slice(0, 2), 10)
  return Number.isFinite(startHour) && startHour < 12 ? 'am' : 'pm'
}

/** "09:00-11:00" → "2 小时" */
function durationOf(timeSlot) {
  const m = /(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})/.exec(String(timeSlot))
  if (!m) return '2 小时'
  const mins = Number(m[3]) * 60 + Number(m[4]) - (Number(m[1]) * 60 + Number(m[2]))
  if (mins <= 0) return '2 小时'
  return mins % 60 === 0 ? `${mins / 60} 小时` : `${(mins / 60).toFixed(1)} 小时`
}

/* ============ 预约跳转（沿用原登录/角色校验逻辑） ============ */
function toBook(s) {
  if (s.remaining <= 0) return
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

/* ============ 日期工具 ============ */
function buildEmptyDays() {
  const result = []
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  for (let i = 0; i < DAYS; i++) {
    const d = new Date(base)
    d.setDate(d.getDate() + i)
    result.push({ date: d, key: fmtDate(d), sessions: [] })
  }
  return result
}
function fmtDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(load)
</script>

<style scoped>
/* 负边距撑满外层 .content 容器（容器 padding 为 24px 16px） */
.session-booking-page {
  margin: -24px -16px 0;
}

/* ===== 横幅 ===== */
.booking-banner {
  height: 170px;
  background: linear-gradient(120deg, #409eff 0%, #2f6bff 55%, #1d4fd6 100%);
  color: #fff;
  padding: 0 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.booking-banner::after {
  content: '';
  position: absolute;
  right: -80px;
  top: -100px;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.banner-crumb {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 10px;
}
.banner-title {
  font-size: 32px;
  letter-spacing: 4px;
  font-weight: 700;
}
.banner-sub {
  margin-top: 10px;
  font-size: 14px;
  opacity: 0.85;
  letter-spacing: 1px;
}

/* ===== 白色悬浮卡片 ===== */
.booking-card {
  margin: -34px 32px 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 24px rgba(31, 67, 138, 0.1);
  padding: 24px 28px 26px;
  position: relative;
  z-index: 2;
  min-height: 420px;
}

/* ===== 日期胶囊条 ===== */
.date-bar {
  display: flex;
  align-items: center;
  gap: 14px;
}
.date-bar-label {
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}
.date-bar-label::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 15px;
  background: #409eff;
  border-radius: 2px;
  margin-right: 8px;
  vertical-align: -2px;
}
.date-arrow {
  width: 34px;
  height: 62px;
  flex-shrink: 0;
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 8px;
  font-size: 18px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}
.date-arrow:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
}
.date-arrow:disabled {
  color: #d3d8e0;
  cursor: not-allowed;
}
.date-scroll {
  flex: 1;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 10px 2px 6px;
}
.date-scroll::-webkit-scrollbar {
  height: 5px;
}
.date-scroll::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}
.date-cell {
  flex-shrink: 0;
  width: 74px;
  height: 74px;
  border: 1.5px solid #e4e7ed;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  background: #fff;
}
.date-cell:hover:not(.disabled) {
  border-color: #79b8ff;
}
.date-cell .dc-week {
  font-size: 12px;
  color: #909399;
}
.date-cell .dc-day {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.25;
}
.date-cell .dc-dot {
  position: absolute;
  bottom: 6px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #67c23a;
}
.date-cell .dc-dot.full {
  background: #c0c4cc;
}
.date-cell.active {
  background: linear-gradient(160deg, #409eff, #2f6bff);
  border-color: #2f6bff;
  box-shadow: 0 5px 14px rgba(64, 158, 255, 0.38);
  transform: translateY(-2px);
}
.date-cell.active .dc-week,
.date-cell.active .dc-day {
  color: #fff;
}
.date-cell.active .dc-dot {
  background: #fff;
}
.dc-tag {
  position: absolute;
  top: -9px;
  left: 50%;
  transform: translateX(-50%);
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 8px;
  white-space: nowrap;
}
.date-cell.active .dc-tag {
  background: #fff;
  color: #f56c6c;
}
.date-cell.disabled {
  cursor: not-allowed;
  background: #fafbfc;
}
.date-cell.disabled .dc-week,
.date-cell.disabled .dc-day {
  color: #c0c4cc;
}
.today-btn {
  flex-shrink: 0;
  height: 34px;
  padding: 0 14px;
  border: 1px solid #409eff;
  background: #fff;
  color: #409eff;
  border-radius: 17px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.today-btn:hover {
  background: #ecf5ff;
}

/* ===== 当日标题 ===== */
.day-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 20px 0 16px;
}
.d-title {
  font-size: 20px;
  font-weight: 700;
}
.d-week {
  font-size: 13px;
  color: #fff;
  background: #409eff;
  padding: 2px 10px;
  border-radius: 10px;
}
.d-stat {
  font-size: 13px;
  color: #909399;
}

/* ===== 时段分组 ===== */
.slot-group {
  margin-bottom: 18px;
}
.group-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}
.g-line {
  flex: 1;
  height: 1px;
  background: #eef0f4;
}
.g-icon {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #ecf5ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

/* ===== 场次卡片 ===== */
.slot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.slot-card {
  border: 1.5px solid #ebeef5;
  border-radius: 12px;
  padding: 18px 18px 16px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  background: #fff;
}
.slot-card:hover:not(.full) {
  border-color: #79b8ff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.14);
  transform: translateY(-3px);
}
.slot-time {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #303133;
}
.slot-duration {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.slot-progress {
  margin: 14px 0 8px;
  height: 8px;
  border-radius: 4px;
  background: #f0f2f5;
  overflow: hidden;
}
.slot-progress .bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}
.slot-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;
  margin-bottom: 14px;
  gap: 8px;
}
.slot-status {
  font-weight: 700;
  white-space: nowrap;
}
.status-ok {
  color: #67c23a;
}
.status-tight {
  color: #e6a23c;
}
.status-full {
  color: #909399;
}
.slot-btn {
  width: 100%;
  height: 38px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.slot-btn.ok {
  background: #409eff;
  color: #fff;
}
.slot-btn.ok:hover {
  background: #2f6bff;
}
.slot-btn.tight {
  background: #fff;
  color: #e6a23c;
  border: 1.5px solid #e6a23c;
}
.slot-btn.tight:hover {
  background: #fdf6ec;
}
.slot-card.full {
  background: #fafbfc;
  cursor: not-allowed;
}
.slot-card.full .slot-time {
  color: #b6bbc3;
}
.slot-btn.full {
  background: #eef0f3;
  color: #a8abb2;
  cursor: not-allowed;
}
.full-ribbon {
  position: absolute;
  top: 12px;
  right: -30px;
  background: #c0c4cc;
  color: #fff;
  font-size: 11px;
  padding: 2px 34px;
  transform: rotate(45deg);
}

/* ===== 当天无场次空状态 ===== */
.empty-day {
  text-align: center;
  padding: 56px 0;
  color: #909399;
}
.e-icon {
  font-size: 46px;
  margin-bottom: 12px;
}

/* ===== 图例 ===== */
.legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 26px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
  font-size: 12px;
  color: #909399;
}
.legend i {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: 0;
}
.legend .dot-empty {
  background: #fff;
  border: 1px solid #dcdfe6;
}
</style>
