<template>
  <aside class="desktop-sidebar">
    <div class="sidebar-logo" @click="$router.push('/dashboard')">
      <span class="logo-icon">🎓</span>
      <span class="logo-text">English AI</span>
    </div>

    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer" v-if="userStore.isLoggedIn">
      <div class="user-card">
        <div class="user-avatar">{{ userStore.username?.charAt(0)?.toUpperCase() }}</div>
        <div class="user-detail">
          <span class="user-name">{{ userStore.username }}</span>
          <span class="user-level">Lv.{{ userStore.eloScore }} · {{ userStore.cetLevel || 'CET4' }}</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/userStore'

const route = useRoute()
const userStore = useUserStore()

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

const navItems = [
  { path: '/dashboard', icon: '📊', label: '仪表盘' },
  { path: '/exercise', icon: '📝', label: '练习中心' },
  { path: '/assess/start', icon: '🎯', label: '能力定级' },
  { path: '/stats', icon: '📈', label: '学习统计' },
  { path: '/exercise/error-book', icon: '📕', label: '错题本' },
  { path: '/profile', icon: '👤', label: '个人中心' },
]
</script>

<style scoped>
.desktop-sidebar { display: none; }

@media (min-width: 768px) {
  .desktop-sidebar {
    display: flex;
    flex-direction: column;
    position: absolute;
    top: 0;
    left: 0;
    width: var(--sidebar-width);
    height: 100vh;
    background: var(--bg-acrylic);
    backdrop-filter: blur(24px) saturate(180%);
    -webkit-backdrop-filter: blur(24px) saturate(180%);
    border-right: 1px solid var(--border-light);
    box-shadow: 1px 0 8px rgba(0, 0, 0, 0.03);
    z-index: 200;
    padding: 24px 14px;
    overflow-y: auto;
  }

  .sidebar-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 10px 20px;
    cursor: pointer;
    border-bottom: 1px solid var(--border-light);
    margin-bottom: 16px;
    user-select: none;
  }
  .logo-icon { font-size: 30px; }
  .logo-text {
    font-family: var(--font-display);
    font-size: var(--text-xl);
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.02em;
  }

  .sidebar-nav {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
  }

  .nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 11px 14px;
    border-radius: var(--radius-md);
    color: var(--text-secondary);
    font-size: var(--text-sm);
    font-weight: 500;
    transition: all var(--duration-fast) var(--ease-out);
    text-decoration: none;
  }
  .nav-item:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }
  .nav-item.active {
    background: var(--color-primary-bg);
    color: var(--color-primary);
    font-weight: 600;
  }
  .nav-icon { font-size: 20px; width: 28px; text-align: center; }
  .nav-label { white-space: nowrap; }

  .sidebar-footer {
    border-top: 1px solid var(--border-light);
    padding-top: 14px;
    margin-top: auto;
  }
  .user-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: var(--radius-md);
    background: var(--bg-subtle);
    transition: background var(--duration-fast);
  }
  .user-card:hover {
    background: var(--bg-hover);
  }
  .user-avatar {
    width: 38px;
    height: 38px;
    border-radius: var(--radius-md);
    background: var(--color-primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: var(--text-sm);
    flex-shrink: 0;
  }
  .user-detail {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .user-name {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .user-level {
    font-size: var(--text-xs);
    color: var(--text-tertiary);
    margin-top: 1px;
  }
}
</style>
