<template>
  <AppLayout>
    <!-- 顶部：返回 + 标题 + 进度 -->
    <div class="flex items-center gap-4 mb-6">
      <button
        v-if="!showResults"
        class="p-2 hover:bg-surface-variant rounded-full transition-colors"
        @click="$router.back()"
      >
        <span class="material-symbols-outlined text-on-surface">arrow_back</span>
      </button>
      <div class="flex-1">
        <h2 class="font-headline-lg text-headline-lg text-on-surface">{{ moduleName }}{{ $t('learning.practice') }}</h2>
        <p v-if="!showResults" class="font-body-md text-body-md text-on-surface-variant mt-1">
          {{ $t('learning.questionProgress', { current: currentQuestionIndex + 1, total: questions.length }) }}
        </p>
      </div>
      <span v-if="correctCount > 0 && !showResults" class="font-body-lg text-body-lg text-green-600">
        {{ $t('learning.correctCount', { count: correctCount }) }}
      </span>
    </div>

    <!-- 进度条 -->
    <div v-if="!showResults" class="w-full h-2 bg-surface-variant rounded-full mb-8 overflow-hidden">
      <div
        class="h-full bg-primary rounded-full transition-all duration-300"
        :style="{ width: progressPercent + '%' }"
      ></div>
    </div>

    <!-- ===== 答题模式 ===== -->
    <template v-if="!showResults">
      <div v-if="loading" class="flex justify-center py-20">
        <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
        <p class="mt-4 text-on-surface-variant font-body-md text-body-md">{{ $t('common.loading') }}</p>
      </div>

      <div v-else-if="currentQuestion" class="max-w-3xl mx-auto">
        <!-- 题干 -->
        <div class="bg-surface rounded-2xl p-6 md:p-8 shadow-soft-glow mb-6">
          <p class="font-body-lg text-body-lg text-on-surface leading-relaxed">{{ currentQuestion.stem }}</p>
        </div>

        <!-- 选择题选项 -->
        <div v-if="parsedOptions.length > 0" class="space-y-3 mb-8">
          <button
            v-for="(opt, idx) in parsedOptions"
            :key="idx"
            :class="[
              'w-full text-left p-4 md:p-5 rounded-xl border-2 transition-all font-body-md text-body-md',
              showResult && submitResult?.correctAnswerKey === opt.key
                ? 'border-green-500 bg-green-50 dark:bg-green-900/30 text-green-800 dark:text-green-300'
                : showResult && selectedAnswer === opt.key && !submitResult?.isCorrect
                  ? 'border-red-400 bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300'
                  : selectedAnswer === opt.key
                    ? 'border-primary bg-primary-container/20 text-primary'
                    : 'border-outline-variant/30 hover:border-primary/50 text-on-surface'
            ]"
            :disabled="showResult"
            @click="selectAnswer(opt.key)"
          >
            <div class="flex items-center gap-3">
              <span
                :class="[
                  'w-8 h-8 rounded-full flex items-center justify-center font-label-lg text-label-lg shrink-0',
                  showResult && submitResult?.correctAnswerKey === opt.key
                    ? 'bg-green-500 text-white'
                    : showResult && selectedAnswer === opt.key && !submitResult?.isCorrect
                      ? 'bg-red-400 text-white'
                      : selectedAnswer === opt.key
                        ? 'bg-primary text-on-primary'
                        : 'bg-surface-variant text-on-surface-variant'
                ]"
              >
                <span v-if="showResult && submitResult?.correctAnswerKey === opt.key">check</span>
                <span v-else-if="showResult && selectedAnswer === opt.key && !submitResult?.isCorrect">close</span>
                <span v-else>{{ opt.key }}</span>
              </span>
              <span class="flex-1">{{ opt.text }}</span>
            </div>
          </button>
        </div>

        <!-- 填空题输入框 -->
        <div v-else class="mb-8">
          <div class="flex justify-center">
            <input
              v-model="selectedAnswer"
              class="w-full max-w-md text-center py-4 px-6 rounded-xl border-2 text-xl font-semibold outline-none transition-colors bg-surface-container-low dark:bg-surface-container-high text-on-surface placeholder-on-surface-variant"
              :class="showResult
                ? (submitResult?.isCorrect
                    ? 'border-green-500 bg-green-50 dark:bg-green-900/30'
                    : 'border-red-400 bg-red-50 dark:bg-red-900/30')
                : 'border-outline-variant/30 focus:border-primary'"
              :disabled="showResult"
              :placeholder="$t('learning.inputAnswer')"
              @keyup.enter="submitCurrentAnswer"
            />
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="max-w-3xl mx-auto space-y-4">
          <button
            v-if="!showResult"
            class="w-full bg-primary text-on-primary py-4 rounded-xl font-label-lg text-label-lg hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!selectedAnswer"
            @click="submitCurrentAnswer"
          >
            {{ isLastQuestion ? $t('learning.confirmSubmit') : $t('learning.submit') }}
          </button>

          <!-- 答题反馈 -->
          <div v-if="showResult" class="space-y-4">
            <div
              class="p-4 rounded-xl font-label-lg text-label-lg text-center"
              :class="submitResult?.isCorrect ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300' : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300'"
            >
              <span class="material-symbols-outlined align-middle mr-1">{{ submitResult?.isCorrect ? 'check_circle' : 'cancel' }}</span>
              {{ submitResult?.isCorrect ? $t('learning.correct') : $t('learning.incorrect') }}
            </div>
            <div v-if="submitResult?.correctAnswer" class="bg-surface rounded-xl p-4 border border-outline-variant/20">
              <p class="font-label-md text-label-md text-on-surface-variant mb-1">{{ $t('learning.correctAnswer') }}</p>
              <p class="font-body-md text-body-md text-primary font-semibold">{{ submitResult.correctAnswer }}</p>
            </div>
            <div v-if="submitResult?.analysis" class="bg-surface rounded-xl p-4 border border-outline-variant/20">
              <p class="font-label-md text-label-md text-on-surface-variant mb-1">{{ $t('learning.analysis') }}</p>
              <p class="font-body-md text-body-md text-on-surface">{{ submitResult.analysis }}</p>
            </div>
          <button
            v-if="showResult"
            class="w-full bg-surface text-on-surface border border-outline-variant py-3 rounded-xl font-label-md text-label-md hover:bg-primary-container/20 transition-colors flex items-center justify-center gap-2"
            @click="handleFavorite"
          >
            <span class="material-symbols-outlined text-lg">favorite</span>
            {{ $t('learning.favoriteSentence') }}
          </button>
            <button
              class="w-full bg-primary text-on-primary py-4 rounded-xl font-label-lg text-label-lg hover:bg-primary/90 transition-colors"
              @click="goNext"
            >
              {{ isLastQuestion ? $t('learning.viewResults') : $t('learning.next') }}
            </button>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-20">
        <span class="material-symbols-outlined text-4xl text-outline">inbox</span>
        <p class="mt-4 text-on-surface-variant">{{ $t('learning.noQuestions') }}</p>
        <button class="mt-4 bg-primary text-on-primary px-6 py-2 rounded-full" @click="$router.back()">
          {{ $t('learning.backToHub') }}
        </button>
      </div>
    </template>

    <!-- ===== 结果回顾页 ===== -->
    <template v-else>
      <div class="max-w-3xl mx-auto">
        <div class="bg-surface rounded-2xl p-8 shadow-soft-glow text-center mb-8">
          <div class="w-20 h-20 rounded-full bg-primary-container/20 flex items-center justify-center mx-auto mb-4">
            <span class="material-symbols-outlined text-4xl text-primary">emoji_events</span>
          </div>
          <h3 class="font-headline-lg text-headline-lg text-on-surface mb-2">{{ $t('learning.practiceComplete') }}</h3>
          <div class="flex items-center justify-center gap-8 mt-6">
            <div class="text-center">
              <p class="font-display-lg text-display-lg text-primary font-bold">{{ correctCount }}</p>
              <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.correctLabel') }}</p>
            </div>
            <div class="text-center">
              <p class="font-display-lg text-display-lg text-on-surface font-bold">{{ questions.length - correctCount }}</p>
              <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.incorrectLabel') }}</p>
            </div>
            <div class="text-center">
              <p class="font-display-lg text-display-lg text-on-surface font-bold">{{ accuracy }}%</p>
              <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.accuracy') }}</p>
            </div>
          </div>
        </div>

        <!-- 逐题回顾 -->
        <div class="space-y-4">
          <h4 class="font-headline-md text-headline-md text-on-surface mb-4">{{ $t('learning.questionReview') }}</h4>
          <div
            v-for="(item, idx) in questionResults"
            :key="idx"
            class="bg-surface rounded-xl p-5 shadow-soft-glow border-l-4"
            :class="item.submitResult?.isCorrect ? 'border-l-green-500' : 'border-l-red-400'"
          >
            <div class="flex items-start gap-3">
              <span
                class="w-7 h-7 rounded-full flex items-center justify-center text-sm shrink-0 mt-0.5"
                :class="item.submitResult?.isCorrect ? 'bg-green-100 dark:bg-green-900/50 text-green-700 dark:text-green-300' : 'bg-red-100 dark:bg-red-900/50 text-red-700 dark:text-red-300'"
              >
                <span class="material-symbols-outlined text-sm">{{ item.submitResult?.isCorrect ? 'check' : 'close' }}</span>
              </span>
              <div class="flex-1 min-w-0">
                <p class="font-body-md text-body-md text-on-surface mb-2">{{ item.question.stem }}</p>
                <div class="flex flex-wrap gap-x-4 gap-y-1 font-label-sm text-label-sm text-on-surface-variant">
                  <span>{{ $t('learning.yourAnswer') }}：<span :class="item.submitResult?.isCorrect ? 'text-green-600 dark:text-green-400' : 'text-red-500 dark:text-red-400'">{{ item.selectedAnswer }}</span></span>
                  <span v-if="!item.submitResult?.isCorrect">{{ $t('learning.correctAnswer') }}：<span class="text-primary font-semibold">{{ item.submitResult?.correctAnswer }}</span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 返回按钮 -->
        <div class="mt-8 flex gap-4">
          <button
            class="flex-1 bg-surface text-on-surface border border-outline-variant py-4 rounded-xl font-label-lg text-label-lg hover:bg-surface-variant transition-colors"
            @click="$router.back()"
          >
            {{ $t('learning.backToHub') }}
          </button>
          <button
            class="flex-1 bg-primary text-on-primary py-4 rounded-xl font-label-lg text-label-lg hover:bg-primary/90 transition-colors"
            @click="restartPractice"
          >
            {{ $t('learning.practiceAgain') }}
          </button>
        </div>
      </div>
    </template>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { selectQuestions, submitAnswer, addFavorite } from '@/api/exercise'
