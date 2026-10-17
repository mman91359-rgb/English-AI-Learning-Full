<template>
  <AppLayout>
    <div class="flex items-center gap-4 mb-6">
      <button class="p-2 hover:bg-surface-variant rounded-full transition-colors" @click="$router.back()">
        <span class="material-symbols-outlined text-on-surface">arrow_back</span>
      </button>
      <h2 class="font-headline-lg text-headline-lg text-on-surface">{{ $t('app.title') }} - {{ $t('learning.myVocabulary') }}</h2>
    </div>

    <!-- 顶部统计卡片 -->
    <div class="grid grid-cols-3 gap-3 mb-6">
      <div class="bg-surface-container-lowest rounded-[16px] p-4 shadow-soft-glow text-center">
        <p class="font-headline-lg text-headline-lg text-primary">{{ stats.total }}</p>
        <p class="font-label-md text-label-md text-on-surface-variant mt-1">单词总数</p>
      </div>
      <div class="bg-surface-container-lowest rounded-[16px] p-4 shadow-soft-glow text-center">
        <p class="font-headline-lg text-headline-lg text-green-600">{{ stats.mastered }}</p>
        <p class="font-label-md text-label-md text-on-surface-variant mt-1">已掌握</p>
      </div>
      <div
        class="bg-surface-container-lowest rounded-[16px] p-4 shadow-soft-glow text-center cursor-pointer hover:bg-surface-container-low transition-colors"
        @click="goReview"
      >
        <p class="font-headline-lg text-headline-lg" :class="stats.review > 0 ? 'text-error' : 'text-on-surface-variant'">
          {{ stats.review }}
        </p>
        <p class="font-label-md text-label-md text-on-surface-variant mt-1">今日待复习</p>
      </div>
    </div>

    <!-- 智能添加单词 -->
    <form class="bg-surface-container-lowest rounded-[20px] p-5 shadow-soft-glow mb-6" @submit.prevent="handleAdd">
      <div class="flex items-center gap-2 mb-3">
        <span class="material-symbols-outlined text-primary text-xl">auto_awesome</span>
        <span class="font-label-lg text-label-lg text-on-surface">智能添加单词</span>
      </div>
      <div class="flex flex-col sm:flex-row gap-3">
        <input
          v-model="newWord"
          class="flex-1 px-4 py-3 bg-background border border-outline-variant rounded-xl text-on-surface placeholder-outline font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
          placeholder="输入单词，AI 自动补全音标、释义、例句"
          required
        />
        <button
          type="submit"
          class="px-6 py-3 bg-primary text-on-primary rounded-xl font-label-md text-label-md hover:bg-primary/90 transition-colors shrink-0 flex items-center gap-2"
          :disabled="adding"
        >
          <span class="material-symbols-outlined text-lg">{{ adding ? 'progress_activity' : 'add' }}</span>
          {{ adding ? 'AI 生成中...' : '智能添加' }}
        </button>
      </div>
      <p v-if="addHint" class="mt-2 text-sm text-on-surface-variant">{{ addHint }}</p>
    </form>

    <!-- 筛选标签 -->
    <div class="flex items-center gap-2 mb-4 overflow-x-auto pb-1">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-4 py-2 rounded-full font-label-md text-label-md whitespace-nowrap transition-colors"
        :class="activeTab === tab.key
          ? 'bg-primary text-on-primary'
          : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low'"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span class="ml-1 opacity-70">({{ tab.count }})</span>
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-20">
      <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
    </div>

    <div v-else-if="filteredWords.length === 0" class="text-center py-20">
      <span class="material-symbols-outlined text-6xl text-outline">bookmark_border</span>
      <p class="mt-4 text-on-surface-variant font-body-lg text-body-lg">{{ emptyHint }}</p>
      <p class="mt-2 text-on-surface-variant/60 font-body-md text-body-md">在上方输入单词开始积累</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="w in filteredWords"
        :key="w.id"
        :class="[
          'bg-surface-container-lowest rounded-[16px] p-4 shadow-soft-glow hover:shadow-md transition-shadow',
          w.isMastered ? 'opacity-60' : ''
        ]"
      >
        <div class="flex items-start gap-4">
          <!-- 掌握度圆圈 -->
          <button
            class="w-10 h-10 rounded-full flex items-center justify-center shrink-0 transition-colors"
            :class="w.isMastered ? 'bg-green-100 text-green-600' : 'bg-outline-variant/20 text-outline'"
            @click="handleToggle(w)"
            :title="`掌握度 ${w.masteryLevel ?? 0}/5`"
          >
            <span class="material-symbols-outlined text-lg">{{ w.isMastered ? 'check_circle' : 'radio_button_unchecked' }}</span>
          </button>

          <div class="flex-1 min-w-0">
            <!-- 单词行：单词 + 词性 + 音标 -->
            <div class="flex items-baseline gap-3 flex-wrap">
              <p class="font-headline-lg text-headline-lg text-on-surface" :class="w.isMastered ? 'line-through' : ''">
                {{ w.word }}
              </p>
              <span v-if="w.pos" class="font-label-md text-label-md text-primary italic">{{ w.pos }}</span>
              <span v-if="w.phonetic" class="font-body-sm text-body-sm text-on-surface-variant">{{ w.phonetic }}</span>
            </div>
            <!-- 释义 -->
            <p class="font-body-md text-body-md text-on-surface-variant mt-1">
              {{ w.translation || '暂无释义' }}
            </p>
            <!-- 例句 -->
            <div v-if="w.example" class="mt-2 pl-3 border-l-2 border-primary/40">
              <p class="font-body-sm text-body-sm text-on-surface italic">{{ w.example }}</p>
              <p v-if="w.exampleTranslation" class="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
                {{ w.exampleTranslation }}
              </p>
            </div>
            <!-- 搭配 -->
            <p v-if="w.collocation" class="mt-2 font-body-sm text-body-sm text-on-surface-variant">
              <span class="text-outline">搭配：</span>{{ w.collocation }}
            </p>
            <!-- 掌握度进度条 + 复习信息 -->
            <div class="mt-2 flex items-center gap-3">
              <div class="flex-1 flex items-center gap-1">
                <div class="flex-1 h-1.5 bg-outline-variant/20 rounded-full overflow-hidden">
                  <div
                    class="h-full bg-primary transition-all"
                    :style="{ width: `${((w.masteryLevel ?? 0) / 5) * 100}%` }"
                  ></div>
                </div>
                <span class="font-label-sm text-label-sm text-on-surface-variant whitespace-nowrap">
                  Lv{{ w.masteryLevel ?? 0 }}/5
                </span>
              </div>
              <span v-if="w.nextReviewAt && !w.isMastered" class="font-label-sm text-label-sm text-on-surface-variant/70 whitespace-nowrap">
                <span class="material-symbols-outlined text-sm align-middle">schedule</span>
                {{ formatReviewDate(w.nextReviewAt) }}
              </span>
            </div>
          </div>

          <button
            class="p-2 hover:bg-error-container/20 rounded-full transition-colors shrink-0"
            @click="handleRemove(w)"
          >
            <span class="material-symbols-outlined text-error">delete</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ============ 复习模式弹窗 ============ -->
    <div
      v-if="reviewMode"
      class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
      @click.self="exitReview"
    >
      <div class="bg-surface-container-lowest rounded-[24px] p-6 shadow-soft-glow w-full max-w-lg">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">school</span>
            <span class="font-headline-md text-headline-md text-on-surface">复习模式</span>
          </div>
          <button class="p-2 hover:bg-surface-variant rounded-full" @click="exitReview">
            <span class="material-symbols-outlined text-on-surface">close</span>
          </button>
        </div>

        <!-- 复习进度 -->
        <div class="flex items-center justify-between mb-4">
          <span class="font-body-md text-body-md text-on-surface-variant">
            {{ reviewIndex + 1 }} / {{ reviewQueue.length }}
          </span>
          <div class="flex-1 mx-4 h-1.5 bg-outline-variant/20 rounded-full overflow-hidden">
            <div
              class="h-full bg-primary transition-all"
              :style="{ width: `${((reviewIndex + 1) / reviewQueue.length) * 100}%` }"
            ></div>
          </div>
        </div>

        <div v-if="reviewQueue.length > 0 && currentReview">
          <!-- 卡片正面：单词 + 音标，点击/按钮翻面看释义 -->
          <div class="bg-background rounded-2xl p-8 min-h-[200px] flex flex-col items-center justify-center text-center">
            <p class="font-headline-lg text-headline-lg text-on-surface">{{ currentReview.word }}</p>
            <p v-if="currentReview.phonetic" class="font-body-md text-body-md text-on-surface-variant mt-2">
              {{ currentReview.phonetic }}
            </p>
            <p v-if="currentReview.pos" class="font-label-md text-label-md text-primary italic mt-1">
              {{ currentReview.pos }}
            </p>

            <transition name="fade">
              <div v-if="flipped" class="mt-4 w-full">
                <p class="font-body-lg text-body-lg text-on-surface">{{ currentReview.translation }}</p>
                <div v-if="currentReview.example" class="mt-3 pl-3 border-l-2 border-primary/40 text-left">
                  <p class="font-body-sm text-body-sm text-on-surface italic">{{ currentReview.example }}</p>
                  <p v-if="currentReview.exampleTranslation" class="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
                    {{ currentReview.exampleTranslation }}
                  </p>
                </div>
              </div>
            </transition>

            <button
              v-if="!flipped"
              class="mt-6 px-4 py-2 bg-surface-container-low text-on-surface rounded-full font-label-md text-label-md hover:bg-surface-container transition-colors flex items-center gap-1"
              @click="flipped = true"
            >
              <span class="material-symbols-outlined text-base">visibility</span>
              查看释义
            </button>
          </div>

          <!-- 操作按钮：翻面后才可评价 -->
          <div class="grid grid-cols-2 gap-3 mt-4">
            <button
              class="py-3 bg-error-container/30 text-error rounded-xl font-label-md text-label-md hover:bg-error-container/50 transition-colors flex items-center justify-center gap-1"
              :disabled="!flipped || reviewing"
              @click="handleReview(false)"
            >
              <span class="material-symbols-outlined text-base">{{ reviewing ? 'progress_activity' : 'close' }}</span>
              没记住
            </button>
            <button
              class="py-3 bg-green-100 text-green-700 rounded-xl font-label-md text-label-md hover:bg-green-200 transition-colors flex items-center justify-center gap-1"
              :disabled="!flipped || reviewing"
              @click="handleReview(true)"
            >
              <span class="material-symbols-outlined text-base">{{ reviewing ? 'progress_activity' : 'check' }}</span>
              记住了
            </button>
          </div>
        </div>

        <!-- 复习完成 -->
        <div v-else class="py-12 text-center">
          <span class="material-symbols-outlined text-6xl text-primary">celebration</span>
          <p class="mt-4 font-headline-md text-headline-md text-on-surface">今日复习完成！</p>
          <p class="mt-2 font-body-md text-body-md text-on-surface-variant">
            记住 {{ reviewCorrect }} 个，未记住 {{ reviewWrong }} 个
          </p>
          <button
            class="mt-6 px-6 py-3 bg-primary text-on-primary rounded-xl font-label-md text-label-md hover:bg-primary/90 transition-colors"
            @click="exitReview"
          >
            完成
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import {
  getVocabulary,
  addVocabularySmart,
  removeVocabulary,
  toggleMastered,
  getReviewList,
  reviewWord,
  type VocabularyVO,
} from '@/api/exercise'

