<template>
  <nav class="bottom-tab">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      class="tab-item"
      :class="{ active: isActive(tab.path) }"
    >
      <span class="tab-icon">{{ tab.icon }}</span>
      <span class="tab-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
function isActive(path) { return route.path === path || route.path.startsWith(path + '/') }

const tabs = [
  { path: '/dashboard', icon: '📊', label: '仪表盘' },
  { path: '/exercise', icon: '📝', label: '练习' },
  { path: '/stats', icon: '📈', label: '统计' },
  { path: '/profile', icon: '👤', label: '我的' },
]
</script>

<style scoped>
.bottom-tab {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--bottombar-height);
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: var(--bg-acrylic);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-top: 1px solid var(--border-light);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 100;
}

@media (min-width: 768px) {
  .bottom-tab { display: none; }
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 18px;
  border-radius: var(--radius-md);
  color: var(--text-tertiary);
  transition: all var(--duration-fast) var(--ease-out);
  text-decoration: none;
  position: relative;
}
.tab-item.active {
  color: var(--color-primary);
}
.tab-item.active::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 0 0 3px 3px;
}
.tab-icon { font-size: 22px; }
.tab-label { font-size: 11px; font-weight: 500; }
</style>
