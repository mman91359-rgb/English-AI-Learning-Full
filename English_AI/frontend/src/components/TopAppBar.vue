<template>
  <header class="h-20 sticky top-0 z-40 bg-background/80 backdrop-blur-md flex items-center justify-between px-gutter w-full max-w-container-max mx-auto">
    <div class="flex items-center gap-4">
      <div class="md:hidden">
        <span class="material-symbols-outlined text-on-surface text-2xl cursor-pointer">menu</span>
      </div>
      <slot name="left" />
    </div>
    <div class="flex items-center gap-2 sm:gap-4">
      <slot name="right">
        <!-- 通知按钮：铃铛 -->
        <button
          class="p-2 text-on-surface-variant hover:bg-surface-container-high/50 rounded-full transition-all relative"
          aria-label="通知"
          @click="showNotificationPopup = !showNotificationPopup"
        >
          <span class="material-symbols-outlined">notifications</span>
          <!-- 红点：有未读通知时显示 -->
          <span
            v-if="unreadCount > 0"
            class="absolute top-1 right-1 w-2 h-2 bg-error rounded-full"
          ></span>
        </button>

        <!-- 通知弹出框 -->
        <div
          v-if="showNotificationPopup"
          class="absolute right-4 top-20 bg-surface-container-lowest rounded-2xl p-4 soft-shadow w-80 z-50 border border-outline-variant/20"
        >
          <div class="flex justify-between items-center mb-3">
            <div class="font-label-lg text-label-lg text-on-surface">{{ $t('topbar.notifications') }}</div>
            <button
              v-if="unreadCount > 0"
              class="text-xs text-primary hover:underline"
              @click="markAllRead"
            >{{ $t('topbar.markAllRead') }}</button>
          </div>
          <div class="space-y-2 max-h-80 overflow-y-auto">
            <div
              v-for="n in notifications"
              :key="n.id"
              :class="[
                'p-3 rounded-xl border cursor-pointer transition-colors',
                n.read
                  ? 'bg-surface border-outline-variant/10'
                  : 'bg-primary-container/10 border-primary/20'
              ]"
              @click="markAsRead(n.id)"
            >
              <div class="font-label-md text-label-md text-on-surface mb-1">{{ $t(n.titleKey, n.titleParams || {}) }}</div>
              <div class="font-body-sm text-body-sm text-on-surface-variant">{{ $t(n.contentKey, n.contentParams || {}) }}</div>
              <div class="font-body-sm text-body-sm text-on-surface-variant mt-1 opacity-60">{{ n.time }}</div>
            </div>
            <div v-if="notifications.length === 0" class="text-center py-6 text-on-surface-variant">
              <span class="material-symbols-outlined text-3xl mb-2 block">notifications_off</span>
              {{ $t('topbar.noNotifications') }}
            </div>
          </div>
          <button
            v-if="notifications.length > 0"
            class="mt-3 w-full text-center text-xs text-on-surface-variant hover:text-error transition-colors py-1"
            @click="clearAll"
          >{{ $t('topbar.clearAll') }}</button>
        </div>

        <!-- 主题切换按钮：太阳/月亮 -->
        <button
          class="p-2 text-on-surface-variant hover:bg-surface-container-high/50 rounded-full transition-all"
          :aria-label="appStore.isDark ? $t('topbar.switchToLight') : $t('topbar.switchToDark')"
          @click="appStore.toggleTheme"
        >
          <span class="material-symbols-outlined">
            {{ appStore.isDark ? 'light_mode' : 'dark_mode' }}
          </span>
        </button>

        <!-- 语言切换按钮：地球网格图标 -->
        <button
          class="p-2 text-on-surface-variant hover:bg-surface-container-high/50 rounded-full transition-all"
          :aria-label="$t('topbar.currentLanguage') + ': ' + currentLanguageLabel"
          @click="cycleLanguage"
        >
          <span class="material-symbols-outlined">language</span>
        </button>

        <!-- 用户头像：有头像显示图片，无头像显示首字母 -->
        <div
          class="w-10 h-10 rounded-full bg-primary text-on-primary flex items-center justify-center cursor-pointer font-bold text-base select-none overflow-hidden"
          :title="userStore.username || '用户'"
          @click="$emit('userClick')"
        >
          <img
            v-if="topbarAvatarUrl"
            :src="topbarAvatarUrl"
            alt="头像"
            class="w-full h-full object-cover"
          />
          <span v-else>{{ avatarText }}</span>
        </div>
      </slot>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useNotifications } from '@/composables/useNotifications'

defineEmits(['userClick'])

const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const { notifications, unreadCount, markAsRead, markAllRead, clearAll } = useNotifications()

// 头像首字母
const avatarText = computed(() => {
  const name = userStore.user?.username || userStore.username || ''
  return name ? name.charAt(0).toUpperCase() : 'M'
})

// 头像完整URL（拼上网关前缀）
const topbarAvatarUrl = computed(() => {
  const url = userStore.avatarUrl
  if (!url) return ''
  return `/api${url}`
})

const currentLanguageLabel = computed(() => {
  const labels: Record<string, string> = {
    'zh-CN': t('settings.langZhCN'),
    'en-US': t('settings.langEnUS'),
  }
  return labels[appStore.language] || appStore.language
})

// 循环切换语言
const cycleLanguage = () => {
  const langs = ['zh-CN', 'en-US'] as const
  const idx = langs.indexOf(appStore.language)
  const next = langs[(idx + 1) % langs.length]
  appStore.setLanguage(next)
}

// 通知弹出框
const showNotificationPopup = ref(false)

// 点击其他地方关闭通知弹窗
const closePopupOnOutsideClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.top-20') && !target.closest('[aria-label="通知"]')) {
    showNotificationPopup.value = false
  }
}
onMounted(() => document.addEventListener('click', closePopupOnOutsideClick))
onBeforeUnmount(() => document.removeEventListener('click', closePopupOnOutsideClick))
</script>
