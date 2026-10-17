<template>
  <div class="assess-start">
    <div class="start-hero">
      <span class="hero-icon">🧠</span>
      <h1>AI 水平定级测试</h1>
      <p>完成 20 道题目，系统将基于 ELO 算法精准评估你的英语水平，为你定制个性化学习路径。</p>
    </div>

    <div class="info-cards">
      <div class="info-card">
        <span>📋</span>
        <strong>20 题</strong>
        <small>词汇 12 题 + 语法 8 题</small>
      </div>
      <div class="info-card">
        <span>⏱️</span>
        <strong>约 10 分钟</strong>
        <small>每题不限时间</small>
      </div>
      <div class="info-card">
        <span>📊</span>
        <strong>实时更新</strong>
        <small>ELO 分实时计算</small>
      </div>
    </div>

    <button class="btn btn-primary btn-block btn-lg" @click="startAssess" :disabled="loading">
      {{ loading ? '加载中...' : '开始定级测试' }}
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useExerciseStore } from '../../store/exerciseStore'
import { getAssessQuestions } from '../../api/exerciseApi'

const router = useRouter()
const exerciseStore = useExerciseStore()
const loading = ref(false)

async function startAssess() {
  loading.value = true
  try {
    const res = await getAssessQuestions()
    if (res.code === 200 && res.data?.length) {
      exerciseStore.startExercise(res.data, 1, 'assess')
      router.push('/assess/question')
    }
  } catch { alert('加载题目失败，请重试') }
  finally { loading.value = false }
}
</script>

<style scoped>
.assess-start { padding: 20px 0; }
.start-hero {
  text-align: center;
  padding: 32px 24px;
  margin-bottom: 24px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--elevation-1);
}
.hero-icon { font-size: 64px; display: block; margin-bottom: 12px; }
.start-hero h1 { font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; margin-bottom: 8px; color: var(--text-primary); }
.start-hero p { font-size: var(--text-sm); color: var(--text-secondary); line-height: 1.6; }

.info-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 24px; }
.info-card {
  padding: 16px 10px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  text-align: center;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
}
.info-card span { font-size: 28px; }
.info-card strong { font-size: var(--text-sm); color: var(--text-primary); }
.info-card small { font-size: 10px; color: var(--text-tertiary); line-height: 1.3; }

@media (min-width: 768px) {
  .assess-start { padding: 20px 0 40px; max-width: 700px; }
  .start-hero { padding: 48px 40px; }
  .hero-icon { font-size: 80px; }
  .start-hero h1 { font-size: var(--text-2xl); }
  .start-hero p { font-size: var(--text-base); max-width: 500px; margin: 0 auto; }
  .info-cards { gap: 16px; }
  .info-card { padding: 24px 16px; }
  .info-card span { font-size: 36px; }
  .info-card strong { font-size: var(--text-base); }
  .info-card small { font-size: var(--text-xs); }
}
</style>
