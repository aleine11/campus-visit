/**
 * 访客用户管理接口（管理员后台用，对标 architecture.md 模块 6 后台部分，全部 🔒 admin）
 *   GET  /admin/visitor/page            访客分页（keyword 一词三搜：用户名/姓名/手机号）
 *   POST /admin/visitor/{id}/freeze     冻结访客（冻结后该访客无法登录 → 40012）
 *   POST /admin/visitor/{id}/unfreeze   解冻访客（恢复登录）
 *
 * 注：访客自己的 PUT /visitor/profile 在 api/auth.js（updateProfile），不重复封装
 */
import request from '@/utils/request'

/**
 * 6.1 访客分页
 * @param {Object} params { keyword, status, current, size } 均可选
 */
export function pageForAdmin(params) {
  return request.get('/admin/visitor/page', { params })
}

/** 6.2 冻结（重复冻结 → 后端 40022，前端按当前状态切换按钮双保险） */
export function freeze(id) {
  return request.post(`/admin/visitor/${id}/freeze`)
}

/** 6.3 解冻（重复解冻 → 后端 40022） */
export function unfreeze(id) {
  return request.post(`/admin/visitor/${id}/unfreeze`)
}