import type { QuestionVO, SubmitResponse } from '@/api/exercise'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const moduleType = computed(() => Number(route.query.moduleType) || 1)
const count = computed(() => Number(route.query.count) || 10)

const moduleName = computed(() => {
  const names: Record<number, string> = {
    1: t('dashboard.moduleVocabulary'),
    2: t('dashboard.moduleGrammar'),
    3: t('dashboard.moduleTranslation'),
    4: t('dashboard.moduleListening')
  }
  return names[moduleType.value] || names[1]
})

// ========== 练习状态 ==========
const loading = ref(false)
const questions = ref<QuestionVO[]>([])
const currentQuestionIndex = ref(0)
const selectedAnswer = ref('')
const showResult = ref(false)
const submitResult = ref<SubmitResponse | null>(null)
const practiceStartTime = ref(Date.now())

// ========== 存储所有答题结果 ==========
export interface QuestionResultItem {
  question: QuestionVO
  selectedAnswer: string
  submitResult: SubmitResponse
}
const questionResults = ref<QuestionResultItem[]>([])
const showResults = ref(false)

const correctCount = computed(() =>
  questionResults.value.filter(r => r.submitResult?.isCorrect).length
)

const accuracy = computed(() =>
  questions.value.length > 0
    ? Math.round((correctCount.value / questions.value.length) * 100)
    : 0
)

