<template>
  <!-- ============ AI 悬浮咨询窗（对标 frontend-prototype.md F12） ============ -->

  <!-- 悬浮按钮：右下角固定，全局每页显示 -->
  <div class="ai-float-btn" @click="openChat">
    <el-icon :size="26"><ChatDotRound /></el-icon>
    <span class="btn-text">AI 咨询</span>
  </div>

  <!-- 抽屉式聊天窗 -->
  <el-drawer v-model="visible" direction="rtl" size="430px" :with-header="false" append-to-body>
    <div class="chat-panel">
      <!-- 头部：标题 + 工具按钮 -->
      <div class="chat-header">
        <div class="chat-title">
          <el-icon><Service /></el-icon>
          <span>AI 智能咨询</span>
        </div>
        <div class="chat-tools">
          <el-tooltip content="新会话" placement="bottom">
            <el-button circle size="small" @click="handleNewSession">
              <el-icon><Plus /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="清空当前会话" placement="bottom">
            <el-button circle size="small" type="danger" plain @click="handleClear">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <!-- 消息区：滚动容器，发送/接收后自动滚到底部 -->
      <div ref="listRef" class="chat-body">
        <!-- 欢迎语（无消息时） -->
        <div v-if="messages.length === 0 && !asking" class="chat-welcome">
          <el-icon :size="40" color="#409EFF"><Service /></el-icon>
          <p class="welcome-title">您好，我是校园 AI 助手</p>
          <p class="welcome-desc">关于参观时间、预约方式、入校规定等问题都可以问我～</p>
          <div class="quick-asks">
            <el-tag
              v-for="q in quickAsks"
              :key="q"
              class="quick-ask"
              effect="plain"
              @click="send(q)"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>

        <!-- 消息气泡列表 -->
        <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
          <div class="bubble" :class="msg.role">
            <span class="bubble-text">{{ msg.content }}</span>
            <!-- AI 命中知识库时展示引用来源，增强可信度 -->
            <div v-if="msg.role === 'assistant' && msg.referDocName" class="refer">
              <el-icon><Document /></el-icon>
              来源：{{ msg.referDocName }}
            </div>
            <div v-else-if="msg.role === 'assistant' && !msg.referDocName && msg.referTip" class="refer miss">
              未命中知识库，建议咨询人工老师
            </div>
          </div>
        </div>

        <!-- AI 思考中动画 -->
        <div v-if="asking" class="msg-row assistant">
          <div class="bubble assistant">
            <AiTyping />
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-footer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入问题，Enter 发送 / Shift+Enter 换行"
          @keydown.enter.exact.prevent="onEnter"
        />
        <el-button type="primary" :loading="asking" class="send-btn" @click="onEnter">
          发送
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
/**
 * AI 悬浮咨询窗（核心组件，挂在访客 Layout 上全局可见）
 *
 * 交互流程：
 *   ① 点悬浮按钮 → 未登录则引导登录；已登录则打开抽屉并加载最近会话
 *   ② 输入问题回车 → 先渲染用户气泡 → 请求 /chat/ask → 打字机效果渲染 AI 回答
 *   ③ 回答带引用文档名时展示"来源"，未命中时提示转人工（对应后端 0.5 防幻觉闸门）
 *   ④ "新会话"=调后端建空会话并清屏；"清空"=删会话消息后清屏
 */
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ask, pageMySessions, pageMessages, newSession, clearSession } from '@/api/chat'
import { useUserStore } from '@/store/user'
import AiTyping from '@/components/AiTyping.vue'

const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const messages = ref([]) // [{ role, content, referDocName?, referTip? }]
const input = ref('')
const asking = ref(false) // true=AI 正在思考（禁用发送）
const currentSessionId = ref(null) // 当前聊的会话 ID
const listRef = ref(null)

/** 常见问题快捷标签（点击直接发送） */
const quickAsks = ['学校参观时间是几点到几点？', '进校园需要预约吗？', '参观需要带什么证件？']

/** 消息列表自动滚动到底部（nextTick 等 DOM 渲染完再滚） */
async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

/** 打开聊天窗：未登录引导登录；已登录加载最近一次会话 */
async function openChat() {
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm('AI 咨询需要先登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '先逛逛',
      type: 'info',
    })
      .then(() => router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } }))
      .catch(() => {})
    return
  }
  visible.value = true
  await loadLatestSession()
}

/** 加载最近一次会话的消息（会话列表按最后消息时间倒序，取第 1 条） */
async function loadLatestSession() {
  try {
    const page = await pageMySessions({ current: 1, size: 1 })
    if (page.records?.length) {
      currentSessionId.value = page.records[0].id
      await loadMessages()
    } else {
      currentSessionId.value = null
      messages.value = []
    }
  } catch {
    /* 接口失败静默（拦截器已弹提示） */
  }
}

