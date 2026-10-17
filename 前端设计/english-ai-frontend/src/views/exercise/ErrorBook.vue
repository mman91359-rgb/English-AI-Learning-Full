<template>
  <div class="error-book">
    <h2 class="page-title">错题本</h2>
    <p class="page-sub">{{ errorList.length }} 道错题记录</p>

    <div v-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="errorList.length === 0" class="empty-state">
      <span>🎉</span>
      <p>暂无错题记录</p>
      <small>继续保持！</small>
    </div>

    <div v-else class="error-list">
      <div v-for="item in errorList" :key="item.id" class="error-item">
        <div class="error-meta">
          <span class="error-module">{{ item.moduleType === 1 ? '词汇' : '语法' }}</span>
          <span class="error-count">错误 {{ item.wrongCount || 1 }} 次</span>
        </div>
        <div class="error-stem">{{ item.stem }}</div>
        <div class="error-answer" v-if="item.correctAnswer">
          正确答案: <strong>{{ item.correctAnswer }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../store/userStore'
import { getErrorBook } from '../../api/exerciseApi'

const userStore = useUserStore()
const errorList = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getErrorBook(userStore.userId)
    if (res.code === 200) { errorList.value = res.data || [] }
  } catch { /* empty */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.error-book { padding: 8px 0 20px; }
.page-title { font-family: var(--font-display); font-size: var(--text-xl); font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: var(--text-sm); color: var(--text-secondary); margin: 4px 0 16px; }

.loading-state { text-align: center; padding: 40px; color: var(--text-tertiary); }

.empty-state {
  text-align: center; padding: 60px 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}
.empty-state span { font-size: 48px; display: block; margin-bottom: 12px; }
.empty-state p { font-size: var(--text-lg); font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.empty-state small { color: var(--text-tertiary); }

.error-list { display: flex; flex-direction: column; gap: 10px; }
.error-item {
  padding: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--elevation-1);
  transition: all var(--duration-fast) var(--ease-out);
}
.error-item:hover { box-shadow: var(--elevation-2); }
.error-meta { display: flex; justify-content: space-between; margin-bottom: 8px; }
.error-module {
  font-size: var(--text-xs); font-weight: 600; padding: 3px 10px;
  background: var(--color-error-bg); color: var(--color-error); border-radius: var(--radius-full);
}
.error-count { font-size: var(--text-xs); color: var(--text-tertiary); }
.error-stem { font-size: var(--text-base); font-weight: 500; line-height: 1.5; margin-bottom: 8px; color: var(--text-primary); }
.error-answer { font-size: var(--text-sm); color: var(--text-secondary); }
.error-answer strong { color: var(--color-primary); }

@media (min-width: 768px) {
  .error-book { padding: 20px 0 40px; max-width: 800px; }
  .page-title { font-size: var(--text-2xl); }
  .page-sub { font-size: var(--text-base); margin-bottom: 24px; }
  .error-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
  .error-item { padding: 20px; }
}
</style>
