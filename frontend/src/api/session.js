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

/* ================= 管理员后台（对标 architecture.md 模块 12，全部 🔒 admin） ================= */

/**
 * 12.1 场次分页（含下架和过期场次，前台 available 只看得到"开放且未过期"的）
 * @param {Object} params { visitDate, status, current, size } 均可选
 */
export function pageForAdmin(params) {
  return request.get('/admin/session/page', { params })
}

/** 12.2 新增场次（SessionSaveDTO：visitDate/timeSlot/maxPeople/status） */
export function create(data) {
  return request.post('/admin/session', data)
}

/** 12.3 编辑场次（缩容保护：maxPeople < 已约人数 → 后端 40022） */
export function update(id, data) {
  return request.put(`/admin/session/${id}`, data)
}

/** 12.4 上架（下架 → 开放） */
export function online(id) {
  return request.post(`/admin/session/${id}/online`)
}

/** 12.5 下架（开放 → 下架，前台立即不可见） */
export function offline(id) {
  return request.post(`/admin/session/${id}/offline`)
}

/** 12.6 删除（已有预约 usedPeople>0 → 后端 40022 禁删，前端按钮也置灰双保险） */
export function remove(id) {
  return request.delete(`/admin/session/${id}`)
}
