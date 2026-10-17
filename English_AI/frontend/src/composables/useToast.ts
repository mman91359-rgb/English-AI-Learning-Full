import { ref } from 'vue'

export interface Toast {
  id: string
  message: string
  type: 'success' | 'error' | 'warning' | 'info'
  duration: number
}

const toasts = ref<Toast[]>([])

let counter = 0

export function useToast() {
  const show = (message: string, type: Toast['type'] = 'info', duration: number = 3000) => {
    const id = `toast-${++counter}`
    toasts.value.push({ id, message, type, duration })
    if (duration > 0) {
      setTimeout(() => remove(id), duration)
    }
  }

  const remove = (id: string) => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  return { toasts, show, remove }
}

