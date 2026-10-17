<template>
  <div class="exercise-hub">
    <h2 class="page-title">练习中心</h2>
    <p class="page-sub">选择模块开始针对性练习</p>

    <div class="hub-grid">
      <div class="hub-card" v-for="m in modules" :key="m.type" @click="startModule(m.type)">
        <div class="hub-icon" :style="{ background: m.bg }">{{ m.icon }}</div>
        <div class="hub-info">
          <span class="hub-name">{{ m.name }}</span>
          <span class="hub-desc">{{ m.desc }}</span>
        </div>
        <span class="hub-arrow">→</span>
      </div>
    </div>

    <div class="hub-card" @click="$router.push('/exercise/error-book')">
      <div class="hub-icon" style="background:var(--color-error-bg)">📕</div>
      <div class="hub-info">
        <span class="hub-name">错题本</span>
        <span class="hub-desc">查看所有错题记录</span>
      </div>
      <span class="hub-arrow">→</span>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useExerciseStore } from '../../store/exerciseStore'
import { useUserStore } from '../../store/userStore'
import { getQuestions } from '../../api/exerciseApi'

const router = useRouter()
const exerciseStore = useExerciseStore()
const userStore = useUserStore()

const modules = [
  { type: 1, name: '词汇练习', desc: '单词辨析与用法', icon: '📖', bg: 'var(--color-primary-bg)' },
  { type: 2, name: '语法练习', desc: '句型与语法规则', icon: '📝', bg: 'var(--color-accent-bg)' }
]

async function startModule(moduleType) {
  const res = await getQuestions({ userId: userStore.userId, moduleType, count: 10 })
  if (res.code === 200 && res.data?.length) {
    exerciseStore.startExercise(res.data, moduleType, 'practice')
    router.push('/exercise/question')
  } else {
    alert('暂无可练习题，请先完成定级测试')
  }
}
</script>

<style scoped>
.exercise-hub { padding: 8px 0 20px; }
.page-title { font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: var(--text-sm); color: var(--text-secondary); margin: 4px 0 20px; }

.hub-grid { display: flex; flex-direction: column; gap: 10px; margin-bottom: 10px; }
.hub-card {
  display: flex; align-items: center; gap: 14px; padding: 16px;
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.hub-card:hover { transform: translateY(-2px); box-shadow: var(--elevation-2); }
.hub-card:active { transform: scale(0.98); }
.hub-icon {
  width: 48px; height: 48px; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md); font-size: 24px; flex-shrink: 0;
}
.hub-info { flex: 1; }
.hub-name { font-weight: 600; display: block; color: var(--text-primary); }
.hub-desc { font-size: var(--text-xs); color: var(--text-tertiary); }
.hub-arrow { color: var(--text-tertiary); font-size: 18px; }

@media (min-width: 768px) {
  .exercise-hub { padding: 20px 0 40px; max-width: 800px; }
  .page-title { font-size: var(--text-2xl); }
  .page-sub { font-size: var(--text-base); margin-bottom: 28px; }
  .hub-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
  .hub-card { padding: 20px; }
}
</style>