const userStore = useUserStore()
const words = ref<VocabularyVO[]>([])
const loading = ref(true)
const adding = ref(false)
const newWord = ref('')
const addHint = ref('')
const activeTab = ref<'all' | 'review' | 'mastered'>('all')

// ===== 统计 =====
const stats = computed(() => ({
  total: words.value.length,
  mastered: words.value.filter(w => w.isMastered === 1).length,
  review: words.value.filter(w => w.isMastered === 0 && w.nextReviewAt && new Date(w.nextReviewAt) <= new Date()).length,
}))

// ===== 筛选 =====
const tabs = computed(() => [
  { key: 'all' as const, label: '全部', count: words.value.length },
  { key: 'review' as const, label: '待复习', count: stats.value.review },
  { key: 'mastered' as const, label: '已掌握', count: stats.value.mastered },
])

const filteredWords = computed(() => {
  if (activeTab.value === 'review') {
    return words.value.filter(w => w.isMastered === 0 && w.nextReviewAt && new Date(w.nextReviewAt) <= new Date())
  }
  if (activeTab.value === 'mastered') {
    return words.value.filter(w => w.isMastered === 1)
  }
  return words.value
})

const emptyHint = computed(() => {
  if (activeTab.value === 'review') return '今日没有待复习的单词'
  if (activeTab.value === 'mastered') return '还没有已掌握的单词'
  return '单词本为空'
})

