<template>
  <nav class="md:hidden fixed bottom-0 left-0 w-full bg-surface border-t border-outline-variant/30 flex justify-around items-center h-16 z-50 pb-safe">
    <router-link
      v-for="item in navItems"
      :key="item.name"
      :to="item.path"
      :class="[
        'flex flex-col items-center gap-1',
        isActive(item.path) ? 'text-primary' : 'text-on-surface-variant'
      ]"
    >
      <span class="material-symbols-outlined" :class="{ 'fill': isActive(item.path) }">{{ item.icon }}</span>
      <span :class="['font-label-md text-[10px]', isActive(item.path) ? 'font-bold' : '']">{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { name: 'home', path: '/dashboard', label: t('nav.home'), icon: 'home' },
  { name: 'stats', path: '/stats', label: t('nav.stats'), icon: 'analytics' },
  { name: 'learning', path: '/learning', label: t('nav.learning'), icon: 'school' },
  { name: 'profile', path: '/profile', label: t('nav.profile'), icon: 'person' },
]

const isActive = (path: string) => {
  if (path === '/dashboard') {
    return route.path === '/dashboard' || route.path === '/'
  }
  return route.path.startsWith(path)
}
</script>

