
# Recall 英语四六级学习系统 — 前端代码生成提示词

---

## 一、项目总览

**项目名称**：Recall  
**定位**：面向大学生的英语四六级（CET-4 / CET-6）智能学习 SPA  
**设计风格**：Epop 现代潮流风 → 暗色主导、霓虹渐变点缀、毛玻璃卡片、大圆角、流畅微动效  
**设计关键词**：高级感 · 科技感 · 沉浸式 · 年轻化 · 激励感

---

## 二、技术栈

| 层 | 选型 | 版本 |
|---|------|------|
| 框架 | Vue 3 (Composition API + `<script setup>`) | ^3.4 |
| 构建 | Vite | ^5 |
| UI 组件库 | Element Plus | ^2.6 |
| 状态管理 | Pinia | ^2.1 |
| 可视化 | ECharts (via `vue-echarts` 或原生) | ^5.5 |
| 路由 | Vue Router | ^4 |
| HTTP | Axios | ^1.6 |
| 图标 | `@element-plus/icons-vue` + 自定义 SVG | - |
| CSS 预处理 | SCSS | - |
| 工具库 | dayjs, lodash-es | - |

---

## 三、设计系统（Design Tokens）

### 3.1 色彩体系

```
// 暗色主题（默认）
--bg-primary: #0a0e1a;          // 最深背景
--bg-secondary: #111827;        // 卡片/面板背景
--bg-tertiary: #1a1f2e;         // 悬浮态背景
--bg-glass: rgba(17, 24, 39, 0.65); // 毛玻璃
--border-subtle: rgba(255, 255, 255, 0.06);
--border-active: rgba(99, 102, 241, 0.4);

--text-primary: #f1f5f9;
--text-secondary: #94a3b8;
--text-muted: #475569;

// 品牌渐变（Epop 霓虹）
--brand-gradient: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
--accent-gradient: linear-gradient(135deg, #f59e0b 0%, #ef4444 50%, #ec4899 100%);
--success-gradient: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
--glow-indigo: 0 0 20px rgba(99, 102, 241, 0.5);
--glow-purple: 0 0 20px rgba(139, 92, 246, 0.5);

// 功能色
--color-cet4: #06b6d4;           // CET-4 标识色（青）
--color-cet6: #f59e0b;           // CET-6 标识色（琥珀）
--color-correct: #10b981;
--color-wrong: #ef4444;
```

### 3.2 字体层级

```
--font-display: 'Plus Jakarta Sans', 'PingFang SC', sans-serif;  // 大标题
--font-body: 'Inter', 'PingFang SC', sans-serif;                   // 正文
--font-mono: 'JetBrains Mono', monospace;                          // 英文单词展示

// 字号阶梯
--text-xs: 0.75rem;    --text-sm: 0.875rem;   --text-base: 1rem;
--text-lg: 1.125rem;   --text-xl: 1.25rem;    --text-2xl: 1.5rem;
--text-3xl: 1.875rem;  --text-4xl: 2.25rem;   --text-5xl: 3rem;
```

### 3.3 圆角 & 阴影

```
--radius-sm: 8px;       --radius-md: 12px;      --radius-lg: 16px;
--radius-xl: 20px;      --radius-2xl: 24px;     --radius-full: 9999px;

--shadow-card: 0 4px 24px rgba(0, 0, 0, 0.3);
--shadow-elevated: 0 8px 40px rgba(0, 0, 0, 0.5);
--shadow-glow: 0 0 30px rgba(99, 102, 241, 0.25);
```

### 3.4 动效规范

```
--ease-out-expo: cubic-bezier(0.19, 1, 0.22, 1);
--ease-out-back: cubic-bezier(0.34, 1.56, 0.64, 1);
--duration-fast: 150ms;
--duration-normal: 300ms;
--duration-slow: 500ms;
```

---

## 四、路由设计

```
/                        →  重定向到 /dashboard
/dashboard               →  学习仪表盘（首页）
/wordbank                →  单词库（CET-4 / CET-6 分类）
/wordbank/:id            →  单词详情
/study                   →  学习模式选择
/study/flashcard         →  闪卡记忆模式
/study/dictation         →  听写模式
/study/quiz              →  选择题测验
/listening               →  听力训练
/listening/:id           →  听力详情
/reading                 →  阅读理解
/reading/:id             →  阅读文章详情
/writing                 →  写作训练
/mock-exam               →  模拟考试
/mock-exam/result/:id    →  考试成绩分析
/profile                 →  个人中心
/profile/statistics      →  详细数据统计
```

---

## 五、Pinia Store 设计

