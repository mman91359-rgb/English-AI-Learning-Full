<template>
  <div class="assess-question">
    <div class="progress-bar-wrap">
      <div class="progress-bar"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div>
      <span class="progress-text">{{ exerciseStore.currentIndex + 1 }} / {{ exerciseStore.totalCount }}</span>
    </div>

    <QuestionCard
      v-if="exerciseStore.currentQuestion"
      :key="exerciseStore.currentQuestion.id"
      ref="questionCardRef"
      :question="exerciseStore.currentQuestion"
      mode="assess"
      :show-next="true"
      :is-last-question="isLast"
      @answer="handleAnswer"
      @next="handleNextOrFinish"
    />

    <div v-if="allDone" class="done-notice">
      <span>🎉</span>
      <p>全部答完！</p>
      <button class="btn btn-primary btn-block" @click="finishAssess">查看定级结果</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useExerciseStore } from '../../store/exerciseStore'
import { useUserStore } from '../../store/userStore'
import { submitAnswer } from '../../api/exerciseApi'
import { submitAssess } from '../../api/userApi'
import QuestionCard from '../../components/QuestionCard.vue'

const router = useRouter()
const exerciseStore = useExerciseStore()
const userStore = useUserStore()
const questionCardRef = ref(null)
const allDone = ref(false)

const progress = computed(() => {
  if (!exerciseStore.totalCount) return 0
  return Math.round((exerciseStore.currentIndex / exerciseStore.totalCount) * 100)
})
const isLast = computed(() => exerciseStore.currentIndex >= exerciseStore.totalCount - 1)

async function handleAnswer({ userAnswer, questionId, moduleType }) {
  try {
    const res = await submitAnswer({ userId: userStore.userId, questionId, userAnswer, moduleType, durationSeconds: 0 })
    if (res.code === 200 && res.data) {
      exerciseStore.submitAnswer(questionId, res.data)
      questionCardRef.value?.showResult(res.data, res.data.isCorrect)
      submitAssess({
        userId: userStore.userId,
        difficultyLevel: exerciseStore.currentQuestion?.difficultyLevel || 1,
        moduleType,
        isCorrect: res.data.isCorrect
      }).catch(() => {})
    } else {
      questionCardRef.value?.showError()
    }
  } catch {
    questionCardRef.value?.showError()
  }
}

function handleNextOrFinish() {
  if (isLast.value) {
    allDone.value = true
  } else {
    exerciseStore.nextQuestion()
  }
}

function finishAssess() {
  exerciseStore.reset()
  router.push('/assess/result')
}
</script>

<style scoped>
.assess-question { padding: 16px 0; }
.progress-bar-wrap { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.progress-bar { flex: 1; height: 6px; background: var(--bg-subtle); border-radius: 3px; overflow: hidden; }
.progress-fill {
  height: 100%; background: var(--color-primary);
  border-radius: 3px; transition: width 0.3s ease;
}
.progress-text { font-size: var(--text-sm); font-weight: 600; color: var(--text-secondary); white-space: nowrap; }
.done-notice {
  text-align: center; padding: 40px 20px; margin-top: 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
}
.done-notice span { font-size: 48px; display: block; margin-bottom: 12px; }
.done-notice p { font-size: var(--text-lg); font-weight: 600; margin-bottom: 20px; color: var(--text-primary); }
</style>
