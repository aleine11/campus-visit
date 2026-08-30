<template>
  <div class="profile-page">
    <h2 class="page-title">个人中心</h2>

    <el-row :gutter="20">
      <!-- ===== 个人信息（访客可改：realName/phone；用户名锁定） ===== -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>个人信息</template>
          <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="80px">
            <el-form-item label="用户名">
              <!-- 用户名是登录身份锚点，后端不给改 → 禁用输入框 -->
              <el-input v-model="infoForm.username" disabled>
                <template #append>不可修改</template>
              </el-input>
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="infoForm.realName" maxlength="10" clearable />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" maxlength="11" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingInfo" @click="handleSaveInfo">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- ===== 修改密码 ===== -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>修改密码</template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6~20 位，须含字母和数字" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 个人中心（对标 F9：左信息修改 + 右密码修改）
 * 修改密码成功后强制重新登录（为安全起见踢回登录页）
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { rules } from '@/utils/validate'

const router = useRouter()
const userStore = useUserStore()

const infoFormRef = ref(null)
const savingInfo = ref(false)
const infoForm = reactive({ username: '', realName: '', phone: '' })
const infoRules = { realName: rules.realName, phone: rules.phone }

const pwdFormRef = ref(null)
const savingPwd = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 新密码走后端同款规则 + 确认密码自定义校验
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: rules.password,
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  try {
    const profile = await getProfile()
    infoForm.username = profile.username
    infoForm.realName = profile.realName
    infoForm.phone = profile.phone
  } catch {
    /* 静默 */
  }
})

/** 保存个人信息：只传 realName/phone（后端 VisitorProfileDTO 只收这两个） */
async function handleSaveInfo() {
  const valid = await infoFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingInfo.value = true
  try {
    await updateProfile({ realName: infoForm.realName, phone: infoForm.phone })
    userStore.updateRealName(infoForm.realName) // 同步导航栏姓名
    ElMessage.success('个人信息已更新')
  } catch {
    /* 静默 */
  } finally {
    savingInfo.value = false
  }
}

/** 修改密码：成功后确认弹窗 → 登出 → 回登录页 */
async function handleChangePwd() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    savingPwd.value = false
    await ElMessageBox.alert('密码修改成功，请使用新密码重新登录', '提示', {
      confirmButtonText: '去登录',
      type: 'success',
    })
    userStore.logout()
    router.push('/login')
  } catch {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.page-title {
  margin-bottom: 18px;
  color: #303133;
}
</style>
