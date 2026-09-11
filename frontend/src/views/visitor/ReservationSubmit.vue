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
        <el-form-item label="参观事由" prop="reasonType">
          <el-radio-group v-model="form.reasonType" class="reason-group">
            <el-radio
              v-for="r in REASON_OPTIONS"
              :key="r"
              :value="r"
              class="reason-option"
              :class="{ checked: form.reasonType === r }"
            >
              {{ r }}
            </el-radio>
            <el-radio :value="OTHER_FLAG" class="reason-option" :class="{ checked: form.reasonType === OTHER_FLAG }">
              其他（需填写）
            </el-radio>
          </el-radio-group>
          <!-- 选"其他"才展开输入框 -->
          <el-input
            v-if="form.reasonType === OTHER_FLAG"
            v-model="form.reasonCustom"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            class="reason-other-input"
            placeholder="请填写具体事由（5~200 字）"
            @blur="revalidateReason"
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
 *
 * 参观事由：5 个常见预设选项（默认选中第一个，不打字直接可约）+ "其他"手填
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

/**
 * 参观事由预设选项（校园参观最常见的 5 类）
 * 每个文案均 ≥5 个汉字，直接满足后端 5~200 字校验，选中即可提交、无需打字；
 * 只有选"其他"时才需要手写。
 */
const REASON_OPTIONS = [
  '高校招生咨询参观',
  '校园开放日参观',
  '校友返校参观',
  '学术交流活动',
  '研学实践活动',
]
/** "其他"选项的内部标记值（不会提交给后端） */
const OTHER_FLAG = '__OTHER__'

const form = reactive({
  realName: '', // 默认带出注册时的姓名
  phone: '', // 默认带出注册时的手机号
  peopleCount: 1,
  reasonType: REASON_OPTIONS[0], // 默认选中最常见的"高校招生咨询参观"，进页面即可直接提交
  reasonCustom: '', // 仅"其他"时使用
})

/** 业务上限（≤50）与剩余名额取较小 */
const peopleMax = computed(() => {
  if (!session.value) return 1
  return Math.min(50, session.value.remaining)
})

/**
 * 事由校验：预设选项天然合法（进页面默认已选）；
 * 仅当选"其他"时，才校验手填内容非空且 5~200 字。
 */
function validateReason(_rule, _value, callback) {
  if (form.reasonType === OTHER_FLAG) {
    const v = (form.reasonCustom || '').trim()
    if (!v) return callback(new Error('请填写具体事由'))
    if (v.length < 5 || v.length > 200) return callback(new Error('事由长度须在 5~200 字之间'))
  }
  callback()
}

// 基础规则 + 人数必填 + 事由条件校验
const formRules = {
  realName: rules.realName,
  phone: rules.phone,
  peopleCount: [{ required: true, message: '请填写参观人数', trigger: 'blur' }],
  reasonType: [{ validator: validateReason, trigger: 'change' }],
}

/** 手填事由失焦时立即重新校验（输入框是条件渲染的，需主动触发） */
function revalidateReason() {
  formRef.value?.validateField('reasonType')
}

/** 最终提交给后端的事由：预设直接用文案，其他用手写内容 */
function finalReason() {
  return form.reasonType === OTHER_FLAG ? form.reasonCustom.trim() : form.reasonType
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
      reason: finalReason(),
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

/* ===== 参观事由：选项卡片式单选 ===== */
.reason-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 12px;
  width: 100%;
}
/* 整块卡片可点 */
.reason-option {
  margin: 0;
  height: 40px;
  padding: 0 16px;
  border: 1.5px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  transition: all 0.2s;
  user-select: none;
}
.reason-option:hover {
  border-color: #79b8ff;
  color: #409eff;
}
.reason-option.checked {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}
/* 去掉 Element-Plus radio 默认圆点，选中态用卡片颜色表达 */
.reason-option :deep(.el-radio__input) {
  display: none;
}
.reason-option :deep(.el-radio__label) {
  padding-left: 0;
  font-size: 14px;
}
.reason-other-input {
  margin-top: 12px;
  width: 100%;
}
</style>
