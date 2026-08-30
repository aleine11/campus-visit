/**
 * 路由配置（模块 11 全量版）
 *
 * 路由 meta 约定：
 *   title          浏览器标签页标题
 *   requiresAuth   需要登录（未登录 → /login 并带 redirect 参数）
 *   role           需要的角色（'visitor' / 'admin'，不匹配 → /forbidden）
 *
 * 访客前台 12 页中 5 页需要登录：预约提交 / 我的预约 / 预约详情 / AI 历史 / 个人中心
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  // ===== 根路径：按登录态分流（未登录去首页，访客去首页，管理员去后台） =====
  {
    path: '/',
    redirect: () => {
      const store = useUserStore()
      return store.isAdmin ? '/admin' : '/visitor/home'
    },
  },

  // ===== 公共页 =====
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/forbidden',
    name: 'Forbidden',
    component: () => import('@/views/common/Forbidden.vue'),
    meta: { title: '无权限' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: { title: '页面不存在' },
  },

  // ===== 访客前台（Layout 是带顶部导航 + AI 悬浮窗的整体框架） =====
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
      {
        path: 'notice',
        name: 'NoticeList',
        component: () => import('@/views/visitor/NoticeList.vue'),
        meta: { title: '校园公告' },
      },
      {
        path: 'notice/:id',
        name: 'NoticeDetail',
        component: () => import('@/views/visitor/NoticeDetail.vue'),
        meta: { title: '公告详情' },
      },
      {
        path: 'session',
        name: 'SessionList',
        component: () => import('@/views/visitor/SessionList.vue'),
        meta: { title: '预约参观' },
      },
      {
        path: 'reservation/submit/:sessionId',
        name: 'ReservationSubmit',
        component: () => import('@/views/visitor/ReservationSubmit.vue'),
        meta: { title: '提交预约', requiresAuth: true, role: 'visitor' },
      },
      {
        path: 'reservation/list',
        name: 'MyReservations',
        component: () => import('@/views/visitor/MyReservations.vue'),
        meta: { title: '我的预约', requiresAuth: true, role: 'visitor' },
      },
      {
        path: 'reservation/:id',
        name: 'ReservationDetail',
        component: () => import('@/views/visitor/ReservationDetail.vue'),
        meta: { title: '预约详情', requiresAuth: true, role: 'visitor' },
      },
      {
        path: 'chat/history',
        name: 'ChatHistory',
        component: () => import('@/views/visitor/ChatHistory.vue'),
        meta: { title: 'AI 历史会话', requiresAuth: true, role: 'visitor' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/visitor/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true, role: 'visitor' },
      },
    ],
  },

  // ===== 管理员后台（模块 12 实现页面，路由先占位对标设计文档） =====
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: '',
        name: 'AdminHome',
        component: () => import('@/views/admin/Home.vue'),
        meta: { title: '管理后台', requiresAuth: true, role: 'admin' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 切页后回到顶部（浏览器默认会保留滚动位置，长列表跳详情体验差）
  scrollBehavior() {
    return { top: 0 }
  },
})

// 全局前置守卫：登录校验 + 角色校验（对标 frontend-prototype.md 7.2）
router.beforeEach((to) => {
  // 标签页标题
  document.title = to.meta.title
    ? `${to.meta.title} - 校园参观预约与智能咨询系统`
    : '校园参观预约与智能咨询系统'

  const store = useUserStore()

  // ① 需要登录但没登录 → 去登录页，记住原目标（登录成功后跳回来）
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // ② 角色不匹配 → 403 页（访客进后台、管理员进访客专属页都算越权）
  if (to.meta.role && store.isLoggedIn && store.role !== to.meta.role) {
    return { path: '/forbidden' }
  }

  // ③ 已登录用户再访问 /login /register → 直接送回对应首页（没有重复登录的意义）
  if ((to.path === '/login' || to.path === '/register') && store.isLoggedIn) {
    return store.isAdmin ? '/admin' : '/visitor/home'
  }

  // ④ 其余情况放行
  return true
})

export default router
