<!--
  词汇练习页面
  视觉参考设计稿：左侧 SideNavBar + 顶部奖杯进度条 + 居中题目卡片 + 底部操作栏
  适用题型：FILL_BLANK（词汇填空）
  真实数据：selectQuestions(userId, 1, 10)
-->
<template>
  <div class="bg-background text-on-background min-h-screen flex">
    <!-- 左侧导航栏 -->
    <SideNavBar />

    <!-- 主内容区 + AI侧边栏 容器 -->
    <div class="flex-1 flex min-w-0">
      <!-- 主内容区：w-full 占满可用空间，max-w-3xl 限制最大宽度，mx-auto 水平居中 -->
      <main class="w-full max-w-3xl mx-auto flex flex-col min-h-screen relative">
      <!-- 顶部栏：返回 + 奖杯进度条 + 后台操作 -->
      <header class="h-20 sticky top-0 z-40 bg-background/80 backdrop-blur-md flex items-center justify-between px-gutter w-full">
        <div class="flex items-center gap-4 flex-1">
          <!-- 返回按钮 -->
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all"
            :aria-label="$t('common.back')"
            @click="goBack"
          >
            <span class="material-symbols-outlined">arrow_back_ios_new</span>
          </button>

          <!-- 奖杯 + 进度条 -->
          <div class="flex-1 max-w-md mx-auto relative flex items-center">
            <div class="absolute left-0 top-1/2 -translate-y-1/2 -ml-2 bg-white dark:bg-surface shadow-lg rounded-full w-14 h-14 flex items-center justify-center z-10 border-4 border-background dark:border-surface">
              <span
                class="material-symbols-outlined text-[#1a7ba5] dark:text-[#4db8d6] text-2xl"
                style="font-variation-settings: 'FILL' 1;"
              >emoji_events</span>
            </div>
            <div class="w-full h-12 bg-gray-100 dark:bg-surface-variant/50 rounded-full overflow-hidden ml-4 relative flex items-center">
              <div
                class="h-full bg-[#7c6fae] dark:bg-primary/80 transition-all duration-500 ease-out"
                :style="{ width: progressPercent + '%' }"
              ></div>
              <span class="absolute inset-0 flex items-center justify-center text-lg font-bold text-[#5d4e8a] dark:text-primary">
                {{ answeredCount }}/{{ totalCount }}
              </span>
            </div>
          </div>
        </div>

        <!-- 右侧操作按钮组 -->
        <div class="flex items-center gap-2">
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all"
            :aria-label="appStore.isDark ? $t('topbar.switchToLight') : $t('topbar.switchToDark')"
            @click="appStore.toggleTheme"
          >
            <span class="material-symbols-outlined">
              {{ appStore.isDark ? 'light_mode' : 'dark_mode' }}
            </span>
          </button>
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all relative"
            :aria-label="$t('topbar.notifications')"
          >
            <span class="material-symbols-outlined">notifications</span>
            <span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full"></span>
          </button>
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all"
            :aria-label="$t('topbar.currentLanguage')"
            @click="cycleLanguage"
          >
            <span class="material-symbols-outlined">language</span>
          </button>
          <div
            class="w-8 h-8 rounded-full bg-primary text-on-primary ml-2 flex items-center justify-center cursor-pointer hover:ring-2 hover:ring-primary/50 transition-all text-sm font-bold select-none overflow-hidden"
            :title="userStore.username || ''"
          >
            <img v-if="quizAvatarUrl" :src="quizAvatarUrl" :alt="$t('profile.userInfo')" class="w-full h-full object-cover" />
            <span v-else>{{ avatarText }}</span>
          </div>
        </div>
      </header>

      <!-- 题目画布 -->
      <div class="flex-1 w-full max-w-container-max mx-auto px-gutter py-8 flex flex-col relative">
        <!-- 加载态 -->
        <div v-if="loading" class="flex-1 flex flex-col items-center justify-center">
          <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
          <p class="mt-4 text-on-surface-variant">{{ $t('common.loading') }}</p>
        </div>

        <!-- 答题中 -->
        <template v-else-if="!showResults && currentQuestion">
          <!-- 题目卡片 -->
          <div
            class="bg-surface rounded-3xl p-8 md:p-12 shadow-[0_4px_20px_rgba(51,58,72,0.05)] w-full max-w-3xl mx-auto relative border border-outline-variant/20 mt-8 mb-24"
          >
            <!-- 左上：难度等级徽章 -->
            <div class="absolute top-6 left-6 flex items-center gap-2">
              <span class="bg-secondary-fixed/50 text-secondary font-label-md text-label-md px-3 py-1 rounded-full flex items-center gap-1">
                {{ $t('learning.difficultyLevel', { level: currentDifficulty }) }}
                <span class="material-symbols-outlined text-[14px]">help</span>
              </span>
            </div>
            <!-- 右上：全新徽章 -->
            <div class="absolute top-6 right-6">
              <span class="bg-error text-on-error font-label-md text-label-md px-3 py-1 rounded-full relative shadow-sm">
                {{ $t('learning.brandNew') }}
                <span class="absolute -bottom-1 right-3 w-2 h-2 bg-error transform rotate-45"></span>
              </span>
            </div>

            <div class="mt-8 space-y-12">
              <!-- 中文题干（来自 analysis 字段，括号高亮） -->
              <div
                class="font-headline-md text-headline-md text-on-surface leading-relaxed text-center"
              >
                <span v-html="chineseDisplay"></span>
              </div>

              <!-- 英文题干 + 填空输入框 -->
              <div
                class="font-headline-lg text-headline-lg text-on-background leading-relaxed text-center flex flex-wrap justify-center items-center gap-x-2 gap-y-4"
              >
                <template v-for="(part, pIdx) in englishParts" :key="pIdx">
                  <span v-if="!part.isBlank" class="whitespace-pre-wrap">{{ part.text }}</span>
                  <div v-else class="relative inline-block w-48 mx-2">
                    <input
                      ref="blankInput"
                      v-model="userAnswer"
                      class="w-full h-14 bg-surface-container-low border-b-2 rounded-t-lg px-3 text-center font-headline-md text-headline-md transition-all duration-200 outline-none"
                      :class="showFeedback
                        ? (isCorrect
                          ? 'border-green-500 text-green-700 dark:text-green-300 bg-green-50/50 dark:bg-green-900/20'
                          : 'border-red-500 text-red-600 dark:text-red-400 bg-red-50/50 dark:bg-red-900/20')
                        : inputStatus === 'wrong'
                          ? 'border-red-500 text-red-600 dark:text-red-400 bg-red-50/60 dark:bg-red-900/30 shadow-[0_0_0_3px_rgba(239,68,68,0.15)]'
                          : inputStatus === 'correct'
                            ? 'border-green-500 text-green-700 dark:text-green-400 bg-green-50/60 dark:bg-green-900/30 shadow-[0_0_0_3px_rgba(34,197,94,0.15)]'
                            : 'border-primary-container focus:border-primary text-primary focus:outline-none focus:shadow-[0_0_0_3px_rgba(0,0,0,0.05)]'"
                      :disabled="showFeedback"
                      :placeholder="''"
                      @keyup.enter="handleSubmit"
                    />
                    <span
                      v-if="!showFeedback && inputStatus !== 'correct'"
                      class="absolute left-3 top-1/2 -translate-y-1/2 w-0.5 h-7 bg-primary animate-pulse pointer-events-none"
                    ></span>
                    <!-- 实时拼写状态图标 -->
                    <span
                      v-if="inputStatus === 'correct' && !showFeedback"
                      class="material-symbols-outlined absolute -right-8 top-1/2 -translate-y-1/2 text-green-500 text-2xl pointer-events-none animate-pulse"
                    >check_circle</span>
                    <span
                      v-if="inputStatus === 'wrong' && !showFeedback && userAnswer.trim().length >= 2"
                      class="material-symbols-outlined absolute -right-8 top-1/2 -translate-y-1/2 text-red-500 text-2xl pointer-events-none"
                    >error</span>
                  </div>
                </template>
              </div>

              <!-- 来源标签 -->
              <div class="text-center font-label-md text-label-md text-outline-variant pt-4">
                {{ $t('learning.questionSource') }}
              </div>

              <!-- 答题反馈 -->
              <div v-if="showFeedback" class="mt-6 space-y-3">
                <div
                  class="p-3 rounded-xl font-label-lg text-label-lg text-center"
                  :class="isCorrect
                    ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                    : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300'"
                >
                  <span class="material-symbols-outlined align-middle mr-1">{{ isCorrect ? 'check_circle' : 'cancel' }}</span>
                  {{ isCorrect ? $t('learning.correct') : $t('learning.incorrect') }}
                </div>
                <!-- 语义匹配：判对但展示标准答案供参考 -->
                <div v-if="isCorrect && submitResult?.isSemanticMatch && submitResult" class="bg-surface-container-low rounded-xl p-4 space-y-3">
                  <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg p-3">
                    <p class="font-label-sm text-label-sm text-amber-700 dark:text-amber-300 mb-1">{{ $t('learning.bestAnswer') }}</p>
                    <p class="font-body-md text-body-md text-amber-800 dark:text-amber-200 font-semibold">{{ submitResult.correctAnswer }}</p>
                  </div>
                  <div v-if="submitResult.heuristicHint">
                    <p class="font-body-sm text-body-sm text-on-surface-variant">{{ submitResult.heuristicHint }}</p>
                  </div>
                  <div v-if="submitResult.analysis">
                    <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.analysis') }}</p>
                    <p class="font-body-md text-body-md text-on-surface">{{ submitResult.analysis }}</p>
                  </div>
                </div>
                <div v-if="!isCorrect && submitResult" class="bg-surface-container-low rounded-xl p-4 space-y-2">
                  <div>
                    <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.correctAnswer') }}</p>
                    <p class="font-body-md text-body-md text-primary font-semibold">{{ submitResult.correctAnswer }}</p>
                  </div>
                  <div v-if="chineseWord">
                    <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.chineseMeaning') }}</p>
                    <p class="font-body-md text-body-md text-on-surface font-semibold">{{ chineseWord }}</p>
                  </div>
                  <div v-if="submitResult.analysis">
                    <p class="font-label-sm text-label-sm text-on-surface-variant mb-1">{{ $t('learning.analysis') }}</p>
                    <p class="font-body-md text-body-md text-on-surface">{{ submitResult.analysis }}</p>
                  </div>
                </div>
                <!-- 收藏按钮 -->
                <button
                  class="w-full bg-surface text-on-surface border border-outline-variant py-3 rounded-xl font-label-md text-label-md hover:bg-primary-container/20 transition-colors flex items-center justify-center gap-2"
                  @click="handleFavorite"
                >
                  <span class="material-symbols-outlined text-lg">favorite</span>
                  {{ $t('learning.favoriteSentence') }}
                </button>
              </div>
            </div>
          </div>

          <!-- 实时拼写反馈提示卡片（拼错时显示区别提示） -->
          <Transition name="hint">
            <div
              v-if="showInstantHint"
              class="w-full max-w-3xl mx-auto -mt-16 mb-24 bg-[#fdecec] dark:bg-red-900/30 border border-red-200/60 dark:border-red-800/50 rounded-2xl p-5 shadow-sm flex items-start gap-3"
            >
              <span class="material-symbols-outlined text-red-400 text-2xl shrink-0 mt-0.5">lightbulb</span>
              <p class="flex-1 text-[15px] text-gray-800 dark:text-on-surface leading-relaxed">
                {{ instantHintMessage }}
              </p>
              <button
                class="shrink-0 w-9 h-9 rounded-full hover:bg-red-100/80 dark:hover:bg-red-900/50 flex items-center justify-center text-red-300 dark:text-red-400 transition-colors"
                :title="$t('learning.favoriteHint')"
              >
                <span class="material-symbols-outlined text-xl">favorite_border</span>
              </button>
            </div>
          </Transition>

          <!-- 底部操作工具栏 -->
          <div class="mt-auto flex justify-between items-end w-full max-w-3xl mx-auto pb-8">
            <div class="flex gap-8 pl-4">
              <!-- 查看提示 -->
              <button
                class="flex flex-col items-center gap-2 text-primary hover:text-primary-fixed-variant transition-colors group"
                :disabled="showFeedback"
                @click="toggleHint"
              >
                <div class="w-12 h-12 rounded-full bg-primary-container/20 flex items-center justify-center group-hover:bg-primary-container/30 transition-colors">
                  <span class="material-symbols-outlined text-[28px]">{{ showHint ? 'visibility_off' : 'help' }}</span>
                </div>
                <span class="font-label-md text-label-md font-bold">{{ showHint ? $t('learning.hideHint') : $t('learning.hint') }}</span>
              </button>

              <!-- 语音模式 -->
              <button
                v-if="speechSupported"
                class="flex flex-col items-center gap-2 text-primary hover:text-primary-fixed-variant transition-colors group"
                :disabled="showFeedback"
                @click="toggleSpeech"
              >
                <div class="w-12 h-12 rounded-full bg-primary-container/20 flex items-center justify-center group-hover:bg-primary-container/30 transition-colors relative">
                  <span class="material-symbols-outlined text-[28px]">{{ isListening ? 'mic_off' : 'mic' }}</span>
                  <div
                    v-if="!isListening"
                    class="absolute -top-12 left-1/2 -translate-x-1/2 bg-inverse-surface text-inverse-on-surface font-label-md text-label-md whitespace-nowrap px-3 py-1.5 rounded-md shadow-lg flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"
                  >
                    <span class="material-symbols-outlined text-[14px]">touch_app</span> {{ $t('learning.handsFreeMode') }}
                    <div class="absolute -bottom-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-inverse-surface transform rotate-45"></div>
                  </div>
                </div>
                <span class="font-label-md text-label-md font-bold">{{ isListening ? $t('learning.stopRecording') : $t('learning.voiceMode') }}</span>
              </button>

              <!-- 问 AI -->
              <button
                class="flex flex-col items-center gap-2 text-primary hover:text-primary-fixed-variant transition-colors group"
                @click="askAi"
              >
                <div class="w-12 h-12 rounded-full bg-primary-container/20 flex items-center justify-center group-hover:bg-primary-container/30 transition-colors">
                  <span class="material-symbols-outlined text-[28px]">smart_toy</span>
                </div>
                <span class="font-label-md text-label-md font-bold">{{ $t('learning.askAi') }}</span>
              </button>
            </div>

            <!-- 确认 / 下一题按钮 -->
            <button
              class="bg-primary text-on-primary font-headline-md text-headline-md font-bold py-4 px-12 rounded-2xl flex items-center gap-2 hover:bg-primary/90 transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!showFeedback && !userAnswer.trim()"
              @click="showFeedback ? goNextOrResults() : handleSubmit()"
            >
              <span class="material-symbols-outlined font-bold">{{ showFeedback ? 'arrow_forward' : 'check' }}</span>
              {{ showFeedback ? (isLastQuestion ? $t('learning.viewResults') : $t('learning.next')) : $t('learning.confirmBtn') }}
            </button>
          </div>

          <!-- 提示面板 -->
          <div
            v-if="showHint && !showFeedback"
            class="fixed bottom-32 left-1/2 -translate-x-1/2 bg-surface border border-primary/30 rounded-2xl p-4 shadow-lg max-w-md z-30"
          >
            <p class="font-label-md text-label-md text-primary mb-2">{{ $t('learning.hintTitle') }}</p>
            <p class="font-body-md text-body-md text-on-surface">
              {{ $t('learning.firstTwoLetters') }}<span class="text-primary font-bold text-lg">{{ hintLetters }}</span>
            </p>
            <p v-if="hintLoading" class="font-body-sm text-body-sm text-on-surface-variant mt-1 flex items-center gap-1">
              <span class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
              {{ $t('learning.loadingMoreHint') }}
            </p>
            <p v-else-if="questionAnswer" class="font-body-md text-body-md text-on-surface mt-1">
              {{ $t('learning.wordLengthLabel') }}<span class="text-primary font-semibold">{{ hintLength }}</span> {{ $t('learning.lettersCount') }}
            </p>
          </div>
        </template>

        <!-- 结果页 -->
        <template v-else-if="showResults">
          <div class="max-w-3xl mx-auto">
            <!-- 成绩卡片 -->
            <div class="bg-surface rounded-3xl p-8 md:p-12 shadow-soft-glow text-center mb-8 border border-outline-variant/20">
              <div class="w-20 h-20 rounded-full bg-primary-container/20 flex items-center justify-center mx-auto mb-4">
                <span
                  class="material-symbols-outlined text-4xl text-secondary"
                  style="font-variation-settings: 'FILL' 1;"
                >emoji_events</span>
              </div>
              <h3 class="font-headline-lg text-headline-lg text-on-surface mb-2">{{ $t('learning.practiceComplete') }}</h3>
              <div class="flex items-center justify-center gap-8 mt-6">
                <div class="text-center">
                  <p class="font-display-lg text-display-lg text-primary font-bold">{{ correctCount }}</p>
                  <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.correctLabel') }}</p>
                </div>
                <div class="text-center">
                  <p class="font-display-lg text-display-lg text-on-surface font-bold">{{ totalCount - correctCount }}</p>
                  <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.incorrectLabel') }}</p>
                </div>
                <div class="text-center">
                  <p class="font-display-lg text-display-lg text-on-surface font-bold">{{ accuracy }}%</p>
                  <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.accuracy') }}</p>
                </div>
              </div>
            </div>

            <!-- 逐题回顾 -->
            <div class="space-y-4 mb-8">
              <h4 class="font-headline-md text-headline-md text-on-surface mb-4">{{ $t('learning.questionReview') }}</h4>
              <div
                v-for="(item, idx) in results.items"
                :key="idx"
                class="bg-surface rounded-xl p-5 shadow-soft-glow border-l-4"
                :class="item.correct ? 'border-l-green-500' : 'border-l-red-400'"
              >
                <div class="flex items-start gap-3">
                  <span
                    class="w-7 h-7 rounded-full flex items-center justify-center text-sm shrink-0 mt-0.5"
                    :class="item.correct ? 'bg-green-100 dark:bg-green-900/50 text-green-700 dark:text-green-300' : 'bg-red-100 dark:bg-red-900/50 text-red-700 dark:text-red-300'"
                  >
                    <span class="material-symbols-outlined text-sm">{{ item.correct ? 'check' : 'close' }}</span>
                  </span>
                  <div class="flex-1 min-w-0">
                    <p class="font-body-md text-body-md text-on-surface mb-2">{{ item.question.stem }}</p>
                    <div class="flex flex-wrap gap-x-4 gap-y-1 font-label-sm text-label-sm text-on-surface-variant">
                      <span>{{ $t('learning.yourAnswer') }}：<span :class="item.correct ? 'text-green-600 dark:text-green-400' : 'text-red-500 dark:text-red-400'">{{ item.answer }}</span></span>
                      <span v-if="!item.correct">{{ $t('learning.correctAnswer') }}：<span class="text-primary font-semibold">{{ item.question.correctAnswer || '' }}</span></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex gap-4">
              <button
                class="flex-1 bg-surface text-on-surface border border-outline-variant py-4 rounded-xl font-label-lg text-label-lg hover:bg-surface-variant transition-colors"
                @click="goBack"
              >
                {{ $t('learning.backToHub') }}
              </button>
              <button
                class="flex-1 bg-primary text-on-primary py-4 rounded-xl font-label-lg text-label-lg hover:bg-primary/90 transition-colors"
                @click="restartQuiz"
              >
                {{ $t('learning.practiceAgain') }}
              </button>
            </div>
          </div>
        </template>

        <!-- 无题 -->
        <div v-else class="flex-1 flex flex-col items-center justify-center py-20">
          <span class="material-symbols-outlined text-4xl text-outline">inbox</span>
          <p class="mt-4 text-on-surface-variant">{{ $t('learning.noQuestions') }}</p>
          <button
            class="mt-4 bg-primary text-on-primary px-6 py-2 rounded-full"
            @click="goBack"
          >
            {{ $t('learning.backToHub') }}
          </button>
        </div>
      </div>
    </main>

      <!-- AI 侧边栏（inline 模式，不覆盖） -->
      <AiSidebar mode="inline" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 词汇练习页 — 填空题（CHOICE 模式已拆出去，仅留 FILL_BLANK）
 * 数据流：selectQuestions(userId, 1, 10) → 逐题作答 → submitAnswer → 记录结果
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import SideNavBar from '@/components/SideNavBar.vue'
import AiSidebar from '@/components/AiSidebar.vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { selectQuestions, submitAnswer as submitAnswerApi, getQuestionById, addFavorite } from '@/api/exercise'
import { useSpeechRecognition } from '@/composables/useSpeechRecognition'
import { useAiStore } from '@/stores/ai'
import type { QuestionVO, SubmitResponse } from '@/api/exercise'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const appStore = useAppStore()
const { isListening, transcript, error: speechError, isSupported: speechSupported, start: startSpeech, stop: stopSpeech } = useSpeechRecognition()

