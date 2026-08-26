<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-title">校园参观预约与智能咨询系统</div>
      <div class="login-subtitle">访客登录</div>

      <el-form :model="form" label-width="80px" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin">登录</el-button>
          <el-button @click="goRegister">注册</el-button>
          <el-button link @click="goAdminLogin">管理员登录</el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">提示：Stage0 仅搭骨架，登录功能将在 Stage2 用户认证模块实现</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

const handleLogin = () => {
  // Stage2 实现
  router.push('/visitor/home')
}

const goRegister = () => {
  // Stage2 实现
}

const goAdminLogin = () => {
  // Stage2 实现
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 20px;
}
.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}
.login-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}
</style>
