<template>
  <div class="acrylic-card" :class="{ 'hover-lift': hover }">
    <div v-if="$slots.header" class="acrylic-header">
      <slot name="header" />
    </div>
    <div class="acrylic-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  hover: { type: Boolean, default: false }
})
</script>

<style scoped>
.acrylic-card {
  background: var(--bg-acrylic);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
  overflow: hidden;
}
.acrylic-card.hover-lift:hover {
  transform: translateY(-2px);
  box-shadow: var(--elevation-2);
}
.acrylic-header {
  margin-bottom: 16px;
}
</style>
