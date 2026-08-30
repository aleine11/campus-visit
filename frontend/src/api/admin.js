/**
 * 管理员账号管理接口（对标 architecture.md 模块 7，全部 🔒 超管 superOnly）
 *   GET  /admin/admin/page                    管理员分页
 *   POST /admin/admin                         新增管理员
 *   POST /admin/admin/{id}/reset-password     重置密码
 *
 * 非超管调用 → 后端 40301；前端页面 + 菜单也按 isSuper 隐藏（双保险）
 */
import request from '@/utils/request'

/**
 * 7.1 管理员分页
 * @param {Object} params { keyword, current, size } 均可选
 */
export function page(params) {
  return request.get('/admin/admin/page', { params })
}

/**
 * 7.2 新增管理员（AdminSaveDTO：username/password/realName）
 * 校验对齐后端：账号 4~20 位字母数字下划线唯一 / 密码 6~20 位含字母数字 / 姓名 2~10 字
 */
export function create(data) {
  return request.post('/admin/admin', data)
}

/**
 * 7.3 重置密码（ResetPasswordDTO：newPassword，6~20 位含字母数字）
 * 重置后对方下次登录用新密码（后端 BCrypt 加密存储）
 */
export function resetPassword(id, newPassword) {
  return request.post(`/admin/admin/${id}/reset-password`, { newPassword })
}
