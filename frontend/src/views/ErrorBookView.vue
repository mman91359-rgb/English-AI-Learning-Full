<template>
  <AppLayout>
    <div class="flex items-center gap-4 mb-8">
      <button class="p-2 hover:bg-surface-variant rounded-full transition-colors" @click="$router.back()">
        <span class="material-symbols-outlined text-on-surface">arrow_back</span>
      </button>
      <h2 class="font-headline-lg text-headline-lg text-on-surface">{{ $t('app.title') }} - {{ $t('learning.myErrorBook') }}</h2>
    </div>

    <div v-if="loading" class="flex justify-center py-20">
      <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
    </div>

    <div v-else-if="errors.length === 0" class="text-center py-20">
      <span class="material-symbols-outlined text-6xl text-outline">check_circle</span>
      <p class="mt-4 text-on-surface-variant font-body-lg text-body-lg">{{ $t('learning.noErrorsFound') }}</p>
      <p class="mt-2 text-on-surface-variant/60 font-body-md text-body-md">{{ $t('learning.keepItUp') }}</p>
      <router-link
        to="/learning"
        class="inline-block mt-6 px-6 py-3 bg-primary text-on-primary rounded-xl font-label-md text-label-md hover:bg-primary/90 transition-colors"
      >
        {{ $t('learning.continuePractice') }}
      </router-link>
    </div>

    <div v-else>
      <!-- 薄弱知识点卡片 -->
      <div v-if="weakPoints.length > 0" class="bg-surface-container-lowest rounded-[20px] p-5 shadow-soft-glow mb-6">
        <div class="flex items-center gap-2 mb-4">
          <span class="material-symbols-outlined text-warning text-2xl">lightbulb</span>
          <h3 class="font-headline-sm text-headline-sm text-on-surface">薄弱知识点</h3>
          <span class="font-label-sm text-label-sm text-on-surface-variant">（错题数排名前10）</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="(wp, idx) in weakPoints"
            :key="wp.knowledgePointId"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full font-label-sm text-label-sm"
            :class="idx === 0 ? 'bg-error-container/30 text-error' : 'bg-surface-variant/50 text-on-surface-variant'"
          >
            <span class="font-label-md text-label-md">{{ idx + 1 }}</span>
            {{ wp.knowledgePointName }}
            <span class="font-label-xs text-label-xs opacity-70">×{{ wp.errorCount }}</span>
          </span>
        </div>
      </div>

      <!-- 分类筛选 + 清除 -->
      <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
        <div class="flex gap-1.5 flex-wrap">
          <button
            v-for="tab in moduleTabs"
            :key="tab.value"
            :class="[
              'px-4 py-1.5 rounded-full font-label-sm text-label-sm transition-colors',
              moduleFilter === tab.value
                ? 'bg-primary text-on-primary'
                : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
            ]"
            @click="moduleFilter = tab.value"
          >
            {{ tab.label }} ({{ tab.count }})
          </button>
        </div>
        <button
          class="flex items-center gap-2 px-4 py-2.5 border border-error/40 text-error rounded-xl font-label-md text-label-md hover:bg-error/5 transition-colors shrink-0"
          @click="clearAll"
        >
          <span class="material-symbols-outlined text-lg">delete_sweep</span>
          {{ $t('learning.clearAll') }}
        </button>
      </div>

      <!-- 筛选后无结果 -->
      <div v-if="filteredErrors.length === 0" class="text-center py-12">
        <span class="material-symbols-outlined text-5xl text-outline">filter_alt_off</span>
        <p class="mt-3 text-on-surface-variant font-body-md text-body-md">{{ $t('learning.noFilterResult') }}</p>
        <button
          class="mt-3 px-5 py-2 rounded-full border border-outline-variant text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant transition-colors"
          @click="moduleFilter = 0"
        >
          {{ $t('learning.resetFilter') }}
        </button>
      </div>

      <div class="space-y-4">
        <div
          v-for="(q, idx) in filteredErrors"
          :key="q.id"
          class="bg-surface-container-lowest rounded-[20px] p-5 shadow-soft-glow"
        >
          <div class="flex items-start justify-between gap-4 mb-3">
            <div class="flex items-center gap-2">
              <span class="px-3 py-0.5 rounded-full font-label-sm text-label-sm"
                :class="moduleTagClass(q.moduleType)"
              >
                {{ moduleTagName(q.moduleType) }}
              </span>
              <span class="px-3 py-0.5 rounded-full bg-error-container/20 text-error font-label-sm text-label-sm">
                {{ q.difficultyLevel ? 'Lv.' + q.difficultyLevel : '' }}
              </span>
            </div>
            <span class="font-label-sm text-label-sm text-on-surface-variant">#{{ idx + 1 }}/{{ filteredErrors.length }}</span>
          </div>

          <!-- ===== 题干/提示区 ===== -->
          <!-- 语法题：显示知识点 + 直译（不直接展示带 ___ 占位符的原始 stem） -->
          <template v-if="q.moduleType === 2">
            <div class="mb-4 space-y-2">
              <div class="flex items-start gap-2">
                <span class="bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 px-2.5 py-0.5 rounded-lg font-label-sm text-label-sm whitespace-nowrap mt-0.5">
                  {{ $t('learning.knowledgePoint') }}
                </span>
                <p class="font-body-sm text-body-sm text-on-surface leading-relaxed flex-1">
                  {{ grammarStates[q.id]?.knowledgePoint || $t('learning.defaultKnowledgePoint') }}
                </p>
              </div>
              <div class="flex items-start gap-2">
                <span class="bg-surface-variant/50 text-on-surface-variant px-2.5 py-0.5 rounded-lg font-label-sm text-label-sm whitespace-nowrap mt-0.5">
                  {{ $t('learning.literalTranslation') }}
                </span>
                <p class="font-body-sm text-body-sm text-on-surface leading-relaxed flex-1">
                  {{ grammarStates[q.id]?.literalTranslation || q.stem }}
                </p>
              </div>
            </div>
          </template>
          <!-- 其他题：显示原始题干 -->
          <p v-else class="font-body-md text-body-md text-on-surface mb-4">{{ q.stem }}</p>

          <!-- ===== 答题输入区 ===== -->
          <!-- 语法题：词语排序交互 -->
          <div v-if="q.moduleType === 2" class="mb-4">
            <!-- 拼句区标题 + 清空按钮 -->
            <div class="flex items-center justify-between mb-2">
              <span class="font-label-sm text-label-sm text-on-surface-variant/50">{{ $t('learning.sentenceAreaHint') }}</span>
              <button
                v-if="getSelectedWords(q.id).length > 0 && !showAnswer[q.id]"
                class="font-label-sm text-label-sm text-error/70 hover:text-error flex items-center gap-1 transition-colors"
                @click="clearGrammarWords(q.id)"
              >
                <span class="material-symbols-outlined text-sm">delete</span>{{ $t('learning.clearBtn') }}
              </button>
            </div>
            <!-- 已选单词区 -->
            <div
              class="bg-surface-container-low rounded-2xl p-4 min-h-[100px] flex flex-wrap gap-2 content-start mb-3"
              :class="showAnswer[q.id] && !submitted[q.id]?.isCorrect ? 'ring-2 ring-red-300' : ''"
            >
              <template v-if="getSelectedWords(q.id).length > 0">
                <button
                  v-for="(word, sIdx) in getSelectedWords(q.id)"
                  :key="'sel-' + sIdx"
                  class="bg-surface text-on-surface px-4 py-2 rounded-xl font-body-sm text-body-sm shadow-sm border-2 border-transparent hover:border-outline-variant/30 transition-all flex items-center gap-1"
                  :disabled="showAnswer[q.id]"
                  @click="removeGrammarWord(q.id, sIdx)"
                >
                  {{ word }}
                  <span v-if="!showAnswer[q.id]" class="material-symbols-outlined text-xs text-on-surface-variant">close</span>
                </button>
              </template>
              <div v-else class="w-full flex flex-col items-center justify-center text-on-surface-variant/40 py-6 gap-1">
                <span class="material-symbols-outlined text-2xl">drag_indicator</span>
                <span class="font-label-sm text-label-sm">{{ $t('learning.dragOrClickHint') }}</span>
              </div>
            </div>
            <!-- 单词库 -->
            <p class="font-label-sm text-label-sm text-on-surface-variant/50 mb-2 text-center">{{ $t('learning.selectFromBelow') }}</p>
            <div class="flex flex-wrap justify-center gap-2">
              <button
                v-for="(word, wIdx) in grammarStates[q.id]?.wordBank || []"
                :key="'bank-' + wIdx"
                :disabled="showAnswer[q.id] || isWordUsed(q.id, wIdx)"
                :class="[
                  'px-4 py-2 rounded-2xl font-body-sm text-body-sm border-2 transition-all min-w-[50px] text-center',
                  showAnswer[q.id] || isWordUsed(q.id, wIdx)
                    ? 'opacity-30 cursor-not-allowed bg-surface-variant/30 border-transparent'
                    : 'bg-surface text-on-surface shadow-sm border-transparent hover:shadow-md hover:-translate-y-0.5 cursor-pointer'
                ]"
                @click="selectGrammarWord(q.id, wIdx)"
              >
                {{ word }}
              </button>
            </div>
          </div>

          <!-- 选择题（非语法题且有 options） -->
          <div v-else-if="parsedOptions(q.options).length > 0" class="space-y-2 mb-4">
            <button
              v-for="opt in parsedOptions(q.options)"
              :key="opt.key"
              :class="[
                'w-full text-left p-3 rounded-xl border transition-all font-body-sm text-body-sm',
                selectedAnswers[q.id] === opt.key
                  ? 'border-primary bg-primary-container/20 text-primary'
                  : 'border-outline-variant/20 hover:border-primary/40 text-on-surface'
              ]"
              :disabled="showAnswer[q.id]"
              @click="selectOption(q.id, opt.key)"
            >
              {{ opt.key }}. {{ opt.text }}
            </button>
          </div>

          <!-- 填空题（非语法题且无 options，如词汇填空） -->
          <div v-else class="mb-4">
            <input
              v-model="selectedAnswers[q.id]"
              class="w-full text-center py-3 px-4 rounded-xl border-2 font-body-md text-body-md outline-none transition-colors bg-surface-container-low text-on-surface placeholder-on-surface-variant"
              :class="showAnswer[q.id]
                ? (submitted[q.id]?.isCorrect
                    ? 'border-green-500 bg-green-50/40'
                    : 'border-red-400 bg-red-50/40')
                : 'border-outline-variant/30 focus:border-primary'"
              :disabled="showAnswer[q.id]"
              :placeholder="$t('learning.inputAnswer')"
              @keyup.enter="handleReanswer(q)"
            />
          </div>

          <!-- ===== 答题反馈区 ===== -->
          <div v-if="showAnswer[q.id]" class="space-y-2">
            <div
              v-if="submitted[q.id]?.isCorrect"
              class="bg-green-50/40 rounded-xl p-4 text-center space-y-3 border border-green-200/50"
            >
              <p class="font-label-lg text-label-lg text-green-700">{{ $t('learning.congratsRemoved') }}</p>
              <router-link
                to="/learning"
                class="inline-flex items-center gap-1 text-primary font-label-md underline underline-offset-4 hover:text-primary/80 transition-colors"
              >
                {{ $t('learning.continuePractice') }}
                <span class="material-symbols-outlined text-sm">arrow_forward</span>
              </router-link>
            </div>

            <div v-else class="bg-surface-container rounded-xl p-4 space-y-2">
              <p class="font-label-md text-label-md text-on-surface-variant">
                {{ $t('learning.yourAnswer') }}：<span class="text-error">{{ getSubmittedAnswerText(q) }}</span>
              </p>
              <p class="font-label-md text-label-md text-primary">
                {{ $t('learning.correctAnswer') }}：{{ getDisplayCorrectAnswer(q) }}
              </p>
              <p v-if="submitted[q.id]?.analysis" class="font-body-sm text-body-sm text-on-surface-variant">
                {{ submitted[q.id].analysis }}
              </p>
            </div>
          </div>

          <!-- ===== 操作按钮 ===== -->
          <div class="flex gap-2 mt-3">
            <button
              v-if="!showAnswer[q.id]"
              class="flex-1 bg-primary text-on-primary py-2.5 rounded-xl font-label-md text-label-md hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!isAnswerReady(q)"
              @click="handleReanswer(q)"
            >
              {{ $t('learning.confirmSubmit') }}
            </button>
            <button
              v-else-if="!submitted[q.id]?.isCorrect"
              class="flex-1 bg-on-surface text-surface py-2.5 rounded-xl font-label-md text-label-md hover:bg-on-surface/90 transition-colors"
              @click="nextQuestion(q.id)"
            >
              {{ $t('learning.next') }}
            </button>
            <!-- 生成同类练习按钮 -->
            <button
              class="bg-secondary/10 text-secondary border border-secondary/30 py-2.5 px-4 rounded-xl font-label-md text-label-md hover:bg-secondary/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
              :disabled="generating[q.id]"
              @click="handleGenerateSimilar(q)"
            >
              <span v-if="generating[q.id]" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
              <span v-else class="material-symbols-outlined text-sm">auto_fix_high</span>
              {{ generating[q.id] ? '生成中...' : '生成同类练习' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 同类题练习弹窗 ===== -->
    <Teleport to="body">
      <div
        v-if="similarModalVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
        @click.self="closeSimilarModal"
      >
        <div class="bg-surface rounded-2xl w-[90vw] max-w-2xl max-h-[85vh] overflow-y-auto shadow-2xl p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-headline-md text-headline-md text-on-surface">
              同类强化练习
              <span class="font-label-sm text-label-sm text-on-surface-variant ml-2">
                {{ currentSimilarIndex + 1 }} / {{ similarQuestions.length }}
              </span>
            </h3>
            <button
              class="p-2 hover:bg-surface-variant rounded-full transition-colors"
              @click="closeSimilarModal"
            >
              <span class="material-symbols-outlined text-on-surface">close</span>
            </button>
          </div>

          <div v-if="similarQuestions.length > 0" class="space-y-4">
            <!-- 进度条 -->
            <div class="w-full h-1.5 bg-surface-variant rounded-full overflow-hidden">
              <div
                class="h-full bg-secondary rounded-full transition-all duration-300"
                :style="{ width: ((currentSimilarIndex + 1) / similarQuestions.length * 100) + '%' }"
              ></div>
            </div>

            <!-- 当前题目 -->
            <div class="bg-surface-container-lowest rounded-[20px] p-5">
              <div class="flex items-center gap-2 mb-3">
                <span class="px-3 py-0.5 rounded-full font-label-sm text-label-sm"
                  :class="moduleTagClass(similarQuestions[currentSimilarIndex]?.moduleType || 1)"
                >
                  {{ moduleTagName(similarQuestions[currentSimilarIndex]?.moduleType || 1) }}
                </span>
                <span class="px-3 py-0.5 rounded-full bg-error-container/20 text-error font-label-sm text-label-sm">
                  {{ similarQuestions[currentSimilarIndex]?.difficultyLevel ? 'Lv.' + similarQuestions[currentSimilarIndex].difficultyLevel : '' }}
                </span>
              </div>

              <!-- 题干 -->
              <p class="font-body-md text-body-md text-on-surface mb-4">
                {{ similarQuestions[currentSimilarIndex]?.stem }}
              </p>

              <!-- 选择题选项 -->
              <div v-if="parsedOptions(similarQuestions[currentSimilarIndex]?.options || '').length > 0" class="space-y-2 mb-4">
                <button
                  v-for="opt in parsedOptions(similarQuestions[currentSimilarIndex]?.options || '')"
                  :key="opt.key"
                  :class="[
                    'w-full text-left p-3 rounded-xl border transition-all font-body-sm text-body-sm',
                    similarSelectedAnswers[currentSimilarIndex] === opt.key
                      ? 'border-primary bg-primary-container/20 text-primary'
                      : 'border-outline-variant/20 hover:border-primary/40 text-on-surface'
                  ]"
                  :disabled="similarShowAnswer[currentSimilarIndex]"
                  @click="similarSelectedAnswers[currentSimilarIndex] = opt.key"
                >
                  {{ opt.key }}. {{ opt.text }}
                </button>
              </div>

              <!-- 填空题 -->
              <div v-else class="mb-4">
                <input
                  v-model="similarSelectedAnswers[currentSimilarIndex]"
                  class="w-full text-center py-3 px-4 rounded-xl border-2 font-body-md text-body-md outline-none transition-colors bg-surface-container-low text-on-surface placeholder-on-surface-variant"
                  :class="similarShowAnswer[currentSimilarIndex]
                    ? (similarSubmitted[currentSimilarIndex]?.isCorrect
                        ? 'border-green-500 bg-green-50/40'
                        : 'border-red-400 bg-red-50/40')
                    : 'border-outline-variant/30 focus:border-primary'"
                  :disabled="similarShowAnswer[currentSimilarIndex]"
                  :placeholder="$t('learning.inputAnswer')"
                  @keyup.enter="handleSimilarSubmit"
                />
              </div>

              <!-- 答题反馈 -->
              <div v-if="similarShowAnswer[currentSimilarIndex]" class="space-y-2">
                <div
                  v-if="similarSubmitted[currentSimilarIndex]?.isCorrect"
                  class="bg-green-50/40 rounded-xl p-3 text-center"
                >
                  <p class="font-label-md text-label-md text-green-700">回答正确！</p>
                </div>
                <div v-else class="bg-surface-container rounded-xl p-3 space-y-1">
                  <p class="font-label-sm text-label-sm text-on-surface-variant">
                    你的答案：<span class="text-error">{{ similarSelectedAnswers[currentSimilarIndex] }}</span>
                  </p>
                  <p class="font-label-sm text-label-sm text-primary">
                    正确答案：{{ similarQuestions[currentSimilarIndex]?.correctAnswer }}
                  </p>
                  <p v-if="similarQuestions[currentSimilarIndex]?.analysis" class="font-body-sm text-body-sm text-on-surface-variant">
                    {{ similarQuestions[currentSimilarIndex].analysis }}
                  </p>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="flex gap-2 mt-3">
                <button
                  v-if="!similarShowAnswer[currentSimilarIndex]"
                  class="flex-1 bg-secondary text-on-secondary py-2.5 rounded-xl font-label-md text-label-md hover:bg-secondary/90 transition-colors"
                  @click="handleSimilarSubmit"
                >
                  提交答案
                </button>
                <button
                  v-else
                  class="flex-1 bg-on-surface text-surface py-2.5 rounded-xl font-label-md text-label-md hover:bg-on-surface/90 transition-colors"
                  @click="nextSimilarQuestion"
                >
                  {{ currentSimilarIndex < similarQuestions.length - 1 ? '下一题' : '完成' }}
                </button>
              </div>
            </div>
          </div>

          <div v-else class="text-center py-12">
            <span class="material-symbols-outlined text-5xl text-outline">quiz</span>
            <p class="mt-3 text-on-surface-variant font-body-md text-body-md">暂无生成的题目</p>
          </div>
        </div>
      </div>
    </Teleport>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { getErrorBook, submitAnswer, getWeakPoints, generateSimilar, type QuestionVO, type SubmitResponse, type WeakKnowledgePoint } from '@/api/exercise'
import { useToast } from '@/composables/useToast'

const { t } = useI18n()
const userStore = useUserStore()
const { show: toast } = useToast()

const errors = ref<QuestionVO[]>([])
const loading = ref(true)
const selectedAnswers = ref<Record<number, string>>({})
const showAnswer = ref<Record<number, boolean>>({})
const submitted = ref<Record<number, SubmitResponse>>({})
const answerStartTime = ref<Record<number, number>>({})

// ========== 语法题词语排序状态 ==========
// 每道语法题独立维护：完整句子、打乱单词库、已选单词在 wordBank 中的索引、知识点、直译
interface GrammarState {
    fullSentence: string
    wordBank: string[]
    selectedIndices: number[]
    knowledgePoint: string
    literalTranslation: string
}
const grammarStates = reactive<Record<number, GrammarState>>({})

// ========== 薄弱知识点 ==========
const weakPoints = ref<WeakKnowledgePoint[]>([])

// ========== 同类题生成 ==========
const generating = ref<Record<number, boolean>>({})
const similarModalVisible = ref(false)
const similarQuestions = ref<QuestionVO[]>([])
const currentSimilarIndex = ref(0)
const similarSelectedAnswers = ref<Record<number, string>>({})
const similarShowAnswer = ref<Record<number, boolean>>({})
const similarSubmitted = ref<Record<number, { isCorrect: boolean }>>({})

// ========== 分类筛选 ==========
const moduleFilter = ref(0)

const moduleTabs = computed(() => {
  const all = errors.value.length
  const vocab = errors.value.filter(q => q.moduleType === 1).length
  const grammar = errors.value.filter(q => q.moduleType === 2).length
  const translation = errors.value.filter(q => q.moduleType === 3).length
  const listening = errors.value.filter(q => q.moduleType === 4).length
  return [
    { label: t('learning.all'), value: 0, count: all },
    { label: t('dashboard.moduleVocabulary'), value: 1, count: vocab },
    { label: t('dashboard.moduleGrammar'), value: 2, count: grammar },
    { label: t('dashboard.moduleTranslation'), value: 3, count: translation },
    { label: t('dashboard.moduleListening'), value: 4, count: listening },
  ]
})

const filteredErrors = computed(() => {
  if (moduleFilter.value === 0) return errors.value
  return errors.value.filter(q => q.moduleType === moduleFilter.value)
})

const moduleTagName = (type: number) => {
  const map: Record<number, string> = {
    1: t('dashboard.moduleVocabulary'),
    2: t('dashboard.moduleGrammar'),
    3: t('dashboard.moduleTranslation'),
    4: t('dashboard.moduleListening'),
  }
  return map[type] || ''
}

const moduleTagClass = (type: number) => {
  const map: Record<number, string> = {
    1: 'bg-primary-container/30 text-primary',
    2: 'bg-secondary-container/30 text-secondary',
    3: 'bg-tertiary-container/30 text-tertiary',
    4: 'bg-surface-variant text-on-surface-variant',
  }
  return map[type] || 'bg-surface-variant text-on-surface-variant'
}

const load = async () => {
  const uid = userStore.userId
  if (!uid) return
  loading.value = true
  try {
    const list = await getErrorBook(uid)
    errors.value = list || []
    // 预计算每道语法题的词语排序状态
    errors.value.forEach(q => {
      if (q.moduleType === 2 && !grammarStates[q.id]) {
        buildGrammarState(q)
      }
    })
    // 加载薄弱知识点
    try {
      weakPoints.value = await getWeakPoints(uid) || []
    } catch (e) {
      console.warn('加载薄弱知识点失败', e)
    }
  } catch (e) {
    console.warn('加载错题本失败', e)
  } finally {
    loading.value = false
  }
}

// ========== 通用辅助 ==========
/** 数组洗牌（Fisher-Yates） */
function shuffle<T>(arr: T[]): T[] {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

// ========== 语法题：构建词语排序状态 ==========
/** 把选择题格式（stem 带 ___ 占位符 + options + correctAnswer）转为词语排序题状态 */
function buildGrammarState(q: QuestionVO) {
  const stem = q.stem || ''
  const analysisRaw = q.analysis || ''
  const sepIdx = analysisRaw.indexOf('||')
  let knowledgePoint = ''
  let literalTranslation = ''
  if (sepIdx >= 0) {
    knowledgePoint = analysisRaw.substring(0, sepIdx)
    literalTranslation = analysisRaw.substring(sepIdx + 2)
  } else {
    knowledgePoint = analysisRaw || t('learning.defaultKnowledgePoint')
    literalTranslation = stem
  }

  // 从 correctAnswer（如 "A. established"）提取正确选项字母和索引
  const correctRaw = (q.correctAnswer || '').trim()
  const correctKey = correctRaw.toUpperCase().match(/^[A-D]/)?.[0] || ''
  const correctIdx = correctKey ? correctKey.charCodeAt(0) - 65 : -1

  // 解析 options JSON 数组，去掉选项字母前缀
  let optionTexts: string[] = []
  if (q.options) {
    try {
      const parsed = JSON.parse(q.options)
      if (Array.isArray(parsed)) {
        optionTexts = parsed.map((o: string) => String(o).replace(/^[A-D][.、)\s]+\s*/, '').trim())
      }
    } catch {
      optionTexts = q.options.split(/\n|\\n/).map(l =>
        l.replace(/^[A-D][.、)\s]+\s*/, '').trim()
      ).filter(Boolean)
    }
  }

  // 正确选项的纯文本
  const correctText = correctIdx >= 0
    ? (optionTexts[correctIdx] || correctRaw.replace(/^[A-D][.、)\s]+\s*/, '').trim())
    : correctRaw

  // 用正确答案替换 stem 中的占位符（___、[BLANK]、...），重建完整句子
  // 兜底：旧数据中 stem 可能直接包含选项字母（如 "English C she"），而非占位符
  // 无论是否有占位符，都需要清理内联选项字母 A-D，避免残留（如 "Only C C solve" → "Only can we solve"）
  const placeholderRegex = /_{2,}|\[BLANK\]|\.{3,}/g
  let fullSentence: string
  if (placeholderRegex.test(stem)) {
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
    // 没有占位符 → 尝试清理内联选项字母（旧数据格式，选项字母通常大写）
    const cleaned = stem.replace(/\b[A-D]\b/g, correctText).replace(/\s+/g, ' ').trim()
    fullSentence = cleaned || stem.trim()
  } else {
    fullSentence = correctText
  }

  // 完整句子拆词并打乱
  const words = fullSentence.split(/\s+/).filter(Boolean)
  const wordBank = shuffle(words)

  grammarStates[q.id] = {
    fullSentence,
    wordBank,
    selectedIndices: [],
    knowledgePoint,
    literalTranslation,
  }
}

