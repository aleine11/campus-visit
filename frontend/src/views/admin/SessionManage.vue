<template>
  <div class="session-page">
    <el-card shadow="never">
      <!-- 工具栏：筛选 + 新增 -->
      <div class="toolbar">
        <div class="filters">
          <el-date-picker
            v-model="query.visitDate"
            type="date"
            placeholder="按参观日期筛选"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 160px"
            @change="search"
          />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="search">
            <el-option label="开放" :value="0" />
            <el-option label="下架" :value="1" />
          </el-select>
        </div>
        <el-button type="primary" @click="openForm()">新增场次</el-button>
      </div>

      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="参观日期" width="130">
            <template #default="{ row }">{{ formatDate(row.visitDate) }}</template>
          </el-table-column>
          <el-table-column prop="timeSlot" label="时段" width="130" />
          <el-table-column label="名额" min-width="180">
            <template #default="{ row }">
              <el-progress
                :percentage="row.maxPeople ? Math.round((row.usedPeople / row.maxPeople) * 100) : 0"
                :stroke-width="14"
                :status="row.usedPeople >= row.maxPeople ? 'exception' : undefined"
              />
              <span class="quota-text">{{ row.usedPeople }} / {{ row.maxPeople }}（余 {{ row.remaining }}）</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '开放' : '下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="是否过期" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="isExpired(row)" type="warning" size="small">已过期</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openForm(row)">编辑</el-button>
              <el-button v-if="row.status === 0" text type="warning" size="small" @click="toggle(row, 'offline')">
                下架
              </el-button>
              <el-button v-else text type="success" size="small" @click="toggle(row, 'online')">上架</el-button>
              <!-- 删除保护：已有预约的场次置灰（后端 40022 双保险） -->
              <el-button text type="danger" size="small" :disabled="row.usedPeople > 0" @click="handleRemove(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else-if="!loading" description="暂无场次，点击右上角新增" />
      </div>
      <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
    </el-card>

    <!-- ===== 新增/编辑弹窗（对标 A2 表单：日期禁选过去 / 时段 / 人数 1~500 / 状态） ===== -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑场次' : '新增场次'" width="440px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="参观日期" prop="visitDate">
          <!-- disabled-date：今天之前的日期全部禁点，对齐后端 @FutureOrPresent -->
          <el-date-picker
            v-model="form.visitDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="(d) => d.getTime() < Date.now() - 86400000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlot">
          <el-select v-model="form.timeSlot" placeholder="选择时段" style="width: 100%" allow-create filterable>
            <el-option v-for="slot in TIME_SLOTS" :key="slot" :label="slot" :value="slot" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大人数" prop="maxPeople">
          <el-input-number v-model="form.maxPeople" :min="1" :max="500" style="width: 100%" />
          <div v-if="form.id && editingRow?.usedPeople > 0" class="form-tip">
            已有 {{ editingRow.usedPeople }} 人预约，人数不可低于该值（后端缩容保护 40022）
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">开放</el-radio>
            <el-radio :value="1">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 参观场次管理页（对标 frontend-prototype.md A2 + architecture.md 模块 12 后台接口）
 *
 * 三大业务约束（后端强制，前端同步拦截）：
 *   1. 新增日期不早于今天（前端 disabled-date + 后端 @FutureOrPresent）
 *   2. 编辑缩容保护：maxPeople < 已约人数 → 40022（按钮提示 + 后端兜底）
 *   3. 删除保护：usedPeople > 0 禁删 → 40022（按钮置灰 + 后端兜底）
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageForAdmin, create, update, online, offline, remove } from '@/api/session'
import { formatDate } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

/** 常用时段预设（支持 allow-create 自定义输入） */
const TIME_SLOTS = ['08:30-10:30', '09:00-11:00', '10:00-12:00', '14:00-16:00', '15:00-17:00']

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ visitDate: null, status: null, current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const page = await pageForAdmin({ ...query })
    list.value = page.records || []
    total.value = Number(page.total) || 0
  } catch {
    /* 拦截器已提示 */
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

/** 场次是否已过期（参观日期在今天之前）——仅用于展示"已过期"标签 */
function isExpired(row) {
  return row.visitDate < new Date().toISOString().slice(0, 10)
}

/* ===== 上下架 / 删除 ===== */
async function toggle(row, action) {
  const toOffline = action === 'offline'
  try {
    await ElMessageBox.confirm(
      toOffline
        ? `下架后前台立即不可见、无法再预约（已预约的不受影响）。确定下架 ${formatDate(row.visitDate)} ${row.timeSlot} 吗？`
        : `上架后前台恢复可见、可继续预约。确定上架 ${formatDate(row.visitDate)} ${row.timeSlot} 吗？`,
      toOffline ? '下架场次' : '上架场次',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await (toOffline ? offline(row.id) : online(row.id))
    ElMessage.success(toOffline ? '已下架' : '已上架')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${formatDate(row.visitDate)} ${row.timeSlot} 场次吗？删除后不可恢复。`,
      '删除场次',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示（40022 已有预约禁删） */
  }
}

/* ===== 新增 / 编辑弹窗 ===== */
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editingRow = ref(null)
const form = reactive({ id: null, visitDate: '', timeSlot: '', maxPeople: 50, status: 0 })

const formRules = {
  visitDate: [{ required: true, message: '请选择参观日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择或输入时段', trigger: 'change' }],
  maxPeople: [{ required: true, message: '请填写最大容纳人数', trigger: 'blur' }],
}

function openForm(row) {
  editingRow.value = row || null
  form.id = row?.id ?? null
  form.visitDate = row?.visitDate || ''
  form.timeSlot = row?.timeSlot || ''
  form.maxPeople = row?.maxPeople ?? 50
  form.status = row?.status ?? 0
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = {
      visitDate: form.visitDate,
      timeSlot: form.timeSlot,
      maxPeople: form.maxPeople,
      status: form.status,
    }
    if (form.id) {
      await update(form.id, data)
      ElMessage.success('场次已更新')
    } else {
      await create(data)
      ElMessage.success('场次已创建')
    }
    formVisible.value = false
    load()
  } catch {
    /* 拦截器已提示（40001 过去日期 / 40022 缩容等） */
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
.filters {
  display: flex;
  gap: 10px;
}
.quota-text {
  font-size: 12px;
  color: #909399;
}
.form-tip {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.4;
  margin-top: 2px;
}
</style>
