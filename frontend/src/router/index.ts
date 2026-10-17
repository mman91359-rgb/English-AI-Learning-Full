import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import HomeView from '../views/HomeView.vue'
import LearningView from '../views/LearningView.vue'
import StatsView from '../views/StatsView.vue'
import ProfileView from '../views/ProfileView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import SettingsView from '../views/SettingsView.vue'
import FavoritesView from '../views/FavoritesView.vue'
import ErrorBookView from '../views/ErrorBookView.vue'
import HistoryView from '../views/HistoryView.vue'
import VocabularyView from '../views/VocabularyView.vue'
import PracticeView from '../views/PracticeView.vue'
import VocabQuizView from '../views/VocabQuizView.vue'
import GrammarQuizView from '../views/GrammarQuizView.vue'
import AssessView from '../views/AssessView.vue'
import AdminQuestions from '../views/AdminQuestions.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      redirect: () => {
        const userStore = useUserStore()
        return userStore.isLoggedIn ? '/dashboard' : '/login'
      },
    },
    {
      path: '/dashboard',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/learning',
      name: 'learning',
      component: LearningView,
      meta: { requiresAuth: true },
    },
    {
      path: '/stats',
      name: 'stats',
      component: StatsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/privacy',
      name: 'settings-privacy',
      component: SettingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/account',
      name: 'settings-account',
      component: SettingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/notifications',
      name: 'settings-notifications',
      component: SettingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/assess',
      name: 'assess',
      component: AssessView,
      meta: { requiresAuth: true },
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: FavoritesView,
      meta: { requiresAuth: true },
    },
    {
      path: '/error-book',
      name: 'error-book',
      component: ErrorBookView,
      meta: { requiresAuth: true },
    },
    {
      path: '/learning/practice',
      name: 'practice',
      component: PracticeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/history',
      name: 'history',
      component: HistoryView,
      meta: { requiresAuth: true },
    },
    {
      path: '/vocabulary',
      name: 'vocabulary',
      component: VocabularyView,
      meta: { requiresAuth: true },
    },
    {
      path: '/vocab-quiz',
      name: 'vocab-quiz',
      component: VocabQuizView,
      meta: { requiresAuth: true },
    },
    {
      path: '/grammar-quiz',
      name: 'grammar-quiz',
      component: GrammarQuizView,
      meta: { requiresAuth: true },
    },
    {
      path: '/logout',
      name: 'logout',
      component: LoginView,
      meta: { requiresAuth: true },
      beforeEnter() {
        const userStore = useUserStore()
        userStore.logout()
        return { path: '/login' }
      },
    },
    {
      path: '/admin/questions',
      name: 'admin-questions',
      component: AdminQuestions,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 未登录 + 需要登录 → 强制跳转登录页
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录 + 访问公开页面（如 /login、/register）→ 跳到首页
  if (!to.meta.requiresAuth && userStore.isLoggedIn) {
    next({ path: '/dashboard' })
    return
  }

  next()
})

export default router


