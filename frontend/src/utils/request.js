/**
 * Axios 请求封装
 *
 * 功能：
 *  1. 统一 baseURL：/api
 *  2. 请求拦截器：自动加上 JWT token
 *  3. 响应拦截器：统一处理 Result 返回格式
 *  4. 错误处理：401 跳登录、其他错误弹 ElMessage
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api', // 走 vite 代理到后端 8088
  timeout: 30000, // 默认 30 秒；AI 问答接口单独设置更长
})

// 请求拦截：加 token
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

// 响应拦截：拆 Result 外壳
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data // 成功直接返回 data
    } else {
      ElMessage.error(res.msg || '操作失败')
      // 401 未登录，跳登录页
      if (res.code === 401) {
        localStorage.removeItem('campus_token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    }
  },
  (error) => {
    // 网络错误或超时
    let msg = '网络异常'
    if (error.message.includes('timeout')) msg = '请求超时'
    if (error.response?.status === 404) msg = '接口不存在'
    if (error.response?.status === 500) msg = '服务器异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
