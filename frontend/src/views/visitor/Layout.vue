<template>
  <div class="visitor-layout">
    <!-- ===== 顶部导航 ===== -->
    <header class="header">
      <div class="header-inner">
        <router-link to="/visitor/home" class="logo">
          <el-icon :size="22"><School /></el-icon>
          <span>哈尔滨剑桥学院 · 校园参观预约</span>
        </router-link>

        <nav class="nav">
          <router-link to="/visitor/home" :class="{ active: isActive('/visitor/home') }">首页</router-link>
          <router-link to="/visitor/notice" :class="{ active: isActive('/visitor/notice') }">公告</router-link>
          <router-link to="/visitor/session" :class="{ active: isActive('/visitor/session') }">预约参观</router-link>
          <router-link
            v-if="userStore.isVisitor"
            to="/visitor/reservation/list"
            :class="{ active: isActive('/visitor/reservation') }"
          >
            我的预约
          </router-link>
          <router-link
            v-if="userStore.isVisitor"
            to="/visitor/chat/history"
            :class="{ active: isActive('/visitor/chat') }"
          >
            AI 历史
          </router-link>
        </nav>

        <!-- 登录区：未登录显示登录/注册；访客显示姓名+退出；管理员显示进后台 -->
        <div class="auth-area">
          <template v-if="!userStore.isLoggedIn">
            <el-button type="primary" size="small" @click="goLogin">登录</el-button>
            <el-button size="small" @click="goRegister">注册</el-button>
          </template>
          <template v-else-if="userStore.isVisitor">
            <el-dropdown @command="handleCommand">
              <span class="user-chip">
                <el-icon><UserFilled /></el-icon>
                {{ userStore.realName }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <!-- 管理员浏览前台：提供回后台入口 -->
            <el-button size="small" type="warning" plain @click="router.push('/admin')">返回后台</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- ===== 内容区：宽 1200 居中（对标 5.3 布局规范） ===== -->
    <main class="content">
      <router-view />
    </main>

    <!-- ===== 页脚 ===== -->
    <footer class="footer">
      <p>© 2026 哈尔滨剑桥学院 智能科学与工程学院 · 基于 SpringBoot + RAG 的校园参观预约与智能咨询系统</p>
    </footer>

    <!-- ===== AI 悬浮咨询窗（全局组件，F12） ===== -->
    <AiFloatChat />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import AiFloatChat from '@/components/AiFloatChat.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 导航高亮：startsWith 匹配让"我的预约"和"预约详情"同组高亮 */
function isActive(prefix) {
  return route.path.startsWith(prefix)
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}
function goRegister() {
  router.push('/register')
}

/** 下拉菜单：个人中心 / 退出登录 */
function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/visitor/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/visitor/home')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.visitor-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* ===== 顶部导航 ===== */
.header {
  position: sticky;
  top: 0;
  z-index: 1500;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.header-inner {
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 16px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: #409eff;
  text-decoration: none;
  white-space: nowrap;
}
.nav {
  display: flex;
  gap: 4px;
  flex: 1;
}
.nav a {
  padding: 6px 14px;
  border-radius: 4px;
  color: #606266;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}
.nav a:hover {
  color: #409eff;
  background: #ecf5ff;
}
.nav a.active {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 600;
}
.auth-area {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
}
.user-chip:hover {
  background: #f5f7fa;
}

/* ===== 内容区 ===== */
.content {
  flex: 1;
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  padding: 24px 16px;
  box-sizing: border-box;
}

/* ===== 页脚 ===== */
.footer {
  background: #303133;
  color: #909399;
  text-align: center;
  padding: 18px 0;
  font-size: 13px;
}
</style>