/** 语法题：获取已选单词列表（按选择顺序） */
function getSelectedWords(qid: number): string[] {
  const state = grammarStates[qid]
  if (!state) return []
  return state.selectedIndices.map(i => state.wordBank[i])
}

/** 语法题：单词库中某索引是否已被使用 */
function isWordUsed(qid: number, wIdx: number): boolean {
  const state = grammarStates[qid]
  return state ? state.selectedIndices.includes(wIdx) : false
}

/** 语法题：选择单词（加入已选列表末尾） */
function selectGrammarWord(qid: number, wIdx: number) {
  const state = grammarStates[qid]
  if (!state || showAnswer.value[qid]) return
  if (state.selectedIndices.includes(wIdx)) return
  state.selectedIndices.push(wIdx)
  if (!answerStartTime.value[qid]) {
    answerStartTime.value[qid] = Date.now()
  }
}

/** 语法题：移除已选单词（按已选列表索引移除） */
function removeGrammarWord(qid: number, sIdx: number) {
  const state = grammarStates[qid]
  if (!state || showAnswer.value[qid]) return
  state.selectedIndices.splice(sIdx, 1)
}

/** 语法题：清空已选单词 */
function clearGrammarWords(qid: number) {
  const state = grammarStates[qid]
  if (!state || showAnswer.value[qid]) return
  state.selectedIndices = []
}