/** 拉取当前会话的消息记录（最近 50 条，够用且省流量） */
async function loadMessages() {
  if (!currentSessionId.value) return
  try {
    const page = await pageMessages(currentSessionId.value, { current: 1, size: 50 })
    // 后端按 id 升序返回（先问后答），直接映射成气泡
    messages.value = (page.records || []).map((m) => ({
      role: m.role,
      content: m.content,
      referDocName: m.referDocName,
      // 未命中的固定话术回答没有引用文档 → 标记给"建议人工"提示用
      referTip: m.role === 'assistant' && !m.referDocName,
    }))
    scrollToBottom()
  } catch {
    /* 静默 */
  }
}

/** Enter 发送（输入非空且不在思考中） */
function onEnter() {
  const text = input.value.trim()
  if (!text || asking.value) return
  send(text)
}

/** 发送问题：渲染用户气泡 → 调 AI → 打字机渲染回答 */
async function send(text) {
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  asking.value = true
  scrollToBottom()
  try {
    const vo = await ask({ question: text })
    asking.value = false
    // 后端可能自动新建了会话（首问），同步会话 ID
    if (vo.sessionId) currentSessionId.value = vo.sessionId
    // 打字机效果：把回答逐字追加到气泡，模拟"正在打字"
    messages.value.push({ role: 'assistant', content: '', referDocName: vo.referDocName, referTip: !vo.referDocName })
    // ⚠️ 必须从数组里取"响应式代理"再逐字改：
    // 若持有 push 前的原始对象引用，改 content 不会触发 Vue 重新渲染（气泡会一直空白）
    const reactiveBubble = messages.value[messages.value.length - 1]
    await typeWriter(reactiveBubble, vo.answer || '(空回答)')
  } catch {
    asking.value = false
    messages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂时不可用，请稍后重试。' })
  }
  scrollToBottom()
}

/**
 * 打字机效果：每 18ms 追加 2 个字符（3 秒内出完 300 字，速度接近真人打字观感）
 * 直接改 bubble.content（ref 数组内对象是响应式的）
 */
function typeWriter(bubble, fullText) {
  return new Promise((resolve) => {
    let i = 0
    const timer = setInterval(() => {
      i += 2
      bubble.content = fullText.slice(0, i)
      scrollToBottom()
      if (i >= fullText.length) {
        clearInterval(timer)
        resolve()
      }
    }, 18)
  })
}

/** 新会话：调后端建空会话 → 清屏 → 会话 ID 切到新会话 */
async function handleNewSession() {
  try {
    const id = await newSession()
    currentSessionId.value = id
    messages.value = []
    ElMessage.success('已开启新会话')
  } catch {
    /* 静默 */
  }
}

/** 清空当前会话：二次确认 → 删会话 → 清屏（当前会话 ID 置空，下次提问自动新建） */
async function handleClear() {
  if (!currentSessionId.value) {
    ElMessage.info('当前没有会话')
    return
  }
  try {
    await ElMessageBox.confirm('确定清空当前会话的全部记录吗？删除后不可恢复。', '警告', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户点了取消
  }
  try {
    await clearSession(currentSessionId.value)
    currentSessionId.value = null
    messages.value = []
    ElMessage.success('会话已清空')
  } catch {
    /* 静默 */
  }
}
</script>

<style scoped>
/* ===== 悬浮按钮 ===== */
.ai-float-btn {
  position: fixed;
  right: 28px;
  bottom: 34px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.5);
  transition: transform 0.2s;
}
.ai-float-btn:hover {
  transform: scale(1.08);
}
.btn-text {
  font-size: 11px;
  line-height: 1;
}

/* ===== 抽屉内聊天面板 ===== */
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #dcdfe6;
}
.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.chat-tools {
  display: flex;
  gap: 8px;
}

/* 消息区 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 4px;
  background: #f5f7fa;
  border-radius: 6px;
  margin: 12px 0;
}
.chat-welcome {
  text-align: center;
  padding-top: 40px;
  color: #606266;
}
.welcome-title {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 6px;
}
.welcome-desc {
  font-size: 13px;
  color: #909399;
}
.quick-asks {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
  padding: 0 24px;
}
.quick-ask {
  cursor: pointer;
  justify-content: center;
  height: 32px;
}
.quick-ask:hover {
  color: #409eff;
  border-color: #409eff;
}

/* 气泡 */
.msg-row {
  display: flex;
  margin-bottom: 14px;
  padding: 0 8px;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-row.assistant {
  justify-content: flex-start;
}
.bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap; /* 保留后端回答里的换行 */
  word-break: break-word;
}
.bubble.user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}
.bubble.assistant {
  background: #fff;
  color: #303133;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 2px;
}
.refer {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #909399;
}
.refer.miss {
  color: #e6a23c;
}

/* 输入区 */
.chat-footer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.send-btn {
  height: 54px;
}
</style>
