<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-title">校园参观预约与智能咨询系统</div>
      <div class="login-subtitle">哈尔滨剑桥学院 · 智能科学与工程学院</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
        <div class="login-links">
          <span>还没有账号？</span>
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
        <div class="login-tip">访客与管理员使用同一登录入口，登录后按角色自动跳转</div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 登录页（对标 F10：访客+管理员共用入口，按角色跳转）
 *
 * 跳转优先级：
 *   ① URL 带 redirect 参数（被守卫踢过来时记录的原目标）→ 回原页面
 *   ② 无 redirect → 按角色分流：admin → /admin，visitor → /visitor/home
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { rules } from '@/utils/validate'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  // 表单整体校验（validate 回调风格：不通过直接 return）
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const vo = await login(form) // 返回 LoginVO：token/role/userId/realName/isSuper
    userStore.setLogin(vo)
    ElMessage.success(`欢迎您，${vo.realName || vo.username}`)
    // ① 优先跳回被踢之前的页面 ② 按角色分流
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else {
      router.push(vo.role === 'admin' ? '/admin' : '/visitor/home')
    }
  } catch {
    /* 拦截器已提示具体错误（用户名或密码错误 40010 / 冻结 40012） */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  /* 背景渐变（不用外链图，纯 CSS 稳定） */
  background: linear-gradient(135deg, #1d3f72 0%, #409eff 60%, #6db3f2 100%);
}
.login-card {
  width: 400px;
  border-radius: 8px;
  padding: 8px 12px 4px;
}
.login-title {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  color: #303133;
}
.login-subtitle {
  font-size: 13px;
  text-align: center;
  color: #909399;
  margin: 8px 0 24px;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
}
.login-links {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: #909399;
}
.link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
.login-tip {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
}
</style>
