<template>
  <div class="radar-wrapper">
    <h3 class="chart-title">{{ title }}</h3>
    <div ref="chartEl" class="chart-container"></div>
    <div v-if="loading" class="chart-skeleton"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: { type: String, default: '能力雷达图' },
  data: { type: Object, default: () => ({}) },
  dimensions: { type: Array, default: () => ['词汇', '语法', '听力', '阅读'] },
  max: { type: Number, default: 100 }
})

const chartEl = ref(null)
const loading = ref(true)
let chart = null

function getValues(d) {
  if (!d) return [0, 0, 0, 0]
  return [
    d.vocabularyPercent ?? d.vocabularyScore ?? 0,
    d.grammarPercent ?? d.grammarScore ?? 0,
    d.listeningPercent ?? d.listeningScore ?? 0,
    d.readingPercent ?? d.readingScore ?? 0
  ]
}

function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value)
  }
  const option = {
    backgroundColor: 'transparent',
    radar: {
      center: ['50%', '55%'],
      radius: '65%',
      indicator: props.dimensions.map(name => ({ name, max: props.max })),
      axisName: { color: '#6B7280', fontSize: 12 },
      axisLine: { lineStyle: { color: 'rgba(0,0,0,0.08)' } },
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.06)' } },
      splitArea: {
        areaStyle: { color: ['rgba(67,97,238,0.03)', 'rgba(67,97,238,0.01)'] }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: getValues(props.data),
        name: '当前能力',
        areaStyle: { color: 'rgba(67,97,238,0.15)' },
        lineStyle: { color: '#4361EE', width: 2 },
        itemStyle: { color: '#4361EE' },
        symbol: 'circle',
        symbolSize: 6
      }]
    }]
  }
  chart.setOption(option, true)
  loading.value = false
}

function handleResize() { chart?.resize() }

onMounted(() => { nextTick(renderChart) })

watch(() => props.data, (val) => {
  if (val && chart) { loading.value = true; nextTick(renderChart) }
}, { deep: true })

window.addEventListener('resize', handleResize)
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); chart?.dispose() })
</script>

<style scoped>
.radar-wrapper {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--elevation-1);
}
.chart-title {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  margin-bottom: 8px;
  text-align: center;
  color: var(--text-secondary);
}
.chart-container { width: 100%; height: 280px; }
.chart-skeleton {
  width: 100%; height: 280px;
  background: linear-gradient(90deg, var(--bg-subtle) 25%, var(--bg-page) 50%, var(--bg-subtle) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-md);
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
