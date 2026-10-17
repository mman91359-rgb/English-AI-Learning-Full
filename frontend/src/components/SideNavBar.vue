<template>
  <aside class="hidden md:flex flex-col h-screen fixed left-0 top-0 w-sidebar-width bg-surface border-r border-outline-variant py-8 px-4 z-50">
    <div class="mb-10 px-4">
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-primary text-3xl">school</span>
        <h1 class="text-headline-md font-headline-md font-bold text-primary">{{ $t('app.title') }}</h1>
      </div>
      <div class="mt-2 flex flex-col">
        <span class="text-label-md font-label-md text-on-surface-variant">{{ $t('dashboard.streakDays') }}</span>
        <span class="text-headline-md font-headline-md text-primary font-bold">{{ streakDays }} {{ $t('stats.days') }}<span v-if="streakDays >= 3" class="ml-1">🔥</span></span>
      </div>
    </div>
    <nav class="flex-1 flex flex-col gap-2">
      <router-link
        v-for="item in mainNavItems"
        :key="item.name"
        :to="item.path"
        :class="[
          'flex items-center gap-4 px-4 py-3 rounded-xl transition-colors',
          isActive(item.path)
            ? 'bg-primary-container/10 text-primary border-l-2 border-primary font-bold'
            : 'text-on-surface-variant hover:bg-primary-container/10 hover:text-primary'
        ]"
      >
        <span class="material-symbols-outlined" :class="{ 'fill': isActive(item.path) }">{{ item.icon }}</span>
        <span class="font-label-lg text-label-lg">{{ item.label }}</span>
      </router-link>
    </nav>
    <div class="mt-auto flex flex-col gap-2">
      <router-link
        v-for="item in bottomNavItems"
        :key="item.name"
        :to="item.path"
        :class="[
          'flex items-center gap-4 px-4 py-3 rounded-xl transition-colors',
          isActive(item.path)
            ? 'bg-primary-container/10 text-primary border-l-2 border-primary font-bold'
            : 'text-on-surface-variant hover:bg-primary-container/10 hover:text-primary'
        ]"
      >
        <span class="material-symbols-outlined">{{ item.icon }}</span>
        <span class="font-label-lg text-label-lg">{{ item.label }}</span>
      </router-link>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const route = useRoute()
const userStore = useUserStore()

// 连续学习天数：从 userStore 读取后端真实数据，无回退硬编码
const streakDays = computed(() => userStore.streakDays)

// 组件挂载时，如果 store 中还没有统计数据则加载一次
onMounted(() => {
  if (userStore.userId && userStore.streakDays === 0) {
    userStore.loadUserStats()
  }
})

const mainNavItems = computed(() => [
  { name: 'home', path: '/dashboard', label: t('nav.home'), icon: 'home' },
  { name: 'stats', path: '/stats', label: t('nav.stats'), icon: 'analytics' },
  { name: 'learning', path: '/learning', label: t('nav.learning'), icon: 'school' },
  { name: 'profile', path: '/profile', label: t('nav.profile'), icon: 'person' },
])

const bottomNavItems = computed(() => [
  { name: 'settings', path: '/settings', label: t('nav.settings'), icon: 'settings' },
  { name: 'logout', path: '/logout', label: t('nav.logout'), icon: 'logout' },
])

const isActive = (path: string) => {
  if (path === '/dashboard') {
    return route.path === '/dashboard' || route.path === '/'
  }
  if (path === '/learning') {
    return route.path === '/learning'
      || route.path.startsWith('/learning/practice')
      || route.path === '/vocab-quiz'
      || route.path === '/grammar-quiz'
  }
  return route.path.startsWith(path)
}
</script>
