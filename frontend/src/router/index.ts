import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { TOKEN_KEY } from '@/api/request'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/Register.vue')
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '资产概览' }
        },
        {
          path: 'trading',
          name: 'trading',
          component: () => import('@/views/Trading.vue'),
          meta: { title: '交易下单' }
        },
        {
          path: 'portfolio',
          name: 'portfolio',
          component: () => import('@/views/Portfolio.vue'),
          meta: { title: '我的持仓' }
        }
      ]
    },
    // 兜底：未匹配的地址回首页
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const loggedIn = Boolean(localStorage.getItem(TOKEN_KEY))

  if (to.path === '/login' || to.path === '/register') {
    // 已登录还想去登录页，直接送回首页
    return loggedIn ? { path: '/dashboard' } : true
  }
  return loggedIn ? true : { path: '/login', query: { redirect: to.fullPath } }
})

export default router
