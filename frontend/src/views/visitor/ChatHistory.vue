<template>
  <div class="chat-history-page">
    <div class="page-head">
      <h2 class="page-title">AI 历史会话</h2>
      <el-button type="primary" :icon="Plus" @click="handleNewSession">新会话</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 左：会话列表 -->
      <el-col :span="9">
        <el-card shadow="never" class="session-panel">
          <template #header>会话列表</template>
          <div v-loading="sessionsLoading">
            <div
              v-for="s in sessions"
              :key="s.id"
              class="session-item"
              :class="{ active: s.id === activeSessionId }"
              @click="selectSession(s.id)"
            >
              <div class="session-title">
                <el-icon><ChatLineRound /></el-icon>
                {{ s.title || '新会话' }}
              </div>
              <div class="session-time">
                最后提问：{{ s.lastMessageTime ? formatDateTime(s.lastMessageTime) : '无提问' }}
              </div>
              <div class="session-ops">
                <el-button
                  text
                  type="danger"
                  size="small"
                  @click.stop="handleClear(s)"
                >
                  清空
                </el-button>
              </div>
            </div>
            <EmptyState v-if="!sessionsLoading && sessions.length === 0" description="暂无会话，点右上角开始提问" />
          </div>
          <PaginationBar
            :current="sessionQuery.current"
            :size="sessionQuery.size"
            :total="sessionTotal"
            @change="onSessionPageChange"
          />
        </el-card>
      </el-col>

      <!-- 右：选中会话的消息记录 -->
      <el-col :span="15">
        <el-card shadow="never" class="msg-panel">
          <template #header>
            {{ activeTitle ? `会话：${activeTitle}` : '消息记录' }}
          </template>
          <div ref="msgListRef" class="msg-list" v-loading="msgsLoading">
            <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
              <div class="bubble" :class="m.role">
                <span>{{ m.content }}</span>
                <div v-if="m.role === 'assistant' && m.referDocName" class="refer">
                  <el-icon><Document /></el-icon> 来源：{{ m.referDocName }}
                </div>
              </div>
            </div>
            <EmptyState
              v-if="!msgsLoading && messages.length === 0"
              :description="activeSessionId ? '该会话暂无消息' : '请选择左侧会话查看消息'"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * AI 历史会话页（对标 F8：左会话列表 + 右消息记录 + 新会话 + 清空）
 * 与悬浮窗（AiFloatChat）数据同源，都是 /chat 模块的 5 个接口
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChatLineRound, Document } from '@element-plus/icons-vue'
import { pageMySessions, pageMessages, newSession, clearSession } from '@/api/chat'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const sessions = ref([])
const sessionTotal = ref(0)
const sessionQuery = reactive({ current: 1, size: 10 })
const sessionsLoading = ref(false)

const activeSessionId = ref(null)
const activeTitle = ref('')
const messages = ref([])
const msgsLoading = ref(false)
const msgListRef = ref(null)

/** 拉会话分页 */
async function loadSessions() {
  sessionsLoading.value = true
  try {
    const page = await pageMySessions(sessionQuery)
    sessions.value = page.records || []
    sessionTotal.value = Number(page.total) || 0
    // 当前选中的会话如果不在列表里（被删/翻页），自动选第 1 条
    if (!sessions.value.some((s) => s.id === activeSessionId.value) && sessions.value.length) {
      selectSession(sessions.value[0].id, sessions.value[0].title)
    }
  } catch {
    /* 静默 */
  } finally {
    sessionsLoading.value = false
  }
}

/** 选中某会话 → 加载其消息（最近 100 条） */
async function selectSession(id, title) {
  activeSessionId.value = id
  activeTitle.value = title || ''
  msgsLoading.value = true
  try {
    const page = await pageMessages(id, { current: 1, size: 100 })
    messages.value = page.records || []
    scrollToBottom()
  } catch {
    /* 静默 */
  } finally {
    msgsLoading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  })
}

function onSessionPageChange({ current, size }) {
  sessionQuery.current = current
  sessionQuery.size = size
  loadSessions()
}

/** 新会话：后端建空会话 → 刷新列表并选中 */
async function handleNewSession() {
  try {
    const id = await newSession()
    await loadSessions()
    activeSessionId.value = id
    activeTitle.value = ''
    messages.value = []
    ElMessage.success('新会话已创建，可通过右下角 AI 悬浮窗提问')
  } catch {
    /* 静默 */
  }
}

/** 清空会话：二次确认 → 删除 → 刷新 */
async function handleClear(s) {
  try {
    await ElMessageBox.confirm(`确定清空会话「${s.title || '新会话'}」吗？删除后不可恢复。`, '警告', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await clearSession(s.id)
    ElMessage.success('会话已清空')
    loadSessions()
  } catch {
    /* 静默 */
  }
}

onMounted(loadSessions)
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.page-title {
  color: #303133;
}

/* 会话条目 */
.session-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  position: relative;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.session-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  /* 标题超长省略 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 56px;
}
.session-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
}
.session-ops {
  position: absolute;
  right: 10px;
  top: 10px;
}

/* 消息列表 */
.msg-list {
  height: 520px;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px 12px;
}
.msg-row {
  display: flex;
  margin-bottom: 14px;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-row.assistant {
  justify-content: flex-start;
}
.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble.user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}
.bubble.assistant {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 2px;
}
.refer {
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
