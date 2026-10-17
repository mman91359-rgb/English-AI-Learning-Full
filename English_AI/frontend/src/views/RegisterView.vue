<template>
  <div class="bg-background text-on-background min-h-screen flex">
    <aside class="hidden md:flex flex-col h-screen fixed left-0 top-0 w-sidebar-width bg-surface border-r border-outline-variant py-8 px-4 z-50">
      <div class="flex items-center gap-3 mb-8 px-4">
        <div class="w-10 h-10 bg-primary-fixed rounded-xl flex items-center justify-center">
          <span class="material-symbols-outlined text-primary text-3xl">school</span>
        </div>
        <span class="text-headline-md font-headline-md font-bold text-primary">{{ $t('app.title') }}</span>
      </div>
      <nav class="flex-1 flex flex-col gap-2">
        <router-link
          to="/login"
          :class="[
            'flex items-center gap-4 px-4 py-3 rounded-xl transition-colors',
            isLogin
              ? 'bg-primary-container/10 text-primary border-l-2 border-primary font-bold'
              : 'text-on-surface-variant hover:bg-primary-container/10 hover:text-primary'
          ]"
        >
          <span class="material-symbols-outlined">login</span>
          <span class="font-label-lg text-label-lg">{{ $t('auth.login') }}</span>
        </router-link>
        <router-link
          to="/register"
          :class="[
            'flex items-center gap-4 px-4 py-3 rounded-xl transition-colors',
            !isLogin
              ? 'bg-primary-container/10 text-primary border-l-2 border-primary font-bold'
              : 'text-on-surface-variant hover:bg-primary-container/10 hover:text-primary'
          ]"
        >
          <span class="material-symbols-outlined">person_add</span>
          <span class="font-label-lg text-label-lg">{{ $t('auth.register') }}</span>
        </router-link>
      </nav>
    </aside>

    <main class="flex-1 w-full md:ml-sidebar-width flex flex-col min-h-screen relative">
      <header class="h-20 sticky top-0 z-40 bg-background/80 backdrop-blur-md flex items-center justify-between px-gutter w-full max-w-container-max mx-auto">
        <div class="flex items-center gap-4">
          <div class="md:hidden">
            <span class="material-symbols-outlined text-on-surface text-2xl cursor-pointer">menu</span>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <button class="p-2 text-on-surface-variant hover:bg-surface-container-high/50 rounded-full transition-all"></button>
        </div>
      </header>

      <div class="flex-1 px-gutter py-8 w-full max-w-container-max mx-auto space-y-8 flex flex-col justify-center">
        <div class="grid grid-cols-1 md:grid-cols-12 gap-gutter">
          <div class="col-span-1 md:col-span-8 md:col-start-3 lg:col-span-6 lg:col-start-4 bg-surface rounded-[24px] p-8 sm:p-10 shadow-soft-glow relative overflow-hidden">
            <div class="text-center mb-10">
              <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary-container/10 mb-4">
                <span class="material-symbols-outlined text-primary text-3xl fill">person_add</span>
              </div>
              <h1 class="font-headline-xl text-headline-xl text-on-surface mb-2">{{ $t('auth.register') }}</h1>
              <p class="font-body-md text-body-md text-on-surface-variant">{{ $t('app.subtitle') }}</p>
            </div>

            <form class="space-y-5" @submit.prevent="handleRegister">
              <div class="relative group">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-secondary group-focus-within:text-primary transition-colors">person</span>
                </div>
                <input
                  v-model="form.username"
                  class="w-full py-3.5 pl-12 pr-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
                  :placeholder="$t('auth.username')"
                  required
                  type="text"
                />
              <p v-if="validationErrors.username" class="mt-1.5 px-1 font-label-sm text-label-sm text-red-500 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">error</span>
                {{ validationErrors.username }}
              </p>
            </div>

              <div class="relative group">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-secondary group-focus-within:text-primary transition-colors">smartphone</span>
                </div>
                <input
                  v-model="form.phone"
                  class="w-full py-3.5 pl-12 pr-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
                  :placeholder="$t('auth.phone')"
                  required
                  type="tel"
                />
                <p v-if="validationErrors.phone" class="mt-1.5 px-1 font-label-sm text-label-sm text-red-500 flex items-center gap-1">
                  <span class="material-symbols-outlined text-sm">error</span>
                  {{ validationErrors.phone }}
                </p>
              </div>

              <div class="relative group">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-secondary group-focus-within:text-primary transition-colors">lock</span>
                </div>
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="w-full py-3.5 pl-12 pr-12 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
                  :placeholder="$t('auth.password')"
                  required
                />
                <button
                  class="absolute inset-y-0 right-0 pr-4 flex items-center text-secondary hover:text-on-surface transition-colors"
                  type="button"
                  @click="togglePassword"
                >
                  <span class="material-symbols-outlined">{{ showPassword ? 'visibility' : 'visibility_off' }}</span>
                </button>
              
              <p v-if="validationErrors.password" class="mt-1.5 px-1 font-label-sm text-label-sm text-red-500 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">error</span>
                {{ validationErrors.password }}
              </p>
            </div>

              <div class="relative group">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-secondary group-focus-within:text-primary transition-colors">lock_reset</span>
                </div>
                <input
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  class="w-full py-3.5 pl-12 pr-12 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all"
                  :placeholder="$t('auth.confirmPassword')"
                  required
                />
                <button
                  class="absolute inset-y-0 right-0 pr-4 flex items-center text-secondary hover:text-on-surface transition-colors"
                  type="button"
                  @click="toggleConfirmPassword"
                >
                  <span class="material-symbols-outlined">{{ showConfirmPassword ? 'visibility' : 'visibility_off' }}</span>
                </button>
              
              <p v-if="validationErrors.confirmPassword" class="mt-1.5 px-1 font-label-sm text-label-sm text-red-500 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">error</span>
                {{ validationErrors.confirmPassword }}
              </p>
            </div>

              <div class="relative group">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-secondary group-focus-within:text-primary transition-colors">school</span>
                </div>
                <select
                  v-model="form.cetLevel"
                  class="w-full py-3.5 pl-12 pr-4 bg-background border border-outline-variant rounded-[16px] text-on-surface font-body-md text-body-md focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all appearance-none cursor-pointer"
                  required
                >
                  <option value="" disabled>{{ $t('auth.cefLevel') }}</option>
                  <option value="CET4">{{ $t('auth.cet4') }}</option>
                  <option value="CET6">{{ $t('auth.cet6') }}</option>
                </select>
              
              <p v-if="validationErrors.cetLevel" class="mt-1.5 px-1 font-label-sm text-label-sm text-red-500 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">error</span>
                {{ validationErrors.cetLevel }}
              </p>
            </div>

              <div class="pt-2">
                <button
                  :disabled="isLoading"
                  class="w-full py-3.5 px-4 bg-primary hover:bg-on-primary-fixed-variant text-on-primary font-label-lg text-label-lg rounded-full shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2 active:scale-95 disabled:opacity-80 disabled:cursor-not-allowed"
                  type="submit"
                >
                  <span>{{ isLoading ? $t('auth.register') + '...' : $t('auth.register') }}</span>
                  <span v-if="isLoading" class="material-symbols-outlined animate-spin">progress_activity</span>
                </button>
              </div>
            </form>

            <div class="mt-8 text-center">
              <p class="font-body-md text-body-md text-on-surface-variant">
                {{ $t('auth.hasAccount') }}
                <router-link class="text-primary font-bold hover:underline underline-offset-4 transition-all" to="/login">{{ $t('auth.goLogin') }}</router-link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { show: toast } = useToast()

