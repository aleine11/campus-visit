/**
 * 统计接口（管理员后台用，模块 12 会用到；模块 11 先建好文件占位对标）
 *   GET /admin/stats/dashboard    后台看板（6 块数据）
 *   GET /admin/stats/chat-log/page  问答日志分页（visitorId/keyword/startDate/endDate）
 *   GET /admin/stats/hot-keywords   高频问题统计（days 默认 30）
 */
import request from '@/utils/request'

export function dashboard() {
  return request.get('/admin/stats/dashboard')
}

export function pageChatLogs(params) {
  return request.get('/admin/stats/chat-log/page', { params })
}

export function hotKeywords(days) {
  return request.get('/admin/stats/hot-keywords', { params: { days } })
}
