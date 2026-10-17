<template>
  <AppLayout>
    <div class="mb-8">
      <h1 class="text-headline-lg font-headline-lg text-on-surface mb-6">{{ $t('stats.title') }}</h1>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- 左侧：顶部卡片 + 热力图 + 趋势图 -->
      <div class="lg:col-span-8 space-y-6">
        <!-- 顶部统计卡片（3列布局） -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="bg-surface-container-lowest rounded-xl p-6 card-shadow">
            <div class="flex items-center text-on-surface-variant mb-2">
              <span class="material-symbols-outlined mr-2 text-primary text-[20px]">schedule</span>
              <span class="font-label-md text-label-md">{{ $t('stats.todayHours') }}</span>
            </div>
            <div class="text-headline-lg font-headline-lg text-on-surface">
              {{ todayHours }}<span class="text-body-md font-body-md text-on-surface-variant ml-1">{{ $t('stats.hours') }}</span>
              {{ todayMinutes }}<span class="text-body-md font-body-md text-on-surface-variant ml-1">{{ $t('stats.minutes') }}</span>
            </div>
          </div>
          <div class="bg-surface-container-lowest rounded-xl p-6 card-shadow">
            <div class="flex items-center text-on-surface-variant mb-2">
              <span class="material-symbols-outlined mr-2 text-secondary text-[20px]" :class="{ 'opacity-30': streakDays < 3 }">local_fire_department</span>
              <span class="font-label-md text-label-md">{{ $t('stats.streakDays') }}</span>
            </div>
            <div class="text-headline-lg font-headline-lg text-on-surface">
              {{ streakDays }}<span class="text-body-md font-body-md text-on-surface-variant ml-1">{{ $t('stats.days') }}</span>
            </div>
          </div>
          <div class="bg-surface-container-lowest rounded-xl p-6 card-shadow">
            <div class="flex items-center text-on-surface-variant mb-2">
              <span class="material-symbols-outlined mr-2 text-tertiary text-[20px]">calendar_month</span>
              <span class="font-label-md text-label-md">{{ $t('stats.activeDays') }}</span>
            </div>
            <div class="text-headline-lg font-headline-lg text-on-surface">
              {{ activeDays }}<span class="text-body-md font-body-md text-on-surface-variant ml-1">{{ $t('stats.days') }}</span>
            </div>
          </div>
        </div>

        <!-- 年度贡献热力图 -->
        <div class="bg-surface-container-lowest rounded-[24px] p-6 md:p-8 card-shadow">
          <div class="flex flex-wrap justify-between items-center gap-4 mb-6">
            <h3 class="font-headline-md text-headline-md text-on-surface">{{ $t('stats.yearlyHeatmap') }}</h3>
            <div class="flex items-center gap-1 text-label-sm font-label-sm text-on-surface-variant">
              <span>{{ $t('stats.low') }}</span>
              <div
                v-for="level in 5"
                :key="level"
                class="w-3 h-3 rounded-sm"
                :style="{ backgroundColor: heatmapColor(level) }"
                :title="levelMinuteLabels[level]"
              ></div>
              <span>{{ $t('stats.high') }}</span>
            </div>
          </div>

          <div class="overflow-x-auto">
            <div class="inline-flex min-w-[680px]">
              <!-- 星期标签 -->
              <div class="flex flex-col gap-[3px] mr-2 pt-[18px]">
                <span
                  v-for="(day, di) in dayLabels"
                  :key="di"
                  class="text-[10px] leading-[12px] text-on-surface-variant/50 h-[12px]"
                  :style="{ marginTop: di % 2 === 0 ? '0' : '0' }"
                >{{ di % 2 === 1 ? '' : day }}</span>
              </div>

              <div class="flex-1">
                <!-- 月份标签 -->
                <div class="flex text-[10px] leading-[12px] text-on-surface-variant/50 mb-1 h-[12px] relative ml-0">
                  <span
                    v-for="(ml, mi) in monthLabelPositions"
                    :key="mi"
                    class="absolute top-0 whitespace-nowrap"
                    :style="{ left: ml.col * heatmapGrid.colWidth + 'px' }"
                  >{{ ml.label }}</span>
                </div>

                <!-- 热力图网格：7行 × 最多53列 -->
                <div class="grid grid-rows-7 grid-flow-col gap-[3px]">
                  <div
                    v-for="(cell, i) in paddedHeatmap"
                    :key="i"
                    class="w-[12px] h-[12px] rounded-[2px] transition-colors duration-200 border border-surface/30"
                    :class="cell.level > 0 ? 'cursor-pointer hover:ring-2 hover:ring-primary/30' : ''"
                    :style="{ backgroundColor: heatmapColor(cell.level) }"
                    :title="cell.tooltip"
                    @mouseenter="hoveredCell = i"
                    @mouseleave="hoveredCell = -1"
                  ></div>
                </div>
              </div>
            </div>

            <!-- Hover tooltip -->
            <div
              v-if="hoveredCell >= 0 && !paddedHeatmap[hoveredCell]?.isEmpty"
              class="mt-3 text-label-sm font-label-sm text-on-surface-variant bg-surface-container-lowest/90 px-3 py-1.5 rounded-lg inline-block"
            >
              {{ paddedHeatmap[hoveredCell]?.tooltip }}
            </div>
          </div>
        </div>

        <!-- 学习时间曲线图 -->
        <div class="bg-surface-container-lowest rounded-[24px] p-6 md:p-8 card-shadow">
          <div class="flex justify-between items-center mb-2">
            <h3 class="font-headline-md text-headline-md text-on-surface">{{ $t('stats.studyTime') }}</h3>
          </div>
          <p class="text-label-md font-label-md text-on-surface-variant mb-4">
            {{ $t('stats.weekAvg') }}{{ weeklyAvgMinutes }}分
          </p>
          <div ref="chartRef" class="w-full h-[300px]"></div>
        </div>
      </div><!-- /lg:col-span-8 -->

      <!-- 右侧：词汇量卡片 -->
      <div class="lg:col-span-4 space-y-6">
        <div class="bg-gradient-to-br from-primary-fixed to-surface-container-lowest rounded-[24px] p-8 card-shadow relative overflow-hidden">
          <div class="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-full blur-2xl -mr-10 -mt-10"></div>
          <div class="relative z-10 text-center mb-6">
            <div class="inline-flex items-center justify-center w-20 h-20 bg-surface-container-lowest rounded-full shadow-sm mb-4">
              <span class="material-symbols-outlined text-4xl text-[#FFC107] fill">trophy</span>
            </div>
            <h3 class="font-headline-md text-headline-md text-on-surface">{{ $t('stats.myVocabulary') }}</h3>
            <p class="text-label-md font-label-md text-on-surface-variant mt-1">{{ $t('stats.testedAt') }} {{ vocabTestDate }}</p>
          </div>
          <div class="space-y-6 relative z-10">
            <div class="bg-surface-container-lowest/60 backdrop-blur-sm rounded-xl p-4 text-center border border-surface-container-lowest">
              <p class="text-label-md font-label-md text-on-surface-variant mb-1">{{ $t('stats.masteredWords') }}</p>
              <div class="text-headline-lg font-headline-lg text-primary flex items-center justify-center">
                <span class="material-symbols-outlined mr-2 text-primary text-[20px]">auto_stories</span>
                {{ masteredWords.toLocaleString() }}
              </div>
            </div>
            <div class="bg-surface-container-lowest/60 backdrop-blur-sm rounded-xl p-4 text-center border border-surface-container-lowest">
              <p class="text-label-md font-label-md text-on-surface-variant mb-1">{{ $t('stats.vocabularyRate') }}</p>
              <div class="text-headline-lg font-headline-lg text-primary flex items-center justify-center">
                <span class="material-symbols-outlined mr-2 text-primary text-[20px]">check_circle</span>
                {{ vocabularyRate }}%
              </div>
            </div>
            <div class="bg-surface-container-lowest/60 backdrop-blur-sm rounded-xl p-4 text-center border border-surface-container-lowest">
              <p class="text-label-md font-label-md text-on-surface-variant mb-1">{{ $t('stats.nationalRank') }}</p>
              <div class="text-headline-lg font-headline-lg text-primary flex items-center justify-center">
                <span class="material-symbols-outlined mr-2 text-primary text-[20px]">trending_up</span>
                {{ $t('stats.top') }} {{ nationalRank }}%
              </div>
            </div>
          </div>
        </div>
      </div><!-- /lg:col-span-4 -->
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { getExerciseStats } from '@/api/exercise'

