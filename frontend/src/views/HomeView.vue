<template>
  <AppLayout>
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
      <div>
        <h2 class="font-display-lg text-display-lg text-on-surface mb-2">{{ greetingText }}，{{ nickname }}！</h2>
        <p class="font-body-lg text-body-lg text-on-surface-variant">{{ $t('dashboard.readyForToday') }}</p>
      </div>
      <button
        class="bg-primary text-on-primary px-8 py-4 rounded-[16px] font-label-lg text-label-lg flex items-center gap-2 hover:bg-primary/90 transition-colors shadow-sm"
        @click="startDailyLesson"
      >
        <span class="material-symbols-outlined">play_arrow</span>
        {{ $t('dashboard.startDailyLesson') }}
      </button>
    </div>
    <div class="grid grid-cols-1 md:grid-cols-12 gap-gutter">
      <!-- 每日目标进度 -->
      <div class="col-span-1 md:col-span-8 bg-surface rounded-[24px] p-8 shadow-soft-glow relative overflow-hidden">
        <div class="absolute -right-20 -top-20 w-64 h-64 bg-primary-container/10 rounded-full blur-3xl"></div>
        <div class="flex flex-col md:flex-row gap-8 items-center relative z-10">
          <div class="relative w-40 h-40 flex-shrink-0">
            <svg class="w-full h-full" viewBox="0 0 100 100">
              <circle class="text-surface-variant stroke-current" cx="50" cy="50" fill="transparent" r="40" stroke-width="8"></circle>
              <circle
                class="text-primary stroke-current progress-ring-circle"
                cx="50" cy="50" fill="transparent" r="40"
                :stroke-dasharray="251.2"
                :stroke-dashoffset="251.2 - (251.2 * dailyProgress.percent / 100)"
                stroke-linecap="round" stroke-width="8"
              ></circle>
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <span class="font-headline-lg text-headline-lg text-primary font-bold">{{ dailyProgress.percent }}%</span>
            </div>
          </div>
          <div class="flex-1 text-center md:text-left">
            <div class="flex items-center gap-2 justify-center md:justify-start mb-2">
              <span class="material-symbols-outlined text-primary text-xl">auto_awesome</span>
              <span class="font-label-lg text-label-lg text-primary uppercase tracking-wider">Daily Goal</span>
            </div>
            <div class="flex items-center gap-2 justify-center md:justify-start mb-2">
              <h3 class="font-headline-lg text-headline-lg text-on-surface">{{ dailyProgress.moduleName }}</h3>
              <button
                class="p-1 rounded-full hover:bg-surface-variant/50 transition-colors"
                title="设置每日目标"
                @click.stop="startEditTarget"
              >
                <span class="material-symbols-outlined text-outline text-lg">edit</span>
              </button>
            </div>
            <!-- 目标编辑输入框 -->
            <div v-if="editingTarget" class="flex items-center gap-2 justify-center md:justify-start mb-4">
              <span class="font-body-md text-body-md text-on-surface-variant">每日目标：</span>
              <input
                ref="targetInputRef"
                v-model.number="editTargetValue"
                type="number"
                min="1"
                max="999"
                class="w-20 px-2 py-1 text-center border border-outline-variant rounded-lg text-on-surface focus:border-primary focus:outline-none"
                @keydown.enter="confirmTarget"
                @keydown.escape="cancelTarget"
                @blur="confirmTarget"
              />
              <span class="font-body-md text-body-md text-on-surface-variant">个单词</span>
            </div>
            <p v-else class="font-body-md text-body-md text-on-surface-variant mb-6">
              {{ $t('dashboard.progressText', { total: dailyProgress.total, completed: dailyProgress.completed }) }}
            </p>
            <div v-if="!editingTarget" class="flex flex-wrap gap-2 justify-center md:justify-start">
              <span class="bg-primary-container/15 text-primary px-3 py-1 rounded-[12px] font-label-md text-label-md">{{ $t('dashboard.learnedWords', { count: dailyProgress.completed }) }}</span>
              <span class="bg-surface-variant/50 text-on-surface-variant px-3 py-1 rounded-[12px] font-label-md text-label-md">{{ $t('dashboard.remainingWords', { count: dailyProgress.remaining }) }}</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 学习连续天数 -->
      <div class="col-span-1 md:col-span-4 bg-surface rounded-[24px] p-8 shadow-soft-glow flex flex-col">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-headline-md text-headline-md text-on-surface">{{ $t('dashboard.streakDays') }}</h3>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
        <div class="flex items-end gap-2 mb-8">
          <span class="font-display-lg text-display-lg text-on-surface leading-none">{{ streakDays }}</span>
          <span class="font-body-lg text-body-lg text-on-surface-variant pb-1">{{ $t('stats.days') }}</span>
        </div>
        <div class="flex justify-between mt-auto">
          <div
            v-for="(day, index) in weekDays"
            :key="index"
            class="flex flex-col items-center gap-2"
          >
            <div
              class="w-8 h-8 rounded-full flex items-center justify-center"
              :class="day.class"
            >
              <span
                v-if="day.isFire && streakDays >= 3"
                class="material-symbols-outlined text-sm fill"
                :class="day.iconClass"
              >local_fire_department</span>
              <span
                v-else-if="day.isToday"
                class="font-label-md text-xs font-bold"
                :class="day.textClass"
              >{{ day.label }}</span>
              <span
                v-else
                class="font-label-md text-xs"
                :class="day.textClass"
              >{{ day.label }}</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 学习量统计 -->
      <div class="col-span-1 md:col-span-8 bg-surface rounded-[24px] p-8 shadow-soft-glow">
        <div class="flex items-center justify-between mb-8">
          <div>
            <h3 class="font-headline-md text-headline-md text-on-surface mb-1">{{ $t('dashboard.studyVolume') }}</h3>
            <p class="font-body-md text-body-md text-on-surface-variant">
              {{ studySummaryText }}
            </p>
          </div>
          <div class="bg-surface-container-low rounded-full p-1 flex">
            <button
              :class="['px-4 py-1.5 rounded-full font-label-md text-label-md transition-all', studyPeriod === 'week' ? 'bg-white shadow-sm text-on-surface' : 'text-on-surface-variant hover:text-on-surface']"
              @click="studyPeriod = 'week'"
            >{{ $t('dashboard.week') }}</button>
            <button
              :class="['px-4 py-1.5 rounded-full font-label-md text-label-md transition-all', studyPeriod === 'month' ? 'bg-white shadow-sm text-on-surface' : 'text-on-surface-variant hover:text-on-surface']"
              @click="studyPeriod = 'month'"
            >{{ $t('dashboard.month') }}</button>
          </div>
        </div>
          <p class="font-body-sm text-body-sm text-on-surface-variant mb-4">{{ studySubtitle }}</p>
        <div class="h-48 flex items-end gap-1 border-b border-surface-variant pb-2">
          <div
            v-for="(bar, index) in studyBars"
            :key="index"
            class="bar-col flex-1 flex flex-col justify-end items-center gap-0.5 h-full relative"
          >
            <!-- 自定义悬浮提示 -->
            <div v-if="bar.hasData" class="bar-tooltip">
              <span class="font-label-md text-label-md">{{ bar.tooltip }}</span>
            </div>
            <template v-if="bar.hasData">
              <div
                class="w-3/4 max-w-12 bg-primary rounded-t-sm transition-all duration-500 cursor-pointer hover:bg-primary/80"
                :style="{ height: bar.vocab + '%' }"
              ></div>
            </template>
            <template v-else>
              <div class="w-3/4 max-w-12 bg-surface-variant/30 rounded-t-sm h-2"></div>
            </template>
          </div>
        </div>
        <div class="flex gap-1 mt-2">
          <span
            v-for="(label, index) in barLabels"
            :key="index"
            class="flex-1 text-center font-label-md text-label-md text-outline"
          >{{ label }}</span>
        </div>
      </div>
      <!-- 继续学习 & 每日单词 -->
      <div class="col-span-1 md:col-span-4 flex flex-col gap-gutter">
        <div
          class="bg-surface rounded-[24px] p-6 shadow-soft-glow border border-transparent hover:border-primary/20 transition-colors cursor-pointer group"
          @click="continueLearning"
        >
          <div class="w-12 h-12 rounded-full bg-secondary-container/20 text-secondary flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined">menu_book</span>
          </div>
          <h4 class="font-headline-md text-headline-md text-on-surface mb-1">{{ $t('dashboard.continueLearning') }}</h4>
          <p class="font-body-md text-body-md text-on-surface-variant">{{ continueInfo }}</p>
        </div>
        <div
          class="bg-surface rounded-[24px] p-6 shadow-soft-glow relative overflow-hidden group cursor-pointer hover:shadow-md transition-shadow"
          @click="goToDailyWordDetail"
        >
          <div class="absolute right-0 top-0 w-32 h-32 bg-tertiary-container/10 rounded-bl-full z-0 transition-transform group-hover:scale-125"></div>
          <div class="relative z-10">
            <div class="flex items-center gap-2 mb-4">
              <span class="material-symbols-outlined text-tertiary">wb_sunny</span>
              <span class="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">{{ $t('dashboard.dailyWord') }}</span>
            </div>

            <template v-if="dailyWord">
              <div class="mb-1">
                <h4 class="font-display-lg text-display-lg text-on-surface leading-tight">{{ dailyWord.word }}</h4>
                <span class="font-body-sm text-body-sm text-outline">{{ dailyWord.phonetic }}</span>
              </div>

              <div class="flex items-center gap-2 mb-3">
                <span class="inline-block bg-tertiary-container/20 text-tertiary px-3 py-0.5 rounded-[8px] font-label-sm text-label-sm">{{ dailyWord.pos }}</span>
                <span class="font-body-md text-body-md text-on-surface">{{ dailyWord.translation }}</span>
              </div>

              <div class="bg-surface-container-low/50 rounded-[12px] p-3 mt-3">
                <p class="font-body-sm text-body-sm text-on-surface-variant italic leading-relaxed">"{{ dailyWord.example }}"</p>
                <p class="font-body-sm text-body-sm text-outline mt-1">{{ dailyWord.exampleTranslation }}</p>
              </div>

              <div class="flex items-center justify-between mt-3">
                <span class="font-label-sm text-label-sm text-outline">{{ dailyWord.level }}</span>
                <span class="material-symbols-outlined text-outline text-sm group-hover:text-primary transition-colors">arrow_forward</span>
              </div>
            </template>

            <template v-else>
              <div class="py-4 flex flex-col items-center justify-center text-outline">
                <span class="material-symbols-outlined text-2xl mb-2 animate-pulse">hourglass</span>
                <span class="font-body-md text-body-md">{{ $t('common.loading') }}</span>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { getDailyWord, getExerciseStats } from '@/api/exercise'
