<template>
  <div class="admin-layout">
    <!-- 后台左侧菜单 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">校园参观后台</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/admin">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/admin/session">
          <el-icon><Calendar /></el-icon>
          <span>参观场次管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/reservation">
          <el-icon><Document /></el-icon>
          <span>预约订单审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/visitor">
          <el-icon><User /></el-icon>
          <span>访客用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/notice">
          <el-icon><Bell /></el-icon>
          <span>校园公告管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><Collection /></el-icon>
          <span>RAG 知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/chat-log">
          <el-icon><ChatLineRound /></el-icon>
          <span>问答日志统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/admin">
          <el-icon><UserFilled /></el-icon>
          <span>管理员账号</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span>{{ pageTitle }}</span>
        <el-button link @click="logout">退出登录</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => route.meta.title || '管理后台')

const logout = () => {
  localStorage.removeItem('campus_token')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
}
.sidebar {
  background: #304156;
  color: #bfcbd9;
}
.sidebar .logo {
  padding: 20px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background: #2b2f3a;
}
.sidebar :deep(.el-menu) {
  border-right: none;
  background: #304156;
}
.sidebar :deep(.el-menu-item) {
  color: #bfcbd9;
}
.sidebar :deep(.el-menu-item.is-active) {
  background: #409eff;
  color: #fff;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  font-size: 18px;
  font-weight: 600;
}
.main {
  background: #f5f7fa;
}
</style>