const progressPercent = computed(() =>
  questions.value.length > 0
    ? ((currentQuestionIndex.value + (showResult.value ? 1 : 0)) / questions.value.length) * 100
    : 0
)

const isLastQuestion = computed(() =>
  currentQuestionIndex.value >= questions.value.length - 1
)

const currentQuestion = computed(() =>
  questions.value.length > 0 ? questions.value[currentQuestionIndex.value] : null
)

// ========== 选项解析 ==========
const parsedOptions = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.options) return []
  try {
    const opts = JSON.parse(q.options)
    if (Array.isArray(opts)) {
      return opts.map((item: string, idx: number) => {
        const match = String(item).match(/^([A-D])[.\s]*\s*(.+)/)
        if (match) return { key: match[1], text: match[2].trim() }
        return { key: String.fromCharCode(65 + idx), text: String(item).trim() }
      })
    }
    return Object.entries(opts).map(([key, text]) => ({
      key, text: String(text).replace(/^[A-D][.\s]*\s*/, '').trim()
    }))
  } catch {
    return q.options.split('\n').filter(Boolean).map((line, idx) => {
      const match = line.match(/^([A-D])[.\s]*\s*(.+)/)
      if (match) return { key: match[1], text: match[2].trim() }
      return { key: String.fromCharCode(65 + idx), text: line.trim() }
    })
  }
})

// ========== 进度持久化 ==========
const saveKey = computed(() => `practice-progress-${moduleType.value}`)

interface PracticeSavedProgress {
  userId: number
  questions: QuestionVO[]
  currentQuestionIndex: number
  questionResults: QuestionResultItem[]
  moduleType: number
}

