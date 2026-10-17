<template>
  <div class="stat-tile">
    <div class="tile-icon" :style="{ background: iconBg, color: iconColor }">
      <span>{{ icon }}</span>
    </div>
    <div class="tile-info">
      <span class="tile-value">{{ value }}</span>
      <span class="tile-label">{{ label }}</span>
      <span v-if="trend !== null && trend !== undefined" class="tile-trend" :class="trend >= 0 ? 'up' : 'down'">
        {{ trend >= 0 ? '↑' : '↓' }} {{ Math.abs(trend) }}%
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, default: '📊' },
  value: { type: [String, Number], default: '--' },
  label: { type: String, default: '' },
  trend: { type: Number, default: null },
  color: { type: String, default: 'indigo' }
})

const colorMap = {
  indigo: { bg: 'var(--color-primary-bg)', color: 'var(--color-primary)' },
  blue:   { bg: 'var(--color-accent-bg)', color: 'var(--color-accent)' },
  green:  { bg: 'var(--color-success-bg)', color: 'var(--color-success)' },
  amber:  { bg: 'var(--color-warning-bg)', color: 'var(--color-warning)' },
  rose:   { bg: 'var(--color-error-bg)', color: 'var(--color-error)' }
}

const iconBg = computed(() => (colorMap[props.color] || colorMap.indigo).bg)
const iconColor = computed(() => (colorMap[props.color] || colorMap.indigo).color)
</script>

<style scoped>
.stat-tile {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.stat-tile:hover {
  transform: translateY(-2px);
  box-shadow: var(--elevation-2);
}
.tile-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22px;
}
.tile-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.tile-value {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}
.tile-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}
.tile-trend {
  font-size: var(--text-xs);
  font-weight: 600;
  margin-top: 2px;
}
.tile-trend.up { color: var(--color-success); }
.tile-trend.down { color: var(--color-error); }
</style>
