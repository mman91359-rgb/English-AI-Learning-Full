<template>
  <div class="auth-page">
    <!-- 浮动装饰球 -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>

    <div class="auth-card" :class="{ shake: isShaking }">
      <div class="card-glow card-glow-tr"></div>
      <div class="card-glow card-glow-bl"></div>

      <!-- 头部 -->
      <div class="auth-header">
        <div class="logo-box">
          <svg class="logo-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <h1>创建账号</h1>
        <p>制定你的专属学习计划</p>
      </div>

      <!-- 验证错误提示 -->
      <transition name="toast">
        <div v-if="validationError" class="toast">
          <svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span>{{ validationError }}</span>
        </div>
      </transition>

      <!-- 表单 -->
      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="input-wrap">
          <span class="input-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </span>
          <input v-model="form.username" type="text" placeholder="请输入用户名（4-16位）" required />
        </div>

        <div class="input-wrap">
          <span class="input-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </span>
          <input v-model="form.password" :type="showPassword ? 'text' : 'password'" placeholder="至少6位密码" required />
          <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
            <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
            </svg>
          </button>
        </div>

        <!-- 密码强度指示 -->
        <div v-if="form.password" class="pwd-strength">
          <div class="strength-header">
            <span>密码强度</span>
            <span :class="strength.color">{{ strength.text }}</span>
          </div>
          <div class="strength-bars">
            <span v-for="(f, i) in strength.bars" :key="i" class="bar" :class="f ? strength.barColor : ''"></span>
          </div>
        </div>

        <div class="input-wrap">
          <span class="input-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.952 11.952 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </span>
          <input v-model="form.confirmPassword" :type="showConfirm ? 'text' : 'password'" placeholder="确认密码" required />
          <button type="button" class="toggle-pwd" @click="showConfirm = !showConfirm">
            <svg v-if="!showConfirm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
            </svg>
          </button>
        </div>

        <div class="input-wrap cet-group">
          <span class="input-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </span>
          <div class="cet-radios">
            <label class="cet-radio" :class="{ active: form.cetLevel === 'CET4' }">
              <input type="radio" v-model="form.cetLevel" value="CET4" />
              <span>CET-4 四级</span>
            </label>
            <label class="cet-radio" :class="{ active: form.cetLevel === 'CET6' }">
              <input type="radio" v-model="form.cetLevel" value="CET6" />
              <span>CET-6 六级</span>
            </label>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <svg v-if="loading" class="spinner" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25" />
            <path d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" fill="currentColor" opacity="0.75" />
          </svg>
          <span>{{ loading ? '注 册 中...' : '注 册' }}</span>
        </button>
      </form>

      <p class="auth-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../api/userApi'

const router = useRouter()
const form = reactive({ username: '', password: '', confirmPassword: '', cetLevel: 'CET4' })
const loading = ref(false)
const showPassword = ref(false)
const showConfirm = ref(false)
const validationError = ref('')
const isShaking = ref(false)

function triggerShake() {
  isShaking.value = true
  setTimeout(() => { isShaking.value = false }, 500)
}

