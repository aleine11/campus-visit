/**
 * 登录凭证（token）存取工具
 *
 * localStorage 键名与后端设计文档约定：campus_token
 * 附带 JWT payload 解析工具（用于读取过期时间 exp，前端提前感知过期）
 */

const TOKEN_KEY = 'campus_token'

/** 读 token（没登录返回 null） */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/** 存 token（登录成功时调用） */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 删 token（登出 / 401 时调用） */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 解析 JWT 第二段（payload）为 JSON 对象
 *
 * JWT 结构：header.payload.signature（三段 base64url，用 . 分隔）
 * 浏览器原生 atob 只认标准 base64，JWT 是 base64url（- 和 _ 变体），
 * 所以要先替换字符；再用 escape 技巧处理中文（payload 里存了 realName 等）。
 *
 * @returns {Object|null} payload 对象（含 userId/role/exp 等）；解析失败返回 null
 */
export function parseJwtPayload(token) {
  if (!token) return null
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const jsonStr = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonStr)
  } catch {
    return null
  }
}

/**
 * token 是否已过期（前端本地预判，后端每次请求仍会严格校验）
 *
 * @returns {boolean} true=已过期（或解析不出来），false=仍有效
 */
export function isTokenExpired(token) {
  const payload = parseJwtPayload(token)
  if (!payload?.exp) return true
  // exp 是"秒级"时间戳，Date.now() 是毫秒，要 ×1000
  return payload.exp * 1000 < Date.now()
}
