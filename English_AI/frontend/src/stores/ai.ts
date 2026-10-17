import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatStream, type ChatRequest } from '@/api/ai'

export interface ChatMessage {
  id: string
  role: 'user' | 'ai' | 'system'
  content: string
  timestamp: number
  isStreaming?: boolean
}

let msgSeq = 0
function nextId() {
  return `msg_${Date.now()}_${++msgSeq}`
}

export const useAiStore = defineStore('ai', () => {
  const isOpen = ref(false)
  const inlineMode = ref(false)
  const messages = ref<ChatMessage[]>([])
  const isLoading = ref(false)
  const contextType = ref('general')
  const questionContent = ref('')
  const userAnswer = ref('')

  function open(context?: { contextType?: string; questionContent?: string; userAnswer?: string }) {
    if (context) {
      contextType.value = context.contextType || 'general'
      questionContent.value = context.questionContent || ''
      userAnswer.value = context.userAnswer || ''
      // 切题时清除上一题的对话记录，确保AI聚焦当前题目
      messages.value = []
    }
    isOpen.value = true
  }

  function close() {
    isOpen.value = false
  }

  function toggle() {
    if (isOpen.value) close()
    else open()
  }

  function setInlineMode(enabled: boolean) {
    inlineMode.value = enabled
  }

  function addMessage(role: 'user' | 'ai' | 'system', content: string) {
    const msg: ChatMessage = {
      id: nextId(),
      role,
      content,
      timestamp: Date.now(),
    }
    messages.value.push(msg)
    return msg
  }

  async function sendMessage(text: string) {
    if (!text.trim() || isLoading.value) return
    addMessage('user', text.trim())

    const aiMsg = addMessage('ai', '')
    aiMsg.isStreaming = true
    isLoading.value = true

    const ctx: ChatRequest = {
      message: text.trim(),
      contextType: contextType.value,
      questionContent: questionContent.value || undefined,
      userAnswer: userAnswer.value || undefined,
    }

    await chatStream(
      ctx,
      (token) => {
        aiMsg.content += token
      },
      () => {
        aiMsg.isStreaming = false
        isLoading.value = false
      },
      (err) => {
        aiMsg.content = '抱歉，AI服务暂时不可用：' + err
        aiMsg.isStreaming = false
        isLoading.value = false
      },
    )
  }

  function clearMessages() {
    messages.value = []
  }

  function sendScoreRequest(content: string, scoreType: 'speaking' | 'writing') {
    const prompt = scoreType === 'speaking'
      ? `请对以下口语内容进行评分：\n\n${content}`
      : `请对以下作文进行评分：\n\n${content}`
    sendMessage(prompt)
  }

  return {
    isOpen,
    inlineMode,
    messages,
    isLoading,
    contextType,
    questionContent,
    userAnswer,
    open,
    close,
    toggle,
    setInlineMode,
    sendMessage,
    clearMessages,
    sendScoreRequest,
  }
})
