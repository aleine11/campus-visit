<template>
  <div class="admin-layout">
    <!-- 左侧菜单（对标 frontend-prototype.md 5.3：左侧菜单 220px + 右侧内容自适应） -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">校园参观后台</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/admin">
          <el-icon><HomeFilled /></el-icon>
          <span>后台首页</span>
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
        <el-menu-item index="/admin/chatlog">
          <el-icon><ChatLineRound /></el-icon>
          <span>问答日志统计</span>
        </el-menu-item>
        <!-- 账号管理仅超管可见（前端隐藏 + 后端 40301 + 路由守卫三重保险） -->
        <el-menu-item v-if="userStore.isSuper" index="/admin/admin">
          <el-icon><UserFilled /></el-icon>
          <span>管理员账号</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ pageTitle }}</span>
        <div class="header-right">
          <span class="admin-name">{{ userStore.realName || '管理员' }}</span>
          <el-button link type="primary" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
/**
 * 管理员后台整体布局（对标 frontend-prototype.md A 系列）
 *
 * 与访客 Layout 的区别：
 *   - 访客版：顶部导航 + 商务风，管理员版：左侧菜单 + 控制台风
 *   - 菜单用 el-menu 的 router 模式：index 就是跳转路径，点击即路由跳转
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled,
  Calendar,
  Document,
  User,
  Bell,
  Collection,
  ChatLineRound,
  UserFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pageTitle = computed(() => route.meta.title || '管理后台')

/** 退出登录：清 Pinia + localStorage（token/用户信息），回登录页 */
function logout() {
  userStore.logout()
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
  display: flex;
  flex-direction: column;
}
.sidebar .logo {
  padding: 20px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background: #2b2f3a;
  flex-shrink: 0;
}
.menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.admin-name {
  font-size: 14px;
  color: #606266;
}
.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