const { tm } = useI18n()
const userStore = useUserStore()
const appStore = useAppStore()

// ========== 顶部统计 ==========
const todayHours = ref(0)
const todayMinutes = ref(0)
const streakDays = ref(0)
const activeDays = ref(0)

// ========== 热力图 ==========
type HeatmapCell = {
  date: string
  questionCount: number
  activityLevel: number
  dailyMinutes: number
}
const heatmapData = ref<HeatmapCell[]>([])

// 星期标签（周一~周日）
const dayLabels = ['一', '二', '三', '四', '五', '六', '日']

// 基于学习时长（分钟）计算颜色等级
const minutesToLevel = (minutes: number): number => {
  if (minutes <= 0) return 0
  if (minutes <= 10) return 1
  if (minutes <= 30) return 2
  if (minutes <= 60) return 3
  if (minutes <= 120) return 4
  return 5
}

// 各等级对应的时长范围 tooltip
const levelMinuteLabels = ['0分钟', '1~10分钟', '11~30分钟', '31~60分钟', '61~120分钟', '>120分钟']

// 热力图颜色映射（基于学习时长等级）
const heatmapColor = (level: number): string => {
  const colors = [
    '#e5e7eb',        // 0: 无记录 — 浅灰（通过 CSS class 覆盖，此处保底）
    '#e9d5ff',        // 1: 1~10分钟 浅紫
    '#d8b4fe',        // 2: 11~30分钟 淡紫
    '#c084fc',        // 3: 31~60分钟 中紫
    '#a855f7',        // 4: 61~120分钟 深紫
    '#9333ea'         // 5: >120分钟 最深紫
  ]
  return colors[level] || colors[0]
}

