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
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all"
            :aria-label="$t('common.back')"
            @click="goBack"
          >
            <span class="material-symbols-outlined text-2xl">arrow_back_ios_new</span>
          </button>

          <div class="flex-1 max-w-md mx-auto relative flex items-center">
            <div class="absolute left-0 top-1/2 -translate-y-1/2 -ml-2 bg-white dark:bg-surface shadow-lg rounded-full w-14 h-14 flex items-center justify-center z-10 border-4 border-background dark:border-surface">
              <span class="material-symbols-outlined text-[#1a7ba5] dark:text-[#4db8d6] text-2xl" style="font-variation-settings: 'FILL' 1;">emoji_events</span>
            </div>
            <div class="w-full h-12 bg-gray-100 dark:bg-surface-variant/50 rounded-full overflow-hidden ml-4 relative flex items-center">
              <div class="h-full bg-[#7c6fae] dark:bg-primary/80 transition-all duration-500 ease-out" :style="{ width: progressPercent + '%' }"></div>
              <span class="absolute inset-0 flex items-center justify-center text-lg font-bold text-[#5d4e8a] dark:text-primary">
                {{ answeredCount }}/{{ totalCount }}
              </span>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all"
            :aria-label="appStore.isDark ? $t('topbar.switchToLight') : $t('topbar.switchToDark')"
            @click="appStore.toggleTheme"
          >
            <span class="material-symbols-outlined text-xl">{{ appStore.isDark ? 'light_mode' : 'dark_mode' }}</span>
          </button>
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all relative"
            :aria-label="$t('topbar.notifications')"
          >
            <span class="material-symbols-outlined text-xl">notifications</span>
            <span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full"></span>
          </button>
          <button
            class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-high/50 transition-all"
            :aria-label="$t('topbar.currentLanguage')"
            @click="cycleLanguage"
          >
            <span class="material-symbols-outlined text-xl">language</span>
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
      <div class="flex-1 p-gutter flex flex-col items-center max-w-container-max mx-auto w-full">
        <!-- 加载态 -->
        <div v-if="loading" class="flex-1 flex flex-col items-center justify-center py-20">
          <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
          <p class="mt-4 text-on-surface-variant font-body-md text-body-md">{{ $t('common.loading') }}</p>
        </div>

        <!-- 答题中 -->
        <template v-else-if="!showResults && currentQuestion">
          <!-- 主题目卡片：淡蓝渐变顶部 + 白色大圆角 -->
          <div class="bg-white dark:bg-surface-container-lowest rounded-[28px] w-full max-w-3xl shadow-[0_4px_24px_rgba(0,0,0,0.06)] overflow-hidden flex flex-col relative mb-12">
            <!-- 顶部渐变背景 -->
            <div class="h-28 bg-gradient-to-b from-[#c7e6ff] via-[#d9edff] to-transparent dark:from-[#1a3547] dark:via-[#1e3a4a] dark:to-transparent absolute top-0 left-0 w-full z-0 pointer-events-none"></div>

            <div class="p-8 z-10 flex flex-col flex-grow relative">
              <!-- 卡片头部：徽章 -->
              <div class="flex justify-between items-center mb-8">
                <!-- 左上：难度等级徽章 -->
                <div class="bg-[#cce8ff]/90 dark:bg-[#2a4757]/90 text-[#2a6b85] dark:text-[#9fdbf0] px-3.5 py-1.5 rounded-full flex items-center gap-1.5 text-sm font-medium">
                  {{ $t('learning.difficultyLevelGrammar', { level: currentDifficulty }) }}
                  <span class="material-symbols-outlined text-base">help</span>
                </div>
                <!-- 右上：New 红色徽章 -->
                <div class="bg-[#ff6b6b] text-white px-3.5 py-1.5 rounded-full text-sm font-medium shadow-sm flex items-center relative">
                  {{ $t('learning.brandNew') }}
                  <div class="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-0 h-0 border-l-[6px] border-l-transparent border-t-[6px] border-t-[#ff6b6b] border-r-[6px] border-r-transparent"></div>
                </div>
              </div>

              <!-- 任务提示：知识点 + 直译 -->
              <div class="flex flex-col gap-5 mb-8">
                <div class="flex items-start gap-4">
                  <div class="bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 px-3 py-1 rounded-lg text-sm font-medium whitespace-nowrap mt-0.5">{{ $t('learning.knowledgePoint') }}</div>
                  <p class="text-lg text-gray-800 dark:text-on-surface leading-relaxed">{{ knowledgePoint }}</p>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-gray-200/70 dark:bg-surface-variant/50 text-gray-500 dark:text-on-surface-variant px-3 py-1 rounded-lg text-sm font-medium whitespace-nowrap mt-0.5">{{ $t('learning.literalTranslation') }}</div>
                  <p class="text-lg text-gray-800 dark:text-on-surface leading-relaxed">{{ literalTranslation }}</p>
                </div>
              </div>

              <!-- 拼句输入区 -->
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-on-surface-variant/50">{{ $t('learning.sentenceAreaHint') }}</span>
                <button
                  v-if="selectedWords.length > 0 && !showFeedback"
                  class="text-xs text-error/70 hover:text-error flex items-center gap-1 transition-colors"
                  @click="clearAllWords()"
                >
                  <span class="material-symbols-outlined text-sm">delete</span>{{ $t('learning.clearBtn') }}
                </button>
              </div>
              <div
                class="bg-gray-50 dark:bg-surface-variant/30 rounded-2xl p-6 min-h-[140px] flex flex-wrap gap-3 content-start transition-all select-none"
                :class="[
                  showFeedback && !isCorrect ? 'ring-2 ring-red-300 bg-red-50/50 dark:bg-red-900/20' : '',
                  dragOverAnswerArea ? 'ring-2 ring-primary bg-primary-container/10' : ''
                ]"
                @dragover.prevent="handleAnswerAreaDragOver"
                @dragleave="handleAnswerAreaDragLeave"
                @drop="handleAnswerAreaDrop"
              >
                <template v-if="selectedWords.length > 0">
                  <div
                    v-for="(token, idx) in selectedWords"
                    :key="'sel-' + idx + '-' + token.bankIdx"
                    class="bg-white dark:bg-surface text-gray-800 dark:text-on-surface px-6 py-2.5 rounded-xl text-base font-medium shadow-[0_2px_8px_rgba(0,0,0,0.08)] border-2 cursor-grab active:cursor-grabbing transition-all flex items-center gap-1.5 relative"
                    :class="[
                      showFeedback && !isCorrect
                        ? 'border-red-200 hover:bg-red-50 dark:hover:bg-red-900/30'
                        : 'border-transparent hover:border-gray-300 dark:hover:border-outline-variant/30',
                      dragOverSelectedIdx === idx ? 'ring-2 ring-primary' : ''
                    ]"
                    :draggable="!showFeedback"
                    @dragstart="(e) => handleSelectedDragStart(e, idx)"
                    @dragover.prevent.stop="handleSelectedDragOver($event, idx)"
                    @dragleave="handleSelectedDragLeave(idx)"
                    @drop.stop="handleSelectedDrop($event, idx)"
                    @click="!showFeedback && removeWord(idx)"
                  >
                    {{ token.word }}
                    <span v-if="!showFeedback" class="material-symbols-outlined text-sm text-gray-400">close</span>
                    <div
                      v-if="dragOverSelectedIdx === idx && dragOverSide === 'left'"
                      class="absolute -left-1 top-1/2 -translate-y-1/2 w-0.5 h-8 bg-primary rounded-full"
                    ></div>
                    <div
                      v-if="dragOverSelectedIdx === idx && dragOverSide === 'right'"
                      class="absolute -right-1 top-1/2 -translate-y-1/2 w-0.5 h-8 bg-primary rounded-full"
                    ></div>
                  </div>
                </template>
                <div
                  v-else
                  class="w-full flex flex-col items-center justify-center text-gray-400 dark:text-on-surface-variant/40 text-base py-8 gap-2"
                >
                  <span class="material-symbols-outlined text-3xl">drag_indicator</span>
                  {{ $t('learning.dragOrClickHint') }}
                </div>

                <!-- 反馈区 -->
                <div v-if="showFeedback" class="w-full mt-4 space-y-2">
                  <div
                    class="p-3 rounded-xl text-sm font-medium text-center"
                    :class="isCorrect
                      ? 'bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400'
                      : 'bg-red-50 dark:bg-red-900/30 text-red-500 dark:text-red-400'"
                  >
                    <span class="material-symbols-outlined align-middle mr-1 text-base">{{ isCorrect ? 'check_circle' : 'cancel' }}</span>
                    {{ isCorrect ? $t('learning.correct') : $t('learning.incorrect') }}
                  </div>
                  <div v-if="!isCorrect" class="bg-white dark:bg-surface-container-low rounded-xl p-3 space-y-2">
                    <div>
                      <p class="text-xs text-gray-500 dark:text-on-surface-variant mb-0.5">{{ $t('learning.correctAnswer') }}</p>
                      <p class="text-sm text-primary font-medium">{{ correctSentence }}</p>
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
          </div>

          <!-- 单词库 -->
          <div class="w-full max-w-3xl mb-8">
            <p class="text-xs text-on-surface-variant/50 mb-2 text-center">{{ $t('learning.selectFromBelow') }}</p>
            <div
              class="flex flex-wrap justify-center gap-3 min-h-[60px] transition-all"
              :class="dragOverBankArea ? 'bg-surface-variant/20 rounded-2xl py-4' : ''"
              @dragover.prevent="handleBankAreaDragOver"
              @dragleave="handleBankAreaDragLeave"
              @drop="handleBankAreaDrop"
            >
              <div
                v-for="(word, wIdx) in wordBank"
                :key="'bank-' + wIdx"
              :draggable="!showFeedback && !usedBankIndices.has(wIdx)"
              class="bg-white dark:bg-surface text-gray-800 dark:text-on-surface px-6 py-2.5 rounded-2xl text-base font-medium shadow-[0_2px_10px_rgba(0,0,0,0.08)] border-2 transition-all active:scale-95 select-none min-w-[60px] text-center"
              :class="[
                showFeedback || usedBankIndices.has(wIdx)
                  ? 'opacity-30 cursor-not-allowed shadow-sm border-transparent'
                  : 'cursor-grab active:cursor-grabbing border-transparent hover:shadow-[0_4px_16px_rgba(0,0,0,0.12)] hover:-translate-y-0.5'
              ]"
              @dragstart="(e) => handleBankDragStart(e, wIdx)"
              @click="addWord(wIdx)"
            >
              {{ word }}
            </div>
          </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="w-full max-w-2xl flex items-center gap-3 mt-auto pb-8">
            <!-- 查看答案 -->
            <button
              class="bg-gray-100 dark:bg-surface-variant/50 text-gray-600 dark:text-on-surface-variant px-5 py-3 rounded-xl text-sm font-medium hover:bg-gray-200 dark:hover:bg-surface-variant transition-colors flex flex-col items-center disabled:opacity-50"
              :disabled="showFeedback"
              @click="revealAnswer"
            >
              <span class="material-symbols-outlined mb-0.5 text-lg">done_all</span>
              {{ $t('learning.viewAnswerBtn') }}
            </button>
            <!-- 语音模式 -->
            <button
              v-if="speechSupported"
              class="w-12 h-12 bg-white dark:bg-surface rounded-xl shadow-[0_2px_8px_rgba(0,0,0,0.08)] flex items-center justify-center text-primary hover:scale-105 transition-transform disabled:opacity-50"
              :disabled="showFeedback"
              @click="toggleSpeech"
              :aria-label="$t('learning.voiceMode')"
            >
              <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' 1;">mic</span>
            </button>
            <!-- 问AI -->
            <button
              class="w-12 h-12 bg-white dark:bg-surface rounded-xl shadow-[0_2px_8px_rgba(0,0,0,0.08)] flex items-center justify-center text-primary hover:scale-105 transition-transform"
              @click="askAi"
              :aria-label="$t('learning.askAi')"
            >
              <span class="material-symbols-outlined text-xl">smart_toy</span>
            </button>
            <!-- 确认/下一题大按钮 -->
            <button
              class="flex-1 py-3 rounded-xl text-base font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              :class="showFeedback
                ? 'bg-primary text-on-primary hover:bg-primary/90 shadow-[0_2px_8px_rgba(0,0,0,0.1)]'
                : 'bg-gray-200 dark:bg-surface-variant text-gray-600 dark:text-on-surface-variant hover:bg-gray-300 dark:hover:bg-surface-dim'"
              :disabled="!showFeedback && selectedWords.length === 0"
              @click="showFeedback ? goNextOrResults() : handleSubmit()"
            >
              {{ showFeedback ? (isLastQuestion ? $t('learning.viewResults') : $t('learning.next')) : $t('learning.confirmBtn') }}
            </button>
          </div>
        </template>

        <!-- 结果页 -->
        <template v-else-if="showResults">
          <div class="max-w-3xl mx-auto w-full">
            <div class="bg-surface rounded-3xl p-8 md:p-12 shadow-soft-glow text-center mb-8 border border-outline-variant/20">
              <div class="w-20 h-20 rounded-full bg-primary-container/20 flex items-center justify-center mx-auto mb-4">
                <span class="material-symbols-outlined text-4xl text-secondary" style="font-variation-settings: 'FILL' 1;">emoji_events</span>
              </div>
              <h3 class="font-headline-md text-headline-md text-on-surface mb-2">{{ $t('learning.grammarComplete') }}</h3>
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

            <div class="flex gap-4">
              <button
                class="flex-1 bg-surface text-on-surface border border-outline-variant py-4 rounded-xl font-label-lg text-label-lg hover:bg-surface-variant transition-colors"
                @click="goBack"
              >
                {{ $t('learning.backToLearning') }}
              </button>
              <button
                class="flex-1 bg-primary text-on-primary py-4 rounded-xl font-label-lg text-label-lg hover:bg-primary/90 transition-colors"
                @click="restartQuiz"
              >
                {{ $t('learning.practiceAgainGrammar') }}
              </button>
            </div>
          </div>
        </template>

        <!-- 无题 -->
        <div v-else class="flex-1 flex flex-col items-center justify-center py-20">
          <span class="material-symbols-outlined text-4xl text-outline">inbox</span>
          <p class="mt-4 text-on-surface-variant font-body-md text-body-md">{{ $t('learning.noGrammarQuestions') }}</p>
          <button class="mt-4 bg-primary text-on-primary px-6 py-2 rounded-full" @click="goBack">{{ $t('common.back') }}</button>
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
 * 语法练习页 —— 造句题（截图风格）
 * 数据流：selectQuestions(userId, 2, 10) → 智能拆分为拼句题
 *   - analysis 字段 → "知识点||中文直译" 拆分
 *   - correctAnswer（A/B/C/D）→ 对应选项文本 → 拆成单词 = 正确句子
 *   - 其他选项 → 提取干扰词加入单词库
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
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

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const aiStore = useAiStore()

