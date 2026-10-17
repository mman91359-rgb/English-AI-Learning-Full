<template>
  <AppLayout>
    <!-- 顶部标题栏 -->
    <div class="flex items-center gap-4 mb-6">
      <button class="p-2 hover:bg-surface-variant rounded-full transition-colors" @click="$router.back()">
        <span class="material-symbols-outlined text-on-surface">arrow_back</span>
      </button>
      <h2 class="font-headline-lg text-headline-lg text-on-surface">{{ $t('app.title') }} - {{ $t('learning.learningHistory') }}</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="mb-6 space-y-3">
      <!-- 模块类型筛选 -->
      <div class="flex items-center gap-2">
        <span class="font-label-md text-label-md text-on-surface-variant shrink-0 w-16">{{ $t('learning.moduleType') }}</span>
        <div class="flex gap-1.5 flex-wrap">
          <button
            v-for="m in moduleFilters"
            :key="m.value"
            :class="[
              'px-4 py-1.5 rounded-full font-label-sm text-label-sm transition-colors',
              moduleFilter === m.value
                ? 'bg-primary text-on-primary'
                : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
            ]"
            @click="moduleFilter = m.value; applyFilter()"
          >
            {{ m.label }}
          </button>
        </div>
      </div>
      <!-- 正确/错误筛选 -->
      <div class="flex items-center gap-2">
        <span class="font-label-md text-label-md text-on-surface-variant shrink-0 w-16">{{ $t('learning.result') }}</span>
        <div class="flex gap-1.5 flex-wrap">
          <button
            v-for="c in correctFilters"
            :key="c.value"
            :class="[
              'px-4 py-1.5 rounded-full font-label-sm text-label-sm transition-colors',
              correctFilter === c.value
                ? 'bg-primary text-on-primary'
                : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
            ]"
            @click="correctFilter = c.value; applyFilter()"
          >
            {{ c.label }}
          </button>
        </div>
      </div>
      <!-- 时间范围筛选 -->
      <div class="flex items-center gap-2">
        <span class="font-label-md text-label-md text-on-surface-variant shrink-0 w-16">{{ $t('learning.time') }}</span>
        <div class="flex gap-1.5 flex-wrap">
          <button
            v-for="r in timeRangeFilters"
            :key="r.value"
            :class="[
              'px-4 py-1.5 rounded-full font-label-sm text-label-sm transition-colors',
              timeRangeFilter === r.value
                ? 'bg-primary text-on-primary'
                : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
            ]"
            @click="timeRangeFilter = r.value; applyFilter()"
          >
            {{ r.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="history.length === 0" class="text-center py-20">
      <span class="material-symbols-outlined text-5xl text-outline">filter_alt_off</span>
      <p class="mt-3 text-on-surface-variant font-body-md text-body-md">{{ $t('learning.noFilterResult') }}</p>
      <button
        class="mt-3 px-5 py-2 rounded-full border border-outline-variant text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant transition-colors"
        @click="moduleFilter = 0; correctFilter = -1; timeRangeFilter = 'all'; applyFilter()"
      >
        {{ $t('learning.resetFilter') }}
      </button>
    </div>

    <!-- 历史记录列表 -->
    <div v-else class="space-y-3">
      <!-- 每条历史记录 -->
      <div
        v-for="item in history"
        :key="item.logId"
        class="bg-surface-container-lowest rounded-[16px] shadow-soft-glow overflow-hidden transition-shadow"
        :class="expandedLogId === item.logId ? 'shadow-md' : 'hover:shadow-md'"
      >
        <!-- 紧凑行（可点击展开） -->
        <div
          class="p-4 flex items-center gap-4 cursor-pointer select-none"
          @click="toggleExpand(item.logId)"
        >
          <!-- 正确/错误图标 -->
          <div
            class="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
            :class="item.isCorrect ? 'bg-green-100' : 'bg-red-100'"
          >
            <span
              class="material-symbols-outlined text-lg"
              :class="item.isCorrect ? 'text-green-600' : 'text-red-600'"
            >
              {{ item.isCorrect ? 'check' : 'close' }}
            </span>
          </div>
          <!-- 题目摘要 -->
          <div class="flex-1 min-w-0">
            <p class="font-body-md text-body-md text-on-surface truncate">
              <!-- 语法题显示知识点，其他题显示题干 -->
              {{ item.moduleType === 2 ? extractKnowledgePoint(item) : (item.stem || $t('learning.stem') + ' #' + item.questionId) }}
            </p>
            <div class="flex items-center gap-3 mt-1">
              <span
                class="font-label-sm text-label-sm"
                :class="item.moduleType === 1 ? 'text-primary' : 'text-secondary'"
              >
                {{ moduleNameMap(item.moduleType) || $t('learning.sentenceMaking') }}
              </span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">
                Lv.{{ item.difficultyLevel || '-' }}
              </span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">
                {{ item.durationSeconds || 0 }}s
              </span>
            </div>
          </div>
          <!-- 时间与结果 -->
          <div class="text-right shrink-0">
            <span class="font-label-sm text-label-sm text-on-surface-variant block">
              {{ formatTime(item.createdAt) }}
            </span>
            <span
              class="font-label-sm text-label-sm"
              :class="item.isCorrect ? 'text-green-600' : 'text-red-600'"
            >
              {{ item.isCorrect ? $t('learning.correctTag') : $t('learning.incorrectTag') }}
            </span>
          </div>
          <!-- 展开箭头 -->
          <span
            class="material-symbols-outlined text-on-surface-variant shrink-0 transition-transform duration-200"
            :class="expandedLogId === item.logId ? 'rotate-180' : ''"
          >
            expand_more
          </span>
        </div>

        <!-- 展开详情区 -->
        <div
          v-if="expandedLogId === item.logId"
          class="px-4 pb-4 border-t border-outline-variant/20"
        >
          <div class="pt-4 space-y-3">
            <!-- 语法题：直译 + 正确答案（关键词高亮） + 你的答案（错误提示） -->
            <template v-if="item.moduleType === 2">
              <!-- 直译 -->
              <div>
                <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.literalTranslation') }}</p>
                <p class="font-body-md text-body-md text-on-surface bg-surface-container-low p-3 rounded-xl leading-relaxed">
                  {{ extractLiteralTranslation(item) }}
                </p>
              </div>
              <!-- 正确答案（关键词高亮） -->
              <div>
                <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.correctAnswer') }}</p>
                <p class="font-body-md text-body-md text-green-600 font-semibold bg-green-50/40 p-3 rounded-xl">
                  <span v-html="highlightKeyword(item)"></span>
                </p>
              </div>
              <!-- 你的答案（错误提示） -->
              <div>
                <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.yourAnswer') }}</p>
                <div>
                  <p
                    class="font-body-md text-body-md font-semibold"
                    :class="item.isCorrect ? 'text-green-600' : 'text-red-600'"
                  >
                    {{ formatUserAnswer(item) }}
                  </p>
                  <!-- 错误提示 -->
                  <p v-if="!item.isCorrect" class="font-label-sm text-label-sm text-error/70 mt-1">
                    {{ getGrammarErrorHint(item) }}
                  </p>
                </div>
              </div>
            </template>
            <!-- 其他题：普通展示 -->
            <template v-else>
              <!-- 题目题干 -->
              <div>
                <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.stem') }}</p>
                <p class="font-body-md text-body-md text-on-surface bg-surface-container-low p-3 rounded-xl leading-relaxed">
                  {{ item.stem || $t('learning.stem') + ' #' + item.questionId }}
                </p>
              </div>
              <!-- 你的答案 -->
              <div>
                <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.yourAnswer') }}</p>
                <p
                  class="font-body-md text-body-md font-semibold"
                  :class="item.isCorrect ? 'text-green-600' : 'text-red-600'"
                >
                  {{ formatUserAnswer(item) }}
                </p>
              </div>
              <!-- 正确答案（始终展示，方便对比） -->
              <div>
                <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.correctAnswer') }}</p>
                <p class="font-body-md text-body-md text-green-600 font-semibold">
                  {{ formatCorrectAnswer(item.correctAnswer) }}
                </p>
              </div>
            </template>
            <!-- 题目信息 -->
            <div class="flex items-center gap-4 pt-1">
              <span class="font-label-sm text-label-sm text-on-surface-variant">
                {{ $t('learning.moduleType') }}：{{ moduleNameMap(item.moduleType) }}
              </span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">
                {{ $t('learning.difficulty') }}：Lv.{{ item.difficultyLevel || '-' }}
              </span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">
                {{ $t('learning.questionType') }}：{{ item.questionType || '-' }}
              </span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">
                {{ $t('learning.duration') }}：{{ item.durationSeconds || 0 }}{{ $t('learning.seconds') }}
              </span>
            </div>

          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > history.length" class="flex justify-center gap-4 pt-4 pb-8">
        <button
          class="px-6 py-2 rounded-full border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-variant transition-colors disabled:opacity-40"
          :disabled="page <= 1"
          @click="changePage(page - 1)"
        >
          {{ $t('common.prev') }}
        </button>
        <span class="flex items-center font-label-md text-label-md text-on-surface-variant">
          {{ page }} / {{ totalPages }}
        </span>
        <button
          class="px-6 py-2 rounded-full border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-variant transition-colors disabled:opacity-40"
          :disabled="page >= totalPages"
          @click="changePage(page + 1)"
        >
          {{ $t('common.next') }}
        </button>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { getExerciseHistory, type ExerciseHistoryVO } from '@/api/exercise'

const { t, locale } = useI18n()
const userStore = useUserStore()

// ========== 数据状态 ==========
const history = ref<ExerciseHistoryVO[]>([])
const loading = ref(true)
const page = ref(1)
const total = ref(0)
const size = 20

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

// ========== 模块名称映射 ==========
const moduleNameMap = computed(() => {
  const map: Record<number, string> = {
    1: t('dashboard.moduleVocabulary'),
    2: t('dashboard.moduleGrammar'),
    3: t('dashboard.moduleTranslation'),
    4: t('dashboard.moduleListening'),
    5: t('learning.moduleSentenceMaking')
  }
  return (type: number) => map[type] || map[5]
})

// ========== 筛选配置 ==========
const moduleFilters = computed(() => [
  { label: t('learning.all'), value: 0 },
  { label: t('dashboard.moduleVocabulary'), value: 1 },
  { label: t('dashboard.moduleGrammar'), value: 2 },
  { label: t('dashboard.moduleTranslation'), value: 3 },
  { label: t('dashboard.moduleListening'), value: 4 }
])

const correctFilters = computed(() => [
  { label: t('learning.all'), value: -1 },
  { label: t('learning.correctLabel'), value: 1 },
  { label: t('learning.incorrectLabel'), value: 0 }
])

const timeRangeFilters = computed(() => [
  { label: t('learning.allTime'), value: 'all' },
  { label: t('learning.today'), value: 'today' },
  { label: t('learning.thisWeek'), value: 'week' },
  { label: t('learning.thisMonth'), value: 'month' }
])

const moduleFilter = ref(0)
const correctFilter = ref(-1)
const timeRangeFilter = ref('all')

// ========== 展开状态 ==========
const expandedLogId = ref<number | null>(null)

const toggleExpand = (logId: number) => {
  expandedLogId.value = expandedLogId.value === logId ? null : logId
}

// ========== 时间范围计算辅助函数 ==========
const getTimeRangeStart = (range: string): Date | null => {
  const now = new Date()
  switch (range) {
    case 'today': {
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      return start
    }
    case 'week': {
      const dayOfWeek = now.getDay()
      const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1
      const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - diff)
      monday.setHours(0, 0, 0, 0)
      return monday
    }
    case 'month': {
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return start
    }
    default:
      return null
  }
}

// ========== 筛选变更时重置页码并重新加载 ==========
const applyFilter = () => {
  page.value = 1
  load()
}

// ========== 数据加载 ==========
const load = async () => {
  const uid = userStore.userId
  if (!uid) return
  loading.value = true
  expandedLogId.value = null
  try {
    const startTime = timeRangeFilter.value !== 'all' 
      ? getTimeRangeStart(timeRangeFilter.value)?.toISOString() 
      : undefined
    const res = await getExerciseHistory(uid, page.value, size, 
      moduleFilter.value, 
      correctFilter.value >= 0 ? correctFilter.value : undefined, 
      startTime)
    history.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.warn('加载学习历史失败', e)
  } finally {
    loading.value = false
  }
}

const changePage = (p: number) => {
  page.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ========== 时间格式化 ==========
const formatTime = (d: string) => {
  if (!d) return ''
  return new Date(d).toLocaleString(locale.value, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

// ========== 答案显示格式化 ==========
/**
 * 旧数据中 userAnswer 可能存的是选项字母（A/B/C/D），对词语排序/造句题不直观。
 * 如果 userAnswer 是单字母且 correctAnswer 以该字母开头（如 "A. the full sentence"），
 * 则提取 correctAnswer 的纯文本部分展示。
 */
const formatUserAnswer = (item: ExerciseHistoryVO): string => {
  const ua = (item.userAnswer || '').trim()
  const ca = (item.correctAnswer || '').trim()
  // 仅当 userAnswer 是单字母 A-D 时尝试转换
  if (/^[A-Da-d]$/.test(ua) && ca.length > 1) {
    const caClean = ca.replace(/^[A-Da-d][.、)]?\s*/, '').trim()
    if (caClean) return caClean
  }
  return ua || t('learning.unanswered')
}

/**
 * 旧数据中 correctAnswer 可能存为 "A. the full sentence" 格式，
 * 提取纯文本部分展示（更直观）。
 */
const formatCorrectAnswer = (correctAnswer: string): string => {
  if (!correctAnswer) return '-'
  const ca = correctAnswer.trim()
  // 如果格式是 "A. text" → 提取 text 部分
  const match = ca.match(/^[A-Da-d][.、)]?\s*(.+)/)
  if (match && match[1]) return match[1]
  return ca
}

/**
 * 从 analysis 中提取知识点（analysis 格式：知识点||直译）
 */
const extractKnowledgePoint = (item: ExerciseHistoryVO): string => {
  const analysis = item.analysis || ''
  const sepIdx = analysis.indexOf('||')
  if (sepIdx >= 0) {
    return analysis.substring(0, sepIdx).trim() || t('learning.defaultKnowledgePoint')
  }
  return analysis.trim() || t('learning.defaultKnowledgePoint')
}

/**
 * 从 analysis 中提取直译（analysis 格式：知识点||直译）
 */
const extractLiteralTranslation = (item: ExerciseHistoryVO): string => {
  const analysis = item.analysis || ''
  const sepIdx = analysis.indexOf('||')
  if (sepIdx >= 0) {
    return analysis.substring(sepIdx + 2).trim() || item.stem || '-'
  }
  return item.stem || '-'
}

/**
 * 语法题正确答案关键词高亮：从 correctAnswer 中提取关键词，用下划线高亮显示
 * 关键词来源于知识点描述中的关键词（如 since, for, if, were 等）
 */
const highlightKeyword = (item: ExerciseHistoryVO): string => {
  const correctSentence = item.stem || formatCorrectAnswer(item.correctAnswer) || ''
  const knowledgePoint = extractKnowledgePoint(item)
  
  const keywords = new Set<string>()
  const keywordPatterns = [
    /\bsince\b/i, /\bfor\b/i, /\bif\b/i, /\bwere\b/i, /\bhad\b/i,
    /\bwould\b/i, /\bcould\b/i, /\bshould\b/i, /\bmight\b/i,
    /\bhave\b/i, /\bhas\b/i, /\bbeen\b/i, /\bbeing\b/i,
    /\bdid\b/i, /\bwas\b/i, /\bbeen\b/i, /\bthat\b/i, /\bwhich\b/i,
    /\bwho\b/i, /\bwhom\b/i, /\bwhose\b/i, /\bwhen\b/i, /\bwhere\b/i,
    /\bwhy\b/i, /\bbecause\b/i, /\balthough\b/i, /\bthough\b/i,
    /\bwhile\b/i, /\buntil\b/i, /\bunless\b/i, /\bthan\b/i, /\bas\b/i
  ]
  
  keywordPatterns.forEach(pattern => {
    if (pattern.test(knowledgePoint)) {
      const match = knowledgePoint.match(pattern)
      if (match) keywords.add(match[0].toLowerCase())
    }
  })
  
  if (keywords.size === 0) {
    return escapeHtml(correctSentence)
  }
  
  const escaped = escapeHtml(correctSentence)
  const keywordRegex = new RegExp(`\\b(${Array.from(keywords).join('|')})\\b`, 'gi')
  return escaped.replace(keywordRegex, '<u>$1</u>')
}

/**
 * HTML转义
 */
const escapeHtml = (str: string): string => {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return str.replace(/[&<>"']/g, m => map[m])
}

/**
 * 根据知识点给出语法错误提示
 */
const getGrammarErrorHint = (item: ExerciseHistoryVO): string => {
  const knowledgePoint = extractKnowledgePoint(item)
  const hints: Record<string, string> = {
    '虚拟语气': '* 虚拟语气使用错误，请检查时态匹配和动词形式',
    'since': '* 连词使用错误，请仔细核对语法规则',
    'for': '* 连词使用错误，请仔细核对语法规则',
    '条件状语': '* 条件状语从句使用错误，请检查时态搭配',
    '定语从句': '* 定语从句关系词使用错误，请检查先行词和从句结构',
    '时态': '* 时态使用错误，请根据时间状语选择正确时态',
    '主谓一致': '* 主谓一致错误，请检查主语和谓语的数是否一致',
    '比较级': '* 比较级使用错误，请检查比较对象和修饰词',
    '被动语态': '* 被动语态使用错误，请检查助动词和过去分词',
    '情态动词': '* 情态动词使用错误，请检查情态动词的含义和搭配'
  }
  for (const [keyword, hint] of Object.entries(hints)) {
    if (knowledgePoint.includes(keyword)) {
      return hint
    }
  }
  return '* 语法使用错误，请仔细核对语法规则'
}

onMounted(load)
</script>