// 用空单元格补齐，使第一列对应正确的星期
interface PaddedCell {
  level: number
  isEmpty: boolean
  tooltip: string
}
const paddedHeatmap = computed<PaddedCell[]>(() => {
  if (heatmapData.value.length === 0) {
    return Array.from({ length: 7 * 53 }, () => ({ level: 0, isEmpty: true, tooltip: '' }))
  }
  const firstDate = new Date(heatmapData.value[0].date + 'T00:00:00')
  // getDay() 0=Sun, 1=Mon ... 6=Sat → 我们映射为 0=Mon ... 6=Sun
  let startDay = firstDate.getDay() - 1
  if (startDay < 0) startDay = 6

  const cells: PaddedCell[] = []
  // 补充前面的空白
  for (let i = 0; i < startDay; i++) {
    cells.push({ level: 0, isEmpty: true, tooltip: '' })
  }
  // 实际数据 — 基于学习时长（分钟）计算颜色等级
  for (const d of heatmapData.value) {
    const level = minutesToLevel(d.dailyMinutes)
    cells.push({
      level,
      isEmpty: level === 0,
      tooltip: level > 0
        ? `${d.date}\n学习 ${d.dailyMinutes} 分钟\n答题 ${d.questionCount} 道`
        : `${d.date}\n无记录`
    })
  }
  // 补齐到完整行
  while (cells.length % 7 !== 0) {
    cells.push({ level: 0, isEmpty: true, tooltip: '' })
  }
  return cells
})

// 月份标签位置
const monthLabelPositions = computed(() => {
  if (heatmapData.value.length === 0) return []
  // 用 tm() 取得原始数组（t() 在非 legacy 模式下会把数组当成字符串返回，导致取到单个字符）
  const months = (tm('stats.months') as unknown as string[]) || ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
  const firstDate = new Date(heatmapData.value[0].date + 'T00:00:00')
  let startDay = firstDate.getDay() - 1
  if (startDay < 0) startDay = 6

  const positions: { label: string; col: number }[] = []
  let lastMonth = -1
  for (let i = 0; i < heatmapData.value.length; i++) {
    const d = new Date(heatmapData.value[i].date + 'T00:00:00')
    const month = d.getMonth()
    if (month !== lastMonth) {
      const col = Math.floor((startDay + i) / 7)
      positions.push({ label: months[month], col })
      lastMonth = month
    }
  }
  return positions
})

const hoveredCell = ref(-1)

// 热力图网格常量 → 每列宽度 = 格子12px + 间距3px = 15px
// 月份标签的 left 偏移 = 列号 × 每列宽度
const heatmapGrid = {
  cellSize: 12,
  gap: 3,
  colWidth: 15
}