const askAi = () => {
  const q = currentQuestion.value
  aiStore.open({
    contextType: 'grammar',
    questionContent: q ? `${q.stem || ''} — ${q.analysis || ''}` : '',
    userAnswer: selectedWords.value.join(' ') || '',
  })
}

const {
  isListening: speechIsListening,
  transcript: speechTranscript,
  isSupported: speechSupported,
  start: startSpeech,
  stop: stopSpeech,
} = useSpeechRecognition()

// ========== 状态 ==========
const loading = ref(true)
const questions = ref<QuestionVO[]>([])
const currentIndex = ref(0)
const showFeedback = ref(false)
const isCorrect = ref(false)
const submitResult = ref<SubmitResponse | null>(null)
const showResults = ref(false)
const startTime = ref(Date.now())

// 造句相关
const knowledgePoint = ref('')     // 知识点（analysis 前半段）
const literalTranslation = ref('') // 直译 — 中文翻译（analysis 后半段）
const correctSentence = ref('')    // 正确答案句子
const wordBank = ref<string[]>([]) // 单词库（含干扰词）

// 已选中的词：{ word, bankIdx } 保留单词库索引便于撤回后释放
interface SelectedToken { word: string; bankIdx: number }
const selectedWords = ref<SelectedToken[]>([])

