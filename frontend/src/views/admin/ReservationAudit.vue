<template>
  <div class="audit-page">
    <!-- ===== 筛选栏（对标 A3：姓名模糊 / 状态 / 提交时间范围） ===== -->
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="访客姓名">
          <el-input
            v-model="query.realName"
            placeholder="姓名模糊搜索"
            clearable
            style="width: 160px"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="(v, k) in RESERVATION_STATUS" :key="k" :label="v.text" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="截止"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===== 订单表格 ===== -->
    <el-card shadow="never">
      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" stripe>
          <el-table-column prop="id" label="订单号" width="80" />
          <el-table-column prop="realName" label="访客姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="场次" width="190">
            <template #default="{ row }">{{ formatDate(row.visitDate) }} {{ row.timeSlot }}</template>
          </el-table-column>
          <el-table-column prop="peopleCount" label="人数" width="70" align="center" />
          <el-table-column prop="reason" label="参观事由" min-width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="reservationTag(row.status)">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="审核信息" width="170">
            <template #default="{ row }">
              <template v-if="row.auditTime">
                <div>{{ row.auditAdminName }} · {{ formatDateTime(row.auditTime) }}</div>
                <div v-if="row.status === 2" class="reject-reason" :title="row.rejectReason">
                  原因：{{ row.rejectReason }}
                </div>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button type="success" size="small" @click="openAudit(row, true)">通过</el-button>
                <el-button type="danger" size="small" @click="openAudit(row, false)">驳回</el-button>
              </template>
              <el-button v-else text type="primary" size="small" @click="showDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else-if="!loading" description="没有符合条件的订单" />
      </div>
      <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
    </el-card>

    <!-- ===== 审核弹窗：通过 / 驳回（驳回必填原因 5~200 字，对齐 ReservationAuditDTO） ===== -->
    <el-dialog v-model="auditVisible" :title="auditForm.pass ? '通过预约' : '驳回预约'" width="480px">
      <el-descriptions :column="1" border size="small" class="audit-desc">
        <el-descriptions-item label="访客">{{ current.realName }}（{{ current.phone }}）</el-descriptions-item>
        <el-descriptions-item label="场次">{{ formatDate(current.visitDate) }} {{ current.timeSlot }}</el-descriptions-item>
        <el-descriptions-item label="人数">{{ current.peopleCount }} 人</el-descriptions-item>
        <el-descriptions-item label="事由">{{ current.reason }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="auditForm.pass"
        title="通过后名额保持不变（提交时已扣减），访客将收到通过状态"
        type="success"
        :closable="false"
        class="audit-tip"
      />
      <el-alert
        v-else
        title="驳回后名额立即回滚给其他人，请填写具体驳回原因"
        type="warning"
        :closable="false"
        class="audit-tip"
      />

      <el-form v-if="!auditForm.pass" ref="auditFormRef" :model="auditForm" :rules="auditRules">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="auditForm.rejectReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写驳回原因（5~200 字），将以站内信息展示给访客"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="auditForm.pass ? 'success' : 'danger'" :loading="auditing" @click="confirmAudit">
          确认{{ auditForm.pass ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 预约订单审核页（对标 frontend-prototype.md A3 + architecture.md 模块 5）
 *
 * 审核状态机（后端硬约束，前端按钮同步）：
 *   只有 status=0（待审核）显示"通过/驳回"；已审核订单只能看详情
 *   驳回必填原因 5~200 字（后端 ReservationAuditDTO @Valid 兜底）
 */
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageForAdmin, adminDetail, audit } from '@/api/reservation'
import { formatDate, formatDateTime, reservationTag, RESERVATION_STATUS } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref(null)
const query = reactive({ realName: '', status: null, current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    // 状态下拉清空时是 null，axios 会把 null 序列化丢弃（后端 optional），不用特判
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const page = await pageForAdmin(params)
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

function reset() {
  query.realName = ''
  query.status = null
  dateRange.value = null
  search()
}

function onPageChange({ current, size }) {
  query.current = current
  query.size = size
  load()
}

/* ===== 审核弹窗 ===== */
const auditVisible = ref(false)
const auditing = ref(false)
const auditFormRef = ref(null)
const current = reactive({})
const auditForm = reactive({ pass: true, rejectReason: '' })

// 驳回原因规则：对齐后端 ReservationAuditDTO（pass=false 时 5~200 字）
const auditRules = {
  rejectReason: [
    { required: true, message: '驳回时必须填写原因', trigger: 'blur' },
    { min: 5, max: 200, message: '原因长度须在 5~200 字之间', trigger: 'blur' },
  ],
}

function openAudit(row, pass) {
  Object.assign(current, row)
  auditForm.pass = pass
  auditForm.rejectReason = ''
  auditVisible.value = true
  auditFormRef.value?.clearValidate()
}

async function confirmAudit() {
  // 驳回时先跑表单校验；通过时无表单直接走
  if (!auditForm.pass) {
    const valid = await auditFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  auditing.value = true
  try {
    const data = auditForm.pass
      ? { pass: true }
      : { pass: false, rejectReason: auditForm.rejectReason }
    await audit(current.id, data)
    ElMessage.success(auditForm.pass ? '已通过该预约' : '已驳回该预约，名额已回滚')
    auditVisible.value = false
    load()
  } catch {
    /* 拦截器已提示（40022 重复审核等） */
  } finally {
    auditing.value = false
  }
}

/** 已审核订单看详情（含驳回原因/取消时间） */
async function showDetail(row) {
  try {
    const vo = await adminDetail(row.id)
    ElMessageBox.alert(
      renderDetail(vo),
      `订单 #${vo.id} 详情`,
      { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
    )
  } catch {
    /* 拦截器已提示 */
  }
}

function renderDetail(vo) {
  const line = (k, v) => `<p><b>${k}：</b>${v ?? '-'}</p>`
  return (
    line('访客', `${vo.realName ?? '-'}（${vo.phone ?? '-'}）`) +
    line('场次', `${formatDate(vo.visitDate)} ${vo.timeSlot}`) +
    line('人数', vo.peopleCount) +
    line('事由', vo.reason) +
    line('状态', vo.statusText) +
    line('提交时间', formatDateTime(vo.submitTime, true)) +
    (vo.auditTime ? line('审核', `${vo.auditAdminName} · ${formatDateTime(vo.auditTime)}`) : '') +
    (vo.rejectReason ? line('驳回原因', vo.rejectReason) : '') +
    (vo.cancelTime ? line('取消时间', formatDateTime(vo.cancelTime)) : '')
  )
}

onMounted(() => {
  // 从"访客管理 → 查看预约"跳来时带 ?realName=xxx，自动填入筛选（对齐 A4 联动）
  const initial = useRoute().query.realName
  if (initial) query.realName = String(initial)
  load()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}
.reject-reason {
  font-size: 12px;
  color: #f56c6c;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.audit-desc {
  margin-bottom: 12px;
}
.audit-tip {
  margin-bottom: 12px;
}
</style>
