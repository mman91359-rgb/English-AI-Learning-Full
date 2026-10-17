<template>
  <AppLayout>
    <!-- 个人信息头部 -->
    <section class="bg-surface-container-low rounded-[32px] overflow-hidden relative soft-shadow">
      <div class="h-48 bg-primary-fixed-dim/30 w-full absolute top-0 left-0"></div>
      <div class="relative z-10 pt-24 pb-12 px-8 flex flex-col items-center text-center">
        <!-- 头像区域：点击可上传 -->
        <div
          class="w-40 h-40 bg-surface-container-lowest rounded-3xl soft-shadow mb-6 flex items-center justify-center border-4 border-surface relative cursor-pointer group overflow-hidden"
          @click="triggerAvatarUpload"
        >
          <img
            v-if="avatarFullUrl"
            :src="avatarFullUrl"
            alt="头像"
            class="w-full h-full object-cover"
          />
          <span v-else class="material-symbols-outlined text-[80px] text-primary fill">featured_seasonal_and_gifts</span>
          <!-- 悬浮遮罩提示 -->
          <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center rounded-3xl">
            <span class="material-symbols-outlined text-white text-4xl">camera_alt</span>
          </div>
        </div>
        <!-- 隐藏的文件输入框 -->
        <input
          ref="avatarInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleAvatarChange"
        />
        <!-- 上传提示 -->
        <p v-if="avatarUploading" class="text-label-sm font-label-sm text-primary mb-2">上传中...</p>
        <div class="flex items-center gap-2 group cursor-pointer" @click="openNicknameDialog">
          <h1 class="font-headline-lg text-headline-lg text-on-background">{{ displayName }}</h1>
          <span class="material-symbols-outlined text-outline group-hover:text-primary transition-colors text-xl">edit</span>
        </div>
        <p class="font-body-md text-body-md text-on-surface-variant mt-2">{{ userLevel }}</p>

        <!-- 修改昵称弹窗 -->
        <div
          v-if="nicknameDialogVisible"
          class="fixed inset-0 bg-black/40 flex items-center justify-center z-[100]"
          @click.self="closeNicknameDialog"
        >
          <div class="bg-surface rounded-[24px] p-8 w-full max-w-md mx-4 soft-shadow">
            <div class="flex items-center justify-between mb-6">
              <h3 class="font-headline-md text-headline-md text-on-surface">修改昵称</h3>
              <button
                class="p-1 text-on-surface-variant hover:text-on-surface rounded-full transition-colors"
                @click="closeNicknameDialog"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <input
              ref="nicknameInputRef"
              v-model="nicknameInput"
              type="text"
              maxlength="20"
              placeholder="请输入新昵称（最多20个字符）"
              class="w-full py-3 px-4 bg-background border border-outline-variant rounded-[16px] text-on-surface placeholder-outline font-body-md text-body-md focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-all mb-4"
              @keyup.enter="submitNickname"
            />
            <div class="flex gap-3">
              <button
                class="flex-1 py-3 rounded-full font-label-lg text-label-lg bg-surface-container-low text-on-surface hover:bg-surface-container-high transition-colors"
                @click="closeNicknameDialog"
              >
                取消
              </button>
              <button
                :disabled="nicknameSubmitting || !nicknameInput.trim()"
                class="flex-1 py-3 rounded-full font-label-lg text-label-lg bg-primary text-on-primary hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                @click="submitNickname"
              >
                {{ nicknameSubmitting ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
        <div class="grid grid-cols-3 gap-8 mt-12 w-full max-w-2xl mx-auto border-t border-outline-variant/20 pt-8">
          <div class="flex flex-col items-center">
            <span class="material-symbols-outlined text-secondary-fixed-dim mb-2 text-3xl fill">schedule</span>
            <span class="font-label-lg text-label-lg text-on-surface-variant mb-1">{{ $t('profile.totalStudyTime') }}</span>
            <span class="font-headline-md text-headline-md text-on-background">{{ totalStudyTime }}</span>
          </div>
          <div class="flex flex-col items-center border-l border-r border-outline-variant/20 px-4">
            <span class="material-symbols-outlined text-primary-container mb-2 text-3xl fill" :class="{ 'opacity-30': streakDays < 3 }">local_fire_department</span>
            <span class="font-label-lg text-label-lg text-on-surface-variant mb-1">{{ $t('dashboard.streakDays') }}</span>
            <span class="font-headline-md text-headline-md text-on-background">{{ streakDays }}{{ $t('stats.days') }}</span>
          </div>
          <div class="flex flex-col items-center">
            <span class="material-symbols-outlined text-tertiary-fixed-dim mb-2 text-3xl fill">calendar_month</span>
            <span class="font-label-lg text-label-lg text-on-surface-variant mb-1">{{ $t('stats.activeDays') }}</span>
            <span class="font-headline-md text-headline-md text-on-background">{{ activeDays }}{{ $t('stats.days') }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 积分与设置 -->
    <section class="grid grid-cols-1 md:grid-cols-3 gap-8">
      <!-- 个人积分 -->
      <div class="md:col-span-2 bg-surface-container-lowest rounded-[24px] p-8 soft-shadow border border-surface-container-low">
        <div class="flex justify-between items-center mb-8">
          <h3 class="font-headline-md text-headline-md text-on-background">{{ $t('profile.title') }}</h3>
        </div>
        <div class="bg-surface-container-low rounded-2xl p-6 text-center mb-8">
          <p class="font-label-md text-label-md text-on-surface-variant mb-2 uppercase tracking-wider">{{ $t('profile.totalScore') }}</p>
          <p class="font-display-lg text-display-lg text-primary-fixed-dim">{{ totalScore }}</p>
        </div>
        <div class="space-y-4">
          <div class="flex items-center justify-between p-4 rounded-xl hover:bg-surface-container-low transition-colors group cursor-default">
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-primary-fixed-dim/20 flex items-center justify-center text-primary">
                <span class="material-symbols-outlined">menu_book</span>
              </div>
              <span class="font-body-lg text-body-lg text-on-surface">{{ $t('dashboard.moduleVocabulary') }}</span>
            </div>
            <span class="font-headline-md text-headline-md text-on-surface">{{ vocabScore }}</span>
          </div>
          <div class="flex items-center justify-between p-4 rounded-xl hover:bg-surface-container-low transition-colors group cursor-default">
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-secondary-fixed-dim/20 flex items-center justify-center text-secondary">
                <span class="material-symbols-outlined">spellcheck</span>
              </div>
              <span class="font-body-lg text-body-lg text-on-surface">{{ $t('dashboard.moduleGrammar') }}</span>
            </div>
            <span class="font-headline-md text-headline-md text-on-surface">{{ grammarScore }}</span>
          </div>
          <div class="flex items-center justify-between p-4 rounded-xl hover:bg-surface-container-low transition-colors group cursor-default">
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-tertiary-fixed-dim/20 flex items-center justify-center text-tertiary">
                <span class="material-symbols-outlined">forum</span>
              </div>
              <span class="font-body-lg text-body-lg text-on-surface">{{ $t('dashboard.moduleTranslation') }}</span>
            </div>
            <span class="font-headline-md text-headline-md text-on-surface">{{ speakingScore }}</span>
          </div>
          <div class="flex items-center justify-between p-4 rounded-xl hover:bg-surface-container-low transition-colors group cursor-default">
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-surface-variant flex items-center justify-center text-on-surface-variant">
                <span class="material-symbols-outlined">headphones</span>
              </div>
              <span class="font-body-lg text-body-lg text-on-surface">{{ $t('dashboard.moduleListening') }}</span>
            </div>
            <span class="font-headline-md text-headline-md text-on-surface">{{ listeningScore }}</span>
          </div>
        </div>
      </div>

      <!-- 设置入口 -->
      <div class="flex flex-col gap-4">
        <router-link to="/settings/privacy" class="bg-surface-container-lowest rounded-[24px] p-6 soft-shadow flex items-center justify-between hover:-translate-y-1 transition-transform group">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center group-hover:bg-primary-fixed-dim/20 transition-colors">
              <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">shield_person</span>
            </div>
            <span class="font-body-lg text-body-lg text-on-surface font-medium">{{ $t('profile.privacySettings') }}</span>
          </div>
          <span class="material-symbols-outlined text-outline-variant">chevron_right</span>
        </router-link>
        <router-link to="/settings/account" class="bg-surface-container-lowest rounded-[24px] p-6 soft-shadow flex items-center justify-between hover:-translate-y-1 transition-transform group">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center group-hover:bg-primary-fixed-dim/20 transition-colors">
              <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">manage_accounts</span>
            </div>
            <span class="font-body-lg text-body-lg text-on-surface font-medium">{{ $t('profile.accountSettings') }}</span>
          </div>
          <span class="material-symbols-outlined text-outline-variant">chevron_right</span>
        </router-link>
        <router-link to="/settings/notifications" class="bg-surface-container-lowest rounded-[24px] p-6 soft-shadow flex items-center justify-between hover:-translate-y-1 transition-transform group">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center group-hover:bg-primary-fixed-dim/20 transition-colors">
              <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">notifications_active</span>
            </div>
            <span class="font-body-lg text-body-lg text-on-surface font-medium">{{ $t('profile.notificationSettings') }}</span>
          </div>
          <span class="material-symbols-outlined text-outline-variant">chevron_right</span>
        </router-link>
        <div class="mt-auto bg-primary-fixed-dim/10 rounded-[24px] p-6 border border-primary-fixed-dim/20 flex flex-col items-center justify-center text-center">
          <span class="material-symbols-outlined text-primary text-4xl mb-2">workspace_premium</span>
          <h4 class="font-label-lg text-label-lg text-primary mb-1">{{ $t('profile.upgradePro') }}</h4>
          <p class="font-label-md text-label-md text-on-surface-variant mb-4">{{ $t('profile.upgradeDesc') }}</p>
          <button class="w-full bg-primary text-on-primary py-2 rounded-xl font-label-lg text-label-lg hover:bg-primary/90 transition-colors">
            {{ $t('profile.viewPlans') }}
          </button>
        </div>
      </div>
    </section>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '../components/AppLayout.vue'
import { useUserStore } from '@/stores/user'
import { getRadarData } from '@/api/stats'
import { getExerciseStats } from '@/api/exercise'
import { uploadAvatar, updateNickname } from '@/api/user'
import { useToast } from '@/composables/useToast'
import type { RadarData } from '@/api/stats'

const { t } = useI18n()
const userStore = useUserStore()
const { show: toast } = useToast()

// ========== 用户基本信息 ==========
const displayName = computed(() => {
  return userStore.user?.nickname || userStore.user?.username || '同学'
})

const userLevel = ref(t('profile.beginner'))

// ========== 头像上传 ==========
const avatarInputRef = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)

/** 拼接头像完整URL（加上网关前缀） */
const avatarFullUrl = computed(() => {
  const url = userStore.avatarUrl
  if (!url) return ''
  // avatarUrl 格式: /user/avatar/xxx.jpg，前端需要加 /api 前缀走网关
  return `/api${url}`
})

/** 点击头像触发文件选择 */
const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

/** 选择文件后上传 */
const handleAvatarChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 前端校验
  if (!file.type.startsWith('image/')) {
    toast('请选择图片文件', 'error')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast('头像文件不能超过5MB', 'error')
    return
  }

  const userId = userStore.userId
  if (!userId) return

  avatarUploading.value = true
  try {
    const avatarUrl = await uploadAvatar(userId, file)
    userStore.updateAvatar(avatarUrl)
    toast('头像更新成功', 'success')
  } catch (e: any) {
    toast(e.message || '头像上传失败', 'error')
  } finally {
    avatarUploading.value = false
    // 重置 input，允许再次选择同一文件
    input.value = ''
  }
}

// ========== 修改昵称 ==========
const nicknameDialogVisible = ref(false)
const nicknameInput = ref('')
const nicknameSubmitting = ref(false)
const nicknameInputRef = ref<HTMLInputElement | null>(null)

/** 打开修改昵称弹窗 */
const openNicknameDialog = () => {
  nicknameInput.value = userStore.user?.nickname || userStore.username || ''
  nicknameDialogVisible.value = true
  // 弹窗渲染后自动聚焦输入框
  nextTick(() => {
    nicknameInputRef.value?.focus()
    nicknameInputRef.value?.select()
  })
}

/** 关闭修改昵称弹窗 */
const closeNicknameDialog = () => {
  nicknameDialogVisible.value = false
  nicknameInput.value = ''
}

/** 提交昵称修改 */
const submitNickname = async () => {
  const newNickname = nicknameInput.value.trim()
  if (!newNickname) {
    toast('昵称不能为空', 'warning')
    return
  }
  if (newNickname.length > 20) {
    toast('昵称长度不能超过20个字符', 'warning')
    return
  }

  const userId = userStore.userId
  if (!userId) return

  // 昵称未变化直接关闭
  if (newNickname === userStore.user?.nickname) {
    closeNicknameDialog()
    return
  }

  nicknameSubmitting.value = true
  try {
    await updateNickname(userId, newNickname)
    userStore.updateNickname(newNickname)
    toast('昵称修改成功', 'success')
    closeNicknameDialog()
  } catch (e: any) {
    toast(e.message || '昵称修改失败', 'error')
  } finally {
    nicknameSubmitting.value = false
  }
}

// ========== 顶部统计（从后端真实数据获取） ==========
const totalStudyTime = ref(`0${t('stats.minutes')}`)
const streakDays = computed(() => userStore.streakDays)
const activeDays = computed(() => userStore.activeDays)

// ========== 积分数据 ==========
const totalScore = ref(0)
const vocabScore = ref(0)
const grammarScore = ref(0)
const speakingScore = ref(0)
const listeningScore = ref(0)

// ========== 加载数据 ==========
const loadProfileData = async () => {
  const userId = userStore.userId
  if (!userId) return

  // 先刷新用户统计数据（连续学习天数、累计活跃天数）
  await userStore.loadUserStats()

  try {
    // 1. 获取能力雷达数据（模块分、ELO等级）
    const radar: RadarData = await getRadarData(userId)
    if (radar) {
      userLevel.value = radar.eloLevel || t('profile.beginner')
      vocabScore.value = Math.round(radar.vocabularyScore || 0)
      grammarScore.value = Math.round(radar.grammarScore || 0)
      listeningScore.value = Math.round(radar.listeningScore || 0)
      speakingScore.value = Math.round(radar.readingScore || 0)
      // {{ $t('profile.totalScore') }} = ELO分
      totalScore.value = radar.eloScore || 0
    }
  } catch (e) {
    console.warn('获取雷达数据失败', e)
  }

  try {
    // 2. 获取累计学习时长（使用后端真实时长，不再估算）
    const stats = await getExerciseStats(userId)
    if (stats) {
      const hours = Math.floor(stats.totalHours || 0)
      const mins = stats.totalMinutes || 0
      if (hours > 0) {
        totalStudyTime.value = `${hours}${t('stats.hours')}${mins}${t('stats.minutes')}`
      } else {
        totalStudyTime.value = `${mins}${t('stats.minutes')}`
      }
    }
  } catch (e) {
    console.warn('获取学习时长失败', e)
  }
}

watch(() => userStore.userId, (newId) => {
  if (newId) loadProfileData()
}, { immediate: true })

onMounted(() => {
  if (userStore.userId) {
    loadProfileData()
  }
})
</script>
