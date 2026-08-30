/**
 * Axios 请求封装（模块 11 重写版）
 *
 * 功能：
 *  1. 统一 baseURL：/api（走 vite 代理到后端 8088）
 *  2. 请求拦截器：自动带上 JWT token（Authorization: Bearer xxx）
 *  3. 响应拦截器：拆 Result 外壳（后端统一返回 {code, message, data}）
 *  4. 错误处理：
 *     - 40101 未登录/token 过期 → 清登录态 + 跳登录页
 *     - 40301 无权限 → 跳 403 页
 *     - 网络/超时/500 → ElMessage 提示
 *
 * ⚠️ 本项目业务错误都是 HTTP 200 + body.code≠200（模块 9/10 实测确认），
 *    所以错误判断基于 res.code 而不是 HTTP 状态码。
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000, // 默认 30 秒；AI 问答接口在 api/chat.js 里单独放宽到 60 秒
})

// 请求拦截：自动加 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('campus_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：拆 Result 外壳 + 统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      // 成功：直接返回业务数据 data，页面代码里少一层 .data 取值
      return res.data
    }
    // 业务失败：按后端 ResultCode 分段处理
    ElMessage.error(res.message || '操作失败')
    if (res.code === 40101) {
      // 未登录 / token 过期：清登录态，踢回登录页
      useUserStore().logout()
      router.push('/login')
    } else if (res.code === 40301) {
      // 无权限：跳 403 页
      router.push('/forbidden')
    }
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    // 网络层错误（超时 / 404 / 5xx / 断网）
    let msg = '网络异常，请稍后重试'
    if (error.message.includes('timeout')) msg = '请求超时，请稍后重试'
    else if (error.response?.status === 404) msg = '接口不存在'
    else if (error.response?.status >= 500) msg = '服务器异常，请稍后重试'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
