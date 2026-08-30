<template>
  <div class="my-reservations-page">
    <div class="page-head">
      <h2 class="page-title">我的预约</h2>
      <el-button type="primary" @click="router.push('/visitor/session')">再约一场</el-button>
    </div>

    <!-- 状态筛选 Tabs（全部/待审核/已通过/已驳回/已取消） -->
    <el-tabs v-model="activeStatus" @tab-change="onTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane v-for="(v, k) in RESERVATION_STATUS" :key="k" :label="v.text" :name="k" />
    </el-tabs>

    <div v-loading="loading">
      <el-table v-if="list.length" :data="list" stripe>
        <el-table-column prop="visitDate" label="参观日期" width="130">
          <template #default="{ row }">{{ formatDate(row.visitDate) }}</template>
        </el-table-column>
        <el-table-column prop="timeSlot" label="时段" width="140" />
        <el-table-column prop="peopleCount" label="人数" width="80" align="center" />
        <el-table-column prop="reason" label="参观事由" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="reservationTag(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/visitor/reservation/${row.id}`)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              text
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else-if="!loading" description="暂无预约记录" />
    </div>

    <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
  </div>
</template>

<script setup>
/**
 * 我的预约列表（对标 F6：状态筛选 + 列表 + 取消操作）
 * status 参数映射：all → 不传（后端 optional）；其他 → 对应数字
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMy, cancel } from '@/api/reservation'
import { formatDate, formatDateTime, reservationTag, RESERVATION_STATUS } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const activeStatus = ref('all')
const query = reactive({ current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (activeStatus.value !== 'all') {
      params.status = Number(activeStatus.value)
    }
    const page = await pageMy(params)
    list.value = page.records || []
    total.value = Number(page.total) || 0
  } catch {
    /* 静默 */
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.current = 1 // 切状态回第 1 页
  load()
}

function onPageChange({ current, size }) {
  query.current = current
  query.size = size
  load()
}

/** 取消预约：二次确认（防误点）→ 调接口 → 刷新列表 */
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确定取消 ${formatDate(row.visitDate)} ${row.timeSlot} 的预约吗？取消后名额立即释放。`,
      '取消预约',
      { confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await cancel(row.id)
    ElMessage.success('预约已取消')
    load()
  } catch {
    /* 拦截器已提示（状态不允许取消 40025 等） */
  }
}

onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.page-title {
  color: #303133;
}
</style>