### 5.1 `useUserStore`
```
{
  state: {
    userInfo: { nickname, avatar, targetExam: 'CET-4' | 'CET-6', examDate, dailyGoal },
    token: string
  },
  actions: { login, logout, updateProfile, updateTarget }
}
```

### 5.2 `useWordStore`
```
{
  state: {
    words: [],                    // 全部单词
    cet4Words: [], cet6Words: [],
    masteredWords: [],            // 已掌握
    reviewWords: [],              // 待复习
    currentWordIndex: number,
    searchQuery: string,
    filterCET: 'all' | 'CET-4' | 'CET-6'
  },
  getters: {
    filteredWords,
    progressPercent,              // 学习进度百分比
    todayNewWords,
    todayReviewWords
  },
  actions: { fetchWords, markMastered, markReview, searchWords, getWordDetail }
}
```

### 5.3 `useStudyStore`
```
{
  state: {
    mode: 'flashcard' | 'dictation' | 'quiz',
    sessionWords: [],             // 当前学习会话的单词
    currentIndex: number,
    sessionStats: { correct, wrong, skipped, startTime },
    quizResults: []
  },
  actions: { startSession, nextWord, answerQuestion, endSession, getSessionReport }
}
```

### 5.4 `useExamStore`
```
{
  state: {
    mockExams: [],                // 历史模拟考
    currentExam: { section, timeRemaining, answers },
    listeningAudios: []
  },
  actions: { startMockExam, submitAnswer, submitExam, getExamResult, fetchHistory }
}
```

### 5.5 `useStatsStore`
```
{
  state: {
    dailyStats: [],               // 每日学习数据（连续天数、单词量、时长）
    skillRadar: { listening, reading, writing, translation, vocabulary, grammar },
    weeklyTrend: [],
    streakDays: number
  },
  actions: { fetchDashboard, fetchSkillRadar, fetchWeeklyTrend }
}
```

---

## 六、页面详细设计

### 6.1 学习仪表盘 `/dashboard`

**布局**：顶部问候语 + 连续打卡徽章 → 中央核心统计卡片（3列） → 下方技能雷达图 + 周趋势柱状图（左右分栏）

```
┌──────────────────────────────────────────────┐
│  👋 下午好, Alex        🔥 连续打卡 23 天    │
├──────────┬──────────┬────────────────────────┤
│ 今日单词 │ 学习时长 │ 掌握率                 │
│   32/50  │  2.5h   │  67.8%                 │
│ [进度环] │ [进度环] │  [进度环]              │
├──────────┴──────────┴────────────────────────┤
│  ┌──────────────┐  ┌──────────────────────┐  │
│  │  技能雷达图   │  │   每周学习趋势       │  │
│  │  (六维雷达)   │  │   (渐变柱状图)       │  │
│  │              │  │                      │  │
│  └──────────────┘  └──────────────────────┘  │
├──────────────────────────────────────────────┤
│  今日推荐复习单词  →  [词卡横向滑动]         │
└──────────────────────────────────────────────┘
```

**ECharts 雷达图**：
- 6 个维度：听力、阅读、写作、翻译、词汇、语法
- 半透明填充 `rgba(99, 102, 241, 0.3)` + 渐变描边
- 分割线使用 `rgba(255,255,255,0.08)`
- 标签颜色 `--text-secondary`

**ECharts 柱状图**：
- 7 天数据，柱子使用 `--brand-gradient` 从上到下渐变
- 圆角柱顶 `borderRadius: [8, 8, 0, 0]`
- 悬浮时发光效果（shadowBlur）
- 网格线极淡

---

### 6.2 单词库 `/wordbank`

**布局**：顶部搜索栏 + CET-4/CET-6 Tab 切换 → 左侧字母索引 → 右侧单词网格卡片

```
┌──────────────────────────────────────────────┐
│  🔍 搜索单词...        [CET-4] [CET-6] [全部]│
├────┬─────────────────────────────────────────┤
│ A  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│ B  │  │abandon│ │absorb│ │abuse │ │accent│  │
│ C  │  │ ★★★  │ │ ★★☆  │ │ ★☆☆  │ │ ★★★  │  │
│ D  │  └──────┘ └──────┘ └──────┘ └──────┘  │
│ ...│  ┌──────┐ ┌──────┐                      │
│    │  │achieve│ │active│ ...                 │
│    │  └──────┘ └──────┘                      │
└────┴─────────────────────────────────────────┘
```

**单词卡片**：
- 毛玻璃背景 `bg-glass`
- 单词使用 `--font-mono` 字体，字号 `--text-xl`，渐变文字 `brand-gradient`
- 音标浅色显示
- 掌握程度用 3 颗星表示（渐变色填充）
- Hover：卡片上浮 `translateY(-4px)` + `shadow-glow`

