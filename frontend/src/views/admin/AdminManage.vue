<template>
  <div class="admin-account-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="账号/姓名搜索"
          clearable
          style="width: 200px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="openCreate">新增管理员</el-button>
      </div>

      <el-alert
        class="page-tip"
        type="warning"
        :closable="false"
        show-icon
        title="本页仅超级管理员可见（后端 superOnly 二级门禁）。新增的管理员不能创建管理员、不能访问本页。"
      />

      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="账号" width="160" />
          <el-table-column prop="realName" label="姓名" width="140" />
          <el-table-column label="角色" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isSuper ? 'danger' : 'primary'">{{ row.isSuper ? '超级管理员' : '普通管理员' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button text type="warning" size="small" @click="openReset(row)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else-if="!loading" description="暂无管理员账号" />
      </div>
      <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
    </el-card>

    <!-- ===== 新增管理员弹窗（username 4~20 唯一 / password 6~20 含字母数字 / realName 2~10 字） ===== -->
    <el-dialog v-model="createVisible" title="新增管理员" width="440px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="createForm.username" maxlength="20" placeholder="4~20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input
            v-model="createForm.password"
            type="password"
            show-password
            maxlength="20"
            placeholder="6~20 位，须同时包含字母和数字"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" maxlength="10" placeholder="2~10 字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- ===== 重置密码弹窗 ===== -->
    <el-dialog v-model="resetVisible" :title="`重置「${resetRow.username}」的密码`" width="420px">
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            show-password
            maxlength="20"
            placeholder="6~20 位，须同时包含字母和数字"
          />
        </el-form-item>
      </el-form>
      <el-alert title="重置后请及时通知对方，其下次登录须使用新密码" type="info" :closable="false" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="warning" :loading="saving" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 管理员账号管理页（对标 frontend-prototype.md A8 + architecture.md 模块 7）
 *
 * 权限三层防护：
 *   1. 菜单隐藏（Layout v-if="userStore.isSuper"）
 *   2. 路由守卫 meta.isSuper → 非超管跳 403
 *   3. 后端 @RequiresRole(superOnly=true) → 非超管调接口 40301
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { page, create, resetPassword } from '@/api/admin'
import { rules } from '@/utils/validate' // username/password/realName 规则直接复用（对齐后端 DTO）
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const p = await page({ ...query })
    list.value = p.records || []
    total.value = Number(p.total) || 0
  } catch {
    /* 拦截器已提示（40301 非超管） */
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function onPageChange({ current, size }) {
  query.current = current
  query.size = size
  load()
}

/* ===== 新增 ===== */
const createVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({ username: '', password: '', realName: '' })

// 规则复用 validate.js（username/password/realName 与注册页同源，都对标后端 DTO）
const createRules = {
  username: rules.username,
  password: rules.password,
  realName: rules.realName,
}

function openCreate() {
  createForm.username = ''
  createForm.password = ''
  createForm.realName = ''
  createVisible.value = true
  createFormRef.value?.clearValidate()
}

async function doCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await create({ ...createForm })
    ElMessage.success(`管理员「${createForm.username}」创建成功`)
    createVisible.value = false
    load()
  } catch {
    /* 拦截器已提示（40011 账号已存在等） */
  } finally {
    saving.value = false
  }
}

/* ===== 重置密码 ===== */
const resetVisible = ref(false)
const resetFormRef = ref(null)
const resetRow = reactive({ id: null, username: '' })
const resetForm = reactive({ newPassword: '' })

const resetRules = {
  newPassword: rules.password, // 与后端 ResetPasswordDTO 的 6~20 位含字母数字一致
}

function openReset(row) {
  resetRow.id = row.id
  resetRow.username = row.username
  resetForm.newPassword = ''
  resetVisible.value = true
  resetFormRef.value?.clearValidate()
}

async function doReset() {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ElMessageBox.confirm(
      `确定将「${resetRow.username}」的密码重置为新密码吗？`,
      '二次确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  saving.value = true
  try {
    await resetPassword(resetRow.id, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.page-tip {
  margin-bottom: 14px;
}
</style>