// ========== 学习时间曲线 (ECharts) ==========
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
const weeklyAvgMinutes = ref(0)
const hourlyStudyMinutes = ref<number[]>(new Array(24).fill(0))

// ========== 词汇量 ==========
const vocabTestDate = ref('')
const masteredWords = ref(0)
const vocabularyRate = ref('0.00')
const nationalRank = ref('0.00')

// ========== ECharts 初始化 ==========
const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(chartRef.value, undefined, { renderer: 'canvas' })
}

const renderChart = () => {
  if (!chartInstance) return

  // 使用后端返回的24小时分布数据
  const hourlyData = hourlyStudyMinutes.value || new Array(24).fill(0)
  const hourLabels: string[] = []
  for (let h = 0; h < 24; h++) {
    hourLabels.push(`${h.toString().padStart(2, '0')}:00`)
  }

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: 'rgba(0,0,0,0.08)',
      textStyle: { color: '#1A1D26', fontSize: 13 },
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        return `${p.name}<br/>学习时长: <b>${p.value} 分钟</b>`
      }
    },
    grid: {
      left: 10,
      right: 20,
      top: 20,
      bottom: 30,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hourLabels,
      axisLine: { lineStyle: { color: 'rgba(0,0,0,0.08)' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#9CA3AF',
        fontSize: 11,
        interval: 2  // 每3小时: 00:00, 03:00, 06:00...
      },
      boundaryGap: true
    },
    yAxis: {
      type: 'value',
      name: '分钟',
      nameTextStyle: { color: '#9CA3AF', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)', type: 'dashed' } },
      axisLabel: { color: '#9CA3AF', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: hourlyData,
      barWidth: '60%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#a855f7' },
          { offset: 1, color: '#c084fc' }
        ]),
        borderRadius: [2, 2, 0, 0]
      }
    }]
  })
}

// 窗口大小变化时重绘
const handleResize = () => {
  chartInstance?.resize()
}

// ========== 加载数据 ==========
const loadStatsData = async () => {
  const userId = userStore.userId
  if (!userId) return

  try {
    const stats = await getExerciseStats(userId)
    if (!stats) return

    // —— 顶部统计 ——
    const todaySeconds = stats.todayDurationSeconds || 0
    todayHours.value = Math.floor(todaySeconds / 3600)
    todayMinutes.value = Math.floor((todaySeconds % 3600) / 60)
    streakDays.value = stats.streakDays || 0
    activeDays.value = stats.activeDays || 0

    // —— 热力图 —— 将 yearlyStudyMinutes 合并到 heatmapData
    const rawHeatmap = (stats.heatmapData || []) as HeatmapCell[]
    const yearlyMinutes = stats.yearlyStudyMinutes || []
    heatmapData.value = rawHeatmap.map((cell, i) => ({
      ...cell,
      dailyMinutes: i < yearlyMinutes.length ? (yearlyMinutes[i] || 0) : 0
    }))

    // —— 学习时间趋势 ——
    const weekMinutes: number[] = stats.recentWeekMinutes || [0, 0, 0, 0, 0, 0, 0]
    const avg = weekMinutes.reduce((a, b) => a + b, 0) / (weekMinutes.length || 1)
    weeklyAvgMinutes.value = Math.round(avg)
    hourlyStudyMinutes.value = stats.hourlyStudyMinutes || new Array(24).fill(0)

    // —— 词汇量 ——
    masteredWords.value = stats.masteredWords || 0
    vocabularyRate.value = (stats.vocabularyComprehension || 0).toFixed(2)
    nationalRank.value = (stats.nationalRankPercent || 0).toFixed(2)
    const today = new Date()
    vocabTestDate.value = today.toLocaleDateString(appStore.language === 'en-US' ? 'en-US' : 'zh-CN')

    // 渲染图表
    await nextTick()
    if (!chartInstance) initChart()
    renderChart()
  } catch (e) {
    console.warn('获取统计数据失败', e)
  }
}

watch(() => userStore.userId, (newId) => {
  if (newId) loadStatsData()
}, { immediate: true })

onMounted(() => {
  if (userStore.userId) {
    loadStatsData()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
/* month label positioning uses absolute with container-relative left */
</style>