// ========== 状态 ==========
const loading = ref(true)
const questions = ref<QuestionVO[]>([])
const currentIndex = ref(0)
const userAnswer = ref('')
const showFeedback = ref(false)
const isCorrect = ref(false)
const submitResult = ref<SubmitResponse | null>(null)
const showHint = ref(false)
const showResults = ref(false)
const startTime = ref(Date.now())
const blankInput = ref<HTMLInputElement | null>(null)
const questionAnswer = ref('')
const hintLoading = ref(false)

interface ResultItem { question: QuestionVO; answer: string; correct: boolean }
const results = ref<{ correct: number; total: number; items: ResultItem[] }>({ correct: 0, total: 0, items: [] })

// ========== 计算属性 ==========
const totalCount = computed(() => questions.value.length)
const currentQuestion = computed<QuestionVO | null>(() => questions.value[currentIndex.value] || null)
const correctCount = computed(() => results.value.correct)
const accuracy = computed(() =>
  results.value.total > 0 ? Math.round((results.value.correct / results.value.total) * 100) : 0
)
const answeredCount = computed(() => currentIndex.value + (showResults.value ? 1 : 0))
const isLastQuestion = computed(() => currentIndex.value >= totalCount.value - 1)
const progressPercent = computed(() => totalCount.value > 0 ? (answeredCount.value / totalCount.value) * 100 : 0)
const currentDifficulty = computed(() => currentQuestion.value?.difficultyLevel || 1)

