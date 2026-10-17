<template>
  <div class="app-layout">
    <!-- 居中容器：sidebar + 内容作为一个整体 -->
    <div class="layout-container">
      <DesktopSidebar />
      <main class="main-content">
        <div class="content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
    <BottomTab />
  </div>
</template>

<script setup>
import BottomTab from './BottomTab.vue'
import DesktopSidebar from './DesktopSidebar.vue'
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background: var(--bg-page);
}

.main-content {
  padding: 16px;
  min-height: 100vh;
  padding-bottom: calc(var(--bottombar-height) + 16px);
}

.content-wrapper {
  width: 100%;
}

/* 桌面端：sidebar + 内容 整体居中 */
@media (min-width: 768px) {
  .layout-container {
    max-width: calc(var(--max-content-width) + var(--sidebar-width));
    margin: 0 auto;
    position: relative;
    min-height: 100vh;
  }

  .main-content {
    margin-left: var(--sidebar-width);
    padding: 32px 40px;
    min-height: 100vh;
    padding-bottom: 32px;
  }

  .content-wrapper {
    max-width: var(--max-content-width);
    margin: 0 auto;
  }
}

/* 页面过渡 */
.page-enter-active {
  animation: fluentIn var(--duration-slow) var(--ease-out);
}
.page-leave-active {
  animation: fluentIn var(--duration-fast) var(--ease-out) reverse;
}

@keyframes fluentIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
