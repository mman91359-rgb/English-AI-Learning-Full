<template>
  <div class="question-card" :class="stateClass">
    <div class="q-header">
      <span class="q-type-badge">{{ typeLabel }}</span>
      <span class="q-difficulty">{{ '⭐'.repeat(question?.difficultyLevel || 1) }}</span>
    </div>

    <div class="q-stem">{{ question?.stem }}</div>

    <div v-if="question?.questionType === 'CHOICE'" class="q-options">
      <button
        v-for="opt in parsedOptions"
        :key="opt.key"
        class="option-btn"
        :class="optionClass(opt.key)"
        :disabled="answered"
        @click="selectOption(opt.key)"
      >
        <span class="opt-key">{{ opt.key }}</span>
        <span class="opt-text">{{ opt.text }}</span>
        <span v-if="answered && opt.key === correctAnswerKey" class="opt-icon correct-icon">✓</span>
        <span v-else-if="answered && opt.key === selectedOption && !isCorrect" class="opt-icon wrong-icon">✗</span>
      </button>
    </div>

    <div v-else-if="question?.questionType === 'FILL'" class="q-fill">
      <input v-model="fillAnswer" class="fill-input" placeholder="请输入答案..." :disabled="answered" @keyup.enter="submitFill" />
      <button v-if="!answered" class="btn btn-primary" @click="submitFill" :disabled="!fillAnswer.trim()">提交</button>
    </div>

    <div v-if="submitting" class="q-loading">
      <span class="loading-dot"></span><span class="loading-dot"></span><span class="loading-dot"></span>
    </div>

    <transition name="reveal">
      <div v-if="answered" class="q-result">
        <div class="result-header" :class="isCorrect ? 'correct' : 'wrong'">
          <span class="result-icon">{{ isCorrect ? '🎉' : '😞' }}</span>
          <span>{{ isCorrect ? '回答正确！' : '回答错误' }}</span>
        </div>
        <div class="result-answer" v-if="!isCorrect">
          正确答案: <strong>{{ correctAnswer }}</strong>
        </div>
        <div class="result-analysis" v-if="analysis" v-html="renderedAnalysis"></div>
        <button class="btn btn-primary btn-block" style="margin-top:12px" @click="$emit('next')" v-if="showNext">
          {{ isLastQuestion ? '完成' : '下一题' }}
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  question: { type: Object, default: null },
  mode: { type: String, default: 'practice' },
  showNext: { type: Boolean, default: true },
  isLastQuestion: { type: Boolean, default: false }
})

const emit = defineEmits(['answer', 'next'])

const selectedOption = ref('')
const fillAnswer = ref('')
const answered = ref(false)
const submitting = ref(false)
const isCorrect = ref(false)
const correctAnswer = ref('')
const correctAnswerKey = ref('')
const analysis = ref('')

const stateClass = computed(() => ({
  'state-correct': answered.value && isCorrect.value,
  'state-wrong': answered.value && !isCorrect.value
}))

const typeLabel = computed(() => ({
  CHOICE: '选择题', FILL: '填空题'
}[props.question?.questionType] || '客观题'))

const parsedOptions = computed(() => {
  if (!props.question?.options) return []
  try {
    const arr = typeof props.question.options === 'string' ? JSON.parse(props.question.options) : props.question.options
    return arr.map(o => {
      const match = o.match(/^([A-D])\.?\s*(.+)/)
      return match ? { key: match[1], text: match[2] } : { key: o[0] || '?', text: o }
    })
  } catch { return [] }
})

const renderedAnalysis = computed(() => analysis.value ? analysis.value.replace(/\n/g, '<br>') : '')

function optionClass(key) {
  if (!answered.value) return selectedOption.value === key ? 'selected' : ''
  if (key === correctAnswerKey.value) return 'correct'
  if (key === selectedOption.value && !isCorrect.value) return 'wrong'
  return ''
}

function selectOption(opt) {
  if (answered.value || submitting.value) return
  selectedOption.value = opt
  submitting.value = true
  emit('answer', { userAnswer: opt, questionId: props.question.id, moduleType: props.question.moduleType })
}

function submitFill() {
  if (answered.value || submitting.value || !fillAnswer.value.trim()) return
  submitting.value = true
  emit('answer', { userAnswer: fillAnswer.value.trim(), questionId: props.question.id, moduleType: props.question.moduleType })
}

