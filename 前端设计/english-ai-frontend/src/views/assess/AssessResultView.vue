<template>
  <div class="assess-result">
    <div class="result-header">
      <span class="result-emoji">{{ emoji }}</span>
      <h1>{{ levelTitle }}</h1>
      <p>你的英语水平为 <strong>{{ userStore.eloLevel }}</strong></p>
    </div>

    <div class="result-stats">
      <ProgressRing :score="eloScore" :min="100" :max="2500" label="ELO分" :level="userStore.eloLevel" />
    </div>

    <RadarChart v-if="radarData" title="能力雷达图" :data="radarData" />

    <div class="result-actions">
      <button class="btn btn-primary btn-block btn-lg" @click="$router.push('/dashboard')">进入仪表盘</button>
      <button class="btn btn-outline btn-block" @click="$router.push('/assess/start')" style="margin-top:12px">重新定级</button>
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

const emoji = computed(() => eloScore.value >= 1200 ? '🏆' : eloScore.value >= 1000 ? '🎯' : '🌱')
const levelTitle = computed(() => eloScore.value >= 1200 ? '继续加油！' : eloScore.value >= 1000 ? '还不错！' : '学习之路刚开始！')

onMounted(async () => {
  try {
    const res = await getRadarData(userStore.userId)
    if (res.code === 200) {
      radarData.value = res.data
      eloScore.value = res.data.eloScore || eloScore.value
      userStore.setRadarData(res.data)
    }
  } catch { /* use defaults */ }
})
</script>

<style scoped>
.assess-result { padding: 20px 0; }
.result-header {
  text-align: center;
  padding: 32px 24px;
  margin-bottom: 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--elevation-1);
}
.result-emoji { font-size: 64px; display: block; margin-bottom: 12px; }
.result-header h1 { font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; margin-bottom: 4px; color: var(--text-primary); }
.result-header p { font-size: var(--text-base); color: var(--text-secondary); }
.result-header p strong { color: var(--color-primary); }

.result-stats { margin-bottom: 20px; }
.result-actions { margin-top: 24px; }

@media (min-width: 768px) {
  .assess-result { padding: 20px 0 40px; max-width: 700px; }
  .result-header { padding: 40px 32px; }
  .result-emoji { font-size: 80px; }
  .result-header h1 { font-size: var(--text-2xl); }
}
</style>