/**
 * 中文题干：来自后端 analysis 字段，格式 "公司于2005年（成立），此后迅速发展壮大。"
 * 紫色高亮括号内的目标词
 */
const chineseDisplay = computed(() => {
  const q = currentQuestion.value
  if (!q) return ''
  const text = q.analysis || ''
  // 把所有 (xxx) 替换为高亮 span
  return text.replace(/[（(]([^）)]+)[）)]/g,
    '<span class="text-tertiary-container font-bold px-1">$1</span>')
})

interface EnglishPart { text: string; isBlank: boolean }
/**
 * 英文题干：按 ___ 拆分为 prefix + blank + suffix
 * 例 "I work in a factory that ___ car parts." → ["I work in a factory that ", blank, " car parts."]
 */
const englishParts = computed<EnglishPart[]>(() => {
  const q = currentQuestion.value
  if (!q || !q.stem) return [{ text: '', isBlank: true }]
  const blankRegex = /_{2,}|\[BLANK\]|\.{3,}/g
  const stem = q.stem
  if (!blankRegex.test(stem)) {
    // 没有占位符 → 整体作为非空段，输入框空着
    return [{ text: stem, isBlank: false }, { text: '', isBlank: true }]
  }
  const parts: EnglishPart[] = []
  const regex = /_{2,}|\[BLANK\]|\.{3,}/g
  let lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = regex.exec(stem)) !== null) {
    if (m.index > lastIndex) {
      parts.push({ text: stem.substring(lastIndex, m.index), isBlank: false })
    }
    parts.push({ text: '', isBlank: true })
    lastIndex = regex.lastIndex
  }
  if (lastIndex < stem.length) {
    parts.push({ text: stem.substring(lastIndex), isBlank: false })
  }
  return parts
})