import type { DailyWordVO } from '@/api/exercise'
import { getReviewTasks } from '@/api/learning'
import type { ReviewTaskVO } from '@/api/learning'
const { t } = useI18n()
const userStore = useUserStore()
const route = useRoute()
const nickname = computed(() => userStore.user?.nickname || userStore.user?.username || '同学')
// 时间问候语
const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return t('dashboard.greeting.morning')
  if (hour < 18) return t('dashboard.greeting.afternoon')
  return t('dashboard.greeting.evening')
})
// ========== 每日目标进度 ==========
const DAILY_TARGET_KEY = 'dailyTarget_vocab'
const dailyTarget = ref(Number(localStorage.getItem(DAILY_TARGET_KEY)) || 20)
const todayVocabCount = ref(0)

const editingTarget = ref(false)
const editTargetValue = ref(dailyTarget.value)
const targetInputRef = ref<HTMLInputElement | null>(null)

const dailyProgress = computed(() => {
  const completed = todayVocabCount.value
  const total = dailyTarget.value
  const percent = total > 0 ? Math.min(100, Math.round((completed / total) * 100)) : 0
  const remaining = Math.max(0, total - completed)
  return {
    percent,
    moduleName: '词汇学习',
    completed,
    total,
    remaining,
  }
})

const startEditTarget = () => {
  editTargetValue.value = dailyTarget.value
  editingTarget.value = true
  nextTick(() => targetInputRef.value?.focus())
}

