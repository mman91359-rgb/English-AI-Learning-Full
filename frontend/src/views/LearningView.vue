<template>
  <AppLayout>
    <h2 class="font-headline-lg text-headline-lg text-on-surface mb-8">{{ $t('learning.title') }}</h2>

    <!-- 分类标签 -->
    <div class="flex gap-2 mb-8 overflow-x-auto pb-2 scrollbar-hide">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="[
          'px-6 py-2 rounded-full font-label-lg text-label-lg whitespace-nowrap transition-colors',
          activeTab === tab.key
            ? 'bg-on-surface text-surface'
            : tab.available
              ? 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
              : 'text-on-surface-variant/50 cursor-not-allowed'
        ]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
        <span v-if="!tab.available" class="ml-1 text-label-sm">{{ $t('learning.comingSoon') }}</span>
      </button>
    </div>

    <!-- ========== 语法 tab 专属布局（截图风格） ========== -->
    <template v-if="activeTab === 'grammar'">
      <!-- 语法学习主卡（淡蓝大卡片） -->
      <div
        class="bg-[#cdf0fa] dark:bg-[#1e3a4a] rounded-[24px] p-8 flex flex-col justify-between relative overflow-hidden cursor-pointer hover:shadow-[0_4px_20px_rgba(51,58,72,0.15)] transition-shadow min-h-[200px] mb-8 group"
        @click="startPractice"
      >
        <div class="relative z-10 text-[#0e4f63] dark:text-[#9fdbf0]">
          <h3 class="font-headline-md text-headline-md mb-2 font-bold">{{ $t('learning.grammarPractice') }}</h3>
          <div class="flex items-center gap-2 font-body-md text-body-md">
            <template v-if="editingTarget">
              <input
                ref="targetInput"
                v-model.number="editValue"
                class="w-16 bg-white/30 rounded px-2 py-0.5 text-center outline-none text-[#0e4f63]"
                @keydown.enter="confirmTarget"
                @keydown.escape="cancelTarget"
                @blur="confirmTarget"
              />
            </template>
            <template v-else>
              <span @click.stop="startEditTarget" class="cursor-pointer">{{ $t('learning.dailyTargetItems', { count: dailyTarget }) }}</span>
              <span class="material-symbols-outlined text-sm cursor-pointer" @click.stop="startEditTarget">edit</span>
            </template>
          </div>
        </div>
        <!-- 右侧装饰：四个点 + 圆形播放按钮 -->
        <div class="absolute right-8 top-1/2 -translate-y-1/2 z-10 flex items-center gap-3">
          <div class="flex flex-col gap-2 opacity-50">
            <div class="w-1.5 h-1.5 rounded-full bg-[#0e4f63] dark:bg-[#9fdbf0]"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-[#0e4f63] dark:bg-[#9fdbf0]"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-[#0e4f63] dark:bg-[#9fdbf0]"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-[#0e4f63] dark:bg-[#9fdbf0]"></div>
          </div>
          <div class="w-16 h-16 bg-white/40 rounded-full flex items-center justify-center backdrop-blur-sm group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-4xl text-[#0e4f63] dark:text-[#9fdbf0]" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
          </div>
        </div>
      </div>

      <!-- 学习过的语法 / 查看所有的AI问答 -->
      <div class="bg-surface-container-lowest rounded-[24px] shadow-soft-glow overflow-hidden">
        <div
          class="flex items-center justify-between p-6 cursor-pointer hover:bg-surface-container-lowest/80 transition-colors border-b border-outline-variant/10"
          @click="goToStudiedGrammar"
        >
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-green-600 dark:text-green-400" style="font-variation-settings: 'FILL' 1;">hourglass</span>
            </div>
            <span class="font-body-lg text-body-lg text-on-surface">{{ $t('learning.studiedGrammar') }}</span>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
        <div
          class="flex items-center justify-between p-6 cursor-pointer hover:bg-surface-container-lowest/80 transition-colors"
          @click="toastComingSoon"
        >
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-cyan-100 dark:bg-cyan-900/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-cyan-600 dark:text-cyan-400" style="font-variation-settings: 'FILL' 1;">lightbulb</span>
            </div>
            <span class="font-body-lg text-body-lg text-on-surface">{{ $t('learning.viewAllAiQA') }}</span>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
      </div>
    </template>

    <!-- ========== 其他 tab 原有布局 ========== -->
    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-12 gap-gutter">
        <!-- 主练习入口 -->
        <div
          class="col-span-1 md:col-span-8 bg-primary-container rounded-[24px] p-8 flex flex-col justify-between relative overflow-hidden group cursor-pointer hover:shadow-[0_4px_20px_rgba(51,58,72,0.15)] transition-shadow min-h-[200px]"
          @click="startPractice"
        >
          <div class="relative z-10 text-on-primary-container">
            <h3 class="font-headline-md text-headline-md mb-2">{{ currentModuleName }}{{ $t('learning.practice') }}</h3>
            <div class="flex items-center gap-2 text-on-primary-container/80 font-body-md text-body-md">
              <template v-if="editingTarget">
                <input
                  ref="targetInput"
                  v-model.number="editValue"
                  class="w-16 bg-white/20 rounded px-2 py-0.5 text-center outline-none"
                  @keydown.enter="confirmTarget"
                  @keydown.escape="cancelTarget"
                  @blur="confirmTarget"
                />
              </template>
              <template v-else>
                <span @click.stop="startEditTarget" class="cursor-pointer">{{ $t('learning.dailyTargetWords', { count: dailyTarget }) }}</span>
                <span class="material-symbols-outlined text-sm cursor-pointer" @click.stop="startEditTarget">edit</span>
              </template>
            </div>
          </div>
          <div class="absolute right-8 top-1/2 -translate-y-1/2 z-10 w-16 h-16 bg-surface-container-lowest/20 rounded-full flex items-center justify-center backdrop-blur-sm group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-4xl text-on-primary-container fill">play_arrow</span>
          </div>
          <div class="absolute right-32 top-1/2 -translate-y-1/2 flex flex-col gap-2 opacity-30">
            <div class="w-1.5 h-1.5 rounded-full bg-on-primary-container"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-on-primary-container"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-on-primary-container"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-on-primary-container"></div>
          </div>
        </div>

        <!-- 收藏 & 错题 -->
        <div class="col-span-1 md:col-span-4 flex flex-col gap-4">
          <div
            class="bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow flex items-center justify-between cursor-pointer hover:bg-surface-container-lowest/80 transition-colors"
            @click="goToFavorites"
          >
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-error-container/30 flex items-center justify-center">
                <span class="material-symbols-outlined text-error fill">favorite</span>
              </div>
              <div>
                <span class="font-body-lg text-body-lg text-on-surface block">{{ $t('learning.myFavorites') }}</span>
                <span class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.favoritesCount', { count: favoriteCount }) }}</span>
              </div>
            </div>
            <span class="material-symbols-outlined text-outline">chevron_right</span>
          </div>
          <div
            class="bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow flex items-center justify-between cursor-pointer hover:bg-surface-container-lowest/80 transition-colors"
            @click="goToErrorBook"
          >
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-secondary-container/30 flex items-center justify-center">
                <span class="material-symbols-outlined text-secondary">build</span>
              </div>
              <div>
                <span class="font-body-lg text-body-lg text-on-surface block">{{ $t('learning.myErrorBook') }}</span>
                <span class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.errorCountLabel', { count: errorCount }) }}</span>
              </div>
            </div>
            <span class="material-symbols-outlined text-outline">chevron_right</span>
          </div>
        </div>

        <!-- 学习历史 & 单词本 & AI问答 -->
        <div class="col-span-1 md:col-span-4 bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow flex items-center justify-between cursor-pointer hover:bg-surface-container-lowest/80 transition-colors" @click="goToHistory">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-tertiary-container/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-tertiary">hourglass_bottom</span>
            </div>
            <div>
              <span class="font-body-lg text-body-lg text-on-surface block">{{ $t('learning.learningHistory') }}</span>
              <span class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.answeredCountLabel', { count: totalAnswered }) }}</span>
            </div>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
        <div class="col-span-1 md:col-span-4 bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow flex items-center justify-between cursor-pointer hover:bg-surface-container-lowest/80 transition-colors" @click="goToVocabulary">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-primary-container/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-primary">bookmark</span>
            </div>
            <div>
              <span class="font-body-lg text-body-lg text-on-surface block">{{ $t('learning.myVocabulary') }}</span>
              <span class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.wordBookCountLabel', { count: wordBookCount }) }}</span>
            </div>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
        <div class="col-span-1 md:col-span-4 bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow flex items-center justify-between cursor-pointer hover:bg-surface-container-lowest/80 transition-colors" @click="openAiChat">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-secondary-container/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-secondary">psychology</span>
            </div>
            <div>
              <span class="font-body-lg text-body-lg text-on-surface block">{{ $t('learning.aiQA') }}</span>
              <span class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.aiAssistant') }}</span>
            </div>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
      </div>

      <!-- 词典 -->
      <div class="mt-12">
        <h3 class="font-headline-md text-headline-md text-on-surface mb-6">{{ $t('learning.dictionary') }}</h3>
        <div class="col-span-1 md:col-span-4 bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow flex items-center justify-between cursor-pointer hover:bg-surface-container-lowest/80 transition-colors" @click="toastComingSoon">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-secondary-container/20 flex items-center justify-center">
              <span class="material-symbols-outlined text-secondary">search</span>
            </div>
            <div>
              <span class="font-body-lg text-body-lg text-on-surface block">{{ $t('learning.englishDictionary') }}</span>
              <span class="font-label-md text-label-md text-on-surface-variant">{{ $t('learning.dictionaryHint') }}</span>
            </div>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
      </div>
    </template>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'
import { getErrorBook, getFavorites, getVocabulary } from '@/api/exercise'
import { getCurveStats } from '@/api/learning'
import { useToast } from '@/composables/useToast'

const { t } = useI18n()
const { show: toast } = useToast()
const userStore = useUserStore()
const aiStore = useAiStore()
const router = useRouter()
const route = useRoute()

// ========== 标签配置 ==========
// 标签顺序按截图：词汇 / 语法 / 翻译 / 听力 / 造句
const tabs = computed(() => [
  { key: 'vocab', label: t('learning.vocabTab'), moduleType: 1, available: true },
  { key: 'grammar', label: t('learning.grammarTab'), moduleType: 2, available: true },
  { key: 'translation', label: t('learning.translationTab'), moduleType: 3, available: false },
  { key: 'listening', label: t('learning.listeningTab'), moduleType: 4, available: false },
  { key: 'sentence', label: t('learning.sentenceTab'), moduleType: 5, available: false },
])
const activeTab = ref('vocab')

const currentTab = computed(() => {
  return tabs.value.find(tab => tab.key === activeTab.value) || tabs.value[0]
})

const currentModuleType = computed(() => {
  return currentTab.value.moduleType
})

const currentModuleName = computed(() => {
  const names: Record<string, string> = {
    vocab: t('learning.vocabTab'),
    grammar: t('learning.grammarTab'),
    translation: t('learning.translationTab'),
    listening: t('learning.listeningTab'),
    sentence: t('learning.sentenceTab'),
  }
  return names[activeTab.value] || t('learning.vocabTab')
})

const DAILY_TARGET_KEY_VOCAB = 'dailyTarget_vocab'
const DAILY_TARGET_KEY_GRAMMAR = 'dailyTarget_grammar'

const defaultTargets: Record<string, number> = {
  vocab: 20,
  grammar: 10,
}

const getTargetKey = () => {
  if (activeTab.value === 'grammar') return DAILY_TARGET_KEY_GRAMMAR
  return DAILY_TARGET_KEY_VOCAB
}

const getDefaultTarget = () => {
  return defaultTargets[activeTab.value] || 20
}

const dailyTarget = ref(Number(localStorage.getItem(getTargetKey())) || getDefaultTarget())

const editingTarget = ref(false)
const editValue = ref(dailyTarget.value)
const targetInput = ref<HTMLInputElement | null>(null)

const loadTarget = () => {
  dailyTarget.value = Number(localStorage.getItem(getTargetKey())) || getDefaultTarget()
}

const startEditTarget = () => {
  editValue.value = dailyTarget.value
  editingTarget.value = true
  setTimeout(() => targetInput.value?.focus(), 0)
}

const confirmTarget = () => {
  const v = Math.max(1, Math.min(999, editValue.value || getDefaultTarget()))
  dailyTarget.value = v
  localStorage.setItem(getTargetKey(), String(v))
  editingTarget.value = false
}

const cancelTarget = () => {
  editingTarget.value = false
}

watch(activeTab, () => {
  loadTarget()
})

// ========== 统计数据 ==========
const favoriteCount = ref(0)
const errorCount = ref(0)
const totalAnswered = ref(0)
const wordBookCount = ref(0)

// ========== 加载学习中心数据 ==========
const loadLearningData = async () => {
  const userId = userStore.userId
  if (!userId) return

  try {
    const errors = await getErrorBook(userId)
    errorCount.value = errors?.length || 0
  } catch (e) {
    console.warn('获取错题本失败', e)
    errorCount.value = 0
  }

  try {
    const favs = await getFavorites(userId)
    favoriteCount.value = favs?.length || 0
  } catch (e) {
    console.warn('获取收藏数失败', e)
    favoriteCount.value = 0
  }

  try {
    const words = await getVocabulary(userId)
    wordBookCount.value = words?.length || 0
  } catch (e) {
    console.warn('获取单词本数量失败', e)
    wordBookCount.value = 0
  }

  try {
    const stats = await getCurveStats(userId)
    totalAnswered.value = stats?.totalQuestions || 0
  } catch (e) {
    console.warn('获取曲线统计失败', e)
  }
}

// ========== 练习入口 ==========
const startPractice = () => {
  const userId = userStore.userId
  if (!userId) {
    toast(t('learning.pleaseLogin'), 'warning')
    return
  }
  if (!currentTab.value.available) {
    toastComingSoon()
    return
  }
  // 词汇/语法走专属练习页（截图风格），其他模块走统一 PracticeView
  if (currentModuleType.value === 1) {
    router.push('/vocab-quiz')
  } else if (currentModuleType.value === 2) {
    router.push('/grammar-quiz')
  } else {
    router.push({ path: '/learning/practice', query: { moduleType: currentModuleType.value, count: dailyTarget.value } })
  }
}

const switchTab = (key: string) => {
  const target = tabs.value.find(tab => tab.key === key)
  if (target && !target.available) {
    toastComingSoon()
    return
  }
  activeTab.value = key
}

// ========== 开发中功能提示 ==========
const toastComingSoon = () => {
  toast(t('common.comingSoon'), 'info')
}

// ========== AI 问答 ==========
const openAiChat = () => {
  aiStore.open({ contextType: 'general' })
}

// ========== 导航 ==========
const goToFavorites = () => {
  router.push('/favorites')
}

const goToErrorBook = () => {
  router.push('/error-book')
}

const goToHistory = () => {
  router.push('/history')
}

const goToVocabulary = () => {
  router.push('/vocabulary')
}

// 学习过的语法：复用错题本（语法模块）
const goToStudiedGrammar = () => {
  router.push('/error-book')
}

watch(() => userStore.userId, (newId) => {
  if (newId) loadLearningData()
}, { immediate: true })

// 导航回学习中心时自动刷新数据
watch(() => route.path, (newPath) => {
  if (newPath === '/learning') loadLearningData()
})

onMounted(() => {
  if (userStore.userId) {
    loadLearningData()
  }
})
</script>
