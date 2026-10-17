<template>
  <div class="auth-page">
    <!-- 浮动装饰球 -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div class="auth-card" :class="{ shake: isShaking }">
      <!-- 卡片内氛围光 -->
      <div class="card-glow card-glow-tr"></div>
      <div class="card-glow card-glow-bl"></div>

      <!-- 头部 -->
      <div class="auth-header">
        <div class="logo-box">
          <svg class="logo-svg" viewBox="0 0 24 24" fill="none">
            <path d="M12 6.25c-1.57-1.12-3.61-1.75-5.75-1.75s-4.18.63-5.75 1.75v12.25c1.57-1.12 3.61-1.75 5.75-1.75s4.18.63 5.75 1.75c1.57-1.12 3.61-1.75 5.75-1.75s4.18.63 5.75 1.75v-12.25c-1.57-1.12-3.61-1.75-5.75-1.75s-4.18.63-5.75 1.75z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M12 6.25v12.25" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M7 10h3M7 14h2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="11.5" r="1.5" class="dot-pulse" fill="white"/>
          </svg>
        </div>
        <h1>English AI</h1>
        <p>开启你的 AI 英语提分之旅</p>
      </div>

      <!-- Toast 提示 -->
      <transition name="toast">
        <div v-if="toast.show" class="toast" :class="toast.type">
          <svg v-if="toast.type === 'error'" class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <svg v-else class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ toast.message }}</span>
        </div>
      </transition>

      <!-- 表单 -->
      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="input-wrap">
          <span class="input-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </span>
          <input v-model="form.username" type="text" placeholder="请输入用户名" required autocomplete="username" />
        </div>

        <div class="input-wrap">
          <span class="input-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </span>
          <input v-model="form.password" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" required autocomplete="current-password" />
          <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
            <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0m5.656 0l-2.121 2.121L3 17l4.172-4.172m10.828 5.656a9 9 0 00-2.828-2.828m-8.707-1.414A9 9 0 002 12c1.274-4.057 5.064-7 9.542-7 1.13 0 2.213.187 3.23.53l-2.12 2.12M17.34 17.34L21 21" />
            </svg>
          </button>
        </div>

        <div class="form-extra">
          <label class="remember">
            <input type="checkbox" v-model="rememberMe" />
            <span>记住我</span>
          </label>
          <a href="#" class="forgot">忘记密码？</a>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <svg v-if="loading" class="spinner" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25" />
            <path d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" fill="currentColor" opacity="0.75" />
          </svg>
          <span>{{ loading ? '登录中...' : '登 录' }}</span>
        </button>
      </form>

      <p class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../../api/userApi'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(false)
const isShaking = ref(false)
const toast = ref({ show: false, message: '', type: 'error' })

onMounted(() => {
  const saved = localStorage.getItem('remember_username')
  if (saved) { form.username = saved; rememberMe.value = true }
})

function showToast(message, type = 'error') {
  toast.value = { show: true, message, type }
  setTimeout(() => { toast.value.show = false }, 4000)
}

function triggerShake() {
  isShaking.value = true
  setTimeout(() => { isShaking.value = false }, 500)
}

async function handleLogin() {
  if (!form.username || !form.password) { showToast('请填写完整信息'); triggerShake(); return }
  loading.value = true
  try {
    const res = await login(form)
    if (res.code === 200) {
      if (rememberMe.value) {
        localStorage.setItem('remember_username', form.username)
      } else {
        localStorage.removeItem('remember_username')
      }
      userStore.setAuth(res.data.token, res.data)
      showToast('登录成功！正在跳转...', 'success')
      setTimeout(() => { router.push('/dashboard') }, 800)
    } else {
      showToast(res.message || '登录失败')
    }
  } catch (e) {
    showToast('网络错误，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* 浮动装饰球 */
.orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.orb-1 {
  width: 288px; height: 288px;
  background: rgba(67, 97, 238, 0.06);
  top: 20%; left: 20%;
  filter: blur(60px);
  animation: orb-float 6s ease-in-out infinite;
}
.orb-2 {
  width: 320px; height: 320px;
  background: rgba(58, 134, 255, 0.05);
  bottom: 20%; right: 20%;
  filter: blur(60px);
  animation: orb-float-reverse 8s ease-in-out infinite;
}
.orb-3 {
  width: 256px; height: 256px;
  background: rgba(67, 97, 238, 0.04);
  top: 40%; right: 30%;
  filter: blur(60px);
  animation: orb-float 7s ease-in-out 1s infinite;
}

@keyframes orb-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-16px) rotate(5deg); }
}
@keyframes orb-float-reverse {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(16px) rotate(-5deg); }
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-2xl);
  box-shadow: var(--elevation-3);
  padding: 48px 36px;
  position: relative;
  overflow: hidden;
  animation: card-enter 0.7s var(--ease-out) both;
}