// ========== 拖拽状态 ==========
const dragOverAnswerArea = ref(false)
const dragOverBankArea = ref(false)
const dragOverSelectedIdx = ref<number>(-1)
const dragOverSide = ref<'left' | 'right'>('left')

// 拖拽数据（dragstart 时设置，drop 时读取）
let dragSource: { type: 'bank' | 'selected'; index: number } | null = null

const handleBankDragStart = (e: DragEvent, bankIdx: number) => {
  if (showFeedback.value || usedBankIndices.value.has(bankIdx)) return
  dragSource = { type: 'bank', index: bankIdx }
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'copy'
    e.dataTransfer.setData('text/plain', wordBank.value[bankIdx])
  }
}

const handleSelectedDragStart = (e: DragEvent, selectedIdx: number) => {
  if (showFeedback.value) return
  dragSource = { type: 'selected', index: selectedIdx }
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', selectedWords.value[selectedIdx].word)
  }
}

// 拼句区 dragover
const handleAnswerAreaDragOver = () => {
  if (showFeedback.value || !dragSource) return
  dragOverAnswerArea.value = true
}

// 拼句区 dragleave
const handleAnswerAreaDragLeave = () => {
  dragOverAnswerArea.value = false
}

// 拼句区 drop
const handleAnswerAreaDrop = () => {
  dragOverAnswerArea.value = false
  dragOverSelectedIdx.value = -1
  if (showFeedback.value || !dragSource) return

  if (dragSource.type === 'bank') {
    addWord(dragSource.index)
  } else if (dragSource.type === 'selected') {
    const fromIdx = dragSource.index
    if (fromIdx < 0 || fromIdx >= selectedWords.value.length) return
    const token = selectedWords.value[fromIdx]
    const newList = [...selectedWords.value]
    newList.splice(fromIdx, 1)
    newList.push(token)
    selectedWords.value = newList
  }
  dragSource = null
}

