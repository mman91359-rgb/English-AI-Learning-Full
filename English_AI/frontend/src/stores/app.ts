import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import i18n from '@/i18n'

type AppLanguage = 'zh-CN' | 'en-US'
type ThemeMode = 'light' | 'dark' | 'system'

const LANGUAGES: AppLanguage[] = ['zh-CN', 'en-US']
const THEME_MODES: ThemeMode[] = ['light', 'dark', 'system']

const normalizeLanguage = (lang: string | null): AppLanguage => {
  return LANGUAGES.includes(lang as AppLanguage) ? lang as AppLanguage : 'zh-CN'
}

const normalizeThemeMode = (mode: string | null): ThemeMode => {
  return THEME_MODES.includes(mode as ThemeMode) ? mode as ThemeMode : 'light'
}

export const useAppStore = defineStore('app', () => {
  const themeMode = ref<ThemeMode>(normalizeThemeMode(localStorage.getItem('themeMode')))
  const language = ref<AppLanguage>(normalizeLanguage(localStorage.getItem('language')))
  const isDark = ref(false)

  const applyTheme = () => {
    let dark = false
    if (themeMode.value === 'dark') {
      dark = true
    } else if (themeMode.value === 'system') {
      dark = window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    isDark.value = dark
    const html = document.documentElement
    if (dark) {
      html.classList.add('dark')
    } else {
      html.classList.remove('dark')
    }
  }

  const toggleTheme = () => {
    themeMode.value = isDark.value ? 'light' : 'dark'
    localStorage.setItem('themeMode', themeMode.value)
    applyTheme()
  }

  const setThemeMode = (mode: string) => {
    const nextMode = normalizeThemeMode(mode)
    themeMode.value = nextMode
    localStorage.setItem('themeMode', nextMode)
    applyTheme()
  }

  const setLanguage = (lang: string) => {
    const nextLanguage = normalizeLanguage(lang)
    language.value = nextLanguage
    localStorage.setItem('language', nextLanguage)
    i18n.global.locale.value = nextLanguage
  }

  const init = () => {
    applyTheme()
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (themeMode.value === 'system') applyTheme()
    })
    i18n.global.locale.value = language.value
  }

  watch(themeMode, () => applyTheme())

  return {
    themeMode,
    language,
    isDark,
    toggleTheme,
    setThemeMode,
    setLanguage,
    init,
  }
})
