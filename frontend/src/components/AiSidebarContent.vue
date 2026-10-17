<template>
  <div class="flex flex-col h-full bg-[#f8f9fc] dark:bg-[#1a1b26]">
    <!-- 头部 -->
    <div class="flex items-center justify-between px-6 py-4 border-b border-outline-variant/30">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[#7c6fae] to-[#a594d4] flex items-center justify-center">
          <span class="material-symbols-outlined text-white text-xl">psychology</span>
        </div>
        <div>
          <div class="font-label-lg text-label-lg text-on-surface font-semibold">AI 学习助手</div>
          <div class="flex items-center gap-1.5 mt-0.5">
            <span class="w-2 h-2 rounded-full bg-green-500"></span>
            <span class="font-body-xs text-body-xs text-on-surface-variant">{{ contextLabel }}</span>
          </div>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button
          v-if="aiStore.messages.length > 0"
          class="w-8 h-8 flex items-center justify-center rounded-full hover:bg-surface-container-high transition-colors"
          title="清空对话"
          @click="aiStore.clearMessages()"
        >
          <span class="material-symbols-outlined text-on-surface-variant text-lg">delete_outline</span>
        </button>
        <button
          class="w-8 h-8 flex items-center justify-center rounded-full hover:bg-surface-container-high transition-colors"
          @click="$emit('close')"
        >
          <span class="material-symbols-outlined text-on-surface-variant">close</span>
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="msgContainer" class="flex-1 overflow-y-auto px-5 py-4">
      <!-- 空状态：欢迎页（图三样式） -->
      <div v-if="aiStore.messages.length === 0" class="flex flex-col items-center text-center pt-8">
        <!-- 头像光晕 -->
        <div class="relative mb-6">
          <div class="absolute inset-0 bg-gradient-to-br from-[#7c6fae]/20 to-[#a594d4]/10 rounded-full blur-2xl scale-150"></div>
          <div class="relative w-20 h-20 rounded-2xl bg-white dark:bg-surface shadow-lg flex items-center justify-center border border-outline-variant/20">
            <span class="material-symbols-outlined text-4xl text-[#7c6fae]" style="font-variation-settings: 'FILL' 1;">auto_awesome</span>
          </div>
        </div>
        <h3 class="font-headline-md text-headline-md text-on-surface mb-2 font-bold">我是你的专属 AI 导师</h3>
        <p class="font-body-md text-body-md text-on-surface-variant max-w-[260px] leading-relaxed">
          遇到不懂的词汇、语法或长难句？<br/>随时问我，帮你深入理解。
        </p>

        <!-- 功能入口卡片 -->
        <div class="w-full mt-8 space-y-3">
          <button
            class="w-full flex items-center gap-4 px-4 py-3.5 rounded-2xl bg-white dark:bg-surface hover:shadow-md transition-all border border-outline-variant/20 text-left group"
            @click="sendQuickAction('vocab')"
          >
            <div class="w-10 h-10 rounded-xl bg-[#ede7f6] dark:bg-[#7c6fae]/20 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-[#7c6fae]">menu_book</span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-xs text-[#ff8a3d]">✨</span>
                <span class="font-body-md text-body-md font-medium text-on-surface">核心词汇精解</span>
              </div>
            </div>
            <span class="material-symbols-outlined text-outline text-lg group-hover:text-primary transition-colors">chevron_right</span>
          </button>

          <button
            class="w-full flex items-center gap-4 px-4 py-3.5 rounded-2xl bg-white dark:bg-surface hover:shadow-md transition-all border border-outline-variant/20 text-left group"
            @click="sendQuickAction('grammar')"
          >
            <div class="w-10 h-10 rounded-xl bg-[#e0f7fa] dark:bg-cyan-900/30 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-cyan-600 dark:text-cyan-400">schema</span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-xs text-[#ff8a3d]">✨</span>
                <span class="font-body-md text-body-md font-medium text-on-surface">分析句子语法树</span>
              </div>
            </div>
            <span class="material-symbols-outlined text-outline text-lg group-hover:text-primary transition-colors">chevron_right</span>
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-else class="space-y-4">
        <div
          v-for="msg in aiStore.messages"
          :key="msg.id"
          class="flex"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[85%] rounded-2xl px-4 py-3"
            :class="msg.role === 'user'
              ? 'bg-primary text-on-primary rounded-br-md'
              : 'bg-white dark:bg-surface text-on-surface rounded-bl-md select-text border border-outline-variant/10 shadow-sm'"
          >
            <div v-if="msg.role === 'ai'" class="flex items-center justify-between mb-1.5">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-sm">smart_toy</span>
                <span class="font-label-sm text-label-sm text-primary">AI助手</span>
              </div>
              <button
                v-if="msg.content && !msg.isStreaming"
                class="text-on-surface-variant hover:text-primary transition-colors p-1 rounded-md hover:bg-surface-container-high"
                title="复制"
                @click="copyMessage(msg.content)"
              >
                <span class="material-symbols-outlined text-[16px]">content_copy</span>
              </button>
            </div>
            <div class="font-body-sm text-body-sm break-words select-text leading-relaxed">
              <span v-html="msg.role === 'ai' ? formatAiContent(msg.content) : msg.content"></span>
              <span
                v-if="msg.isStreaming"
                class="inline-block w-1.5 h-4 bg-primary ml-0.5 animate-pulse align-text-bottom"
              ></span>
            </div>
          </div>
        </div>

        <!-- loading 占位 -->
        <div v-if="aiStore.isLoading && aiStore.messages.length === 0" class="flex justify-start">
          <div class="bg-white dark:bg-surface rounded-2xl px-4 py-3 border border-outline-variant/10 shadow-sm">
            <div class="flex gap-1.5">
              <span class="w-2.5 h-2.5 bg-on-surface-variant/30 rounded-full animate-bounce" style="animation-delay:0s"></span>
              <span class="w-2.5 h-2.5 bg-on-surface-variant/30 rounded-full animate-bounce" style="animation-delay:0.15s"></span>
              <span class="w-2.5 h-2.5 bg-on-surface-variant/30 rounded-full animate-bounce" style="animation-delay:0.3s"></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="px-5 py-4 border-t border-outline-variant/30">
      <div class="flex items-center gap-2 bg-white dark:bg-surface rounded-full px-4 py-2.5 shadow-sm border border-outline-variant/20 focus-within:border-primary/50 transition-colors">
        <span class="material-symbols-outlined text-outline text-xl">add</span>
        <input
          ref="inputRef"
          v-model="inputText"
          class="flex-1 bg-transparent outline-none font-body-md text-body-md text-on-surface placeholder:text-on-surface-variant/50"
          :placeholder="inputPlaceholder"
          maxlength="500"
          :disabled="aiStore.isLoading"
          @keydown.enter.prevent="handleSend"
        />
        <button
          class="w-9 h-9 rounded-full bg-gradient-to-br from-[#7c6fae] to-[#a594d4] text-white flex items-center justify-center shrink-0 disabled:opacity-40 transition-opacity hover:shadow-lg disabled:hover:shadow-none"
          :disabled="!inputText.trim() || aiStore.isLoading"
          @click="handleSend"
        >
          <span class="material-symbols-outlined text-lg" style="font-variation-settings: 'FILL' 1;">arrow_upward</span>
        </button>
      </div>
      <!-- 底部提示 -->
      <div v-if="aiStore.messages.length === 0" class="text-center mt-3">
        <p class="font-body-xs text-body-xs text-on-surface-variant/50">
          <span class="text-[#ff8a3d]">✨</span> AI 生成内容可能存在误差，请以实际为准
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { useAiStore } from '@/stores/ai'

function formatAiContent(text: string): string {
  if (!text) return ''

  let result = text

  // 规范化 ## 标题：确保每个 ## 在新行开始
  result = result.replace(/([^\n])##\s*/g, '$1\n## ')
  result = result.replace(/^\s*##\s*/gm, '## ')

  // 按 ## 标题分割
  const sections = result.split(/\n(?=##\s)/)
  const formattedSections: string[] = []

  for (const section of sections) {
    const trimmed = section.trim()
    if (!trimmed) continue

    // 解析标题和内容
    const firstLineEnd = trimmed.indexOf('\n')
    let title = ''
    let content = ''

    if (firstLineEnd > 0) {
      const firstLine = trimmed.substring(0, firstLineEnd).trim()
      if (firstLine.startsWith('## ')) {
        title = firstLine.substring(3).trim()
      } else {
        title = firstLine
      }
      content = trimmed.substring(firstLineEnd + 1).trim()
    } else {
      if (trimmed.startsWith('## ')) {
        title = trimmed.substring(3).trim()
      } else {
        content = trimmed
      }
    }

    // 渲染标题（保留 emoji 和 **加粗**）
    if (title) {
      const renderedTitle = title
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      formattedSections.push(`<h3 class="ai-title">${renderedTitle}</h3>`)
    }
    if (content) {
      formattedSections.push(formatSection(content))
    }
  }

  return formattedSections.join('<div class="ai-section-gap"></div>')
}

function formatSection(content: string): string {
  if (!content) return ''

  const lines = content.split('\n')
  const formattedLines: string[] = []
  let inList = false
  let listType = ''

  function closeList() {
    if (inList) {
      formattedLines.push(listType === 'ol' ? '</ol>' : '</ul>')
      inList = false
      listType = ''
    }
  }

  function inlineFormat(s: string): string {
    return s
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code class="ai-code">$1</code>')
      .replace(/\b(\/[^/]+\/)\b/g, '<span class="ai-phonetic">$1</span>')
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      closeList()
      formattedLines.push('<div class="ai-spacer"></div>')
      continue
    }

    // 编号列表：1. / 2. / 1、/ 2、
    if (/^\d+[.、\)]\s/.test(line)) {
      if (!inList || listType !== 'ol') {
        closeList()
        formattedLines.push('<ol class="ai-ordered-list">')
        inList = true
        listType = 'ol'
      }
      formattedLines.push(`<li>${inlineFormat(line.replace(/^\d+[.、\)]\s*/, ''))}</li>`)
      continue
    }

    // 无序列表：- / * / •
    if (line.startsWith('- ') || line.startsWith('* ') || line.startsWith('• ')) {
      if (!inList || listType !== 'ul') {
        closeList()
        formattedLines.push('<ul class="ai-unordered-list">')
        inList = true
        listType = 'ul'
      }
      formattedLines.push(`<li>${inlineFormat(line.replace(/^[-*•]\s*/, ''))}</li>`)
      continue
    }

    // 普通段落
    closeList()
    formattedLines.push(`<div class="ai-paragraph">${inlineFormat(line)}</div>`)
  }

  closeList()
  return formattedLines.join('')
}

const emit = defineEmits<{
  (e: 'close'): void
}>()

const aiStore = useAiStore()

const inputText = ref('')
const msgContainer = ref<HTMLDivElement>()

const contextLabel = computed(() => {
  const map: Record<string, string> = {
    vocab: '词汇练习中',
    grammar: '语法练习中',
    general: '随时为你解答',
  }
  return map[aiStore.contextType] || '随时为你解答'
})

const inputPlaceholder = computed(() => {
  if (aiStore.isLoading) return 'AI正在回复...'
  if (aiStore.contextType === 'vocab') return '问这个单词的用法...'
  if (aiStore.contextType === 'grammar') return '问这个语法点...'
  return '问点什么...'
})

function handleSend() {
  const text = inputText.value.trim()
  if (!text || aiStore.isLoading) return
  inputText.value = ''
  aiStore.sendMessage(text)
}

function sendQuickAction(type: 'vocab' | 'grammar') {
  if (type === 'vocab') {
    if (aiStore.questionContent) {
      aiStore.sendMessage(`请详细解释一下这道题中的核心词汇：${aiStore.questionContent}`)
    } else {
      aiStore.sendMessage('请帮我精解核心词汇的用法和搭配')
    }
  } else {
    if (aiStore.questionContent) {
      aiStore.sendMessage(`请分析一下这个句子的语法结构：${aiStore.questionContent}`)
    } else {
      aiStore.sendMessage('请帮我分析句子语法结构')
    }
  }
}

function copyMessage(text: string) {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    const btn = document.createElement('div')
    btn.textContent = '已复制'
    btn.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.8);color:#fff;padding:8px 16px;border-radius:8px;font-size:14px;z-index:9999;'
    document.body.appendChild(btn)
    setTimeout(() => btn.remove(), 1500)
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    try { document.execCommand('copy') } catch {}
    document.body.removeChild(ta)
  })
}

// 新消息自动滚动到底部
watch(
  () => aiStore.messages.length,
  () => {
    nextTick(() => {
      if (msgContainer.value) {
        msgContainer.value.scrollTop = msgContainer.value.scrollHeight
      }
    })
  },
)

// 流式更新时也滚动
watch(
  () => {
    const msgs = aiStore.messages
    return msgs.length > 0 ? msgs[msgs.length - 1].content : ''
  },
  () => {
    nextTick(() => {
      if (msgContainer.value) {
        const el = msgContainer.value
        const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
        if (atBottom) el.scrollTop = el.scrollHeight
      }
    })
  },
)
</script>

<style scoped>
input {
  min-height: 24px;
}

:deep(.ai-section-gap) {
  height: 16px;
}

:deep(.ai-title) {
  font-size: 15px;
  font-weight: 600;
  color: #7c6fae;
  margin: 0 0 12px 0;
  padding-left: 10px;
  border-left: 4px solid #7c6fae;
  line-height: 1.4;
}

:deep(.ai-subtitle) {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin: 12px 0 8px 0;
  line-height: 1.4;
}

:deep(.ai-spacer) {
  height: 6px;
}

:deep(.ai-phonetic) {
  color: #7c6fae;
  font-style: italic;
  font-family: 'Noto Serif SC', serif;
}

:deep(.ai-paragraph) {
  font-size: 13px;
  color: #374151;
  line-height: 1.8;
  margin: 0 0 10px 0;
  padding-left: 10px;
}

:deep(.ai-ordered-list) {
  margin: 0 0 12px 28px;
  padding: 0;
}

:deep(.ai-ordered-list li) {
  font-size: 13px;
  color: #374151;
  line-height: 1.8;
  margin: 6px 0;
  padding-left: 10px;
}

:deep(.ai-unordered-list) {
  margin: 0 0 12px 24px;
  padding: 0;
  list-style: none;
}

:deep(.ai-unordered-list li) {
  font-size: 13px;
  color: #374151;
  line-height: 1.8;
  margin: 6px 0;
  padding-left: 18px;
  position: relative;
}

:deep(.ai-unordered-list li::before) {
  content: '•';
  position: absolute;
  left: 4px;
  color: #7c6fae;
  font-weight: bold;
}

:deep(.ai-code) {
  background-color: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  color: #ef4444;
}

:deep(strong) {
  font-weight: 600;
  color: #1f2937;
  margin-right: 6px;
}

:deep(em) {
  font-style: italic;
  color: #6b7280;
}

.dark :deep(.ai-paragraph),
.dark :deep(.ai-title-content),
.dark :deep(.ai-ordered-list li),
.dark :deep(.ai-unordered-list li) {
  color: #e5e7eb;
}

.dark :deep(.ai-subtitle) {
  color: #9ca3af;
}

.dark :deep(.ai-code) {
  background-color: #374151;
  color: #f87171;
}

.dark :deep(strong) {
  color: #f3f4f6;
}

.dark :deep(em) {
  color: #9ca3af;
}
</style>
