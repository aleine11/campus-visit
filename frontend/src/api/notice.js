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