function showResult(result, isRight) {
  isCorrect.value = isRight
  correctAnswer.value = result.correctAnswer
  correctAnswerKey.value = result.correctAnswerKey || (result.correctAnswer || '').match(/^[A-D]/)?.[0] || ''
  analysis.value = result.analysis
  answered.value = true
  submitting.value = false
}

function showError() {
  isCorrect.value = false
  correctAnswer.value = '加载失败'
  correctAnswerKey.value = ''
  analysis.value = '网络错误，请重试'
  answered.value = true
  submitting.value = false
}

watch(() => props.question?.id, () => {
  selectedOption.value = ''
  fillAnswer.value = ''
  answered.value = false
  submitting.value = false
  isCorrect.value = false
  correctAnswer.value = ''
  correctAnswerKey.value = ''
  analysis.value = ''
})

defineExpose({ showResult, showError })
</script>

<style scoped>
.question-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-normal) var(--ease-out);
}
.state-correct { border-color: var(--color-success); box-shadow: 0 0 0 3px var(--color-success-bg); }
.state-wrong   { border-color: var(--color-error);   box-shadow: 0 0 0 3px var(--color-error-bg); }

.q-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.q-type-badge {
  padding: 4px 12px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
}
.q-difficulty { font-size: var(--text-xs); color: var(--text-tertiary); }

.q-stem {
  font-size: var(--text-lg); font-weight: 500; line-height: 1.7;
  margin-bottom: 24px; color: var(--text-primary);
}

.q-options { display: flex; flex-direction: column; gap: 10px; }
.option-btn {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px;
  background: var(--bg-subtle); border: 1.5px solid transparent;
  border-radius: var(--radius-md); font-size: var(--text-base); color: var(--text-primary);
  transition: all var(--duration-fast) var(--ease-out); text-align: left; cursor: pointer;
}
.option-btn:hover:not(:disabled) {
  background: var(--bg-hover); border-color: var(--color-primary);
}
.option-btn.selected {
  background: var(--color-primary-bg); border-color: var(--color-primary);
}
.option-btn.correct {
  background: var(--color-success-bg); border-color: var(--color-success);
}
.option-btn.wrong {
  background: var(--color-error-bg); border-color: var(--color-error);
}
.option-btn:disabled { cursor: default; }
.opt-key {
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  background: var(--bg-surface); border-radius: 50%;
  font-weight: 700; font-size: var(--text-sm); flex-shrink: 0;
  border: 1px solid var(--border-medium);
}
.opt-text { flex: 1; }
.opt-icon { font-weight: 700; font-size: 16px; }
.correct-icon { color: var(--color-success); }
.wrong-icon { color: var(--color-error); }

.q-fill { display: flex; gap: 10px; margin-bottom: 16px; }
.fill-input {
  flex: 1; padding: 12px 16px; border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md); background: var(--bg-subtle);
  color: var(--text-primary); outline: none; transition: border-color var(--duration-fast);
}
.fill-input:focus { border-color: var(--color-primary); }

.q-result { margin-top: 20px; }
.result-header {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  border-radius: var(--radius-md); font-weight: 600; margin-bottom: 12px;
}
.result-header.correct { background: var(--color-success-bg); color: var(--color-success); }
.result-header.wrong   { background: var(--color-error-bg);   color: var(--color-error); }
.result-icon { font-size: 20px; }

.result-answer {
  padding: 10px 14px; background: var(--color-warning-bg);
  border-radius: var(--radius-sm); margin-bottom: 8px;
  font-size: var(--text-sm); color: var(--text-primary);
}
.result-answer strong { color: var(--color-primary); }
.result-analysis { font-size: var(--text-sm); color: var(--text-secondary); line-height: 1.6; }

.q-loading { display: flex; justify-content: center; gap: 6px; padding: 24px; }
.loading-dot {
  width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary);
  animation: bounce 1.4s infinite ease-in-out both;
}
.loading-dot:nth-child(1) { animation-delay: -0.32s; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.reveal-enter-active { transition: all 0.3s var(--ease-out); }
.reveal-enter-from { opacity: 0; transform: translateY(10px); }
</style>