const saveProgress = () => {
  const userId = userStore.userId
  if (!userId || questions.value.length === 0) return
  const data: PracticeSavedProgress = {
    userId,
    questions: questions.value,
    currentQuestionIndex: currentQuestionIndex.value,
    questionResults: questionResults.value,
    moduleType: moduleType.value,
  }
  sessionStorage.setItem(saveKey.value, JSON.stringify(data))
}

const clearProgress = () => sessionStorage.removeItem(saveKey.value)

const restoreProgress = (): PracticeSavedProgress | null => {
  try {
    const raw = sessionStorage.getItem(saveKey.value)
    if (!raw) return null
    const data = JSON.parse(raw) as PracticeSavedProgress
    if (!data.questions?.length || data.userId !== userStore.userId || data.moduleType !== moduleType.value) {
      clearProgress()
      return null
    }
    return data
  } catch {
    clearProgress()
    return null
  }
}

// ========== 加载题目 ==========
const loadQuestions = async () => {
  const userId = userStore.userId
  if (!userId) return

  // 尝试恢复上次进度
  const saved = restoreProgress()
  if (saved && saved.currentQuestionIndex < saved.questions.length) {
    loading.value = true
    questions.value = saved.questions
    questionResults.value = saved.questionResults
    // 回溯到第一个未回答的题目
    const answeredIds = new Set(saved.questionResults.map(it => it.question.id))
    let resumeIndex = saved.currentQuestionIndex
    if (answeredIds.has(saved.questions[resumeIndex]?.id)) {
      resumeIndex = Math.min(resumeIndex + 1, saved.questions.length - 1)
    }
    currentQuestionIndex.value = resumeIndex
    selectedAnswer.value = ''
    showResult.value = false
    submitResult.value = null
    showResults.value = false
    loading.value = false
    practiceStartTime.value = Date.now()
    return
  }

  // 无保存进度 → 全新加载
  clearProgress()
  loading.value = true
  questions.value = []
  currentQuestionIndex.value = 0
  selectedAnswer.value = ''
  showResult.value = false
  submitResult.value = null
  questionResults.value = []
  showResults.value = false

  try {
    const qs = await selectQuestions(userId, moduleType.value, count.value)
    questions.value = qs || []
  } catch (e) {
    console.warn('获取题目失败', e)
  } finally {
    loading.value = false
  }
}

// ========== 答题逻辑 ==========
const selectAnswer = (key: string) => {
  if (showResult.value) return
  selectedAnswer.value = key
}

const submitCurrentAnswer = async () => {
  const userId = userStore.userId
  const q = currentQuestion.value
  if (!userId || !q || !selectedAnswer.value) return

  // 选择题提交选项文本（更符合语义），填空题提交用户输入内容
  // 旧逻辑直接提交选项字母（A/B/C/D）对词语排序题不合理，改为提交选项对应的文本
  let answerToSubmit = selectedAnswer.value
  if (parsedOptions.value.length > 0) {
    const matched = parsedOptions.value.find(o => o.key === selectedAnswer.value)
    if (matched) answerToSubmit = matched.text
  }

  try {
    const result = await submitAnswer({
      userId,
      questionId: q.id,
      userAnswer: answerToSubmit,
      moduleType: q.moduleType || moduleType.value,
      durationSeconds: Math.round((Date.now() - practiceStartTime.value) / 1000),
    })
    submitResult.value = result
    showResult.value = true
    // 存入结果（同步保存选项文本，便于结果回顾页展示真实答案）
    questionResults.value.push({
      question: q,
      selectedAnswer: answerToSubmit,
      submitResult: result,
    })
    saveProgress()
  } catch (e) {
    console.warn('提交答案失败', e)
  }
}

const goNext = () => {
  if (!isLastQuestion.value) {
    currentQuestionIndex.value += 1
    selectedAnswer.value = ''
    showResult.value = false
    submitResult.value = null
    practiceStartTime.value = Date.now()
  } else {
    showResults.value = true
    clearProgress()
  }
}

const handleFavorite = async () => {
  const q = currentQuestion.value
  const uid = userStore.userId
  if (!uid || !q) return
  try {
    await addFavorite({
      userId: uid,
      sentenceText: q.stem,
      questionId: q.id,
    })
    // Visual feedback - briefly show the button text change
    const btn = document.activeElement
    if (btn) {
      const original = btn.innerHTML
      btn.innerHTML = `<span class="material-symbols-outlined text-lg">favorite</span> ${t('learning.favorited')}`
      setTimeout(() => { btn.innerHTML = original }, 1500)
    }
  } catch (e) {
    console.warn('收藏失败', e)
  }
}

const restartPractice = () => {
  clearProgress()
  loadQuestions()
}

onMounted(() => {
  if (userStore.userId) {
    loadQuestions()
  } else {
    router.push('/login')
  }
})
</script>