---

### 6.3 闪卡记忆模式 `/study/flashcard`

**核心交互**：可翻转卡片 + 滑动手势（左滑不认识 / 右滑已掌握）

```
┌──────────────────────────────────────────────┐
│  ← 不认识                          已掌握 → │
│                                              │
│         ┌─────────────────────┐              │
│         │                     │              │
│         │   **abandon**       │              │
│         │   /əˈbændən/        │              │
│         │                     │              │
│         │   (点击翻转查看释义)  │              │
│         │                     │              │
│         └─────────────────────┘              │
│                                              │
│    进度 12/50  ████████░░░░░░░░  24%        │
└──────────────────────────────────────────────┘
```

**卡片背面**（翻转后）：
- 中英文释义
- 例句（高亮目标单词）
- 词根词缀拆解
- 近义词 / 反义词

**动效**：
- 翻转使用 CSS 3D `rotateY(180deg)` + `perspective`
- 左滑卡片飞出左侧（红色阴影）
- 右滑卡片飞出右侧（绿色阴影）
- 切换下一张：卡片从底部弹入 `--ease-out-back`

---

### 6.4 测验模式 `/study/quiz`

**题型**：英译中选择题（4选1）+ 拼写填空

```
┌──────────────────────────────────────────────┐
│  Q3/20                            ⏱ 00:45   │
│                                              │
│     ┌──────────────────────────────┐         │
│     │  "The evidence was          │         │
│     │   **compelling** enough     │         │
│     │   to convince the jury."    │         │
│     └──────────────────────────────┘         │
│                                              │
│    "compelling" 的意思是？                   │
│                                              │
│   ┌──────────────────────────────────┐       │
│   │  A. 令人信服的   ✓               │  ← 点击后显示正确/错误样式
│   ├──────────────────────────────────┤       │
│   │  B. 复杂的                       │       │
│   ├──────────────────────────────────┤       │
│   │  C. 矛盾的                       │       │
│   ├──────────────────────────────────┤       │
│   │  D. 表面的                       │       │
│   └──────────────────────────────────┘       │
│                                              │
│         [下一题 →]                           │
└──────────────────────────────────────────────┘
```

**交互细节**：
- 选择后立即反馈：正确选项绿色边框 + ✓ 图标，错误选项红色闪烁
- 显示正确答案及解析
- 底部进度条用渐变色

---

### 6.5 听力训练 `/listening`

```
┌──────────────────────────────────────────────┐
│  听力训练  ·  Section A  短篇新闻            │
├──────────────────────────────────────────────┤
│                                              │
│        ┌─────────────────────┐               │
│        │    🎵  Audio Wave   │               │
│        │  ▁▃▅▇█▇▅▃▁▃▅▇█▇   │  ← 动态音频波形 │
│        │                     │               │
│        │   ▶ 00:32 / 02:15   │               │
│        │  ──●────────────    │               │
│        └─────────────────────┘               │
│                                              │
│  播放次数: 1/3                               │
│                                              │
│  Q1: What is the news report mainly about?  │
│  ┌──────────────────────────────────┐       │
│  │  A. A natural disaster...        │       │
│  │  B. An international summit...   │       │
│  │  C. A technological breakthrough │       │
│  │  D. A sports competition...      │       │
│  └──────────────────────────────────┘       │
└──────────────────────────────────────────────┘
```

---

### 6.6 模拟考试 `/mock-exam`

**全屏沉浸式**考试界面：
- 顶部倒计时（大号数字 + 渐变色，剩余 5 分钟时红色脉冲动画）
- 左侧：试卷内容（写作 / 听力 / 阅读 / 翻译 分区）
- 右侧：答题卡概览（题号网格，已答/未答/标记 状态色）
- 底部：交卷按钮

---

### 6.7 考试结果分析 `/mock-exam/result/:id`