// 已选词 dragover（判断放到左边还是右边）
const handleSelectedDragOver = (e: DragEvent, targetIdx: number) => {
  if (showFeedback.value || !dragSource) return
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const midX = rect.left + rect.width / 2
  dragOverSide.value = e.clientX < midX ? 'left' : 'right'
  dragOverSelectedIdx.value = targetIdx
}

// 已选词 dragleave
const handleSelectedDragLeave = (idx: number) => {
  if (dragOverSelectedIdx.value === idx) {
    dragOverSelectedIdx.value = -1
  }
}

// 已选词 drop（插入到目标位置左/右）
const handleSelectedDrop = (_e: DragEvent, targetIdx: number) => {
  dragOverSelectedIdx.value = -1
  if (showFeedback.value || !dragSource) return

  if (dragSource.type === 'bank') {
    const bankIdx = dragSource.index
    if (usedBankIndices.value.has(bankIdx)) { dragSource = null; return }
    const token: SelectedToken = { word: wordBank.value[bankIdx], bankIdx }
    const insertIdx = dragOverSide.value === 'left' ? targetIdx : targetIdx + 1
    const newList = [...selectedWords.value]
    newList.splice(insertIdx, 0, token)
    selectedWords.value = newList
  } else if (dragSource.type === 'selected') {
    const fromIdx = dragSource.index
    if (fromIdx === targetIdx) { dragSource = null; return }
    const newList = [...selectedWords.value]
    const [moved] = newList.splice(fromIdx, 1)
    // 计算插入位置：如果从左拖到右，且插入位置在右边，需要-1
    let insertIdx = dragOverSide.value === 'left' ? targetIdx : targetIdx + 1
    if (fromIdx < insertIdx) insertIdx--
    newList.splice(insertIdx, 0, moved)
    selectedWords.value = newList
  }
  dragSource = null
}