/**
 * 提示：单词长度
 */
/** 从 analysis 中提取括号内的中文释义 */
const chineseWord = computed(() => {
  const text = currentQuestion.value?.analysis || ''
  const m = text.match(/[（(]([^）)]+)[）)]/)
  return m ? m[1] : ''
})

const hintLength = computed(() => questionAnswer.value.length || 0)

/**
 * 提示：前两个字母（优先用题目自带 hint，其次用预加载的答案）
 */
const hintLetters = computed(() => {
  if (currentQuestion.value?.hint) return currentQuestion.value.hint
  return questionAnswer.value?.length >= 2 ? questionAnswer.value.substring(0, 2) : ''
})

// ========== 实时拼写状态 ==========
/**
 * 输入状态：'empty' | 'typing' | 'correct' | 'wrong'
 * - empty  : 未输入
 * - typing : 输入中，且输入内容是正确答案的前缀（正常紫色）
 * - correct: 与正确答案完全匹配（绿色）
 * - wrong  : 输入内容偏离正确拼写（红色），触发即时提示
 */
const inputStatus = computed<'empty' | 'typing' | 'correct' | 'wrong'>(() => {
  const ans = userAnswer.value.trim().toLowerCase()
  if (!ans) return 'empty'
  const correct = questionAnswer.value?.trim().toLowerCase()
  if (!correct) return 'typing'
  if (ans === correct) return 'correct'
  // 输入内容是正确答案的前缀 → 仍在输入中，不报错
  if (correct.startsWith(ans)) return 'typing'
  // 输入长度 >= 正确答案长度 且不匹配 → 错误
  if (ans.length >= correct.length) return 'wrong'
  // 输入内容不是正确答案的前缀 → 拼写已偏离，报错
  return 'wrong'
})

