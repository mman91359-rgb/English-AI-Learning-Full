<template>
  <div class="dashboard">
    <div class="dash-header">
      <div>
        <h1 class="greeting">{{ greetingText }}</h1>
        <p class="greeting-sub">继续保持学习节奏！</p>
      </div>
      <StreakBadge :days="streakDays" size="lg" />
    </div>

    <div class="stat-row">
      <StatTile icon="📝" :value="todayWords + '/50'" label="今日单词" color="indigo" />
      <StatTile icon="⏱️" :value="studyHours + 'h'" label="学习时长" color="blue" />
      <StatTile icon="🎯" :value="masteryRate + '%'" label="掌握率" color="green" :trend="trendValue" />
    </div>

    <div class="chart-row">
      <div class="chart-box">
        <h3 class="chart-title">技能雷达</h3>
        <div ref="radarEl" class="chart-body"></div>
      </div>
      <div class="chart-box">
        <h3 class="chart-title">周学习趋势</h3>
        <div ref="barEl" class="chart-body"></div>
      </div>
    </div>

    <div class="review-section">
      <h3 class="section-title">今日推荐复习</h3>
      <div v-if="reviewTasks.length === 0" class="empty-review">
        <span>🎉</span>
        <p>暂无待复习内容，去练习吧！</p>
      </div>
      <div v-else class="review-scroll">
        <div v-for="task in reviewTasks" :key="task.questionId" class="review-word">
          <span class="review-level" :class="'ml-' + task.masteryLevel">
            {{ masteryLabel(task.masteryLevel) }}
          </span>
          <span class="review-stem">{{ task.stem }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/userStore'
import { useExerciseStore } from '../../store/exerciseStore'
import { getReviewTasks } from '../../api/statsApi'
import { getRadarData } from '../../api/userApi'
import { getQuestions } from '../../api/exerciseApi'
import * as echarts from 'echarts'
import StatTile from '../../components/StatTile.vue'
import StreakBadge from '../../components/StreakBadge.vue'

const router = useRouter()
const userStore = useUserStore()
const exerciseStore = useExerciseStore()

const streakDays = ref(7)
const todayWords = ref(32)
const studyHours = ref(2.5)
const masteryRate = ref(68)
const trendValue = ref(5)
const reviewTasks = ref([])
const radarEl = ref(null)
const barEl = ref(null)
let radarChart = null
let barChart = null

const greetingText = computed(() => {
  const h = new Date().getHours()
  const name = userStore.username || '同学'
  if (h < 12) return `早上好, ${name} 👋`
  if (h < 18) return `下午好, ${name} ☀️`
  return `晚上好, ${name} 🌙`
})

function masteryLabel(level) {
  const labels = { 1: '陌生', 2: '模糊', 3: '熟悉', 4: '熟练', 5: '精通' }
  return labels[level] || '待复习'
}

function initRadarChart() {
  if (!radarEl.value) return
  radarChart = echarts.init(radarEl.value)
  radarChart.setOption({
    backgroundColor: 'transparent',
    radar: {
      center: ['50%', '55%'],
      radius: '60%',
      indicator: [
        { name: '词汇', max: 100 },
        { name: '语法', max: 100 },
        { name: '听力', max: 100 },
        { name: '阅读', max: 100 },
        { name: '写作', max: 100 },
        { name: '翻译', max: 100 }
      ],
      axisName: { color: '#6B7280', fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(0,0,0,0.08)' } },
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.06)' } },
      splitArea: { areaStyle: { color: ['rgba(67,97,238,0.02)', 'rgba(67,97,238,0.04)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [60, 55, 40, 65, 35, 50],
        name: '当前能力',
        areaStyle: { color: 'rgba(67,97,238,0.15)' },
        lineStyle: { color: '#4361EE', width: 2 },
        itemStyle: { color: '#4361EE' },
        symbol: 'circle',
        symbolSize: 6
      }]
    }]
  })
}

function initBarChart() {
  if (!barEl.value) return
  barChart = echarts.init(barEl.value)
  barChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 8, bottom: 24 },
    xAxis: {
      type: 'category',
      data: ['一', '二', '三', '四', '五', '六', '日'],
      axisLine: { lineStyle: { color: 'rgba(0,0,0,0.08)' } },
      axisLabel: { color: '#6B7280' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } },
      axisLabel: { color: '#6B7280' }
    },
    series: [{
      type: 'bar',
      data: [20, 35, 28, 50, 42, 30, 45],
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#4361EE' },
          { offset: 1, color: 'rgba(67,97,238,0.15)' }
        ])
      },
      barWidth: 16
    }]
  })
}

function handleResize() {
  radarChart?.resize()
  barChart?.resize()
}

onMounted(async () => {
  try {
    const res = await getRadarData(userStore.userId)
    if (res.code === 200 && res.data && radarChart) {
      const d = res.data
      radarChart.setOption({
        series: [{
          data: [{
            value: [
              d.vocabularyPercent ?? d.vocabularyScore ?? 0,
              d.grammarPercent ?? d.grammarScore ?? 0,
              d.listeningPercent ?? d.listeningScore ?? 0,
              d.readingPercent ?? d.readingScore ?? 0,
              35, 50
            ]
          }]
        }]
      })
    }
  } catch {}

  try {
    const res = await getReviewTasks(userStore.userId)
    if (res.code === 200) {
      reviewTasks.value = Array.isArray(res.data) ? res.data.slice(0, 10) : []
    }
  } catch {}

  await nextTick()
  initRadarChart()
  initBarChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  radarChart?.dispose()
  barChart?.dispose()
})

async function goReview() {
  try {
    const res = await getQuestions({ userId: userStore.userId, moduleType: 1, count: 10 })
    if (res.code === 200 && res.data?.length) {
      exerciseStore.startExercise(res.data, 1, 'review')
      router.push('/exercise/question')
    }
  } catch {}
}
</script>

<style scoped>
.dashboard { padding-bottom: 20px; }

.dash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.greeting {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
}
.greeting-sub {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-top: 2px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.chart-box {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--elevation-1);
}
.chart-title {
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.chart-body { width: 100%; height: 240px; }

.review-section { margin-bottom: 24px; }
.section-title {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}
.review-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 8px;
  -webkit-overflow-scrolling: touch;
}
.review-scroll::-webkit-scrollbar { height: 4px; }
.review-word {
  min-width: 160px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.review-word:hover { transform: translateY(-2px); box-shadow: var(--elevation-2); }
.review-level {
  font-size: var(--text-xs);
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  align-self: flex-start;
}
.ml-1 { background: var(--color-error-bg); color: var(--color-error); }
.ml-2 { background: var(--color-warning-bg); color: var(--color-warning); }
.ml-3 { background: var(--color-accent-bg); color: var(--color-accent); }
.ml-4 { background: var(--color-primary-bg); color: var(--color-primary); }
.ml-5 { background: var(--color-success-bg); color: var(--color-success); }
.review-stem {
  font-size: var(--text-sm);
  color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-review {
  text-align: center;
  padding: 40px 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}
.empty-review span { font-size: 40px; display: block; margin-bottom: 8px; }
.empty-review p { color: var(--text-tertiary); }

@media (min-width: 768px) {
  .dashboard { max-width: 960px; padding-bottom: 40px; }
  .greeting { font-size: var(--text-3xl); }
  .stat-row { gap: 16px; margin-bottom: 28px; }
  .chart-row {
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin-bottom: 28px;
  }
  .chart-body { height: 280px; }
  .review-word { min-width: 200px; padding: 16px; }
}

@media (max-width: 640px) {
  .stat-row { grid-template-columns: 1fr; }
}
</style>
