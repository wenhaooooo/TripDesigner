import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
// 路由配置。
// 使用 createWebHistory 模式（HTML5 History API）。
// 路由守卫：
// - requiresAuth: 需要登录，未登录重定向到 /login
// - requiresGuest: 仅游客访问，已登录重定向到 /


const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { requiresGuest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/auth/RegisterPage.vue'),
    meta: { requiresGuest: true },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/pages/dashboard/DashboardPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/trips/:id',
    name: 'TripDetail',
    component: () => import('@/pages/trip/TripDetailPage.vue'),
    meta: { requiresAuth: true },
    props: true,
  },
  {
    path: '/trips/:id/edit',
    name: 'TripEdit',
    component: () => import('@/pages/trip/TripDetailPage.vue'),
    meta: { requiresAuth: true, editMode: true },
    props: (route) => ({ id: route.params.id, editMode: true }),
  },
  {
    path: '/ai/generate',
    name: 'AiGenerate',
    component: () => import('@/pages/chat/AiGeneratePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/chat/:id',
    name: 'Chat',
    component: () => import('@/pages/chat/ChatPage.vue'),
    meta: { requiresAuth: true },
    props: true,
  },
  {
    path: '/experiences',
    name: 'Experiences',
    component: () => import('@/pages/memory/ExperiencesPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/memory/preferences',
    name: 'Preferences',
    component: () => import('@/pages/memory/PreferencesPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/price-monitors',
    name: 'PriceMonitors',
    component: () => import('@/pages/price/PriceMonitorPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/advisor',
    name: 'Advisor',
    component: () => import('@/pages/advisor/AdvisorPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/shared/:token',
    name: 'SharedTrip',
    component: () => import('@/pages/trip/SharedTripPage.vue'),
    meta: { requiresAuth: false },
  },
  // ========== P1-P3 新增页面 ==========
  {
    path: '/multimodal',
    name: 'Multimodal',
    component: () => import('@/pages/multimodal/MultimodalPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/pages/community/CommunityPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/community/:id',
    name: 'CommunityPostDetail',
    component: () => import('@/pages/community/PostDetailPage.vue'),
    meta: { requiresAuth: true },
    props: true,
  },
  {
    path: '/teams',
    name: 'Teams',
    component: () => import('@/pages/team/TeamPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/teams/:id',
    name: 'TeamDetail',
    component: () => import('@/pages/team/TeamDetailPage.vue'),
    meta: { requiresAuth: true },
    props: true,
  },
  {
    path: '/checkins',
    name: 'Checkins',
    component: () => import('@/pages/checkin/CheckinPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/pages/statistics/StatisticsPage.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresGuest && auth.isAuthenticated) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
