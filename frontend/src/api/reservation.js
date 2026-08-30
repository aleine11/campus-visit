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