@keyframes card-enter {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-glow {
  position: absolute;
  width: 128px; height: 128px;
  background: rgba(67, 97, 238, 0.03);
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
}
.card-glow-tr { top: -40px; right: -40px; }
.card-glow-bl { bottom: -40px; left: -40px; }

/* 头部 */
.auth-header { text-align: center; margin-bottom: 32px; position: relative; }
.logo-box {
  width: 64px; height: 64px;
  background: var(--color-primary);
  border-radius: var(--radius-lg);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(67, 97, 238, 0.25);
  transition: transform var(--duration-fast) var(--ease-out);
}
.logo-box:hover { transform: scale(1.05); }
.logo-svg { width: 36px; height: 36px; color: #fff; }
.dot-pulse { animation: dot-pulse 2s ease-in-out infinite; }
@keyframes dot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.auth-header h1 {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}
.auth-header p { font-size: var(--text-sm); color: var(--text-secondary); margin-top: 6px; }

/* Toast */
.toast {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 16px; border-radius: var(--radius-md);
  font-size: var(--text-sm); margin-bottom: 20px;
  font-weight: 500;
}
.toast.error { background: var(--color-error-bg); color: var(--color-error); border: 1px solid rgba(239,71,111,0.15); }
.toast.success { background: var(--color-success-bg); color: var(--color-success); border: 1px solid rgba(6,214,160,0.15); }
.toast-icon { width: 20px; height: 20px; flex-shrink: 0; margin-top: 1px; }
.toast-enter-active { transition: all 0.3s var(--ease-out); }
.toast-leave-active { transition: all 0.2s var(--ease-out); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-8px); }

/* 表单 */
.auth-form { display: flex; flex-direction: column; gap: 16px; position: relative; }

.input-wrap {
  position: relative;
  display: flex; align-items: center;
}
.input-icon {
  position: absolute;
  left: 16px; top: 50%;
  transform: translateY(-50%);
  width: 20px; height: 20px;
  color: var(--text-tertiary);
  pointer-events: none;
  transition: color var(--duration-fast);
  z-index: 1;
}
.input-wrap:focus-within .input-icon { color: var(--color-primary); }
.input-wrap input {
  width: 100%;
  padding: 14px 44px 14px 48px;
  background: var(--bg-subtle);
  border: 1.5px solid transparent;
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  font-size: 15px;
  font-family: var(--font-body);
  outline: none;
  transition: all var(--duration-fast);
}
.input-wrap input:focus {
  background: var(--bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-bg);
}
.input-wrap input::placeholder { color: var(--text-tertiary); }

.toggle-pwd {
  position: absolute;
  right: 14px; top: 50%;
  transform: translateY(-50%);
  width: 20px; height: 20px;
  color: var(--text-tertiary);
  background: none; border: none; cursor: pointer;
  padding: 0; display: flex;
  transition: color var(--duration-fast);
  z-index: 1;
}
.toggle-pwd:hover { color: var(--text-secondary); }
.toggle-pwd svg { width: 20px; height: 20px; }

.form-extra {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 4px;
}
.remember {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: var(--text-xs); color: var(--text-secondary);
  cursor: pointer; user-select: none;
  transition: color var(--duration-fast);
}
.remember:hover { color: var(--text-primary); }
.remember input {
  width: 15px; height: 15px;
  accent-color: var(--color-primary);
  cursor: pointer;
}
.forgot {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  transition: color var(--duration-fast);
}
.forgot:hover { color: var(--color-primary); }

.submit-btn {
  width: 100%;
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 15px;
  background: var(--color-primary);
  color: #fff;
  font-family: var(--font-body);
  font-size: var(--text-base);
  font-weight: 600;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(67, 97, 238, 0.25);
  transition: all var(--duration-fast) var(--ease-out);
  margin-top: 4px;
}
.submit-btn:hover {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(67, 97, 238, 0.35);
}
.submit-btn:active { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.spinner {
  width: 20px; height: 20px;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.auth-footer { text-align: center; margin-top: 28px; font-size: var(--text-sm); color: var(--text-tertiary); }
.auth-footer a { color: var(--color-primary); font-weight: 500; }
.auth-footer a:hover { text-decoration: underline; }

/* 震动 */
.shake { animation: shake 0.4s ease-in-out; }
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

@media (min-width: 768px) {
  .auth-card { padding: 48px 44px; }
  .auth-header h1 { font-size: var(--text-2xl); }
}
</style>