// 单词库区域 dragover
const handleBankAreaDragOver = () => {
  if (showFeedback.value || !dragSource) return
  if (dragSource.type === 'selected') {
    dragOverBankArea.value = true
  }
}

// 单词库区域 dragleave
const handleBankAreaDragLeave = () => {
  dragOverBankArea.value = false
}

const handleBankAreaDrop = () => {
  dragOverBankArea.value = false
  if (showFeedback.value || !dragSource) return
  if (dragSource.type === 'selected') {
    removeWord(dragSource.index)
  }
  dragSource = null
}

interface ResultItem { question: QuestionVO; answer: string; correct: boolean }
const results = ref<{ correct: number; total: number; items: ResultItem[] }>({
  correct: 0, total: 0, items: [],
})

// ========== 计算属性 ==========
const totalCount = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const correctCount = computed(() => results.value.correct)
const accuracy = computed(() =>
  results.value.total > 0 ? Math.round((results.value.correct / results.value.total) * 100) : 0
)
const answeredCount = computed(() => currentIndex.value + (showResults.value ? 1 : 0))
const isLastQuestion = computed(() => currentIndex.value >= totalCount.value - 1)
const progressPercent = computed(() => totalCount.value > 0 ? (answeredCount.value / totalCount.value) * 100 : 0)
const currentDifficulty = computed(() => currentQuestion.value?.difficultyLevel || 1)

const usedBankIndices = computed(() => {
  const set = new Set<number>()
  selectedWords.value.forEach(tok => set.add(tok.bankIdx))
  return set
})

// 当前用户拼句（空格连接）
const userAnswerStr = computed(() =>
  selectedWords.value.map(tok => tok.word).join(' ')
)

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