/**
 * 即时提示文案：从 analysis 字段中抽取 "注意 X 和 Y 的区别" 信息
 * 匹配后端 analysis 文本中的常见格式，例如：
 *   "你可能知道这个单词，但搞混拼写了。注意 'produce'（生产）和 'product'（产品）的区别。"
 */
const instantHintMessage = computed(() => {
  const analysis = currentQuestion.value?.analysis || ''
  // 模式 1：注意 "X"（X）和 "Y"（Y）的区别
  let m = analysis.match(/注意\s*['"「]?([^'",」）)]+)['"」]?（([^）)]+)）[^和]*和\s*['"「]?([^'",」）)]+)['"」]?（([^）)]+)）[^的]*的区别/)
  if (m) {
    return `你可能知道这个单词，但搞混拼写了。注意 "${m[1]}"（${m[2]}）和 "${m[3]}"（${m[4]}）的区别。`
  }
  // 模式 2：注意 "X" 和 "Y" 的区别
  m = analysis.match(/注意\s*['"「]?([^'",」]+)['"」]?[^和]*和\s*['"「]?([^'",」]+)['"」]?[^的]*的区别/)
  if (m) {
    return `你可能知道这个单词，但搞混拼写了。注意 "${m[1]}" 和 "${m[2]}" 的区别。`
  }
  // 模式 3：仅区别提示
  if (analysis.includes('区别')) {
    return '你可能知道这个单词，但拼写有误，请注意形近词之间的区别。'
  }
  return '你可能知道这个单词，但拼写有误，请仔细检查。'
})

