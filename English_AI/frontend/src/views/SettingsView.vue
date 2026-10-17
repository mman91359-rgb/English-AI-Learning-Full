<template>
  <AppLayout>
    <h2 class="font-headline-lg text-headline-lg text-on-surface mb-8">{{ $t('settings.title') }}</h2>

    <!-- 通用设置 -->
    <section class="mb-8">
      <h3 class="font-label-lg text-label-lg text-on-surface-variant uppercase tracking-wider mb-3 px-2">{{ $t('settings.general') }}</h3>
      <div class="bg-surface rounded-2xl shadow-soft-glow overflow-hidden divide-y divide-outline-variant/30">

        <!-- 主题模式 -->
        <div class="flex items-center gap-4 p-5">
          <div class="w-10 h-10 rounded-full bg-primary-container/30 flex items-center justify-center text-primary flex-shrink-0">
            <span class="material-symbols-outlined">
              {{ appStore.isDark ? 'dark_mode' : 'light_mode' }}
            </span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-body-lg text-body-lg text-on-surface">{{ $t('settings.theme') }}</p>
            <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('settings.current') }}{{ themeModeLabel }}</p>
          </div>
          <div class="flex bg-surface-container-low rounded-full p-1 gap-1">
            <button
              v-for="m in themeOptions"
              :key="m.value"
              :class="[
                'px-3 py-1.5 rounded-full font-label-md text-label-md transition-all',
                appStore.themeMode === m.value
                  ? 'bg-primary text-on-primary shadow-sm'
                  : 'text-on-surface-variant hover:text-on-surface'
              ]"
              @click="appStore.setThemeMode(m.value)"
            >
              {{ m.label }}
            </button>
          </div>
        </div>

        <!-- 语言设置 -->
        <div class="flex items-center gap-4 p-5 cursor-pointer hover:bg-surface-container-low/50 transition-colors" @click="showLang = !showLang">
          <div class="w-10 h-10 rounded-full bg-secondary-container/30 flex items-center justify-center text-secondary flex-shrink-0">
            <span class="material-symbols-outlined">language</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-body-lg text-body-lg text-on-surface">{{ $t('settings.language') }}</p>
            <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('settings.currentLanguage') }}{{ currentLangLabel }}</p>
          </div>
          <span class="material-symbols-outlined text-outline">{{ showLang ? 'expand_less' : 'chevron_right' }}</span>
        </div>

        <!-- 语言选项展开区 -->
        <div v-if="showLang" class="bg-surface-container-low/40">
          <div
            v-for="l in languageOptions"
            :key="l.value"
            :class="[
              'flex items-center gap-3 px-5 py-4 cursor-pointer transition-colors',
              appStore.language === l.value
                ? 'bg-primary-container/10 text-primary'
                : 'text-on-surface hover:bg-surface-container-low'
            ]"
            @click="selectLang(l.value)"
          >
            <span class="text-xl">{{ l.flag }}</span>
            <span class="font-body-md text-body-md">{{ l.label }}</span>
            <span v-if="appStore.language === l.value" class="material-symbols-outlined ml-auto text-primary">check_circle</span>
          </div>
        </div>

        <!-- 通知设置 -->
        <div class="flex items-center gap-4 p-5 cursor-pointer hover:bg-surface-container-low/50 transition-colors" @click="toastComingSoon">
          <div class="w-10 h-10 rounded-full bg-tertiary-container/30 flex items-center justify-center text-tertiary flex-shrink-0">
            <span class="material-symbols-outlined">notifications</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-body-lg text-body-lg text-on-surface">{{ $t('profile.notificationSettings') }}</p>
            <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('settings.notificationDesc') }}</p>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
      </div>
    </section>

    <!-- 账户设置 -->
    <section class="mb-8">
      <h3 class="font-label-lg text-label-lg text-on-surface-variant uppercase tracking-wider mb-3 px-2">{{ $t('settings.account') }}</h3>
      <div class="bg-surface rounded-2xl shadow-soft-glow overflow-hidden divide-y divide-outline-variant/30">
        <div class="flex items-center gap-4 p-5 cursor-pointer hover:bg-surface-container-low/50 transition-colors" @click="toastComingSoon">
          <div class="w-10 h-10 rounded-full bg-primary-container/30 flex items-center justify-center text-primary flex-shrink-0">
            <span class="material-symbols-outlined">shield</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-body-lg text-body-lg text-on-surface">{{ $t('profile.privacySettings') }}</p>
            <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('settings.privacyDesc') }}</p>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>

        <!-- 修改密码 -->
        <div class="flex items-center gap-4 p-5 cursor-pointer hover:bg-surface-container-low/50 transition-colors" @click="openPasswordDialog">
          <div class="w-10 h-10 rounded-full bg-primary-container/30 flex items-center justify-center text-primary flex-shrink-0">
            <span class="material-symbols-outlined">lock</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-body-lg text-body-lg text-on-surface">{{ $t('profile.changePassword') }}</p>
            <p class="font-label-md text-label-md text-on-surface-variant">{{ $t('settings.passwordDesc') }}</p>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>

        <!-- 绑定手机 -->
        <div class="flex items-center gap-4 p-5 cursor-pointer hover:bg-surface-container-low/50 transition-colors" @click="openPhoneDialog">
          <div class="w-10 h-10 rounded-full bg-primary-container/30 flex items-center justify-center text-primary flex-shrink-0">
            <span class="material-symbols-outlined">phone_iphone</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-body-lg text-body-lg text-on-surface">{{ $t('settings.bindPhone') }}</p>
            <p class="font-label-md text-label-md text-on-surface-variant">
              {{ userStore.phone ? maskPhone(userStore.phone) : $t('settings.phoneNotBound') }}
            </p>
          </div>
          <span class="material-symbols-outlined text-outline">chevron_right</span>
        </div>
      </div>
    </section>

    <!-- 退出登录 -->
    <button
      class="w-full bg-error-container/30 text-error font-label-lg text-label-lg py-4 rounded-2xl hover:bg-error-container/60 transition-colors flex items-center justify-center gap-2"
      @click="handleLogout"
    >
      <span class="material-symbols-outlined">logout</span>
      {{ $t('profile.logout') }}
    </button>

    <!-- 修改密码弹窗 -->
    <div
      v-if="passwordDialogVisible"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-[100]"
      @click.self="closePasswordDialog"
    >
      <div class="bg-surface rounded-[24px] p-8 w-full max-w-md mx-4 soft-shadow">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-headline-md text-headline-md text-on-surface">{{ $t('profile.changePassword') }}</h3>
          <button class="p-1 text-on-surface-variant hover:text-on-surface rounded-full transition-colors" @click="closePasswordDialog">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="space-y-4">
          <input
            ref="oldPwdInputRef"
            v-model="oldPassword"
            type="password"
            :placeholder="$t('settings.oldPassword')"
            class="w-full py-3 px-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
          />
          <input
            v-model="newPassword"
            type="password"
            :placeholder="$t('settings.newPassword')"
            class="w-full py-3 px-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
          />
          <input
            v-model="confirmPassword"
            type="password"
            :placeholder="$t('settings.confirmPassword')"
            class="w-full py-3 px-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
            @keyup.enter="submitPassword"
          />
        </div>
        <div class="flex gap-3 mt-6">
          <button
            class="flex-1 py-3 rounded-full font-label-lg text-label-lg bg-surface-container-low text-on-surface hover:bg-surface-container-high transition-colors"
            @click="closePasswordDialog"
          >
            {{ $t('profile.cancel') }}
          </button>
          <button
            :disabled="passwordSubmitting"
            class="flex-1 py-3 rounded-full font-label-lg text-label-lg bg-primary text-on-primary hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            @click="submitPassword"
          >
            {{ passwordSubmitting ? $t('settings.saving') : $t('profile.confirm') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 绑定手机号弹窗 -->
    <div
      v-if="phoneDialogVisible"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-[100]"
      @click.self="closePhoneDialog"
    >
      <div class="bg-surface rounded-[24px] p-8 w-full max-w-md mx-4 soft-shadow">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-headline-md text-headline-md text-on-surface">{{ $t('settings.bindPhone') }}</h3>
          <button class="p-1 text-on-surface-variant hover:text-on-surface rounded-full transition-colors" @click="closePhoneDialog">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <input
          ref="phoneInputRef"
          v-model="phoneInput"
          type="tel"
          maxlength="11"
          :placeholder="$t('settings.phonePlaceholder')"
          class="w-full py-3 px-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all mb-4"
          @keyup.enter="submitPhone"
        />
        <div class="flex gap-3">
          <button
            class="flex-1 py-3 rounded-full font-label-lg text-label-lg bg-surface-container-low text-on-surface hover:bg-surface-container-high transition-colors"
            @click="closePhoneDialog"
          >
            {{ $t('profile.cancel') }}
          </button>
          <button
            :disabled="phoneSubmitting"
            class="flex-1 py-3 rounded-full font-label-lg text-label-lg bg-primary text-on-primary hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            @click="submitPhone"
          >
            {{ phoneSubmitting ? $t('settings.saving') : $t('profile.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AppLayout from '../components/AppLayout.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'
import { updatePassword, updatePhone } from '@/api/user'

const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const { show: toast } = useToast()

const showLang = ref(false)

const themeOptions = computed(() => [
  { value: 'light', label: t('settings.themeLight') },
  { value: 'dark', label: t('settings.themeDark') },
  { value: 'system', label: t('settings.themeSystem') },
])

const languageOptions = computed(() => [
  { value: 'zh-CN', label: t('settings.langZhCN'), flag: '🇨🇳' },
  { value: 'en-US', label: t('settings.langEnUS'), flag: '🇺🇸' },
])

const themeModeLabel = computed(() => {
  const m = themeOptions.value.find(o => o.value === appStore.themeMode)
  return m?.label || t('settings.themeLight')
})

const currentLangLabel = computed(() => {
  const l = languageOptions.value.find(o => o.value === appStore.language)
  return l?.label || t('settings.langZhCN')
})

const selectLang = (val: string) => {
  appStore.setLanguage(val)
  showLang.value = false
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const toastComingSoon = () => {
  toast(t('settings.comingSoon'), 'info')
}

// 手机号脱敏显示（中间4位用 * 替换）
const maskPhone = (phone: string) => {
  if (phone.length !== 11) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

// ========== 修改密码 ==========
const passwordDialogVisible = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordSubmitting = ref(false)
const oldPwdInputRef = ref<HTMLInputElement | null>(null)

const openPasswordDialog = () => {
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  passwordDialogVisible.value = true
  nextTick(() => oldPwdInputRef.value?.focus())
}

const closePasswordDialog = () => {
  passwordDialogVisible.value = false
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

const submitPassword = async () => {
  if (!oldPassword.value) {
    toast(t('settings.oldPasswordEmpty'), 'warning')
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    toast(t('settings.passwordTooShort'), 'warning')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast(t('settings.passwordMismatch'), 'warning')
    return
  }
  if (newPassword.value === oldPassword.value) {
    toast(t('settings.passwordSameError'), 'warning')
    return
  }

  const userId = userStore.userId
  if (!userId) return

  passwordSubmitting.value = true
  try {
    await updatePassword(userId, oldPassword.value, newPassword.value)
    toast(t('settings.passwordChangeSuccess'), 'success')
    closePasswordDialog()
  } catch (e: any) {
    toast(e.message || t('settings.passwordChangeFail'), 'error')
  } finally {
    passwordSubmitting.value = false
  }
}

// ========== 绑定手机号 ==========
const phoneDialogVisible = ref(false)
const phoneInput = ref('')
const phoneSubmitting = ref(false)
const phoneInputRef = ref<HTMLInputElement | null>(null)

const openPhoneDialog = () => {
  phoneInput.value = userStore.phone || ''
  phoneDialogVisible.value = true
  nextTick(() => phoneInputRef.value?.focus())
}

const closePhoneDialog = () => {
  phoneDialogVisible.value = false
  phoneInput.value = ''
}

const submitPhone = async () => {
  const phone = phoneInput.value.trim()
  if (!phone) {
    toast(t('settings.phoneEmpty'), 'warning')
    return
  }
  // 校验手机号格式（11位数字，1开头）
  if (!/^1\d{10}$/.test(phone)) {
    toast(t('settings.phoneInvalid'), 'warning')
    return
  }

  const userId = userStore.userId
  if (!userId) return

  // 号码未变化直接关闭
  if (phone === userStore.phone) {
    closePhoneDialog()
    return
  }

  phoneSubmitting.value = true
  try {
    await updatePhone(userId, phone)
    userStore.updatePhone(phone)
    toast(t('settings.phoneBindSuccess'), 'success')
    closePhoneDialog()
  } catch (e: any) {
    toast(e.message || t('settings.phoneBindFail'), 'error')
  } finally {
    phoneSubmitting.value = false
  }
}
</script>
