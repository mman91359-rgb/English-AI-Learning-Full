<template>
  <AppLayout>
    <!-- 顶部标题栏 -->
    <div class="flex items-center gap-4 mb-6">
      <button class="p-2 hover:bg-surface-variant rounded-full transition-colors" @click="$router.back()">
        <span class="material-symbols-outlined text-on-surface">arrow_back</span>
      </button>
      <h2 class="font-headline-lg text-headline-lg text-on-surface">AI 题目审核管理</h2>
    </div>

    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex gap-2">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          :class="[
            'px-4 py-2 rounded-full font-label-sm text-label-sm transition-colors',
            currentStatus === tab.value
              ? 'bg-primary text-on-primary'
              : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
          ]"
          @click="currentStatus = tab.value; fetchQuestions()"
        >
          {{ tab.label }}
        </button>
      </div>
      <button
        class="px-5 py-2.5 bg-primary text-on-primary rounded-full font-label-md text-label-md hover:bg-primary/90 transition-colors flex items-center gap-2"
        @click="showGenerateDialog = true"
      >
        <span class="material-symbols-outlined text-lg">auto_awesome</span>
        AI 出题
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="questions.length === 0" class="text-center py-20">
      <span class="material-symbols-outlined text-5xl text-outline">inventory_2</span>
      <p class="mt-3 text-on-surface-variant font-body-md text-body-md">暂无待审核题目</p>
      <button
        class="mt-3 px-5 py-2 rounded-full border border-outline-variant text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant transition-colors"
        @click="showGenerateDialog = true"
      >
        点击 AI 出题 生成新题目
      </button>
    </div>

    <!-- 题目列表 -->
    <div v-else class="space-y-3">
      <div
        v-for="q in questions"
        :key="q.id"
        class="bg-surface-container-lowest rounded-[16px] shadow-soft-glow overflow-hidden transition-shadow hover:shadow-md"
      >
        <!-- 题目主体 -->
        <div class="p-4">
          <!-- 标题行：知识点 + 状态标签 -->
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <span
                class="px-2.5 py-0.5 rounded-full font-label-xs text-label-xs"
                :class="q.moduleType === 1 ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'"
              >
                {{ q.moduleType === 1 ? '词汇' : '语法' }}
              </span>
              <span class="font-label-md text-label-md text-on-surface">{{ q.knowledgePointName }}</span>
              <span class="text-outline-variant">|</span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">{{ q.targetLevel }}</span>
              <span class="text-outline-variant">|</span>
              <span class="font-label-sm text-label-sm text-on-surface-variant">难度 {{ q.difficultyLevel }}/8</span>
            </div>
            <span
              class="px-2.5 py-0.5 rounded-full font-label-xs text-label-xs"
              :class="statusClass(q.status)"
            >
              {{ statusLabel(q.status) }}
            </span>
          </div>

          <!-- 题干 -->
          <div class="bg-background rounded-xl p-3 mb-3">
            <p class="font-body-sm text-body-sm text-on-surface leading-relaxed">{{ q.stem }}</p>
          </div>

          <!-- 选项 -->
          <div v-if="parsedOptions(q.options).length > 0" class="mb-3">
            <p class="font-label-xs text-label-xs text-on-surface-variant mb-1.5">选项</p>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="(opt, idx) in parsedOptions(q.options)"
                :key="idx"
                class="px-3 py-1 bg-background rounded-lg font-body-sm text-body-sm text-on-surface"
              >
                {{ String.fromCharCode(65 + idx) }}. {{ opt }}
              </span>
            </div>
          </div>

          <!-- 正确答案 + 解析 -->
          <div class="flex gap-6">
            <div class="flex-1">
              <p class="font-label-xs text-label-xs text-on-surface-variant mb-1">正确答案</p>
              <p class="font-body-sm text-body-sm text-green-600 font-medium">{{ q.correctAnswer }}</p>
            </div>
            <div class="flex-[2]">
              <p class="font-label-xs text-label-xs text-on-surface-variant mb-1">AI 解析</p>
              <p class="font-body-sm text-body-sm text-on-surface" v-if="q.analysis">{{ q.analysis }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant italic" v-else>暂无解析</p>
            </div>
          </div>

          <!-- 驳回意见 -->
          <div v-if="q.status === 'REJECTED' && q.reviewComment" class="mt-3 pt-3 border-t border-outline-variant">
            <p class="font-label-xs text-label-xs text-error mb-1">驳回意见</p>
            <p class="font-body-sm text-body-sm text-error">{{ q.reviewComment }}</p>
          </div>
        </div>

        <!-- 操作按钮（仅待审核状态显示） -->
        <div v-if="q.status === 'PENDING'" class="px-4 py-3 bg-surface-container flex items-center justify-end gap-2 border-t border-outline-variant">
          <button
            class="px-4 py-2 rounded-full border border-outline-variant text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant transition-colors"
            @click="openRejectDialog(q)"
          >
            驳回
          </button>
          <button
            class="px-4 py-2 rounded-full bg-green-600 text-white font-label-sm text-label-sm hover:bg-green-700 transition-colors flex items-center gap-1"
            @click="handleApprove(q.id)"
          >
            <span class="material-symbols-outlined text-sm">check</span>
            通过
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== AI 出题弹窗 ==================== -->
    <div v-if="showGenerateDialog" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50" @click.self="showGenerateDialog = false">
      <div class="bg-surface-container-lowest rounded-[24px] p-6 w-full max-w-md mx-4 shadow-2xl">
        <h3 class="font-headline-sm text-headline-sm text-on-surface mb-4">AI 批量出题</h3>

        <!-- 知识点 -->
        <div class="mb-4">
          <label class="block font-label-sm text-label-sm text-on-surface-variant mb-1.5">知识点名称</label>
          <input
            v-model="generateForm.knowledgePoint"
            class="w-full px-4 py-2.5 bg-background border border-outline-variant rounded-xl text-on-surface font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
            placeholder="例：虚拟语气、名词性从句"
            required
          />
        </div>

        <!-- 等级 -->
        <div class="mb-4">
          <label class="block font-label-sm text-label-sm text-on-surface-variant mb-1.5">目标等级</label>
          <div class="flex gap-2">
            <button
              v-for="level in ['CET4', 'CET6']"
              :key="level"
              :class="[
                'flex-1 px-4 py-2 rounded-full font-label-sm text-label-sm transition-colors',
                generateForm.cetLevel === level
                  ? 'bg-primary text-on-primary'
                  : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-low'
              ]"
              @click="generateForm.cetLevel = level"
            >
              {{ level }}
            </button>
          </div>
        </div>

        <!-- 题型 -->
        <div class="mb-4">
          <label class="block font-label-sm text-label-sm text-on-surface-variant mb-1.5">题型</label>
          <div class="flex gap-2">
            <button
              v-for="qt in questionTypes"
              :key="qt.value"
              :class="[
                'flex-1 px-4 py-2 rounded-full font-label-sm text-label-sm transition-colors',
                generateForm.questionType === qt.value
                  ? 'bg-primary text-on-primary'
                  : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-low'
              ]"
              @click="generateForm.questionType = qt.value"
            >
              {{ qt.label }}
            </button>
          </div>
        </div>

        <!-- 难度 -->
        <div class="mb-4">
          <label class="block font-label-sm text-label-sm text-on-surface-variant mb-1.5">
            难度等级：{{ generateForm.difficulty }}
          </label>
          <input
            type="range"
            min="1"
            max="8"
            v-model.number="generateForm.difficulty"
            class="w-full accent-primary"
          />
          <div class="flex justify-between text-label-xs text-on-surface-variant mt-1">
            <span>简单</span>
            <span>困难</span>
          </div>
        </div>

        <!-- 数量 -->
        <div class="mb-5">
          <label class="block font-label-sm text-label-sm text-on-surface-variant mb-1.5">
            生成数量：{{ generateForm.count }} 题
          </label>
          <input
            type="range"
            min="1"
            max="20"
            v-model.number="generateForm.count"
            class="w-full accent-primary"
          />
        </div>

        <!-- 弹窗按钮 -->
        <div class="flex justify-end gap-3">
          <button
            class="px-5 py-2.5 rounded-full border border-outline-variant text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant transition-colors"
            @click="showGenerateDialog = false"
          >
            取消
          </button>
          <button
            class="px-5 py-2.5 rounded-full bg-primary text-on-primary font-label-sm text-label-sm hover:bg-primary/90 transition-colors flex items-center gap-2"
            :disabled="generating"
            @click="handleGenerate"
          >
            <span class="material-symbols-outlined text-sm">{{ generating ? 'progress_activity animate-spin' : 'auto_awesome' }}</span>
            {{ generating ? '生成中...' : '开始生成' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== 驳回弹窗 ==================== -->
    <div v-if="showRejectDialog" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50" @click.self="showRejectDialog = false">
      <div class="bg-surface-container-lowest rounded-[24px] p-6 w-full max-w-md mx-4 shadow-2xl">
        <h3 class="font-headline-sm text-headline-sm text-on-surface mb-4">驳回题目</h3>

        <div class="mb-4">
          <label class="block font-label-sm text-label-sm text-on-surface-variant mb-1.5">驳回原因</label>
          <textarea
            v-model="rejectComment"
            class="w-full px-4 py-2.5 bg-background border border-outline-variant rounded-xl text-on-surface font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all resize-none"
            rows="3"
            placeholder="请输入驳回原因..."
          ></textarea>
        </div>

        <div class="flex justify-end gap-3">
          <button
            class="px-5 py-2.5 rounded-full border border-outline-variant text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant transition-colors"
            @click="showRejectDialog = false"
          >
            取消
          </button>
          <button
            class="px-5 py-2.5 rounded-full bg-error text-white font-label-sm text-label-sm hover:bg-error/90 transition-colors"
            @click="handleReject"
          >
            确认驳回
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPendingQuestions, approvePendingQuestion, rejectPendingQuestion, generateQuestions, type PendingQuestion } from '@/api/exercise'
import AppLayout from '@/components/AppLayout.vue'

// ==================== 状态管理 ====================

/** 题目列表 */
const questions = ref<PendingQuestion[]>([])
/** 加载状态 */
const loading = ref(false)
/** 当前筛选状态 */
const currentStatus = ref('PENDING')

/** 状态筛选标签 */
const statusTabs = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]

