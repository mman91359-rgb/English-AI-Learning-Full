<template>
  <div class="home-page">
    <div class="welcome-card">
      <div class="welcome-left">
        <h2>Hi, {{ userStore.username }} 👋</h2>
        <p class="welcome-sub">{{ greetingText }}</p>
        <span class="target-badge">{{ userStore.cetLevel || 'CET4' }}</span>
      </div>
      <ProgressRing :score="eloScore" :min="100" :max="2500" label="ELO分" :level="userStore.eloLevel" />
    </div>

    <div class="task-row">
      <div class="task-card task-review" @click="goReview">
        <span class="task-icon">📋</span>
        <span class="task-num">{{ reviewCount }}</span>
        <span class="task-label">今日待复习</span>
      </div>
      <div class="task-card task-assess" @click="goAssess">
        <span class="task-icon">🎯</span>
        <span class="task-num">{{ eloScore }}</span>
        <span class="task-label">{{ userStore.eloLevel }}</span>
      </div>
    </div>

    <h3 class="section-title">选择练习模块</h3>
    <div class="module-grid">
      <div class="module-card" @click="startExercise(1)">
        <div class="module-icon" style="background:var(--color-primary-bg)">📖</div>
        <div class="module-info">
          <span class="module-name">词汇练习</span>
          <span class="module-desc">单词辨析与用法</span>
        </div>
        <span class="module-arrow">→</span>
      </div>
      <div class="module-card" @click="startExercise(2)">
        <div class="module-icon" style="background:var(--color-accent-bg)">📝</div>
        <div class="module-info">
          <span class="module-name">语法练习</span>
          <span class="module-desc">句型与语法规则</span>
        </div>
        <span class="module-arrow">→</span>
      </div>
    </div>

    <h3 class="section-title">快捷入口</h3>
    <div class="quick-grid">
      <div class="quick-card" @click="$router.push('/exercise/error-book')">
        <span>📕</span><span>错题本</span>
      </div>
      <div class="quick-card" @click="$router.push('/stats')">
        <span>📊</span><span>学习统计</span>
      </div>
      <div class="quick-card" @click="$router.push('/assess/start')">
        <span>🔄</span><span>重新定级</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/userStore'
import { useExerciseStore } from '../../store/exerciseStore'
import { getReviewTasks } from '../../api/statsApi'
import { getQuestions } from '../../api/exerciseApi'
import ProgressRing from '../../components/ProgressRing.vue'

const router = useRouter()
const userStore = useUserStore()
const exerciseStore = useExerciseStore()
const reviewCount = ref(0)
const eloScore = computed(() => userStore.eloScore || 1000)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早上好！今天也要加油哦~'
  if (h < 18) return '下午好！别忘了每日练习~'
  return '晚上好！复习一下今天的内容吧~'
})

onMounted(async () => {
  try {
    const res = await getReviewTasks(userStore.userId)
    reviewCount.value = res.data?.length || res.data?.count || res.data || 0
  } catch { reviewCount.value = 0 }
})

function goReview() { startExercise(1) }
function goAssess() { router.push('/assess/start') }

async function startExercise(moduleType) {
  try {
    const res = await getQuestions({ userId: userStore.userId, moduleType, count: 10 })
    if (res.code === 200 && res.data?.length) {
      exerciseStore.startExercise(res.data, moduleType, 'practice')
      router.push('/exercise/question')
    }
  } catch {
    router.push('/assess/start')
  }
}
</script>

<style scoped>
.home-page { padding-bottom: 20px; }

.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  margin-bottom: 16px;
}
.welcome-left h2 {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--text-primary);
}
.welcome-sub { font-size: var(--text-sm); color: var(--text-secondary); margin: 4px 0 8px; }
.target-badge {
  display: inline-block;
  padding: 4px 12px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
}

.task-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
.task-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 20px 16px;
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.task-card:hover { transform: translateY(-2px); box-shadow: var(--elevation-2); }
.task-card:active { transform: scale(0.97); }
.task-icon { font-size: 28px; }
.task-num { font-family: var(--font-display); font-size: var(--text-2xl); font-weight: 700; color: var(--text-primary); }
.task-label { font-size: var(--text-xs); color: var(--text-tertiary); }
.task-review { border-left: 3px solid var(--color-error); }
.task-assess { border-left: 3px solid var(--color-accent); }

.section-title {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.module-grid { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.module-card {
  display: flex; align-items: center; gap: 14px; padding: 16px;
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.module-card:hover { transform: translateX(4px); box-shadow: var(--elevation-2); }
.module-card:active { transform: scale(0.98); }
.module-icon {
  width: 48px; height: 48px; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md); font-size: 24px; flex-shrink: 0;
}
.module-info { flex: 1; }
.module-name { font-weight: 600; display: block; color: var(--text-primary); }
.module-desc { font-size: var(--text-xs); color: var(--text-tertiary); }
.module-arrow { color: var(--text-tertiary); font-size: 18px; }

.quick-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.quick-card {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 16px 10px; font-size: var(--text-sm); font-weight: 500;
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.quick-card:hover { transform: translateY(-2px); box-shadow: var(--elevation-2); }
.quick-card:active { transform: scale(0.97); }
.quick-card span:first-child { font-size: 24px; }

@media (min-width: 768px) {
  .home-page { max-width: 960px; padding-bottom: 40px; }
  .welcome-card { padding: 32px 28px; margin-bottom: 24px; }
  .welcome-left h2 { font-size: var(--text-2xl); }
  .task-row { gap: 16px; margin-bottom: 32px; }
  .task-card { padding: 28px 24px; }
  .task-icon { font-size: 36px; }
  .task-num { font-size: var(--text-3xl); }
  .section-title { font-size: var(--text-lg); margin-bottom: 16px; }
  .module-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 32px; }
  .module-card { padding: 20px; }
  .quick-grid { gap: 14px; }
  .quick-card { padding: 24px 16px; font-size: var(--text-base); }
  .quick-card span:first-child { font-size: 32px; }
}

@media (min-width: 1024px) {
  .home-page {
    display: grid;
    grid-template-columns: 1fr 280px;
    grid-template-rows: auto auto;
    gap: 20px;
    max-width: 1100px;
    align-items: start;
  }
  .welcome-card { grid-column: 1 / -1; }
  .task-row { grid-column: 1; grid-row: 2; }
  .module-grid, .quick-grid { grid-column: 2; }
  .section-title { grid-column: 1 / -1; }
}
</style>