const strength = computed(() => {
  const pwd = form.password
  if (!pwd) return { text: '', color: '', barColor: '', bars: [false, false, false] }
  if (pwd.length < 6) {
    return { text: '过短（≥6位）', color: 's-weak', barColor: 'bar-weak', bars: [true, false, false] }
  }
  let score = 0
  if (/[a-zA-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++
  if (score <= 1) return { text: '一般', color: 's-weak', barColor: 'bar-weak', bars: [true, false, false] }
  if (score === 2) return { text: '中等', color: 's-mid', barColor: 'bar-mid', bars: [true, true, false] }
  return { text: '强', color: 's-strong', barColor: 'bar-strong', bars: [true, true, true] }
})

async function handleRegister() {
  validationError.value = ''

  const nameRegex = /^[a-zA-Z0-9_\u4e00-\u9fa5]{4,16}$/
  if (!nameRegex.test(form.username)) {
    validationError.value = '用户名 4-16 位，可含中文、英文、数字、下划线'
    triggerShake()
    return
  }
  if (form.password.length < 6) {
    validationError.value = '密码长度不能少于 6 位'
    triggerShake()
    return
  }
  if (form.password !== form.confirmPassword) {
    validationError.value = '两次密码不一致，请检查'
    triggerShake()
    return
  }

  loading.value = true
  try {
    const res = await register({ username: form.username, password: form.password, cetLevel: form.cetLevel })
    if (res.code === 200) {
      router.push('/login?registered=1')
    } else {
      validationError.value = res.message || '注册失败'
    }
  } catch (e) {
    validationError.value = '网络错误，请重试'
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

.orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.orb-1 {
  width: 256px; height: 256px;
  background: rgba(67, 97, 238, 0.06);
  top: 10%; left: 15%;
  filter: blur(64px);
  animation: orb-float 12s ease-in-out infinite;
}
.orb-2 {
  width: 384px; height: 384px;
  background: rgba(58, 134, 255, 0.04);
  bottom: 10%; right: 10%;
  filter: blur(64px);
  animation: orb-float-reverse 15s ease-in-out infinite;
}

@keyframes orb-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-16px) rotate(5deg); }
}
@keyframes orb-float-reverse {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(20px) rotate(-8deg); }
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-2xl);
  box-shadow: var(--elevation-3);
  padding: 44px 36px;
  position: relative;
  overflow: hidden;
  animation: card-enter 0.7s var(--ease-out) both;
}

@keyframes card-enter {
  from { opacity: 0; transform: translateY(24px); }
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
.auth-header { text-align: center; margin-bottom: 28px; position: relative; }
.logo-box {
  width: 64px; height: 64px;
  background: var(--color-primary);
  border-radius: var(--radius-lg);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(67, 97, 238, 0.25);
  transform: rotate(3deg);
  transition: transform var(--duration-fast) var(--ease-out);
}
.logo-box:hover { transform: rotate(6deg) scale(1.05); }
.logo-svg { width: 32px; height: 32px; color: #fff; }
.auth-header h1 {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}
.auth-header p { font-size: var(--text-sm); color: var(--text-secondary); margin-top: 4px; }

/* Toast */
.toast {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 16px; border-radius: var(--radius-md);
  font-size: var(--text-sm); margin-bottom: 20px;
  font-weight: 500;
  background: var(--color-error-bg); color: var(--color-error);
  border: 1px solid rgba(239,71,111,0.15);
}
.toast-icon { width: 20px; height: 20px; flex-shrink: 0; }
.toast-enter-active { transition: all 0.3s var(--ease-out); }
.toast-leave-active { transition: all 0.2s var(--ease-out); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-8px); }

/* 表单 */
.auth-form { display: flex; flex-direction: column; gap: 14px; position: relative; }

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

/* 密码强度 */
.pwd-strength { padding: 0 4px; }
.strength-header {
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--text-xs); margin-bottom: 6px;
}
.strength-header span:first-child { color: var(--text-tertiary); }
.s-weak { color: var(--color-error); font-weight: 600; }
.s-mid { color: var(--color-warning); font-weight: 600; }
.s-strong { color: var(--color-success); font-weight: 600; }

.strength-bars { display: flex; gap: 4px; }
.bar {
  flex: 1; height: 4px;
  background: var(--bg-subtle);
  border-radius: 2px;
  transition: all 0.3s var(--ease-out);
}
.bar-weak { background: var(--color-error); }
.bar-mid { background: var(--color-warning); }
.bar-strong { background: var(--color-success); }

/* CET 单选 */
.cet-group { padding: 0; }
.cet-group .input-icon { top: 16px; transform: none; }
.cet-radios {
  display: flex; gap: 10px; width: 100%;
  padding: 10px 0 10px 48px;
}
.cet-radio {
  flex: 1;
  padding: 12px;
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  background: var(--bg-subtle);
}
.cet-radio input { display: none; }
.cet-radio.active {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 600;
}

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
  margin-top: 6px;
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

.auth-footer { text-align: center; margin-top: 24px; font-size: var(--text-sm); color: var(--text-tertiary); }
.auth-footer a { color: var(--color-primary); font-weight: 500; }
.auth-footer a:hover { text-decoration: underline; }

.shake { animation: shake 0.5s ease-in-out; }
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

@media (min-width: 768px) {
  .auth-card { padding: 48px 44px; }
}
</style>
