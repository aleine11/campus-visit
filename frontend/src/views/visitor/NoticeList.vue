<template>
  <div class="notice-list-page">
    <!-- ===== 横幅标题区（负边距撑满外层内容容器） ===== -->
    <div class="notice-banner">
      <div class="banner-crumb">首页 / 公告</div>
      <h1 class="banner-title">校园公告</h1>
      <p class="banner-sub">NOTICE BOARD · 及时了解校园参观最新安排与通知</p>
    </div>

    <!-- ===== 公告列表卡片（向上压住横幅，形成悬浮效果） ===== -->
    <div class="notice-card" v-loading="loading">
      <div class="card-toolbar">
        <div class="toolbar-title">全部公告</div>
        <div class="toolbar-count">共 <b>{{ total }}</b> 条</div>
      </div>

      <!-- 公告条目 -->
      <div
        v-for="n in list"
        :key="n.id"
        class="notice-item"
        @click="router.push(`/visitor/notice/${n.id}`)"
      >
        <!-- 左侧日历日期块 -->
        <div class="date-box">
          <div class="date-day">{{ dayOf(n.publishTime) }}</div>
          <div class="date-my">{{ yearMonthOf(n.publishTime) }}</div>
        </div>

        <!-- 中间标题 + 摘要 -->
        <div class="notice-body">
          <div class="notice-title">
            <span v-if="isNew(n.publishTime)" class="tag-new">NEW</span>{{ n.title }}
          </div>
          <div class="notice-summary">{{ n.summary }}</div>
        </div>

        <!-- 右侧完整时间 + 箭头 -->
        <div class="notice-side">
          <span class="side-time">{{ formatDateTime(n.publishTime) }}</span>
          <span class="side-arrow">›</span>
        </div>
      </div>

      <!-- 空状态 -->
      <EmptyState v-if="!loading && list.length === 0" description="暂无公告" />
    </div>

    <!-- 分页条（全站通用组件） -->
    <PaginationBar
      :current="query.current"
      :size="query.size"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script setup>
/**
 * 公告列表页（对标 F2：分页浏览已发布公告）
 * 布局：蓝色横幅 + 白色悬浮卡片 + 日历日期块列表
 * 点击条目 → 详情页 F3
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
    // 后端返回 Page 分页对象：records 当前页数据，total 总条数
    const page = await pagePublished(query)
    list.value = page.records || []
    total.value = Number(page.total) || 0
  } catch {
    /* 请求拦截器已统一弹错误提示，这里静默 */
  } finally {
    loading.value = false
  }
}

function onPageChange({ current, size }) {
  query.current = current
  query.size = size
  load()
}

/* ============ 日期块辅助方法 ============ */

/** "2026-09-11T23:26:45" → "11"（几号） */
function dayOf(v) {
  return v ? String(v).slice(8, 10) : '--'
}

/** "2026-09-11T23:26:45" → "2026年9月" */
function yearMonthOf(v) {
  if (!v) return ''
  const year = String(v).slice(0, 4)
  const month = Number(String(v).slice(5, 7)) // 去掉前导 0：09 → 9
  return `${year}年${month}月`
}

/** 发布 7 天内显示 NEW 角标 */
function isNew(v) {
  if (!v) return false
  const t = new Date(String(v).replace(' ', 'T')).getTime()
  return Number.isFinite(t) && Date.now() - t <= 7 * 24 * 60 * 60 * 1000
}

onMounted(load)
</script>

<style scoped>
/* 负边距撑满外层 .content 容器（容器 padding 为 24px 16px） */
.notice-list-page {
  margin: -24px -16px 0;
}

/* ===== 横幅 ===== */
.notice-banner {
  height: 210px;
  background: linear-gradient(120deg, #409eff 0%, #2f6bff 55%, #1d4fd6 100%);
  color: #fff;
  padding: 0 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
/* 装饰圆 */
.notice-banner::after {
  content: '';
  position: absolute;
  right: -80px;
  top: -100px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.notice-banner::before {
  content: '';
  position: absolute;
  right: 160px;
  bottom: -140px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}
.banner-crumb {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 12px;
}
.banner-title {
  font-size: 34px;
  letter-spacing: 4px;
  font-weight: 700;
}
.banner-sub {
  margin-top: 12px;
  font-size: 14px;
  opacity: 0.85;
  letter-spacing: 1px;
}

/* ===== 白色悬浮卡片 ===== */
.notice-card {
  margin: -36px 32px 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 24px rgba(31, 67, 138, 0.1);
  padding: 24px 32px 28px;
  position: relative;
  z-index: 2;
  min-height: 300px;
}
.card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.toolbar-title {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}
.toolbar-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #409eff;
  border-radius: 2px;
  margin-right: 8px;
  vertical-align: -2px;
}
.toolbar-count {
  font-size: 13px;
  color: #909399;
}
.toolbar-count b {
  color: #409eff;
  font-weight: 600;
}

/* ===== 公告条目 ===== */
.notice-item {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 20px 12px;
  border-bottom: 1px solid #f2f4f8;
  cursor: pointer;
  transition: background 0.2s, padding 0.2s;
}
.notice-item:hover {
  background: #f7faff;
  padding-left: 18px;
}
.notice-item:last-child {
  border-bottom: none;
}

/* 左侧日历日期块 */
.date-box {
  width: 66px;
  height: 66px;
  flex-shrink: 0;
  border-radius: 10px;
  background: linear-gradient(160deg, #409eff, #2f6bff);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
}
.date-day {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
}
.date-my {
  font-size: 12px;
  opacity: 0.92;
  margin-top: 3px;
}

/* 中间标题 + 摘要 */
.notice-body {
  flex: 1;
  min-width: 0;
}
.notice-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s;
}
.notice-item:hover .notice-title {
  color: #409eff;
}
.tag-new {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: #f56c6c;
  border-radius: 4px;
  padding: 1px 6px;
  margin-right: 8px;
  vertical-align: 2px;
}
.notice-summary {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 右侧时间 + 箭头 */
.notice-side {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #c0c4cc;
  font-size: 13px;
}
.side-arrow {
  font-size: 18px;
  transition: transform 0.2s, color 0.2s;
}
.notice-item:hover .side-arrow {
  transform: translateX(4px);
  color: #409eff;
}
</style>
