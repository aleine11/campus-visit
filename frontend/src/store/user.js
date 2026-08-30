/**
 * 用户登录态全局状态（Pinia）
 *
 * 为什么用 Pinia 而不是各页面自己读 localStorage？
 *   登录态被十几处代码使用（导航栏显示、路由守卫、AI 窗判断……），
 *   Pinia 提供"一处修改、处处响应"的响应式共享，改了 realName 导航栏立刻变。
 *
 * 持久化策略：token 和用户信息同时存 localStorage，
 * 刷新页面后 store 从 localStorage 恢复（SPA 内存会丢，localStorage 不会）。
 */
import { defineStore } from 'pinia'

const TOKEN_KEY = 'campus_token'
const USER_KEY = 'campus_user'

/** 从 localStorage 恢复用户信息（JSON 解析失败返回空对象，别让坏数据崩页面） */
function loadLocalUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || {}
  } catch {
    return {}
  }
}

export const useUserStore = defineStore('user', {
  state: () => {
    const saved = loadLocalUser()
    return {
      token: localStorage.getItem(TOKEN_KEY) || '',
      userId: saved.userId || null,
      role: saved.role || '', // visitor / admin / ''（未登录）
      realName: saved.realName || '',
      isSuper: saved.isSuper || false,
    }
  },

  getters: {
    /** 是否已登录（有 token 即视为已登录；token 过期由后端 40101 兜底） */
    isLoggedIn: (s) => !!s.token,
    /** 是否访客 */
    isVisitor: (s) => s.isLoggedIn && s.role === 'visitor',
    /** 是否管理员 */
    isAdmin: (s) => s.isLoggedIn && s.role === 'admin',
  },

  actions: {
    /**
     * 登录成功后写入登录态（data 就是后端 LoginVO：token/role/userId/realName/isSuper）
     */
    setLogin(data) {
      this.token = data.token
      this.userId = data.userId
      this.role = data.role
      this.realName = data.realName || ''
      this.isSuper = !!data.isSuper
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(
        USER_KEY,
        JSON.stringify({ userId: data.userId, role: data.role, realName: data.realName, isSuper: !!data.isSuper })
      )
    },

    /**
     * 修改个人信息后同步姓名（个人中心用）
     */
    updateRealName(realName) {
      this.realName = realName
      const saved = loadLocalUser()
      saved.realName = realName
      localStorage.setItem(USER_KEY, JSON.stringify(saved))
    },

    /**
     * 登出：清空内存态 + localStorage（40101 拦截器和手动退出共用）
     */
    logout() {
      this.token = ''
      this.userId = null
      this.role = ''
      this.realName = ''
      this.isSuper = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
