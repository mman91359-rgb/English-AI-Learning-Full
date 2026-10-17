<template>
  <div class="ring-wrapper" :style="{ width: size + 'px', height: size + 'px' }">
    <svg viewBox="0 0 120 120" class="ring-svg">
      <circle
        cx="60" cy="60" :r="radius"
        fill="none"
        stroke="var(--bg-subtle)"
        :stroke-width="strokeWidth"
      />
      <circle
        cx="60" cy="60" :r="radius"
        fill="none"
        :stroke="ringColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        class="ring-progress"
        transform="rotate(-90 60 60)"
      />
    </svg>
    <div class="ring-content">
      <span class="ring-score" :style="{ fontSize: scoreFontSize }">{{ displayScore }}</span>
      <span class="ring-label">{{ label }}</span>
      <span v-if="level" class="ring-level" :style="{ color: ringColor }">{{ level }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, default: 0 },
  min: { type: Number, default: 0 },
  max: { type: Number, default: 2000 },
  label: { type: String, default: 'ELO分' },
  level: { type: String, default: '' },
  size: { type: Number, default: 140 },
  strokeWidth: { type: Number, default: 8 }
})

const radius = 52
const circumference = 2 * Math.PI * radius
const progress = computed(() => Math.min(1, Math.max(0, (props.score - props.min) / (props.max - props.min))))
const dashOffset = computed(() => circumference * (1 - progress.value))

const ringColor = computed(() => {
  const p = progress.value
  if (p >= 0.7) return 'var(--color-success)'
  if (p >= 0.3) return 'var(--color-accent)'
  return 'var(--color-primary)'
})

const displayScore = computed(() => typeof props.score === 'number' ? Math.round(props.score) : props.score)
const scoreFontSize = computed(() => props.size > 100 ? '26px' : '20px')
</script>

<style scoped>
.ring-wrapper {
  position: relative;
  margin: 0 auto;
}
.ring-svg { width: 100%; height: 100%; display: block; }
.ring-progress {
  transition: stroke-dashoffset 0.8s var(--ease-out);
}
.ring-content {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.ring-score {
  font-family: var(--font-display);
  font-weight: 700;
  color: var(--text-primary);
}
.ring-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}
.ring-level {
  font-size: var(--text-sm);
  font-weight: 600;
  margin-top: 2px;
}
</style>
