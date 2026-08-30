<template>
  <div class="visitor-manage-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="filters">
          <el-input
            v-model="query.keyword"
            placeholder="用户名 / 姓名 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="search">
            <el-option label="正常" :value="0" />
            <el-option label="冻结" :value="1" />
          </el-select>
        </div>
        <span class="tip">支持"一词三搜"：一个关键词同时模糊匹配用户名、姓名、手机号</span>
      </div>

      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="realName" label="真实姓名" width="130" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.registerTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="200" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                text
                type="danger"
                size="small"
                @click="toggleFreeze(row, 'freeze')"
              >
                冻结
              </el-button>
              <el-button v-else text type="success" size="small" @click="toggleFreeze(row, 'unfreeze')">
                解冻
              </el-button>
              <el-button text type="primary" size="small" @click="viewOrders(row)">
                查看预约
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else-if="!loading" description="没有符合条件的访客" />
      </div>
      <PaginationBar :current="query.current" :size="query.size" :total="total" @change="onPageChange" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 访客用户管理页（对标 frontend-prototype.md A4 + architecture.md 模块 6 后台接口）
 *
 * 冻结的效果链（模块 6 实测过的跨模块联动）：
 *   冻结 → 该访客登录被拒（40012）→ 解冻 → 恢复登录
 *   冻结不影响已提交订单，仅禁登录
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { pageForAdmin, freeze, unfreeze } from '@/api/visitor'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: null, current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const p = await pageForAdmin({ ...query })
    list.value = p.records || []
    total.value = Number(p.total) || 0
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

/** 冻结/解冻（重复操作后端 40022，前端按状态切按钮双保险） */
async function toggleFreeze(row, action) {
  const freezing = action === 'freeze'
  try {
    await ElMessageBox.confirm(
      freezing
        ? `冻结后「${row.realName || row.username}」将无法登录系统（已提交的订单不受影响）。确定冻结吗？`
        : `解冻后「${row.realName || row.username}」可恢复正常登录。确定解冻吗？`,
      freezing ? '冻结访客' : '解冻访客',
      { type: freezing ? 'warning' : 'info' }
    )
  } catch {
    return
  }
  try {
    await (freezing ? freeze(row.id) : unfreeze(row.id))
    ElMessage.success(freezing ? '已冻结' : '已解冻')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

/** 跳转预约审核页并带上该访客姓名筛选（对齐设计文档 A4"查看该访客全部预约"） */
function viewOrders(row) {
  router.push({ path: '/admin/reservation', query: { realName: row.realName || '' } })
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
.tip {
  font-size: 12px;
  color: #909399;
}
</style>