const confirmTarget = () => {
  const v = Math.max(1, Math.min(999, editTargetValue.value || 20))
  dailyTarget.value = v
  localStorage.setItem(DAILY_TARGET_KEY, String(v))
  editingTarget.value = false
}

const cancelTarget = () => {
  editingTarget.value = false
}
// ========== 连续学习天数（从后端真实数据获取） ==========
const streakDays = computed(() => userStore.streakDays)

const weekDays = computed(() => {
  const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  const today = new Date()
  const todayIndex = today.getDay() === 0 ? 6 : today.getDay() - 1

  return dayNames.map((name, index) => {
    const isToday = index === todayIndex
    const isFire = index <= todayIndex && streakDays.value > 0

    return {
      name: '',
      label: name,
      isFire,
      isToday,
      class: isToday
        ? 'border-2 border-primary'
        : isFire
          ? 'bg-[#FFA500]/20'
          : 'bg-surface-variant',
      iconClass: 'text-[#FFA500]',
      textClass: isToday
        ? 'text-primary font-bold'
        : isFire
          ? 'text-[#FFA500]'
          : 'text-on-surface-variant/50',
      labelClass: '',
    }
  })
})
// ========== 学习量统计 ==========
const studyPeriod = ref<'week' | 'month'>('week')
const weeklyAverage = ref(0)
interface StudyBar {
  hasData: boolean
  vocab: number
  grammar: number
  translation: number
  listening: number
  isToday: boolean
  /** 原始数值（周视图为分钟数，月视图为答题数） */
  rawValue: number
  /** 悬浮提示文案 */
  tooltip: string
}
// ========== 学习量柱状图（使用后端真实数据） ==========
const realWeekMinutes = ref<number[]>([])
const realMonthQuestions = ref<number[]>([])
const studyBars = computed<StudyBar[]>(() => {
  const isWeek = studyPeriod.value === 'week'
  const data = isWeek ? realWeekMinutes.value : realMonthQuestions.value
  const placeholderCount = isWeek ? 7 : 12
  if (!data || data.length === 0) {
    return Array(placeholderCount).fill(null).map(() => ({
      hasData: false, vocab: 0, grammar: 0, translation: 0, listening: 0, isToday: false, rawValue: 0, tooltip: ''
    }))
  }
  const maxValue = Math.max(...data, 1)
  // 计算今天在数组中的索引（后端按周一到周日返回，需要找到今天是周几）
  const todayIdx = isWeek ? getTodayWeekIndex() : -1
  return data.map((value, i) => {
    const percent = Math.round((value / maxValue) * 100)
    const tooltip = isWeek ? `${value} 分钟` : `共 ${value} 道题`
    return {
      hasData: value > 0,
      vocab: percent,
      grammar: 0,
      translation: 0,
      listening: 0,
      isToday: i === todayIdx,
      rawValue: value,
      tooltip,
    }
  })
})
const barLabels = computed(() => {
  if (studyPeriod.value === 'week') {
    return getWeekDayLabels()
  }
  return ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
})
/** 获取今天在本周数组中的索引（0=周一, 6=周日） */
function getTodayWeekIndex(): number {
  const dayOfWeek = new Date().getDay()
  return dayOfWeek === 0 ? 6 : dayOfWeek - 1
}
/** 获取本周一到周日的星期标签（M/T/W/T/F/S/S） */
function getWeekDayLabels(): string[] {
  const labels = ['M', 'T', 'W', 'T', 'F', 'S', 'S']
  return labels
}
// 学习量图表副标题（随视图切换）
const studySubtitle = computed(() => {
  return studyPeriod.value === 'week'
    ? '本周每日学习时长（分钟）'
    : `本年 ${new Date().getFullYear()} 年 1-12 月每月答题数（道）`
})
// 学习量标题下方的统计摘要（随视图切换）
const studySummaryText = computed(() => {
  if (studyPeriod.value === 'week') {
    return `本周平均每天回答 ${weeklyAverage.value} 道题`
  }
  // 月视图：本月总答题数（取 yearlyMonthQuestions 最后一个元素，即当前月份）
  const currentMonthQuestions = realMonthQuestions.value.length > 0
    ? realMonthQuestions.value[new Date().getMonth()]
    : 0
  return `本月共回答 ${currentMonthQuestions} 道题`
})
// ========== 继续学习 ==========
const continueInfo = ref(t('dashboard.unitInfo', { unit: 1 }))
const reviewTasks = ref<ReviewTaskVO[]>([])
// ========== 每日单词 ==========
const dailyWord = ref<DailyWordVO | null>(null)
// ========== 模块名称映射 ==========
const moduleNames: Record<number, string> = {
  1: '词汇',
  2: '语法',
  3: '翻译',
  4: '听力',
}
// ========== 加载真实学习量数据 ==========
const loadStatsData = async () => {
  const uid = userStore.userId
  if (!uid) return
  try {
    const stats = await getExerciseStats(uid)
    if (stats?.recentWeekMinutes) {
      realWeekMinutes.value = stats.recentWeekMinutes
    }
    if (stats?.yearlyMonthQuestions) {
      realMonthQuestions.value = stats.yearlyMonthQuestions
    }
    // 本周平均每天答题数（由后端计算并返回）
    if (stats?.weeklyAverageQuestions !== undefined) {
      weeklyAverage.value = stats.weeklyAverageQuestions
    }
    // 更新今日词汇学习数量（每日自动归零由后端按日期过滤保证）
    if (stats?.todayVocabAnswered !== undefined) {
      todayVocabCount.value = stats.todayVocabAnswered
    }
  } catch (e) {
    console.warn('获取学习量统计失败', e)
  }
}
// ========== 加载数据 ==========
const loadHomeData = async () => {
  await loadStatsData()
  const userId = userStore.userId
  if (!userId) return
  try {
    const tasks = await getReviewTasks(userId)
    reviewTasks.value = tasks || []
    if (tasks && tasks.length > 0) {
      const firstTask = tasks[0]
      continueInfo.value = t('dashboard.reviewTasksPending', {
        count: tasks.length,
        module: moduleNames[firstTask.moduleType] || t('dashboard.moduleVocabulary'),
      })
    } else {
      continueInfo.value = t('dashboard.reviewTasksDone')
    }
  } catch (e) {
    console.warn('Failed to load review tasks', e)
  }
  try {
    const cetLevel = userStore.user?.cetLevel || 'CET4'
    const word = await getDailyWord(cetLevel)
    dailyWord.value = word
  } catch (e) {
    console.warn('Failed to load daily word', e)
    dailyWord.value = null
  }
}
const startDailyLesson = () => {
  window.location.href = '/learning'
}
const continueLearning = () => {
  window.location.href = '/learning'
}
const goToDailyWordDetail = () => {
  if (dailyWord.value) {
    window.location.href = '/learning'
  }
}
watch(() => userStore.userId, (newId) => {
  if (newId) loadHomeData()
}, { immediate: true })

// 每次进入首页都刷新数据（练习完回来时进度能更新）
watch(() => route.path, (newPath) => {
  if (newPath === '/' || newPath === '/home') {
    if (userStore.userId) loadHomeData()
  }
})

onMounted(() => {
  if (userStore.userId) {
    loadHomeData()
  }
})
</script>

<style scoped>
/* 柱形图悬浮提示 */
.bar-tooltip {
  position: absolute;
  top: -32px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(30, 30, 40, 0.88);
  color: #fff;
  padding: 4px 10px;
  border-radius: 8px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 10;
}
/* 小三角箭头 */
.bar-tooltip::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 5px solid rgba(30, 30, 40, 0.88);
}
/* 悬停时显示提示 */
.bar-col:hover .bar-tooltip {
  opacity: 1;
}
</style>
