<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LogIn, UserPlus } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const errorMessage = ref('');
const mode = ref<'login' | 'register'>(route.query.mode === 'register' ? 'register' : 'login');

const loginForm = reactive({
  account: '',
  password: ''
});

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const copy = computed(() => {
  const zh = preferencesStore.languageCode !== 'en_US';

  if (mode.value === 'register') {
    return zh
      ? {
          kicker: '创建账号',
          title: '创建你的学习账号',
          desc: '用一个账号保存文章、收藏、复习卡片和社区互动，之后可以继续完善头像和个人主页。',
          account: '用户名',
          email: '邮箱',
          password: '密码',
          confirmPassword: '确认密码',
          submit: sessionStore.loading ? '创建中' : '创建账号',
          switchText: '已经有账号？',
          switchAction: '去登录',
          invalidConfirm: '两次输入的新密码不一致。',
          invalidLength: '密码至少需要 8 位。',
          invalidPattern: '用户名需为 3-24 位字母、数字或下划线。',
          hint: '用户名支持字母、数字和下划线，注册后可以在个人资料里修改显示名称和头像。',
          unavailable: '注册暂时不可用，请检查本地 API 代理后再试。'
        }
      : {
          kicker: 'Create Account',
          title: 'Create your study account',
          desc: 'Keep your saved posts, review cards, and community activity in one place, then personalize your profile later.',
          account: 'Username',
          email: 'Email',
          password: 'Password',
          confirmPassword: 'Confirm Password',
          submit: sessionStore.loading ? 'Creating account' : 'Create Account',
          switchText: 'Already have an account?',
          switchAction: 'Sign in',
          invalidConfirm: 'The two passwords do not match.',
          invalidLength: 'Password must be at least 8 characters.',
          invalidPattern: 'Username must be 3-24 letters, numbers, or underscores.',
          hint: 'Usernames support letters, numbers, and underscores. You can update your display name and avatar later.',
          unavailable: 'Registration is temporarily unavailable. Please check the local API proxy and try again.'
        };
  }

  return zh
    ? {
        kicker: 'StudyForge 账号',
        title: '登录后同步你的学习',
        desc: '收藏、阅读记录和复习卡片会跟着账号走，换个设备也能接着看。',
        account: '账号',
        password: '密码',
        submit: sessionStore.loading ? '登录中' : '登录',
        switchText: '还没有账号？',
        switchAction: '创建账号',
        badAuth: '账号或密码不正确。',
        unavailable: '登录暂时不可用，请检查本地 API 代理后再试。',
        hint: '测试账号：chen_jiayi / StudyForge@2026'
      }
    : {
        kicker: 'StudyForge Account',
        title: 'Sign in to sync your learning',
        desc: 'Saved posts, reading history, and review cards stay with your account across devices.',
        account: 'Account',
        password: 'Password',
        submit: sessionStore.loading ? 'Signing in' : 'Sign in',
        switchText: 'Need an account?',
        switchAction: 'Create account',
        badAuth: 'Account or password is incorrect.',
        unavailable: 'Login is temporarily unavailable. Please check the local API proxy and try again.',
        hint: 'Local test account: chen_jiayi / StudyForge@2026'
      };
});

function redirectPath() {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/knowledge';
}

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode;
  errorMessage.value = '';
}

function normalizeApiError(error: unknown) {
  if (error instanceof ApiError) {
    if (error.code === 401 || error.code === 403) {
      return copy.value.badAuth ?? copy.value.unavailable;
    }
    return error.message || copy.value.unavailable;
  }

  return copy.value.unavailable;
}

async function submitLogin() {
  errorMessage.value = '';

  try {
    await sessionStore.login(loginForm);
    sessionStore.syncFromStorage();
    await router.push(redirectPath());
  } catch (error) {
    sessionStore.syncFromStorage();
    errorMessage.value = normalizeApiError(error);
  }
}

async function submitRegister() {
  errorMessage.value = '';

  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = copy.value.invalidConfirm;
    return;
  }

  if (registerForm.password.length < 8) {
    errorMessage.value = copy.value.invalidLength;
    return;
  }

  try {
    await sessionStore.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password
    });
    sessionStore.syncFromStorage();
    await router.push(redirectPath());
  } catch (error) {
    sessionStore.syncFromStorage();
    errorMessage.value = normalizeApiError(error);
  }
}

async function submit() {
  if (mode.value === 'register') {
    await submitRegister();
    return;
  }

  await submitLogin();
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-copy">
      <span class="section-kicker">{{ copy.kicker }}</span>
      <h1>{{ copy.title }}</h1>
      <p>{{ copy.desc }}</p>
    </div>

    <form class="auth-card" @submit.prevent="submit">
      <label v-if="mode === 'login'">
        <span>{{ copy.account }}</span>
        <input v-model.trim="loginForm.account" autocomplete="username" required />
      </label>

      <label v-if="mode === 'login'">
        <span>{{ copy.password }}</span>
        <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
      </label>

      <template v-else>
        <label>
          <span>{{ copy.account }}</span>
          <input
            v-model.trim="registerForm.username"
            autocomplete="username"
            pattern="[A-Za-z0-9_]{3,24}"
            :title="copy.invalidPattern"
            required
          />
        </label>

        <label>
          <span>{{ copy.email }}</span>
          <input v-model.trim="registerForm.email" type="email" autocomplete="email" required />
        </label>

        <label>
          <span>{{ copy.password }}</span>
          <input v-model="registerForm.password" type="password" autocomplete="new-password" minlength="8" required />
        </label>

        <label>
          <span>{{ copy.confirmPassword }}</span>
          <input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" minlength="8" required />
        </label>
      </template>

      <p class="auth-hint">{{ errorMessage || copy.hint }}</p>

      <button class="primary-button full-width" type="submit" :disabled="sessionStore.loading">
        <LogIn v-if="mode === 'login'" :size="18" />
        <UserPlus v-else :size="18" />
        <span>{{ copy.submit }}</span>
      </button>

      <div class="auth-switch">
        <span>{{ copy.switchText }}</span>
        <button v-if="mode === 'login'" type="button" @click="switchMode('register')">{{ copy.switchAction }}</button>
        <button v-else type="button" @click="switchMode('login')">{{ copy.switchAction }}</button>
      </div>
    </form>
  </section>
</template>
