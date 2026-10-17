<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="avatar-circle">
        <span>{{ avatarText }}</span>
      </div>
      <h2>{{ userStore.username }}</h2>
      <p class="user-detail">目标: {{ userStore.cetLevel || 'CET4' }}</p>
      <p class="user-detail">ELO: {{ userStore.eloScore }} · {{ userStore.eloLevel }}</p>
    </div>

    <div class="menu-list">
      <div class="menu-item" @click="$router.push('/exercise/error-book')">
        <span>📕</span><span>我的错题本</span><span class="menu-arrow">→</span>
      </div>
      <div class="menu-item" @click="$router.push('/stats')">
        <span>📈</span><span>学习统计</span><span class="menu-arrow">→</span>
      </div>
      <div class="menu-item" @click="$router.push('/assess/start')">
        <span>🔄</span><span>重新定级测试</span><span class="menu-arrow">→</span>
      </div>
    </div>

    <button class="btn btn-block btn-danger" @click="handleLogout">退出登录</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const userStore = useUserStore()

const avatarText = computed(() => (userStore.username || '?')[0].toUpperCase())

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-page { padding: 20px 0; }
.profile-card {
  text-align: center;
  padding: 32px 24px;
  margin-bottom: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--elevation-1);
}
.avatar-circle {
  width: 72px; height: 72px; border-radius: 50%;
  background: var(--color-primary);
  color: #fff; font-size: var(--text-2xl); font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 12px;
  box-shadow: 0 4px 14px rgba(67, 97, 238, 0.3);
}
.profile-card h2 { font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; color: var(--text-primary); }
.user-detail { font-size: var(--text-sm); color: var(--text-secondary); margin-top: 2px; }

.menu-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 24px; }
.menu-item {
  display: flex; align-items: center; gap: 12px; padding: 16px;
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.menu-item:hover { transform: translateX(4px); box-shadow: var(--elevation-2); }
.menu-item:active { transform: scale(0.98); }
.menu-item span:first-child { font-size: 22px; }
.menu-item span:nth-child(2) { flex: 1; font-weight: 500; color: var(--text-primary); }
.menu-arrow { color: var(--text-tertiary); }

@media (min-width: 768px) {
  .profile-page { padding: 20px 0 40px; max-width: 600px; }
  .profile-card { padding: 40px 28px; }
  .avatar-circle { width: 88px; height: 88px; font-size: var(--text-3xl); }
  .profile-card h2 { font-size: var(--text-2xl); }
}
</style>