/** 获取展示用的用户答案文本（语法题显示句子，选择题显示选项文本） */
function getSubmittedAnswerText(q: QuestionVO): string {
  if (q.moduleType === 2) {
    return getSelectedWords(q.id).join(' ') || selectedAnswers.value[q.id] || ''
  }
  const answer = selectedAnswers.value[q.id] || ''
  const opts = parsedOptions(q.options)
  if (opts.length > 0) {
    const matched = opts.find(o => o.key === answer)
    return matched ? matched.text : answer
  }
  return answer
}

/** 获取展示用的正确答案（语法题显示完整句子，其他题显示原始 correctAnswer） */
function getDisplayCorrectAnswer(q: QuestionVO): string {
  if (q.moduleType === 2) {
    return grammarStates[q.id]?.fullSentence || q.correctAnswer || submitted.value[q.id]?.correctAnswerKey || ''
  }
  return q.correctAnswer || submitted.value[q.id]?.correctAnswerKey || ''
}

/** 判断某题是否已准备好提交（语法题需有已选单词，其他题需有 selectedAnswers） */
function isAnswerReady(q: QuestionVO): boolean {
  if (q.moduleType === 2) {
    return getSelectedWords(q.id).length > 0
  }
  return !!selectedAnswers.value[q.id]
}