/** 题型选项 */
const questionTypes = [
  { label: '词汇填空', value: 'FILL_BLANK' },
  { label: '连词成句', value: 'SENTENCE_MAKING' },
]

// ==================== AI 出题弹窗 ====================

/** 是否显示出题弹窗 */
const showGenerateDialog = ref(false)
/** 是否正在生成 */
const generating = ref(false)
/** 出题表单 */
const generateForm = reactive({
  knowledgePoint: '',
  cetLevel: 'CET4',
  difficulty: 3,
  questionType: 'FILL_BLANK',
  count: 5,
})

/** 触发 AI 出题 */
async function handleGenerate() {
  if (!generateForm.knowledgePoint.trim()) {
    alert('请输入知识点名称')
    return
  }
  generating.value = true
  try {
    const count: number = await generateQuestions({
      knowledgePoint: generateForm.knowledgePoint,
      cetLevel: generateForm.cetLevel,
      difficulty: generateForm.difficulty,
      questionType: generateForm.questionType,
      count: generateForm.count,
    })
    alert(`生成完成！成功保存 ${count} 道题目到待审核列表`)
    showGenerateDialog.value = false
    fetchQuestions()
  } catch (e: any) {
    alert('生成失败：' + (e.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

// ==================== 驳回弹窗 ====================

/** 是否显示驳回弹窗 */
const showRejectDialog = ref(false)
/** 驳回原因 */
const rejectComment = ref('')
/** 待驳回的题目ID */
const rejectTargetId = ref<number | null>(null)

/** 打开驳回弹窗 */
function openRejectDialog(q: PendingQuestion) {
  rejectTargetId.value = q.id
  rejectComment.value = ''
  showRejectDialog.value = true
}

/** 确认驳回 */
async function handleReject() {
  if (rejectTargetId.value === null) return
  try {
    await rejectPendingQuestion(rejectTargetId.value, rejectComment.value)
    alert('已驳回')
    showRejectDialog.value = false
    fetchQuestions()
  } catch (e: any) {
    alert('驳回失败：' + (e.message || '未知错误'))
  }
}

// ==================== 审核通过 ====================

/** 审核通过 */
async function handleApprove(id: number) {
  if (!confirm('确认审核通过该题目？通过后将正式入库。')) return
  try {
    await approvePendingQuestion(id)
    alert('审核通过，题目已入库')
    fetchQuestions()
  } catch (e: any) {
    alert('审核失败：' + (e.message || '未知错误'))
  }
}

// ==================== 数据加载 ====================

/** 查询题目列表 */
async function fetchQuestions() {
  loading.value = true
  try {
    questions.value = await getPendingQuestions(currentStatus.value)
  } catch (e: any) {
    alert('加载失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// ==================== 工具函数 ====================

/** 解析 options JSON 字符串为数组 */
function parsedOptions(optionsStr: string): string[] {
  if (!optionsStr) return []
  try {
    return JSON.parse(optionsStr)
  } catch {
    return []
  }
}

/** 状态标签文本 */
function statusLabel(status: string): string {
  switch (status) {
    case 'PENDING': return '待审核'
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已驳回'
    default: return status
  }
}

/** 状态标签样式 */
function statusClass(status: string): string {
  switch (status) {
    case 'PENDING': return 'bg-yellow-100 text-yellow-700'
    case 'APPROVED': return 'bg-green-100 text-green-700'
    case 'REJECTED': return 'bg-red-100 text-red-700'
    default: return 'bg-gray-100 text-gray-700'
  }
}

// ==================== 初始化 ====================

onMounted(() => {
  fetchQuestions()
})
</script>