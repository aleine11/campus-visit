<template>
  <div class="knowledge-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="filters">
          <el-input
            v-model="query.fileName"
            placeholder="文件名模糊搜索"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="query.fileType" placeholder="类型" clearable style="width: 110px" @change="search">
            <el-option label="PDF" value="pdf" />
            <el-option label="TXT" value="txt" />
            <el-option label="DOCX" value="docx" />
          </el-select>
          <el-select v-model="query.status" placeholder="解析状态" clearable style="width: 120px" @change="search">
            <el-option label="解析中" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="失败" :value="2" />
          </el-select>
        </div>
        <el-button type="primary" @click="uploadVisible = true">上传文档</el-button>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="page-tip"
        title="上传后系统自动完成：文本提取 → 500 字分块 → BGE 向量化 → 存入 Milvus 向量库，AI 咨询即可引用该文档内容"
      />

      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.fileType?.toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="100">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="向量块数" width="100" align="center">
            <template #default="{ row }">{{ row.status === 1 ? row.chunkCount : '-' }}</template>
          </el-table-column>
          <el-table-column label="解析状态" width="150" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="warning">
                <el-icon class="is-loading"><Loading /></el-icon> 解析中
              </el-tag>
              <el-tooltip v-else-if="row.status === 2" :content="row.errorMsg || '解析失败'" placement="top">
                <el-tag type="danger">失败</el-tag>
              </el-tooltip>
              <el-tag v-else type="success">已完成</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="uploadAdminName" label="上传人" width="100" />
          <el-table-column label="上传时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" :disabled="row.status === 0" @click="handleReparse(row)">
                重新解析
              </el-button>
              <el-button text type="danger" size="small" @click="handleRemove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else-if="!loading" description="知识库还是空的，上传第一篇文档试试" />
      </div>
      <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
    </el-card>

    <!-- ===== 上传弹窗（pdf/txt/docx ≤50MB，对齐后端 8.2 校验） ===== -->
    <el-dialog v-model="uploadVisible" title="上传知识库文档" width="480px" @closed="resetUpload">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-exceed="() => ElMessage.warning('一次只能上传一个文件')"
        accept=".pdf,.txt,.docx"
      >
        <el-icon :size="40" color="#c0c4cc"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 pdf / txt / docx，单个文件不超过 50MB</div>
        </template>
      </el-upload>

      <el-progress v-if="uploading" :percentage="progress" :stroke-width="10" class="upload-progress" />
      <el-alert
        v-if="uploading"
        title="上传成功后后台异步解析+向量化，列表状态会从「解析中」变为「已完成」"
        type="info"
        :closable="false"
        class="upload-progress"
      />

      <template #footer>
        <el-button :disabled="uploading" @click="uploadVisible = false">关闭</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!file" @click="doUpload">开始上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * RAG 知识库管理页（对标 frontend-prototype.md A6 + architecture.md 模块 8）
 *
 * 解析是异步的：上传成功只是"收到文件"，状态经历 解析中(0) → 已完成(1)/失败(2)。
 * 有"解析中"的行时每 3 秒轮询一次列表刷新状态（文件小一般几秒内完成）。
 */
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, UploadFilled } from '@element-plus/icons-vue'
import { page, upload, remove, reparse } from '@/api/knowledge'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ fileName: '', fileType: null, status: null, current: 1, size: 10 })

let pollTimer = null

/** 字节 → 可读大小（KB/MB） */
function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function load() {
  loading.value = true
  try {
    const p = await page({ ...query })
    list.value = p.records || []
    total.value = Number(p.total) || 0
    // 有解析中的文档 → 开轮询；没有 → 停轮询（省请求）
    if (list.value.some((d) => d.status === 0)) startPolling()
    else stopPolling()
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    // 轮询静默刷新：不转 loading，避免表格闪烁
    page({ ...query })
      .then((p) => {
        list.value = p.records || []
        total.value = Number(p.total) || 0
        if (!list.value.some((d) => d.status === 0)) {
          stopPolling()
          ElMessage.success('文档解析完成，AI 咨询已可引用')
        }
      })
      .catch(() => stopPolling())
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
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

/* ===== 删除 / 重新解析 ===== */
async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(
      `删除《${row.fileName}》将同步删除 Milvus 中的全部 ${row.chunkCount || ''} 个向量块，操作不可逆！`,
      '删除文档',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await remove(row.id)
    ElMessage.success('文档及其向量已删除')
    load()
  } catch {
    /* 拦截器已提示（40050 Milvus 删除失败时 MySQL 不删） */
  }
}

async function handleReparse(row) {
  try {
    await ElMessageBox.confirm(
      `重新解析会删除《${row.fileName}》的旧向量并重新分块向量化，期间 AI 咨询可能无法引用该文档。继续吗？`,
      '重新解析',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await reparse(row.id)
    ElMessage.success('已开始重新解析')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

/* ===== 上传 ===== */
const uploadVisible = ref(false)
const uploading = ref(false)
const file = ref(null)
const progress = ref(0)

function onFileChange(uploadFile) {
  const f = uploadFile.raw
  const okTypes = ['pdf', 'txt', 'docx']
  const ext = f.name.split('.').pop().toLowerCase()
  if (!okTypes.includes(ext)) {
    ElMessage.error('仅支持 pdf / txt / docx 格式')
    uploadVisible.value = false
    return
  }
  if (f.size > 50 * 1024 * 1024) {
    ElMessage.error('文件超过 50MB 上限')
    uploadVisible.value = false
    return
  }
  file.value = f
}

async function doUpload() {
  uploading.value = true
  progress.value = 0
  try {
    await upload(file.value, (p) => (progress.value = p))
    ElMessage.success('上传成功，后台解析中...')
    uploadVisible.value = false
    search()
  } catch {
    /* 拦截器已提示（40030 类型不支持 / 40001 超限） */
  } finally {
    uploading.value = false
  }
}

function resetUpload() {
  file.value = null
  progress.value = 0
}

onMounted(load)
onBeforeUnmount(stopPolling) // 离开页面必须停轮询，否则后台一直打接口
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
.page-tip {
  margin-bottom: 14px;
}
.upload-progress {
  margin-top: 14px;
}
</style>
