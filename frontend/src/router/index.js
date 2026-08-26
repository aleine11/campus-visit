/**
 * 路由配置
 *
 * 当前为初始版本，仅含登录页和首页占位
 * Stage2 开发各模块时会逐步补全路由
 */
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/visitor/home',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/visitor',
    component: () => import('@/views/visitor/Layout.vue'),
    children: [
      {
        path: 'home',
        name: 'VisitorHome',
        component: () => import('@/views/visitor/Home.vue'),
        meta: { title: '校园首页' },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    children: [
      {
        path: '',
        name: 'AdminHome',
        component: () => import('@/views/admin/Home.vue'),
        meta: { title: '管理后台', requiresAuth: true, role: 'admin' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：检查登录和权限（待 Stage2 实现）
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园参观预约与智能咨询系统` : '校园参观预约与智能咨询系统'
  next()
})

export default router