// ========== 答题逻辑 ==========
const selectOption = (qid: number, optKey: string) => {
  selectedAnswers.value[qid] = optKey
  if (!answerStartTime.value[qid]) {
    answerStartTime.value[qid] = Date.now()
  }
}

const handleReanswer = async (q: QuestionVO) => {
  const uid = userStore.userId
  if (!uid || !isAnswerReady(q)) return

  // 根据题型组装提交内容
  let answerToSubmit = ''
  if (q.moduleType === 2) {
    // 语法题：提交拼接后的完整句子
    answerToSubmit = getSelectedWords(q.id).join(' ')
  } else {
    // 选择题提交选项文本（而非字母），填空题提交输入内容
    const answer = selectedAnswers.value[q.id]
    const opts = parsedOptions(q.options)
    if (opts.length > 0) {
      const matched = opts.find(o => o.key === answer)
      answerToSubmit = matched ? matched.text : answer
    } else {
      answerToSubmit = answer
    }
  }

  const elapsedMs = answerStartTime.value[q.id]
    ? Date.now() - answerStartTime.value[q.id]
    : 0
  const durationSeconds = Math.max(1, Math.round(elapsedMs / 1000))

  try {
    const result = await submitAnswer({
      userId: uid,
      questionId: q.id,
      userAnswer: answerToSubmit,
      moduleType: q.moduleType || 1,
      durationSeconds,
    })
    submitted.value[q.id] = result
    showAnswer.value[q.id] = true

    if (result.isCorrect) {
      toast(t('learning.congratsRemoved'), 'success')
      setTimeout(() => {
        errors.value = errors.value.filter(e => e.id !== q.id)
      }, 2500)
    }
  } catch (e) {
    console.warn('提交答案失败', e)
  }
}