const isLogin = computed(() => route.path === '/login')

const form = ref({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  cetLevel: ''
})

const validationErrors = ref({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  cetLevel: ''
})

const showPassword = ref(false)
const showConfirmPassword = ref(false)

const isLoading = computed(() => userStore.isLoading)

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPassword = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

const validate = (): boolean => {
  const err = { username: '', phone: '', password: '', confirmPassword: '', cetLevel: '' }
  let valid = true

  if (!form.value.username.trim()) {
    err.username = '请输入用户名'
    valid = false
  }
  if (!/^1[3-9]\d{9}$/.test(form.value.phone.trim())) {
    err.phone = '请输入有效手机号'
    valid = false
  }
  if (!form.value.password || form.value.password.length < 6) {
    err.password = '密码不能少于6位'
    valid = false
  }
  if (form.value.password !== form.value.confirmPassword) {
    err.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  if (!form.value.cetLevel) {
    err.cetLevel = '请选择考试级别'
    valid = false
  }

  validationErrors.value = err
  return valid
}

const handleRegister = async () => {
  if (!validate()) return

  try {
    // 1. 注册
    await userStore.register({
      username: form.value.username,
      phone: form.value.phone.trim(),
      password: form.value.password,
      cetLevel: form.value.cetLevel
    })
    // 2. 直接登录（免去二次输入）
    await userStore.login({
      username: form.value.username,
      password: form.value.password
    })
    toast('注册成功，欢迎加入！', 'success')
    router.push('/assess')
  } catch (error: any) {
    toast(error.message || '注册失败', 'error')
  }
}
</script>





