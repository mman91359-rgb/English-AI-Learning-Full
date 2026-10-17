<template>
  <div class="stats-page">
    <h2 class="page-title">学习统计</h2>
    <p class="page-sub">追踪你的学习成长</p>

    <div class="stat-elo-card">
      <ProgressRing :score="eloScore" :min="100" :max="2500" label="当前ELO" :level="userStore.eloLevel" />
      <p class="elo-note">ELO 分越高，推送题目难度越大</p>
    </div>

    <RadarChart v-if="radarData" title="能力维度" :data="radarData" />
    <div v-else class="skeleton-card"></div>

    <div class="level-info">
      <h3>等级详情</h3>
      <div class="level-row">
        <span>当前等级</span>
        <strong>{{ userStore.eloLevel || '未定级' }}</strong>
      </div>
      <div class="level-row">
        <span>ELO分</span>
        <strong>{{ eloScore }}</strong>
      </div>
      <div class="level-row">
        <span>目标考试</span>
        <strong>{{ userStore.cetLevel || 'CET4' }}</strong>
      </div>
      <div class="elo-bar-wrap">
        <div class="elo-bar">
          <div class="elo-bar-fill" :style="{ width: eloProgress + '%' }"></div>
        </div>
        <div class="elo-labels">
          <span>100</span>
          <span>2500</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../../store/userStore'
import { getRadarData } from '../../api/userApi'
import ProgressRing from '../../components/ProgressRing.vue'
import RadarChart from '../../components/RadarChart.vue'

const userStore = useUserStore()
const radarData = ref(null)
const eloScore = ref(userStore.eloScore || 1000)

const eloProgress = computed(() => Math.min(100, Math.round(((eloScore.value - 100) / 2400) * 100)))

onMounted(async () => {
  try {
    const res = await getRadarData(userStore.userId)
    if (res.code === 200) {
      radarData.value = res.data
      eloScore.value = res.data.eloScore || eloScore.value
    }
  } catch { /* use defaults */ }
})
</script>

<style scoped>
.stats-page { padding: 8px 0 20px; }
.page-title { font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: var(--text-sm); color: var(--text-secondary); margin: 4px 0 20px; }

.stat-elo-card {
  text-align: center; padding: 24px; margin-bottom: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
}
.elo-note { font-size: var(--text-xs); color: var(--text-tertiary); margin-top: 8px; }

.skeleton-card {
  height: 300px;
  background: linear-gradient(90deg, var(--bg-subtle) 25%, var(--bg-page) 50%, var(--bg-subtle) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.level-info {
  padding: 20px; margin-top: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
}
.level-info h3 { font-size: var(--text-base); font-weight: 600; margin-bottom: 12px; color: var(--text-primary); }
.level-row {
  display: flex; justify-content: space-between; padding: 8px 0;
  font-size: var(--text-sm); border-bottom: 1px solid var(--border-light);
}
.level-row strong { font-weight: 600; color: var(--color-primary); }

.elo-bar-wrap { margin-top: 16px; }
.elo-bar { height: 6px; background: var(--bg-subtle); border-radius: 3px; overflow: hidden; margin-bottom: 4px; }
.elo-bar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 3px;
  transition: width 0.8s ease;
}
.elo-labels { display: flex; justify-content: space-between; font-size: 10px; color: var(--text-tertiary); }

@media (min-width: 768px) {
  .stats-page { padding: 20px 0 40px; max-width: 800px; }
  .page-title { font-size: var(--text-2xl); }
  .page-sub { font-size: var(--text-base); margin-bottom: 28px; }
  .stat-elo-card { padding: 32px; }
}
</style>
