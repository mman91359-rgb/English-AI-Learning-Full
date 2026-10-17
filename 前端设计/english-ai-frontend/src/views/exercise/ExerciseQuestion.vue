<template>
  <div class="exercise-question">
    <div class="exercise-top">
      <button class="back-btn" @click="$router.back()">← 退出</button>
      <span class="exercise-module">{{ moduleName }}</span>
      <span class="exercise-progress">{{ exerciseStore.currentIndex + 1 }}/{{ exerciseStore.totalCount }}</span>
    </div>

    <div class="mini-progress">
      <div class="mini-fill" :style="{ width: progress + '%' }"></div>
    </div>

    <QuestionCard
      v-if="exerciseStore.currentQuestion"
      :key="exerciseStore.currentQuestion.id"
      ref="cardRef"
      :question="exerciseStore.currentQuestion"
      mode="practice"
      :show-next="true"
      :is-last-question="isLast"
      @answer="handleAnswer"
      @next="handleNext"
    />

    <div v-if="finished" class="finish-overlay">
      <div class="finish-card">
        <span class="finish-emoji">🎉</span>
        <h3>练习完成！</h3>
        <div class="finish-stats">
          <div class="stat-item"><strong>{{ correctCount }}</strong><small>正确</small></div>
          <div class="stat-item"><strong>{{ wrongCount }}</strong><small>错误</small></div>
          <div class="stat-item"><strong>{{ accuracy }}%</strong><small>正确率</small></div>
        </div>
        <button class="btn btn-primary btn-block" @click="goBack">返回</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useExerciseStore } from '../../store/exerciseStore'
import { useUserStore } from '../../store/userStore'
import { submitAnswer } from '../../api/exerciseApi'
import QuestionCard from '../../components/QuestionCard.vue'

const router = useRouter()
const exerciseStore = useExerciseStore()
const userStore = useUserStore()
const cardRef = ref(null)
const finished = ref(false)

const moduleName = computed(() => exerciseStore.moduleType === 1 ? '词汇练习' : '语法练习')
const isLast = computed(() => exerciseStore.currentIndex >= exerciseStore.totalCount - 1)
const progress = computed(() => exerciseStore.totalCount ? Math.round((exerciseStore.currentIndex / exerciseStore.totalCount) * 100) : 0)

const correctCount = computed(() => Object.values(exerciseStore.answers).filter(a => a.isCorrect).length)
const wrongCount = computed(() => Object.values(exerciseStore.answers).filter(a => a && !a.isCorrect).length)
const accuracy = computed(() => {
  const t = correctCount.value + wrongCount.value
  return t ? Math.round((correctCount.value / t) * 100) : 0
})

async function handleAnswer({ userAnswer, questionId, moduleType }) {
  try {
    const res = await submitAnswer({ userId: userStore.userId, questionId, userAnswer, moduleType, durationSeconds: 0 })
    if (res.code === 200 && res.data) {
      exerciseStore.submitAnswer(questionId, res.data)
      cardRef.value?.showResult(res.data, res.data.isCorrect)
    } else {
      cardRef.value?.showError()
    }
  } catch {
    cardRef.value?.showError()
  }
}

function handleNext() {
  if (isLast.value) {
    finished.value = true
  } else {
    exerciseStore.nextQuestion()
  }
}

function goBack() {
  exerciseStore.reset()
  router.push('/exercise')
}
</script>

<style scoped>
.exercise-question { padding: 8px 0 20px; }
.exercise-top { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.back-btn { font-size: var(--text-sm); color: var(--text-secondary); background: none; border: none; cursor: pointer; padding: 4px 0; }
.exercise-module { flex: 1; font-weight: 600; font-size: var(--text-base); color: var(--text-primary); }
.exercise-progress { font-size: var(--text-sm); color: var(--text-tertiary); }

.mini-progress { height: 4px; background: var(--bg-subtle); border-radius: 2px; margin-bottom: 16px; overflow: hidden; }
.mini-fill {
  height: 100%; background: var(--color-primary);
  border-radius: 2px; transition: width 0.3s ease;
}

.finish-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 200; padding: 20px; }
.finish-card {
  width: 100%; max-width: 320px; text-align: center; padding: 32px 24px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--elevation-4);
}
.finish-emoji { font-size: 56px; display: block; margin-bottom: 8px; }
.finish-card h3 { font-size: var(--text-lg); font-weight: 700; margin-bottom: 16px; color: var(--text-primary); }
.finish-stats { display: flex; justify-content: center; gap: 24px; margin-bottom: 20px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-item strong {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  color: var(--color-primary);
}
.stat-item small { font-size: var(--text-xs); color: var(--text-tertiary); }

@media (min-width: 768px) {
  .exercise-question { max-width: 800px; padding: 20px 0 40px; }
  .finish-card { max-width: 400px; }
}
</style>
