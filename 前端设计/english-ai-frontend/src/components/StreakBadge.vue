<template>
  <div class="streak-badge" :class="sizeClass">
    <span class="streak-icon">🔥</span>
    <span class="streak-days">{{ days }}</span>
    <span class="streak-label">天</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  days: { type: Number, default: 0 },
  size: { type: String, default: 'md' }
})

const sizeClass = computed(() => `streak-${props.size}`)
</script>

<style scoped>
.streak-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  padding: 6px 16px;
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.streak-sm { padding: 4px 10px; }
.streak-sm .streak-days { font-size: var(--text-sm); }
.streak-lg { padding: 8px 20px; }
.streak-lg .streak-days { font-size: var(--text-xl); }

.streak-icon { font-size: 16px; }
.streak-days {
  font-family: var(--font-display);
  font-weight: 700;
  color: var(--text-primary);
}
.streak-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}
</style>
