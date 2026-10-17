<template>
  <Teleport to="body">
    <div class="fixed top-24 right-4 z-[9999] flex flex-col gap-3 pointer-events-none w-80">
      <TransitionGroup name="toast">
        <div
          v-for="t in toasts"
          :key="t.id"
          :class="[
            'pointer-events-auto flex items-start gap-3 p-4 rounded-2xl shadow-lg border backdrop-blur-md transition-all',
            bgClass(t.type)
          ]"
        >
          <span class="material-symbols-outlined text-lg shrink-0" :class="iconClass(t.type)">
            {{ iconName(t.type) }}
          </span>
          <p class="flex-1 font-body-sm text-body-sm">{{ t.message }}</p>
          <button class="shrink-0 opacity-60 hover:opacity-100 transition-opacity" @click="remove(t.id)">
            <span class="material-symbols-outlined text-base">close</span>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useToast } from '@/composables/useToast'

const { toasts, remove } = useToast()

const iconName = (type: string) => {
  switch (type) {
    case 'success': return 'check_circle'
    case 'error': return 'error'
    case 'warning': return 'warning'
    default: return 'info'
  }
}

const bgClass = computed(() => (type: string) => {
  switch (type) {
    case 'success': return 'bg-green-50 border-green-200 text-green-800'
    case 'error': return 'bg-red-50 border-red-200 text-red-800'
    case 'warning': return 'bg-yellow-50 border-yellow-200 text-yellow-800'
    default: return 'bg-blue-50 border-blue-200 text-blue-800'
  }
})

const iconClass = computed(() => (type: string) => {
  switch (type) {
    case 'success': return 'text-green-600'
    case 'error': return 'text-red-600'
    case 'warning': return 'text-yellow-600'
    default: return 'text-blue-600'
  }
})
</script>

<style scoped>
.toast-enter-active { transition: all 0.3s ease-out; }
.toast-leave-active { transition: all 0.2s ease-in; }
.toast-enter-from { opacity: 0; transform: translateX(40px); }
.toast-leave-to { opacity: 0; transform: translateX(40px); }
</style>
