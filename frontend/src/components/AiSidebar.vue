<template>
  <!-- overlay 模式：全局浮层（Teleport到body），页面有 inline 时自动隐藏 -->
  <template v-if="mode === 'overlay'">
    <template v-if="!aiStore.inlineMode">
      <Teleport to="body">
        <Transition name="sidebar">
          <div v-if="aiStore.isOpen" class="fixed top-0 right-0 h-full w-[400px] max-w-[90vw] bg-surface-container-lowest shadow-2xl z-50 flex flex-col border-l border-outline-variant">
            <AiSidebarContent @close="aiStore.close()" />
          </div>
        </Transition>
        <!-- 遮罩（移动端显示） -->
        <Transition name="fade">
          <div
            v-if="aiStore.isOpen"
            class="fixed inset-0 bg-black/20 z-40 md:hidden"
            @click="aiStore.close()"
          ></div>
        </Transition>
      </Teleport>
    </template>
  </template>

  <!-- inline 模式：内嵌到页面布局中，不覆盖 -->
  <template v-else>
    <Transition name="sidebar-inline">
      <div v-if="aiStore.isOpen" class="h-full w-[400px] flex-shrink-0 bg-surface-container-lowest border-l border-outline-variant flex flex-col">
        <AiSidebarContent @close="aiStore.close()" />
      </div>
    </Transition>
  </template>
</template>

<script setup lang="ts">
import AiSidebarContent from './AiSidebarContent.vue'
import { useAiStore } from '@/stores/ai'

withDefaults(defineProps<{
  mode?: 'overlay' | 'inline'
}>(), {
  mode: 'overlay',
})

const aiStore = useAiStore()
</script>

<style scoped>
.sidebar-enter-active,
.sidebar-leave-active {
  transition: transform 0.3s ease;
}
.sidebar-enter-from,
.sidebar-leave-to {
  transform: translateX(100%);
}

.sidebar-inline-enter-active,
.sidebar-inline-leave-active {
  transition: width 0.3s ease, opacity 0.3s ease;
  overflow: hidden;
}
.sidebar-inline-enter-from,
.sidebar-inline-leave-to {
  width: 0 !important;
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