// ========== 文本归一化（去标点 + 小写 + 单空格） ==========
const normalize = (s: string) =>
  (s || '').toLowerCase().replace(/[.,!?;:'"()[\]{}/\\-]/g, ' ').replace(/\s+/g, ' ').trim()

// ========== 动词时态变化干扰词生成（方案B核心） ==========
// 常见动词的不同时态/形式变化映射
const verbVariations: Record<string, string[]> = {
  be: ['am', 'is', 'are', 'was', 'were', 'been', 'being'],
  have: ['has', 'had', 'having'],
  do: ['does', 'did', 'doing', 'done'],
  go: ['goes', 'went', 'going', 'gone'],
  come: ['comes', 'came', 'coming'],
  see: ['sees', 'saw', 'seeing', 'seen'],
  make: ['makes', 'made', 'making'],
  take: ['takes', 'took', 'taking', 'taken'],
  know: ['knows', 'knew', 'knowing', 'known'],
  think: ['thinks', 'thought', 'thinking'],
  give: ['gives', 'gave', 'giving', 'given'],
  find: ['finds', 'found', 'finding'],
  tell: ['tells', 'told', 'telling'],
  say: ['says', 'said', 'saying'],
  get: ['gets', 'got', 'getting', 'gotten'],
  want: ['wants', 'wanted', 'wanting'],
  use: ['uses', 'used', 'using'],
  look: ['looks', 'looked', 'looking'],
  play: ['plays', 'played', 'playing'],
  live: ['lives', 'lived', 'living'],
  believe: ['believes', 'believed', 'believing'],
  hold: ['holds', 'held', 'holding'],
  bring: ['brings', 'brought', 'bringing'],
  begin: ['begins', 'began', 'beginning', 'begun'],
  keep: ['keeps', 'kept', 'keeping'],
  feel: ['feels', 'felt', 'feeling'],
  put: ['puts', 'putting'],
  run: ['runs', 'ran', 'running'],
  write: ['writes', 'wrote', 'writing', 'written'],
  read: ['reads', 'reading'],
  speak: ['speaks', 'spoke', 'speaking', 'spoken'],
  eat: ['eats', 'ate', 'eating', 'eaten'],
  sleep: ['sleeps', 'slept', 'sleeping'],
  buy: ['buys', 'bought', 'buying'],
  build: ['builds', 'built', 'building'],
  choose: ['chooses', 'chose', 'choosing', 'chosen'],
  teach: ['teaches', 'taught', 'teaching'],
  pay: ['pays', 'paid', 'paying'],
  fly: ['flies', 'flew', 'flying', 'flown'],
  drive: ['drives', 'drove', 'driving', 'driven'],
  grow: ['grows', 'grew', 'growing', 'grown'],
}
function generateVerbVariations(baseWords: string[]): string[] {
  const variations: string[] = []
  const added = new Set<string>()
  const seenBases = new Set<string>()

  for (const word of baseWords) {
    const lower = word.toLowerCase().replace(/[.,!?;:'"()\[\]{}]/g, '')
    if (seenBases.has(lower)) continue
    seenBases.add(lower)
    if (verbVariations[lower]) {
      for (const variant of verbVariations[lower]) {
        if (!baseWords.map(w => w.toLowerCase().replace(/[.,!?;:'"()\[\]{}]/g, '')).includes(variant) &&
            !added.has(variant)) {
          variations.push(variant)
          added.add(variant)
        }
      }
    }
  }

  return variations
}

// 打乱数组
const shuffle = <T,>(arr: T[]): T[] => {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

// ========== 解析单题为造句题形式（完整句子拼图 + 时态干扰词） ==========
function parseQuestionIntoSentence(q: QuestionVO) {
  const stem = q.stem || ''
  const analysisRaw = q.analysis || ''
  const sepIdx = analysisRaw.indexOf('||')
  if (sepIdx >= 0) {
    knowledgePoint.value = analysisRaw.substring(0, sepIdx)
    literalTranslation.value = analysisRaw.substring(sepIdx + 2)
  } else {
    knowledgePoint.value = analysisRaw || t('learning.defaultKnowledgePoint')
    literalTranslation.value = stem || ''
  }

  // 2) 解析 options
  let optionTexts: string[] = []
  if (q.options) {
    try {
      const parsed = JSON.parse(q.options)
      if (Array.isArray(parsed)) {
        optionTexts = parsed.map((o: string) => String(o).replace(/^[A-D][.、)\s]+\s*/, '').trim())
      } else {
        throw new Error('not array')
      }
    } catch {
      optionTexts = q.options.split(/\n|\\n/).map(l =>
        l.replace(/^[A-D][.、)\s]+\s*/, '').trim()).filter(Boolean)
    }
  }

  const correctRaw = (q.correctAnswer || '').trim()
  const correctKey = correctRaw.toUpperCase().match(/^[A-D]/)?.[0] || ''
  const correctIdx = correctKey ? correctKey.charCodeAt(0) - 65 : -1
  const correctText = correctIdx >= 0 ? (optionTexts[correctIdx] || '') : correctRaw

  // 智能构建完整句子：
  // 1) 如果 stem 中有占位符 → 用正确选项替换第一个占位符，清理其余占位符
  // 2) 如果 stem 没有占位符且不为空 → 清理内联选项字母（旧数据如 "English C she"）
  // 3) 如果 stem 为空 → 直接使用正确选项
  // 无论是否有占位符，都需要清理内联选项字母 A-D，避免残留（如 "Only C C solve" → "Only can we solve"）
  const placeholderRegex = /_{2,}|\[BLANK\]|\.{3,}/g
  const hasPlaceholder = placeholderRegex.test(stem)
  let fullSentence: string
  if (hasPlaceholder) {
    let replaced = false
    fullSentence = stem.replace(placeholderRegex, () => {
      if (!replaced) {
        replaced = true
        return correctText
      }
      return ''
    })
    // 清理内联选项字母（即使有占位符，stem 中也可能包含内联选项字母）
    fullSentence = fullSentence.replace(/\b[A-D]\b/g, correctText).replace(/\s+/g, ' ').trim()
  } else if (stem.trim()) {
    // 没有占位符 → 清理内联选项字母（旧数据格式，选项字母通常大写）
    const cleaned = stem.replace(/\b[A-D]\b/g, correctText).replace(/\s+/g, ' ').trim()
    fullSentence = cleaned || stem.trim()
  } else {
    fullSentence = correctText
  }
  correctSentence.value = fullSentence

  // 5) 正确单词列表（完整句子拆分，保留标点附加在词尾，保留重复词）
  const sentenceWords = fullSentence.split(/\s+/).filter(Boolean)
  // 不去重：句子中可能有重复单词（如 the, is 等），必须保留足够数量
  const baseWords = [...sentenceWords]

  // 7) 干扰词池
  const distractorPool: string[] = []

  optionTexts.forEach((txt, i) => {
    if (i === correctIdx) return
    txt.split(/\s+/).forEach(w => {
      const clean = w.replace(/[.,!?;:'"()\[\]{}]/g, '')
      if (clean && !baseWords.includes(clean) && !baseWords.includes(w) && !distractorPool.includes(clean)) {
        distractorPool.push(clean)
      }
    })
  })

  const verbVariationsPool = generateVerbVariations(baseWords)
  const selectedVerbVariations = shuffle(verbVariationsPool).slice(0, Math.min(4, verbVariationsPool.length))
  for (const v of selectedVerbVariations) {
    if (!baseWords.includes(v) && !distractorPool.includes(v)) {
      distractorPool.push(v)
    }
  }

  if (distractorPool.length < 4) {
    const allVariants = new Set<string>()
    for (const w of baseWords) {
      const lower = w.toLowerCase().replace(/[.,!?;:'"()\[\]{}]/g, '')
      if (verbVariations[lower]) {
        verbVariations[lower].forEach(v => allVariants.add(v))
      }
    }
    const extra = shuffle([...allVariants]).filter(v =>
      !baseWords.includes(v) && !distractorPool.includes(v)
    )
    distractorPool.push(...extra.slice(0, 6 - distractorPool.length))
  }

  // 8) 最终单词库：基础词 + 干扰词，打乱
  const finalBank = shuffle([...baseWords, ...distractorPool])

  wordBank.value = finalBank
  selectedWords.value = []
  showFeedback.value = false
  submitResult.value = null
}

const ensureQuestionsWithAnswers = async (list: QuestionVO[]): Promise<QuestionVO[]> => {
  return Promise.all(
    list.map(async (question) => {
      if (question.correctAnswer) return question
      return await getQuestionById(question.id, true)
    }),
  )
}

// ========== 进度持久化 ==========
const SAVE_KEY = 'grammar-quiz-progress'

interface SavedGrammarProgress {
  userId: number
  questions: QuestionVO[]
  currentIndex: number
  results: { correct: number; total: number; items: ResultItem[] }
}

const saveProgress = () => {
  const userId = userStore.userId
  if (!userId || questions.value.length === 0) return
  sessionStorage.setItem(SAVE_KEY, JSON.stringify({
    userId, questions: questions.value, currentIndex: currentIndex.value, results: results.value
  }))
}

const clearProgress = () => sessionStorage.removeItem(SAVE_KEY)

const restoreProgress = (): SavedGrammarProgress | null => {
  try {
    const raw = sessionStorage.getItem(SAVE_KEY)
    if (!raw) return null
    const data = JSON.parse(raw) as SavedGrammarProgress
    if (!data.questions?.length || data.userId !== userStore.userId) { clearProgress(); return null }
    if (data.questions.some(question => !question.correctAnswer)) { clearProgress(); return null }
    return data
  } catch { clearProgress(); return null }
}

// ========== 加载题目 ==========
const loadQuestions = async () => {
  const userId = userStore.userId
  if (!userId) { router.push('/login'); return }

  const dailyTarget = Number(localStorage.getItem('dailyTarget_grammar')) || 10
  const saved = restoreProgress()
  if (saved && saved.currentIndex < saved.questions.length && saved.questions.length === dailyTarget) {
    loading.value = true
    questions.value = saved.questions
    results.value = saved.results
    const answeredIds = new Set(saved.results.items.map(it => it.question.id))
    let resumeIndex = saved.currentIndex
    if (answeredIds.has(saved.questions[resumeIndex]?.id)) {
      resumeIndex = Math.min(resumeIndex + 1, saved.questions.length - 1)
    }
    currentIndex.value = resumeIndex
    showFeedback.value = false
    showResults.value = false
    if (questions.value[currentIndex.value]) parseQuestionIntoSentence(questions.value[currentIndex.value])
    loading.value = false
    startTime.value = Date.now()
    return
  }

  clearProgress()
  loading.value = true
  questions.value = []
  currentIndex.value = 0
  showFeedback.value = false
  showResults.value = false
  results.value = { correct: 0, total: 0, items: [] }
  try {
    const qs = await selectQuestions(userId, 2, Number(localStorage.getItem('dailyTarget_grammar')) || 10)
    const fullQuestions = await ensureQuestionsWithAnswers(qs || [])
    questions.value = fullQuestions
    if (fullQuestions.length > 0) parseQuestionIntoSentence(fullQuestions[0])
  } catch (e) {
    console.warn('获取语法题失败', e)
  } finally {
    loading.value = false
  }
}

// ========== 拼句操作 ==========
const addWord = (bankIdx: number) => {
  if (showFeedback.value) return
  if (usedBankIndices.value.has(bankIdx)) return
  selectedWords.value = [...selectedWords.value, { word: wordBank.value[bankIdx], bankIdx }]
}

const removeWord = (posIdx: number) => {
  if (showFeedback.value) return
  selectedWords.value = selectedWords.value.filter((_, i) => i !== posIdx)
}

const clearAllWords = () => {
  selectedWords.value = []
}

// ========== 提交 ==========
const handleSubmit = async () => {
  const q = currentQuestion.value
  if (!q || selectedWords.value.length === 0 || showFeedback.value) return
  const userId = userStore.userId
  if (!userId) return
  const ans = userAnswerStr.value
  try {
    const result = await submitAnswerApi({
      userId,
      questionId: q.id,
      userAnswer: ans,
      moduleType: 2,
      durationSeconds: Math.round((Date.now() - startTime.value) / 1000),
    })
    submitResult.value = result
    const localOK = normalize(ans) === normalize(correctSentence.value)
    isCorrect.value = result.isCorrect || localOK
    showFeedback.value = true
    results.value.items.push({ question: q, answer: ans, correct: isCorrect.value })
    if (isCorrect.value) results.value.correct++
    results.value.total++
    saveProgress()

    // 不自动跳转，让用户有时间阅读解析和收藏句子
    // 用户可点击"下一题"按钮手动跳转
  } catch (e) {
    console.warn('获取语法题失败', e)
    const localOK = normalize(ans) === normalize(correctSentence.value)
    submitResult.value = { isCorrect: localOK, correctAnswer: correctSentence.value } as SubmitResponse
    isCorrect.value = localOK
    showFeedback.value = true
    results.value.items.push({ question: q, answer: ans, correct: localOK })
    if (localOK) results.value.correct++
    results.value.total++
    saveProgress()
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
    if (questions.value[currentIndex.value]) {
      parseQuestionIntoSentence(questions.value[currentIndex.value])
    }
    startTime.value = Date.now()
  } else {
    showResults.value = true
  }
}

// ========== 查看答案：自动填入正确句子 ==========
const revealAnswer = () => {
  if (!correctSentence.value || showFeedback.value) return
  const target = correctSentence.value.split(/\s+/).filter(Boolean)
  const used = new Set<number>()
  const newTokens: SelectedToken[] = []
  for (const w of target) {
    const idx = wordBank.value.findIndex((bw, i) =>
      !used.has(i) && normalize(bw) === normalize(w)
    )
    if (idx >= 0) {
      used.add(idx)
      newTokens.push({ word: wordBank.value[idx], bankIdx: idx })
    }
  }
  selectedWords.value = newTokens
}

// ========== 语言切换 ==========
const cycleLanguage = () => {
  const langs = ['zh-CN', 'en-US']
  const idx = langs.indexOf(appStore.language)
  const next = langs[(idx + 1) % langs.length]
  appStore.setLanguage(next)
}

// ========== 语音模式 ==========
const toggleSpeech = () => {
  if (speechIsListening.value) stopSpeech()
  else startSpeech()
}
watch(speechTranscript, (val) => {
  if (!val) return
  const target = normalize(val)
  // 语音识别到的词 → 尝试匹配单词库中每个未使用词
  const tokens = target.split(' ').filter(Boolean)
  tokens.forEach(tok => {
    const idx = wordBank.value.findIndex((bw, i) =>
      !usedBankIndices.value.has(i) && normalize(bw) === tok
    )
    if (idx >= 0) addWord(idx)
  })
})

// ========== 导航 ==========
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
      sentenceText: correctSentence.value || q.stem,
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
watch(() => route.path, (p) => {
  if (p === '/grammar-quiz' && totalCount.value === 0 && !loading.value) loadQuestions()
})
</script>
