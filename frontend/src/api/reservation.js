/**
 * 预约接口（访客端，对标 architecture.md 模块 4，全部需要登录）
 *   POST   /reservation           提交预约（ReservationSubmitDTO，返回新订单 ID）
 *   GET    /reservation/my        我的预约分页（current/size/status 可选）
 *   GET    /reservation/{id}      订单详情（ReservationDetailVO）
 *   POST   /reservation/{id}/cancel  取消预约（仅待审核/已通过可取消，后端校验）
 */
import request from '@/utils/request'

export function submit(data) {
  return request.post('/reservation', data)
}

export function pageMy(params) {
  return request.get('/reservation/my', { params })
}

export function getDetail(id) {
  return request.get(`/reservation/${id}`)
}

export function cancel(id) {
  return request.post(`/reservation/${id}/cancel`)
}

/* ================= 管理员后台（对标 architecture.md 模块 5，全部 🔒 admin） ================= */

/**
 * 5.1 预约订单分页（比访客版多返回 phone/reason/auditAdminName/auditTime）
 * @param {Object} params { realName, status, startDate, endDate, current, size } 均可选
 *   startDate/endDate 格式 yyyy-MM-ddTHH:mm:ss（后端 @DateTimeFormat ISO DATE_TIME）
 */
export function pageForAdmin(params) {
  return request.get('/admin/reservation/page', { params })
}

/** 5.2 订单详情（ReservationDetailVO，含 rejectReason/cancelTime） */
export function adminDetail(id) {
  return request.get(`/admin/reservation/${id}`)
}

/**
 * 5.3 审核（通过/驳回）
 * @param {number} id 订单 ID
 * @param {{pass: boolean, rejectReason?: string}} data pass=true 通过；false 驳回时必填 rejectReason（5~200 字）
 */
export function audit(id, data) {
  return request.post(`/admin/reservation/${id}/audit`, data)
}
