<template>
  <div class="reservation-submit-page">
    <h2 class="page-title">提交预约</h2>

    <!-- ===== 场次信息卡 ===== -->
    <el-card v-if="session" class="session-card" shadow="never">
      <div class="session-info">
        <div class="info-item">
          <span class="label">参观日期</span>
          <span class="value">{{ formatDate(session.visitDate) }}</span>
        </div>
        <div class="info-item">
          <span class="label">参观时段</span>
          <span class="value">{{ session.timeSlot }}</span>
        </div>
        <div class="info-item">
          <span class="label">剩余名额</span>
          <span class="value remain" :class="session.remaining > 0 ? 'ok' : 'zero'">{{ session.remaining }} 人</span>
        </div>
      </div>
    </el-card>

    <!-- ===== 预约表单（字段与后端 ReservationSubmitDTO 一致） ===== -->
    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
        style="max-width: 560px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="2~10 字" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位大陆手机号" clearable />
        </el-form-item>
        <el-form-item label="参观人数" prop="peopleCount">
          <el-input-number
            v-model="form.peopleCount"
            :min="1"
            :max="peopleMax"
            :step="1"
            step-strictly
          />
          <span class="count-tip">单笔最多 50 人，当前可选上限 {{ peopleMax }} 人</span>
        </el-form-item>
        <el-form-item label="参观事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="5~200 字，例如：高校招生咨询参观 / 校友返校 / 学术交流活动"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" native-type="submit">
            {{ submitting ? '提交中...' : '提交预约' }}
          </el-button>
          <el-button size="large" @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        title="提交后由管理员审核，审核结果可在「我的预约」中查看"
        type="info"
        :closable="false"
        show-icon
        style="max-width: 560px"
      />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 预约提交页（对标 F5）
 *
 * 人数上限取两者较小值：
 *   业务上限 50（后端注解）与剩余名额（查库才知道）→ el-input-number 动态 max
 *   例：场次剩 3 人 → 上限 3；场次剩 80 人 → 上限 50
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDetail as getSessionDetail } from '@/api/session'
import { submit } from '@/api/reservation'
import { getProfile } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { formatDate } from '@/utils/format'
import { rules } from '@/utils/validate'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)
const session = ref(null)

const form = reactive({
  realName: '', // 默认带出注册时的姓名
  phone: '', // 默认带出注册时的手机号
  peopleCount: 1,
  reason: '',
})

/** 业务上限（≤50）与剩余名额取较小 */
const peopleMax = computed(() => {
  if (!session.value) return 1
  return Math.min(50, session.value.remaining)
})

// 基础规则 + 人数必填
const formRules = {
  realName: rules.realName,
  phone: rules.phone,
  peopleCount: [{ required: true, message: '请填写参观人数', trigger: 'blur' }],
  reason: rules.reason,
}

onMounted(async () => {
  // 先查场次详情（带出剩余名额 + 校验场次是否仍开放）
  try {
    const s = await getSessionDetail(route.params.sessionId)
    if (s.status !== 0) {
      ElMessage.error('该场次已下架，无法预约')
      router.replace('/visitor/session')
      return
    }
    if (s.remaining <= 0) {
      ElMessage.warning('该场次名额已满')
      router.replace('/visitor/session')
      return
    }
    session.value = s
  } catch {
    router.replace('/visitor/session')
    return
  }

  // 再查个人资料带出姓名手机号（注册时填过就不用重复填）
  try {
    const profile = await getProfile()
    form.realName = profile.realName || ''
    form.phone = profile.phone || ''
  } catch {
    /* 静默 */
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true // 提交中禁用按钮，防双击重复下单
  try {
    await submit({
      sessionId: Number(route.params.sessionId),
      realName: form.realName,
      phone: form.phone,
      peopleCount: form.peopleCount,
      reason: form.reason,
    })
    ElMessage.success('预约提交成功，等待管理员审核')
    router.push('/visitor/reservation/list')
  } catch {
    /* 拦截器已提示（名额不足 40021 / 重复预约 40022 / 场次不可预约 40020 等） */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-title {
  margin-bottom: 18px;
}
.session-card {
  margin-bottom: 20px;
}
.session-info {
  display: flex;
  gap: 60px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.info-item .label {
  font-size: 13px;
  color: #909399;
}
.info-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.info-item .value.remain.ok {
  color: #67c23a;
}
.info-item .value.remain.zero {
  color: #f56c6c;
}
.count-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
