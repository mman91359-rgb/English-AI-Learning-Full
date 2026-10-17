import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const radarData = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => user.value?.userId || user.value?.data?.userId)
  const username = computed(() => user.value?.username || user.value?.data?.username)
  const cetLevel = computed(() => user.value?.cetLevel || user.value?.data?.cetLevel)
  const eloScore = computed(() => user.value?.eloScore ?? user.value?.data?.eloScore ?? 1000)
  const eloLevel = computed(() => user.value?.eloLevel || user.value?.data?.eloLevel || '未定级')

  function setAuth(t, u) {
    token.value = t
    user.value = u
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
  }

  function logout() {
    token.value = ''
    user.value = null
    radarData.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function setRadarData(data) {
    radarData.value = data
  }

  return { token, user, radarData, isLoggedIn, userId, username, cetLevel, eloScore, eloLevel, setAuth, logout, setRadarData }
})
