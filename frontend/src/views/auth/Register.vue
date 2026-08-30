<template>
  <div class="register-container">
    <el-card class="register-card">
      <div class="register-title">访客注册</div>
      <div class="register-subtitle">注册后即可预约参观并使用 AI 咨询</div>

      <el-form ref="formRef" :model="form" :rules="formRules" label-width="0" size="large" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（4~20位字母、数字或下划线）" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（6~20位，须含字母和数字）" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名（2~10字）" :prefix-icon="Postcard" clearable />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Iphone" clearable />
        </el-form-item>
        <el-button type="primary" size="large" class="register-btn" :loading="loading" native-type="submit">
          {{ loading ? '注册中...' : '注 册' }}
        </el-button>
        <div class="register-links">
          <span>已有账号？</span>
          <router-link to="/login" class="link">去登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 注册页（对标 F11：访客自助注册）
 * 校验规则与后端 RegisterDTO 完全一致（utils/validate.js）；
 * 确认密码是纯前端校验（后端没有这个字段），用自定义 validator 比较。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard, Iphone } from '@element-plus/icons-vue'
import { register } from '@/api/auth'
import { rules } from '@/utils/validate'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
})

// 基础规则 + 确认密码自定义校验
const formRules = {
  ...rules,
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 后端只要 username/password/realName/phone 四个字段，确认密码不外传
    await register({
      username: form.username,
      password: form.password,
      realName: form.realName,
      phone: form.phone,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    /* 拦截器已提示（用户名已存在 40011 等） */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1d3f72 0%, #409eff 60%, #6db3f2 100%);
}
.register-card {
  width: 420px;
  border-radius: 8px;
  padding: 8px 12px 4px;
}
.register-title {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  color: #303133;
}
.register-subtitle {
  font-size: 13px;
  text-align: center;
  color: #909399;
  margin: 8px 0 20px;
}
.register-btn {
  width: 100%;
  margin-top: 4px;
}
.register-links {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: #909399;
  padding-bottom: 6px;
}
.link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
</style>
