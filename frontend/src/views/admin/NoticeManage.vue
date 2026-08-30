<template>
  <div class="notice-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="filters">
          <el-input
            v-model="query.keyword"
            placeholder="标题模糊搜索"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="search">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </div>
        <el-button type="primary" @click="openForm()">新增公告</el-button>
      </div>

      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openForm(row)">编辑</el-button>
              <el-button v-if="row.status === 0" text type="success" size="small" @click="handlePublish(row)">
                发布
              </el-button>
              <el-button v-else text type="warning" size="small" @click="handleOffline(row)">下架</el-button>
              <el-button text type="danger" size="small" @click="handleRemove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else-if="!loading" description="暂无公告" />
      </div>
      <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
    </el-card>

    <!-- ===== 新增/编辑弹窗（标题 1~100 字 / 正文 1~10000 字，对齐 NoticeSaveDTO） ===== -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑公告' : '新增公告'" width="640px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="10000"
            show-word-limit
            placeholder="公告正文（前台按换行展示）"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">存为草稿</el-radio>
            <el-radio :value="1">保存并发布</el-radio>
          </el-radio-group>
          <div v-if="form.id && editingRow?.status === 1" class="form-tip">
            注意：该公告已发布，保存修改会立即同步到前台
          </div>
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
 * 校园公告管理页（对标 frontend-prototype.md A5 + architecture.md 模块 11 后台接口）
 *
 * 状态机：草稿(0) ⇄ 已发布(1)
 *   发布 → 写入发布时间，前台立即可见
 *   下架 → 回草稿，前台立即不可见
 *   删除 → 逻辑删除，两边都不再显示
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageForAdmin, create, update, publish, offline, remove, getDetail } from '@/api/notice'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: null, current: 1, size: 10 })

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

/* ===== 发布 / 下架 / 删除 ===== */
async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确定发布《${row.title}》吗？发布后前台立即可见。`, '发布公告', { type: 'info' })
  } catch {
    return
  }
  try {
    await publish(row.id)
    ElMessage.success('已发布')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm(
      `下架后《${row.title}》前台将不可见（转为草稿）。确定下架吗？`,
      '下架公告',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await offline(row.id)
    ElMessage.success('已下架')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除《${row.title}》吗？删除后前台和后台都不再显示。`,
      '删除公告',
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
    /* 拦截器已提示 */
  }
}

/* ===== 新增 / 编辑弹窗 ===== */
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editingRow = ref(null)
const form = reactive({ id: null, title: '', content: '', status: 0 })

// 对齐后端 NoticeSaveDTO：title 1~100 字、content 1~10000 字
const formRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度须在 1~100 字之间', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入正文', trigger: 'blur' },
    { min: 1, max: 10000, message: '正文长度须在 1~10000 字之间', trigger: 'blur' },
  ],
}

function openForm(row) {
  editingRow.value = row || null
  form.id = row?.id ?? null
  form.title = row?.title || ''
  // 列表接口只带摘要不带全文 → 编辑时单独拉详情
  form.content = ''
  form.status = row?.status ?? 0
  formVisible.value = true
  formRef.value?.clearValidate()
  if (row) fillContent(row.id)
}

/** 编辑时拉全文（列表 VO 只有 80 字摘要） */
async function fillContent(id) {
  try {
    const vo = await getDetail(id)
    form.content = vo.content || ''
  } catch {
    /* 拦截器已提示 */
  }
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { title: form.title, content: form.content, status: form.status }
    if (form.id) {
      await update(form.id, data)
      ElMessage.success(form.status === 1 ? '已保存并发布' : '已保存')
    } else {
      await create(data)
      ElMessage.success(form.status === 1 ? '已发布' : '已存为草稿')
    }
    formVisible.value = false
    load()
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
.filters {
  display: flex;
  gap: 10px;
}
.form-tip {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.4;
  margin-top: 2px;
}
</style>