// ===== 加载 =====
const load = async () => {
  const uid = userStore.userId
  if (!uid) return
  loading.value = true
  try {
    words.value = await getVocabulary(uid)
  } catch (e) {
    console.warn('加载单词本失败', e)
  } finally {
    loading.value = false
  }
}

// ===== 智能添加 =====
const handleAdd = async () => {
  const uid = userStore.userId
  if (!uid || !newWord.value.trim()) return
  adding.value = true
  addHint.value = ''
  try {
    const vo = await addVocabularySmart({
      userId: uid,
      word: newWord.value.trim(),
      source: 'manual',
    })
    words.value.unshift(vo)
    newWord.value = ''
    addHint.value = 'AI 已自动补全音标、释义、例句'
  } catch (e: any) {
    addHint.value = '添加失败：' + (e?.message || '请稍后重试')
  } finally {
    adding.value = false
    setTimeout(() => (addHint.value = ''), 4000)
  }
}

const handleToggle = async (w: VocabularyVO) => {
  const uid = userStore.userId
  if (!uid) return
  try {
    const updated = await toggleMastered(w.id, uid)
    const idx = words.value.findIndex(v => v.id === w.id)
    if (idx >= 0) words.value[idx] = updated
  } catch (e) {
    console.warn('切换掌握状态失败', e)
  }
}