const nextQuestion = (qid: number) => {
  showAnswer.value[qid] = false
  selectedAnswers.value[qid] = ''
  delete answerStartTime.value[qid]
  // 语法题重置已选单词
  const state = grammarStates[qid]
  if (state) {
    state.selectedIndices = []
  }
}

const clearAll = () => {
  const target = moduleFilter.value === 0 ? errors.value : filteredErrors.value
  if (target.length === 0) return
  if (!confirm(t('learning.clearAllConfirm'))) return
  const ids = new Set(target.map(q => q.id))
  errors.value = errors.value.filter(q => !ids.has(q.id))
  ids.forEach(id => {
    delete selectedAnswers.value[id]
    delete showAnswer.value[id]
    delete submitted.value[id]
    delete answerStartTime.value[id]
    delete grammarStates[id]
  })
  toast(t('learning.cleared'), 'success')
}

const parsedOptions = (optsStr: string) => {
  if (!optsStr) return []
  try {
    const opts = JSON.parse(optsStr)
    if (Array.isArray(opts)) {
      return opts.map((item: string, idx: number) => {
        const match = String(item).match(/^([A-D])[.\s]*\s*(.+)/)
        if (match) return { key: match[1], text: match[2].trim() }
        return { key: String.fromCharCode(65 + idx), text: String(item).trim() }
      })
    }
    return Object.entries(opts).map(([key, text]) => ({
      key,
      text: String(text).replace(/^[A-D][.\s]*\s*/, '').trim()
    }))
  } catch {
    return optsStr.split('\n').filter(Boolean).map((line, idx) => {
      const match = line.match(/^([A-D])[.\s]*\s*(.+)/)
      if (match) return { key: match[1], text: match[2].trim() }
      return { key: String.fromCharCode(65 + idx), text: line.trim() }
    })
  }
}

