/**
 * 参观场次接口（前台公开部分，对标 architecture.md 模块 3）
 *   GET /session/available  可预约场次分页（startDate/endDate/current/size，自动过滤下架+过期）
 *   GET /session/latest     最新可预约场次（count 默认 3，首页用）
 *   GET /session/{id}       场次详情（预约提交页展示场次信息用）
 */
import request from '@/utils/request'

export function pageAvailable(params) {
  return request.get('/session/available', { params })
}

export function latest(count = 3) {
  return request.get('/session/latest', { params: { count } })
}

export function getDetail(id) {
  return request.get(`/session/${id}`)
}
