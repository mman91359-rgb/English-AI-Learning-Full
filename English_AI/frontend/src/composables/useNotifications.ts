import { ref, computed, watch } from 'vue'

export interface Notification {
  id: string
  titleKey: string
  contentKey: string
  titleParams?: Record<string, string | number>
  contentParams?: Record<string, string | number>
  time: string
  read: boolean
  type: 'system' | 'reminder' | 'achievement' | 'level'
}

const STORAGE_KEY = 'notifications'

function seedNotifications(): Notification[] {
  const now = new Date()
  return [
    {
      id: 'welcome',
      titleKey: 'notification.welcomeTitle',
      contentKey: 'notification.welcomeContent',
      time: formatTime(now),
      read: false,
      type: 'system',
    },
    {
      id: 'daily-reminder',
      titleKey: 'notification.dailyReminderTitle',
      contentKey: 'notification.dailyReminderContent',
      time: formatTime(new Date(now.getTime() - 2 * 3600_000)),
      read: false,
      type: 'reminder',
    },
  ]
}

function loadFromStorage(): Notification[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  const seeds = seedNotifications()
  localStorage.setItem(STORAGE_KEY, JSON.stringify(seeds))
  return seeds
}

const notifications = ref<Notification[]>(loadFromStorage())

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(notifications.value))
}

function formatTime(date: Date): string {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return `${Math.floor(diff / 60_000)}分钟前`
  if (diff < 86400_000) return `${Math.floor(diff / 3600_000)}小时前`
  return `${Math.floor(diff / 86400_000)}天前`
}

export function useNotifications() {
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

  const markAsRead = (id: string) => {
    const n = notifications.value.find((x) => x.id === id)
    if (n) {
      n.read = true
      persist()
    }
  }

  const markAllRead = () => {
    notifications.value.forEach((n) => (n.read = true))
    persist()
  }

  const add = (
    titleKey: string,
    contentKey: string,
    type: Notification['type'] = 'system',
    params?: { titleParams?: Record<string, string | number>; contentParams?: Record<string, string | number> },
  ) => {
    const id = `n-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    notifications.value.unshift({
      id,
      titleKey,
      contentKey,
      titleParams: params?.titleParams,
      contentParams: params?.contentParams,
      time: formatTime(new Date()),
      read: false,
      type,
    })
    if (notifications.value.length > 50) {
      notifications.value = notifications.value.slice(0, 50)
    }
    persist()
    return id
  }

  const clearAll = () => {
    notifications.value = []
    persist()
  }

  // 监听：答题后自动添加成就通知
  watch(
    () => localStorage.getItem('lastExerciseResult'),
    (raw) => {
      if (!raw) return
      try {
        const result = JSON.parse(raw)
        if (result.correctCount >= 10) {
          add(
            'notification.achievementTitle',
            'notification.achievementContent',
            'achievement',
            {
              contentParams: { count: result.correctCount, accuracy: result.accuracy },
            },
          )
        }
      } catch { /* ignore */ }
    },
  )

  return {
    notifications,
    unreadCount,
    markAsRead,
    markAllRead,
    add,
    clearAll,
  }
}