/**
 * 是否显示即时提示：
 * - 未提交过
 * - 拼写状态为 wrong
 * - 输入长度达到 2 个字符以上（避免太短误判）
 */
const showInstantHint = computed(() => {
  return !showFeedback.value && inputStatus.value === 'wrong' && userAnswer.value.trim().length >= 2
})

/**
 * 获取提示：请求后端返回正确答案，截取前两个字母展示
 */
const fetchHint = async () => {
  const q = currentQuestion.value
  if (!q || questionAnswer.value) return
  hintLoading.value = true
  try {
    const full = await getQuestionById(q.id, true)
    questionAnswer.value = full.correctAnswer || ''
  } catch (e) {
    console.warn('获取提示失败', e)
  } finally {
    hintLoading.value = false
  }
}

// ========== 进度持久化 ==========
const SAVE_KEY = 'vocab-quiz-progress'

interface SavedProgress {
  userId: number
  questions: QuestionVO[]
  currentIndex: number
  results: { correct: number; total: number; items: ResultItem[] }
}

const saveProgress = () => {
  const userId = userStore.userId
  if (!userId || questions.value.length === 0) return
  const data: SavedProgress = {
    userId,
    questions: questions.value,
    currentIndex: currentIndex.value,
    results: results.value,
  }
  sessionStorage.setItem(SAVE_KEY, JSON.stringify(data))
}

