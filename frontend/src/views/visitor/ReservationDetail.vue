<template>
  <div class="reservation-detail-page" v-loading="loading">
    <template v-if="detail">
      <div class="page-head">
        <h2 class="page-title">预约详情</h2>
        <el-button text @click="router.push('/visitor/reservation/list')">
          <el-icon><Back /></el-icon> 返回列表
        </el-button>
      </div>

      <el-card>
        <template #header>
          <div class="head-row">
            <span>订单号 #{{ detail.id }}</span>
            <el-tag :type="reservationTag(detail.status)">{{ detail.statusText }}</el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="参观日期">{{ formatDate(detail.visitDate) }}</el-descriptions-item>
          <el-descriptions-item label="参观时段">{{ detail.timeSlot }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.realName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="参观人数">{{ detail.peopleCount }} 人</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="参观事由" :span="2">
            <span class="reason">{{ detail.reason }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.auditAdminName" label="审核人">
            {{ detail.auditAdminName }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.auditTime" label="审核时间">
            {{ formatDateTime(detail.auditTime) }}
          </el-descriptions-item>
          <!-- 驳回原因：status=2 才显示 -->
          <el-descriptions-item v-if="detail.status === 2" label="驳回原因" :span="2">
            <span class="reject-reason">{{ detail.rejectReason || '未填写原因' }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelTime" label="取消时间" :span="2">
            {{ formatDateTime(detail.cancelTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 取消按钮：仅待审核/已通过可取消（后端 40025 兜底） -->
        <div v-if="detail.status === 0 || detail.status === 1" class="cancel-area">
          <el-button type="danger" plain @click="handleCancel">取消预约</el-button>
        </div>
      </el-card>
    </template>
    <EmptyState v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script setup>
/**
 * 预约详情页（对标 F7：全部字段 + 驳回原因 + 取消操作）
 */
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { getDetail, cancel } from '@/api/reservation'
import { formatDate, formatDateTime, reservationTag } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)

async function load(id) {
  if (!id) return
  loading.value = true
  try {
    detail.value = await getDetail(id)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 取消：二次确认 → 接口 → 刷新详情（状态变"已取消"） */
async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？取消后名额立即释放。', '取消预约', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await cancel(route.params.id)
    ElMessage.success('预约已取消')
    load(route.params.id)
  } catch {
    /* 静默 */
  }
}

watch(() => route.params.id, (id) => load(id), { immediate: true })
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.page-title {
  color: #303133;
}
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.reason {
  line-height: 1.7;
}
.reject-reason {
  color: #f56c6c;
}
.cancel-area {
  margin-top: 20px;
  text-align: center;
}
</style>
