<template>
  <div class="forbidden-container">
    <el-result icon="warning" title="403" sub-title="抱歉，您没有权限访问该页面">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button v-if="!userStore.isLoggedIn" @click="goLogin">去登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
/**
 * 403 无权限页（对标 C2：访客访问管理员路由时跳到这里）
 */
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

function goHome() {
  router.push(userStore.isAdmin ? '/admin' : '/visitor/home')
}
function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.forbidden-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}
</style>