```
┌──────────────────────────────────────────────┐
│  📊 模拟考试报告    CET-6 · 2026-06-15       │
├──────────────────────────────────────────────┤
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │ 总分 │  │ 听力 │  │ 阅读 │  │写作翻译│     │
│  │ 532  │  │ 145  │  │ 198  │  │ 189  │     │
│  │ /710 │  │/248.5│  │/248.5│  │/213 │     │
│  └──────┘  └──────┘  └──────┘  └──────┘     │
│                                              │
│  ┌──────────────────┐ ┌───────────────────┐  │
│  │   技能雷达图      │ │   各题型正确率     │  │
│  │   (对比上次)      │ │   (分组柱状图)    │  │
│  └──────────────────┘ └───────────────────┘  │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │  错题回顾 · 3题                       │    │
│  │  · Q12 - vocabulary  → [加入复习]    │    │
│  │  · Q18 - grammar      → [加入复习]    │    │
│  │  · Q25 - comprehension → [加入复习]   │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

---

### 6.8 个人中心 `/profile`

- 头像 + 昵称 + 目标考试院校
- 连续打卡日历热力图（类似 GitHub contribution graph）
- 学习统计卡片（累计单词 · 累计时长 · 完成模拟考次数）
- 目标设定（每日单词量 / 考试日期倒计时）
- 成就徽章墙（首次打卡 / 7天连续 / 掌握500词 / 模拟考达标等）

---

## 七、全局组件清单

| 组件名 | 用途 | 关键 Props |
|--------|------|------------|
| `AppLayout.vue` | 整体布局（侧边栏 + 顶栏 + 主内容区） | - |
| `AppSidebar.vue` | 导航侧边栏 | `collapsed` |
| `GlowCard.vue` | 带发光边框的通用卡片容器 | `glowColor`, `padding` |
| `ProgressRing.vue` | 环形进度条（SVG） | `percent`, `size`, `strokeWidth`, `gradient` |
| `WordCard.vue` | 单词展示卡片 | `word`, `phonetic`, `meaning`, `level`, `mastered` |
| `FlipCard.vue` | 可翻转卡片（闪卡模式用） | `frontContent`, `backContent`, `flipped` |
| `StatCard.vue` | 统计数值卡片（图标 + 数字 + 标签） | `icon`, `value`, `label`, `trend`, `color` |
| `ChartRadar.vue` | 技能雷达图（封装 ECharts） | `data`, `dimensions` |
| `ChartBar.vue` | 柱状图（封装 ECharts） | `data`, `xKey`, `yKey`, `gradient` |
| `StreakBadge.vue` | 连续打卡徽章 | `days`, `size` |
| `TimerCountdown.vue` | 倒计时组件（考试用） | `seconds`, `warning` |
| `AudioPlayer.vue` | 音频播放器（听力用） | `src`, `waveform` |
| `EmptyState.vue` | 空状态占位 | `icon`, `title`, `description` |
| `PageTransition.vue` | 页面过渡动画包装器 | - |

---

## 八、全局 SCSS Mixin 示例

```scss
// 毛玻璃卡片
@mixin glass-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
}

// 霓虹发光
@mixin neon-glow($color: var(--brand-gradient)) {
  box-shadow: 0 0 20px rgba($color, 0.3), 0 0 60px rgba($color, 0.1);
}

// 文字渐变
@mixin text-gradient($gradient: var(--brand-gradient)) {
  background: $gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

// 页面进入动画
@mixin page-enter {
  animation: fadeSlideUp var(--duration-slow) var(--ease-out-expo);
}

@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

// 脉冲呼吸
@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 8px rgba(99, 102, 241, 0.4); }
  50%      { box-shadow: 0 0 24px rgba(139, 92, 246, 0.7); }
}
```

---

## 九、代码生成要求总结

1. **所有页面**使用 `@include page-enter` 入场动画
2. **所有卡片**使用 `@include glass-card` 毛玻璃风格
3. **数字/统计数据**使用渐变色或发光效果突出
4. **ECharts 图表**全局暗色主题，`backgroundColor: 'transparent'`，文字颜色使用 `--text-secondary`
5. **交互反馈**：按钮点击有 `scale(0.96)` 微缩放；卡片 hover 上浮 + 发光
6. **过渡动画**：路由切换使用 `<Transition>` 配合 `fadeSlideUp`
7. **响应式**：移动端侧边栏自动折叠为汉堡菜单
8. **暗色模式**：全局默认暗色，后续可扩展亮色切换
9. **代码规范**：使用 `<script setup>` + TypeScript + SCSS scoped

---

## 十、API 接口预留

前端所有数据请求统一通过 `/api/v1/` 前缀，在 `src/api/` 下模块化管理：

```
src/api/
├── request.ts          // Axios 实例（拦截器、token 注入、错误处理）
├── auth.ts             // 登录/注册/用户信息
├── words.ts            // 单词库 CRUD
├── study.ts            // 学习记录上报
├── exam.ts             // 模拟考试
├── listening.ts        // 听力资源
├── reading.ts          // 阅读文章
└── stats.ts            // 统计数据
```

每个 API 模块导出类型定义 + 请求函数，供 Pinia Store 调用。

---
