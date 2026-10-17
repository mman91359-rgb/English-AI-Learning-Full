import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/userStore'

const routes = [
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/home/DashboardView.vue'), meta: { title: '仪表盘' } },
      { path: 'home', name: 'Home', component: () => import('../views/home/HomeView.vue'), meta: { title: '首页' } },
      { path: 'assess/start', name: 'AssessStart', component: () => import('../views/assess/AssessStartView.vue'), meta: { title: 'AI定级测试' } },
      { path: 'assess/question', name: 'AssessQuestion', component: () => import('../views/assess/AssessQuestionView.vue'), meta: { title: '定级测试' } },
      { path: 'assess/result', name: 'AssessResult', component: () => import('../views/assess/AssessResultView.vue'), meta: { title: '定级结果' } },
      { path: 'exercise', name: 'ExerciseHub', component: () => import('../views/exercise/ExerciseHub.vue'), meta: { title: '练习中心' } },
      { path: 'exercise/question', name: 'ExerciseQuestion', component: () => import('../views/exercise/ExerciseQuestion.vue'), meta: { title: '答题' } },
      { path: 'exercise/error-book', name: 'ErrorBook', component: () => import('../views/exercise/ErrorBook.vue'), meta: { title: '错题本' } },
      { path: 'stats', name: 'Stats', component: () => import('../views/stats/StatsView.vue'), meta: { title: '学习统计' } },
      { path: 'profile', name: 'Profile', component: () => import('../views/profile/ProfileView.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/login', name: 'Login', component: () => import('../views/auth/LoginView.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/auth/RegisterView.vue'), meta: { guest: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  if (to.meta.guest && isLoggedIn) {
    return next('/dashboard')
  }
  if (!to.meta.guest && !isLoggedIn) {
    return next('/login')
  }
  document.title = to.meta.title ? `${to.meta.title} - English AI` : 'English AI'
  next()
})

export default router