const handleRemove = async (w: VocabularyVO) => {
  const uid = userStore.userId
  if (!uid) return
  try {
    await removeVocabulary(w.id, uid)
    words.value = words.value.filter(v => v.id !== w.id)
  } catch (e) {
    console.warn('删除单词失败', e)
  }
}

// ===== 日期格式化 =====
const formatReviewDate = (dateStr: string) => {
  const d = new Date(dateStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const diff = Math.round((target.getTime() - today.getTime()) / 86400000)
  if (diff < 0) return '已到期'
  if (diff === 0) return '今日复习'
  return `${diff}天后复习`
}

// ===== 复习模式 =====
const reviewMode = ref(false)
const reviewQueue = ref<VocabularyVO[]>([])
const reviewIndex = ref(0)
const flipped = ref(false)
const reviewing = ref(false)
const reviewCorrect = ref(0)
const reviewWrong = ref(0)

const currentReview = computed(() =>
  reviewIndex.value < reviewQueue.value.length ? reviewQueue.value[reviewIndex.value] : null
)

const goReview = async () => {
  const uid = userStore.userId
  if (!uid) return
  try {
    const list = await getReviewList(uid)
    if (list.length === 0) {
      reviewQueue.value = []
      reviewIndex.value = 0
      reviewCorrect.value = 0
      reviewWrong.value = 0
      reviewMode.value = true
      return
    }
    reviewQueue.value = list
    reviewIndex.value = 0
    flipped.value = false
    reviewCorrect.value = 0
    reviewWrong.value = 0
    reviewMode.value = true
  } catch (e) {
    console.warn('加载复习列表失败', e)
  }
}

const handleReview = async (isCorrect: boolean) => {
  const uid = userStore.userId
  const cur = currentReview.value
  if (!uid || !cur || reviewing.value) return
  reviewing.value = true
  try {
    const updated = await reviewWord(cur.id, { userId: uid, isCorrect })
    // 更新主列表中对应单词
    const idx = words.value.findIndex(v => v.id === cur.id)
    if (idx >= 0) words.value[idx] = updated
    if (isCorrect) reviewCorrect.value++
    else reviewWrong.value++
    // 下一张
    reviewIndex.value++
    flipped.value = false
    if (reviewIndex.value >= reviewQueue.value.length) {
      // 复习完成，刷新统计
      await load()
    }
  } catch (e) {
    console.warn('提交复习结果失败', e)
  } finally {
    reviewing.value = false
  }
}

const exitReview = () => {
  reviewMode.value = false
  flipped.value = false
}

onMounted(load)
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
