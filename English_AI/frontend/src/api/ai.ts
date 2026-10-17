/**
 * AI 对话 API：SSE 流式返回 + 评分。
 */
export interface ChatRequest {
  message: string
  contextType?: string
  questionContent?: string
  userAnswer?: string
  history?: { role: string; content: string }[]
  scoreType?: string
}

const BASE = '/ai'

async function streamRequest(
  endpoint: string,
  request: ChatRequest,
  onToken: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: string) => void,
): Promise<void> {
  const token = sessionStorage.getItem('token')

  try {
    const response = await fetch(`/api${BASE}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      if (response.status === 401) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('user')
        window.location.href = '/login'
        throw new Error('登录已过期，请重新登录')
      }
      throw new Error(`请求失败 (${response.status})`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('不支持流式响应')
    }

    const decoder = new TextDecoder()
    let fullText = ''
    let buffer = ''
    let currentEvent = 'token'
    let doneSent = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.substring(6).trim() || 'token'
          continue
        }

        if (!trimmed.startsWith('data:')) {
          continue
        }

        const data = trimmed.substring(5).trim()
        if (!data) continue

        if (currentEvent === 'done') {
          doneSent = true
          onDone(fullText)
          continue
        }

        if (currentEvent === 'error') {
          throw new Error(data)
        }

        fullText += data
        onToken(data)
      }
    }

    if (!doneSent) {
      onDone(fullText)
    }
  } catch (e: any) {
    onError(e.message || '连接失败')
  }
}

/** SSE 流式对话，前端呈现打字机效果。 */
export async function chatStream(
  request: ChatRequest,
  onToken: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: string) => void,
): Promise<void> {
  return streamRequest('/chat', request, onToken, onDone, onError)
}

/** AI 评分：口语/作文。 */
export async function scoreStream(
  request: ChatRequest,
  onToken: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: string) => void,
): Promise<void> {
  return streamRequest('/score', request, onToken, onDone, onError)
}
