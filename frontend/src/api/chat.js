/**
 * AI 咨询接口（对标 architecture.md 模块 9，全部需要登录）
 *   POST   /chat/ask                  提问（大模型生成较慢，单独放宽超时到 60 秒）
 *   GET    /chat/session/my           我的会话分页（current/size，最新会话在前）
 *   GET    /chat/session/{id}/messages 会话消息分页
 *   POST   /chat/session/new          新建空会话（返回新会话 ID）
 *   DELETE /chat/session/my/{id}      清空指定会话
 */
import request from '@/utils/request'

// 提问要等 BGE 向量化 + Milvus 检索 + qwen-plus 生成，比普通接口慢，超时放宽到 60 秒
export function ask(data) {
  return request.post('/chat/ask', data, { timeout: 60000 })
}

export function pageMySessions(params) {
  return request.get('/chat/session/my', { params })
}

export function pageMessages(sessionId, params) {
  return request.get(`/chat/session/${sessionId}/messages`, { params })
}

export function newSession() {
  return request.post('/chat/session/new')
}

export function clearSession(sessionId) {
  return request.delete(`/chat/session/my/${sessionId}`)
}
