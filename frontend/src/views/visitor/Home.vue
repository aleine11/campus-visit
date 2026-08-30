<template>
  <div class="home-page">
    <!-- ===== Hero 横幅（校园实景图 AI 生成） ===== -->
    <section class="hero">
      <div class="hero-text">
        <h1>走进校园，从这里开始</h1>
        <p>在线预约 · 智能咨询 · 快速审核，为您提供便捷的校园参观服务</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/visitor/session')">立即预约参观</el-button>
          <el-button size="large" @click="router.push('/visitor/notice')">查看公告</el-button>
        </div>
      </div>
      <img class="hero-img" :src="heroImg" alt="校园风光" />
    </section>

    <!-- ===== 两大业务入口卡片 ===== -->
    <section class="entries">
      <div class="entry-card" @click="router.push('/visitor/session')">
        <el-icon :size="30" color="#409EFF"><Calendar /></el-icon>
        <div class="entry-info">
          <h3>预约参观</h3>
          <p>选择日期时段，提交参观申请</p>
        </div>
      </div>
      <div class="entry-card" @click="userStore.isLoggedIn ? router.push('/visitor/chat/history') : router.push('/login')">
        <el-icon :size="30" color="#67C23A"><ChatDotRound /></el-icon>
        <div class="entry-info">
          <h3>AI 智能咨询</h3>
          <p>参观时间、入校规定，AI 一键解答</p>
        </div>
      </div>
    </section>

    <!-- ===== 最新公告 ===== -->
    <section class="section">
      <div class="section-header">
        <h2>最新公告</h2>
        <router-link to="/visitor/notice" class="more-link">更多 &gt;</router-link>
      </div>
      <el-row v-if="notices.length" :gutter="20">
        <el-col v-for="n in notices" :key="n.id" :span="8">
          <el-card class="notice-card" shadow="hover" @click="router.push(`/visitor/notice/${n.id}`)">
            <h3 class="notice-title">{{ n.title }}</h3>
            <p class="notice-summary">{{ n.summary }}</p>
            <div class="notice-time">
              <el-icon><Clock /></el-icon>
              {{ formatDateTime(n.publishTime) }}
            </div>
          </el-card>
        </el-col>
      </el-row>
      <EmptyState v-else-if="loaded" description="暂无公告" />
    </section>

    <!-- ===== 近期可预约场次 ===== -->
    <section class="section">
      <div class="section-header">
        <h2>近期可预约场次</h2>
        <router-link to="/visitor/session" class="more-link">更多 &gt;</router-link>
      </div>
      <el-row v-if="sessions.length" :gutter="20">
        <el-col v-for="s in sessions" :key="s.id" :span="8">
          <el-card class="session-card" shadow="hover" @click="router.push(`/visitor/reservation/submit/${s.id}`)">
            <div class="session-date">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(s.visitDate) }}
            </div>
            <div class="session-slot">{{ s.timeSlot }}</div>
            <el-progress
              :percentage="Math.round((s.usedPeople / s.maxPeople) * 100)"
              :color="progressColor(s)"
              :stroke-width="10"
            />
            <div class="session-remaining">
              剩余 <b>{{ s.remaining }}</b> / {{ s.maxPeople }} 名额
            </div>
          </el-card>
        </el-col>
      </el-row>
      <EmptyState v-else-if="loaded" description="暂无可预约场次" />
    </section>

    <!-- ===== 校园风光（AI 生成示意图） ===== -->
    <section class="scenery">
      <div class="section-header"><h2>校园风光</h2></div>
      <el-row :gutter="20">
        <el-col v-for="(img, i) in sceneryImgs" :key="i" :span="12">
          <img class="scenery-img" :src="img" alt="校园风景" />
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
/**
 * 访客首页（对标 F1：Hero + 最新公告 3 条 + 近期场次 3 条 + 风光展示）
 * 全部真实数据：notice.latest(3) + session.latest(3)
 * 图片：AI 生图接口生成的校园实景示意（无本地图片资源依赖）
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { latest as latestNotices } from '@/api/notice'
import { latest as latestSessions } from '@/api/session'
import { useUserStore } from '@/store/user'
import { formatDateTime, formatDate } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()

const notices = ref([])
const sessions = ref([])
const loaded = ref(false)

// AI 生图：URL 编码后的 prompt + 指定尺寸
const heroImg =
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=modern%20Chinese%20university%20campus%20aerial%20view%2C%20teaching%20buildings%2C%20green%20trees%2C%20blue%20sky%2C%20sunny%20day%2C%20photorealistic%2C%20wide%20angle&image_size=landscape_16_9'
const sceneryImgs = [
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=modern%20university%20library%20exterior%2C%20students%20walking%2C%20golden%20sunlight%2C%20photorealistic&image_size=landscape_16_9',
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=campus%20lake%20with%20pavilion%2C%20willow%20trees%2C%20spring%20scenery%2C%20photorealistic&image_size=landscape_16_9',
]

/** 名额进度条颜色：>50% 绿 / 21~50% 橙 / ≤20% 红 */
function progressColor(s) {
  const pct = (s.usedPeople / s.maxPeople) * 100
  if (pct > 50) return '#67C23A'
  if (pct > 20) return '#E6A23C'
  return '#F56C6C'
}

onMounted(async () => {
  try {
    // 并行加载两组数据（互不依赖）
    const [noticeList, sessionList] = await Promise.all([latestNotices(3), latestSessions(3)])
    notices.value = noticeList || []
    sessions.value = sessionList || []
  } catch {
    /* 静默 */
  } finally {
    loaded.value = true
  }
})
</script>

<style scoped>
/* Hero 横幅 */
.hero {
  display: flex;
  align-items: center;
  gap: 24px;
  background: linear-gradient(135deg, #e8f3ff 0%, #f5f9ff 100%);
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 24px;
}
.hero-text {
  flex: 1;
}
.hero-text h1 {
  font-size: 32px;
  color: #1d3f72;
  margin-bottom: 12px;
}
.hero-text p {
  color: #606266;
  margin-bottom: 24px;
  font-size: 15px;
}
.hero-actions {
  display: flex;
  gap: 12px;
}
.hero-img {
  width: 460px;
  height: 240px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}

/* 入口卡片 */
.entries {
  display: flex;
  gap: 20px;
  margin-bottom: 32px;
}
.entry-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 10px;
  padding: 22px 24px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.entry-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.15);
}
.entry-info h3 {
  font-size: 17px;
  margin-bottom: 4px;
}
.entry-info p {
  font-size: 13px;
  color: #909399;
}

/* 分区通用 */
.section {
  margin-bottom: 32px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.section-header h2 {
  font-size: 20px;
  color: #303133;
}
.more-link {
  color: #409eff;
  text-decoration: none;
  font-size: 13px;
}

/* 公告卡片 */
.notice-card {
  cursor: pointer;
  height: 170px;
}
.notice-title {
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}
.notice-summary {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  height: 62px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
.notice-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #c0c4cc;
}

/* 场次卡片 */
.session-card {
  cursor: pointer;
}
.session-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.session-slot {
  font-size: 14px;
  color: #409eff;
  margin-bottom: 10px;
}
.session-remaining {
  font-size: 13px;
  color: #606266;
  margin-top: 8px;
}
.session-remaining b {
  color: #e6a23c;
  font-size: 16px;
}

/* 风光 */
.scenery-img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 10px;
  margin-bottom: 20px;
}
</style>
