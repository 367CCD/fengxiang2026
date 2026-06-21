import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('../views/PostDetail.vue'),
    meta: { requiresAuth: false }
  },
  // ═══════════════════════════════════
  // 用户端路由
  // ═══════════════════════════════════
  {
    path: '/',
    component: () => import('../components/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/discover',
    children: [
      { path: 'discover', name: 'Discover', component: () => import('../views/Discover.vue') },
      { path: 'publish', name: 'Publish', component: () => import('../views/Publish.vue') },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/Notifications.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue') },
      { path: 'users', name: 'UserList', component: () => import('../views/UserList.vue') }
    ]
  },
  // ═══════════════════════════════════
  // 管理端路由
  // ═══════════════════════════════════
  {
    path: '/admin',
    component: () => import('../components/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/UserManagement.vue') },
      { path: 'posts', name: 'AdminPosts', component: () => import('../views/admin/PostManagement.vue') },
      { path: 'comments', name: 'AdminComments', component: () => import('../views/admin/CommentManagement.vue') },
      { path: 'reports', name: 'AdminReports', component: () => import('../views/admin/ReportManagement.vue') },
      { path: 'tags', name: 'AdminTags', component: () => import('../views/admin/TagManagement.vue') },
      { path: 'notifications', name: 'AdminNotifications', component: () => import('../views/admin/NotificationMgmt.vue') },
      { path: 'search', name: 'AdminSearch', component: () => import('../views/admin/SearchManagement.vue') },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('forum_token')

  // 1. 需要登录但未登录 → 跳转登录页
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  // 2. 需要管理员权限：检查本地存储的 role
  if (to.meta.requiresAdmin) {
    try {
      const raw = localStorage.getItem('forum_user_info')
      const userInfo = raw && raw !== 'undefined' ? JSON.parse(raw) : null
      if (!userInfo || userInfo.role !== 1) {
        next('/discover')
        return
      }
    } catch {
      next('/login')
      return
    }
  }

  next()
})

export default router