const clearProgress = () => sessionStorage.removeItem(SAVE_KEY)

const restoreProgress = (): SavedProgress | null => {
  try {
    const raw = sessionStorage.getItem(SAVE_KEY)
    if (!raw) return null
    const data = JSON.parse(raw) as SavedProgress
    if (!data.questions?.length || data.userId !== userStore.userId) {
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
  if (!userId) { router.push('/login'); return }

  // 尝试恢复上次进度（数量匹配当前每日目标才恢复）
  const dailyTarget = Number(localStorage.getItem('dailyTarget_vocab')) || 20
  const saved = restoreProgress()
  if (saved && saved.currentIndex < saved.questions.length && saved.questions.length === dailyTarget) {
    loading.value = true
    questions.value = saved.questions
    results.value = saved.results
    // 回溯到第一个未回答的题目：检查已保存的 currentIndex 是否已答过
    const answeredIds = new Set(saved.results.items.map(it => it.question.id))
    let resumeIndex = saved.currentIndex
    if (answeredIds.has(saved.questions[resumeIndex]?.id)) {
      resumeIndex = Math.min(resumeIndex + 1, saved.questions.length - 1)
    }
    currentIndex.value = resumeIndex
    userAnswer.value = ''
    showFeedback.value = false
    submitResult.value = null
    showResults.value = false
    showHint.value = false
    questionAnswer.value = ''
    hintLoading.value = false
    loading.value = false
    startTime.value = Date.now()
    nextTick(() => blankInput.value?.focus())
    // 预加载当前题的正确答案（恢复进度时）
    const currentQ = saved.questions[resumeIndex]
    if (currentQ) {
      preloadQuestionAnswer(currentQ.id)
    }
    return
  }

  // 无保存进度 → 全新加载
  clearProgress()
  loading.value = true
  questions.value = []
  currentIndex.value = 0
  userAnswer.value = ''
  showFeedback.value = false
  submitResult.value = null
  showResults.value = false
  showHint.value = false
  questionAnswer.value = ''
  hintLoading.value = false
  results.value = { correct: 0, total: 0, items: [] }
  try {
    const qs = await selectQuestions(userId, 1, dailyTarget)
    questions.value = qs || []
    // 预加载第一题的正确答案（供实时拼写反馈使用，不展示给用户）
    if (qs && qs.length > 0) {
      preloadQuestionAnswer(qs[0].id)
    }
  } catch (e) {
    console.warn('获取题目失败', e)
  } finally {
    loading.value = false
    nextTick(() => blankInput.value?.focus())
  }
}

/**
 * 后台预加载正确答案（用于实时拼写反馈，不会在 UI 上展示答案）
 */
const preloadQuestionAnswer = async (questionId: number) => {
  if (questionAnswer.value) return
  try {
    const full = await getQuestionById(questionId, true)
    questionAnswer.value = full.correctAnswer || ''
  } catch (e) {
    console.warn('预加载答案失败', e)
  }
}

// ========== 提示 ==========
const aiStore = useAiStore()

const askAi = () => {
  const q = currentQuestion.value
  let context = ''
  if (q) {
    if (q.correctAnswer) {
      context += '【核心单词】' + q.correctAnswer + '\n'
    }
    if (q.stem) {
      context += '【题干】' + q.stem + '\n'
    }
    if (q.analysis) {
      context += '【解析】' + q.analysis + '\n'
    }
  }
  aiStore.open({
    contextType: 'vocab',
    questionContent: context,
    userAnswer: userAnswer.value || '',
  })
}

const toggleHint = () => {
  showHint.value = !showHint.value
  if (showHint.value && !questionAnswer.value) {
    fetchHint()
  }
}

// ========== 答题 ==========
const handleSubmit = async () => {
  const q = currentQuestion.value
  if (!q || !userAnswer.value.trim() || showFeedback.value) return
  const userId = userStore.userId
  if (!userId) return
  try {
    const result = await submitAnswerApi({
      userId,
      questionId: q.id,
      userAnswer: userAnswer.value.trim(),
      moduleType: q.moduleType || 1,
      durationSeconds: Math.round((Date.now() - startTime.value) / 1000),
    })
    submitResult.value = result
    isCorrect.value = result.isCorrect
    showFeedback.value = true
    results.value.items.push({ question: q, answer: userAnswer.value.trim(), correct: result.isCorrect })
    if (result.isCorrect) results.value.correct++
    results.value.total++
    saveProgress()

    // 不自动跳转，让用户有时间阅读解析和收藏句子
    // 用户可点击"下一题"按钮手动跳转
  } catch (e) {
    console.warn('提交失败', e)
  }
}

const goNextOrResults = () => {
  if (isLastQuestion.value) {
    showResults.value = true
    clearProgress()
  } else {
    nextQuestion()
  }
}

const nextQuestion = () => {
  if (currentIndex.value < totalCount.value - 1) {
    currentIndex.value++
    userAnswer.value = ''
    showFeedback.value = false
    submitResult.value = null
    showHint.value = false
    questionAnswer.value = ''
    hintLoading.value = false
    startTime.value = Date.now()
    nextTick(() => blankInput.value?.focus())
    // 预加载下一题的正确答案
    const nextQ = questions.value[currentIndex.value]
    if (nextQ) {
      preloadQuestionAnswer(nextQ.id)
    }
  } else {
    showResults.value = true
    clearProgress()
  }
}

const goBack = () => { if (window.history.length > 1) router.back(); else router.push('/learning') }
const restartQuiz = () => { clearProgress(); loadQuestions() }

// ========== 收藏功能 ==========
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
    // 视觉反馈：短暂改变按钮文本
    const btn = document.activeElement as HTMLButtonElement | null
    if (btn) {
      const original = btn.innerHTML
      btn.innerHTML = `<span class="material-symbols-outlined text-lg">favorite</span> ${t('learning.favorited')}`
      btn.disabled = true
      setTimeout(() => { btn.innerHTML = original; btn.disabled = false }, 1500)
    }
  } catch (e) {
    console.warn('收藏失败', e)
  }
}

