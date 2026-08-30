/**
 * 校园公告接口（前台公开部分，对标 architecture.md 模块 2）
 *   GET /notice/list    已发布公告分页（current/size）
 *   GET /notice/latest  最新公告（count 默认 3，首页用）
 *   GET /notice/{id}    公告详情（含 prevId/nextId 上下篇导航）
 */
import request from '@/utils/request'

export function pagePublished(params) {
  return request.get('/notice/list', { params })
}

export function latest(count = 3) {
  return request.get('/notice/latest', { params: { count } })
}

export function getDetail(id) {
  return request.get(`/notice/${id}`)
}

/* ================= 管理员后台（对标 architecture.md 模块 11，全部 🔒 admin） ================= */

/**
 * 11.1 公告分页（含草稿；前台 list 只看得到已发布）
 * @param {Object} params { keyword, status, current, size } 均可选
 */
export function pageForAdmin(params) {
  return request.get('/admin/notice/page', { params })
}

/** 11.2 新增公告（NoticeSaveDTO：title/content/status，status=1 保存即发布） */
export function create(data) {
  return request.post('/admin/notice', data)
}

/** 11.3 编辑公告（标题 1~100 字 / 正文 1~10000 字，后端 @Valid 兜底） */
export function update(id, data) {
  return request.put(`/admin/notice/${id}`, data)
}

/** 11.4 发布（草稿 → 已发布，写入发布时间，前台立即可见） */
export function publish(id) {
  return request.post(`/admin/notice/${id}/publish`)
}

/** 11.5 下架（已发布 → 草稿，前台立即不可见） */
export function offline(id) {
  return request.post(`/admin/notice/${id}/offline`)
}

/** 11.6 删除（逻辑删除，前台/后台列表都不再显示） */
export function remove(id) {
  return request.delete(`/admin/notice/${id}`)
}
