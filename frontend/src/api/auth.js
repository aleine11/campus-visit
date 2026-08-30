/**
 * 认证接口（对标 architecture.md 模块 1 + 模块 6.4 个人信息）
 *   POST /auth/register        访客注册
 *   POST /auth/login           统一登录（返回 LoginVO：token/role/userId/realName/isSuper）
 *   POST /auth/change-password 修改密码（oldPassword/newPassword）
 *   GET  /auth/profile         当前登录人信息（ProfileVO）
 *   PUT  /visitor/profile      访客改个人信息（只收 realName/phone）
 */
import request from '@/utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function changePassword(data) {
  return request.post('/auth/change-password', data)
}

export function getProfile() {
  return request.get('/auth/profile')
}

export function updateProfile(data) {
  return request.put('/visitor/profile', data)
}