// ========== 语言切换 ==========
const cycleLanguage = () => {
  const langs = ['zh-CN', 'en-US']
  const idx = langs.indexOf(appStore.language)
  const next = langs[(idx + 1) % langs.length]
  appStore.setLanguage(next)
}

// 头像首字母
const avatarText = computed(() => {
  const name = userStore.user?.username || userStore.username || ''
  return name ? name.charAt(0).toUpperCase() : 'M'
})

// 头像完整URL
const quizAvatarUrl = computed(() => {
  const url = userStore.avatarUrl
  if (!url) return ''
  return `/api${url}`
})

// ========== 语音模式 ==========
const toggleSpeech = () => {
  if (isListening.value) stopSpeech()
  else startSpeech()
}
watch(transcript, (val) => {
  if (val) {
    userAnswer.value = val
    setTimeout(() => { if (!showFeedback.value) handleSubmit() }, 500)
  }
})
watch(speechError, (val) => { if (val) console.warn('语音错误:', val) })

// ========== 生命周期 ==========
onMounted(() => {
  aiStore.setInlineMode(true)
  if (userStore.userId) loadQuestions()
  startTime.value = Date.now()
})
onUnmounted(() => {
  aiStore.setInlineMode(false)
})
watch(() => userStore.userId, (id) => { if (id) loadQuestions() })

// 切换 tab 重新进入 → 重新拉题（如果是从其他页面切回）
watch(() => route.path, (p) => {
  if (p === '/vocab-quiz' && totalCount.value === 0 && !loading.value) {
    loadQuestions()
  }
})

// 用户开始输入时，如果答案还没加载，立即加载（确保实时拼写反馈可用）
watch(userAnswer, (val) => {
  if (val.trim() && !questionAnswer.value && currentQuestion.value) {
    preloadQuestionAnswer(currentQuestion.value.id)
  }
})
</script>

<style scoped>
/* 实时拼写提示卡片过渡动画 */
.hint-enter-active,
.hint-leave-active {
  transition: all 0.3s ease;
}
.hint-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.hint-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
