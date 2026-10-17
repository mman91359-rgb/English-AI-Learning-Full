import { ref, onUnmounted } from 'vue'

export function useSpeechRecognition() {
  const isListening = ref(false)
  const transcript = ref('')
  const error = ref('')
  const isSupported = ref(false)

  let recognition: any = null

  // 检查浏览器是否支持 Web Speech API
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (SpeechRecognition) {
    isSupported.value = true
    recognition = new SpeechRecognition()
    recognition.lang = 'en-US'
    recognition.continuous = false
    recognition.interimResults = false
    recognition.maxAlternatives = 1

    recognition.onresult = (event: any) => {
      const result = event.results[0][0].transcript
      transcript.value = result.trim()
      isListening.value = false
    }

    recognition.onerror = (event: any) => {
      error.value = event.error || '语音识别出错'
      isListening.value = false
    }

    recognition.onend = () => {
      isListening.value = false
    }
  }

  const start = () => {
    if (!recognition) {
      error.value = '浏览器不支持语音识别'
      return
    }
    error.value = ''
    transcript.value = ''
    isListening.value = true
    try {
      recognition.start()
    } catch (e) {
      error.value = '启动语音识别失败'
      isListening.value = false
    }
  }

  const stop = () => {
    if (recognition) {
      recognition.stop()
    }
    isListening.value = false
  }

  onUnmounted(() => {
    if (recognition) {
      recognition.abort()
    }
  })

  return { isListening, transcript, error, isSupported, start, stop }
}