onMounted(load)

// ========== 同类题生成 ==========

/** 生成同类练习题 */
const handleGenerateSimilar = async (q: QuestionVO) => {
  const uid = userStore.userId
  if (!uid || !q.id) return
  generating.value[q.id] = true
  try {
    const questions = await generateSimilar({
      userId: uid,
      questionId: q.id,
      count: 5,
    })
    if (questions && questions.length > 0) {
      similarQuestions.value = questions
      currentSimilarIndex.value = 0
      similarSelectedAnswers.value = {}
      similarShowAnswer.value = {}
      similarSubmitted.value = {}
      similarModalVisible.value = true
      toast(`已生成 ${questions.length} 道同类题`, 'success')
    } else {
      toast('暂无可生成的同类题，请稍后再试', 'error')
    }
  } catch (e) {
    console.warn('生成同类题失败', e)
    toast('生成同类题失败，请稍后再试', 'error')
  } finally {
    generating.value[q.id] = false
  }
}

/** 提交同类题答案 */
const handleSimilarSubmit = async () => {
  const idx = currentSimilarIndex.value
  const q = similarQuestions.value[idx]
  if (!q) return
  const uid = userStore.userId
  if (!uid) return

  const answer = similarSelectedAnswers.value[idx] || ''
  if (!answer) return

  // AI生成的题目（showAnswer=true，有correctAnswer）本地判分
  if (q.showAnswer && q.correctAnswer) {
    const isCorrect = answer.trim().toLowerCase() === q.correctAnswer.trim().toLowerCase()
    similarSubmitted.value[idx] = { isCorrect }
    similarShowAnswer.value[idx] = true
    return
  }

  // 正式题库题目，调用后端提交接口
  try {
    const result = await submitAnswer({
      userId: uid,
      questionId: q.id,
      userAnswer: answer,
      moduleType: q.moduleType || 1,
      durationSeconds: 10,
    })
    similarSubmitted.value[idx] = { isCorrect: result.isCorrect }
    similarShowAnswer.value[idx] = true
    // 如果后端返回了正确答案，更新题目显示
    if (result.correctAnswer) {
      similarQuestions.value[idx] = { ...q, correctAnswer: result.correctAnswer }
    }
  } catch (e) {
    console.warn('提交同类题失败', e)
    // 兜底：本地简单判分
    const correctAnswer = q.correctAnswer || ''
    const isCorrect = answer.trim().toLowerCase() === correctAnswer.trim().toLowerCase()
    similarSubmitted.value[idx] = { isCorrect }
    similarShowAnswer.value[idx] = true
  }
}

/** 下一题 */
const nextSimilarQuestion = () => {
  if (currentSimilarIndex.value < similarQuestions.value.length - 1) {
    currentSimilarIndex.value++
  } else {
    closeSimilarModal()
  }
}

/** 关闭弹窗 */
const closeSimilarModal = () => {
  similarModalVisible.value = false
  similarQuestions.value = []
  currentSimilarIndex.value = 0
  similarSelectedAnswers.value = {}
  similarShowAnswer.value = {}
  similarSubmitted.value = {}
}
</script>
