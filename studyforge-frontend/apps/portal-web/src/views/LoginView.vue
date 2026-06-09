<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LogIn } from '@lucide/vue';
import { ApiError } from '@/api/http';
import studyforgeLogo from '@/assets/studyforge-logo-mark.png';
import { usePreferencesStore } from '@/stores/preferences';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const preferencesStore = usePreferencesStore();
const form = reactive({
  account: '',
  password: ''
});
const errorMessage = ref('');

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        title: 'StudyForge Content Console',
        desc: 'Moderate knowledge content, monitor service health, and manage the daily community workflow.',
        panel: 'Account Login',
        account: 'Account',
        password: 'Password',
        submitting: 'Signing in',
        submit: 'Sign in',
        badAuth: 'Account or password is incorrect.',
        expired: 'Your previous session expired. Please sign in again.',
        unavailable: 'Login is temporarily unavailable. Please check the local API proxy and try again.'
      }
    : {
        title: 'StudyForge 内容控制台',
        desc: '管理知识内容、查看服务状态，并进入团队的日常运营工作台。',
        panel: '账号登录',
        account: '账号',
        password: '密码',
        submitting: '登录中',
        submit: '登录',
        badAuth: '账号或密码不正确。',
        expired: '上一份登录会话已失效，请重新登录。',
        unavailable: '登录暂时不可用，请检查本地 API 代理后再试。'
      }
);

function normalizeLoginError(error: unknown) {
  if (error instanceof ApiError) {
    if (error.code === 401 || error.code === 403) {
      return copy.value.badAuth;
    }
    return error.message || copy.value.unavailable;
  }

  return copy.value.unavailable;
}

async function submit() {
  errorMessage.value = '';

  try {
    await authStore.login(form);
    authStore.syncFromStorage();
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/feed';
    await router.push(redirect);
  } catch (error) {
    authStore.syncFromStorage();
    errorMessage.value = normalizeLoginError(error);
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-product">
      <div class="brand-lockup auth-brand">
        <span class="brand-mark">
          <img :src="studyforgeLogo" alt="" />
        </span>
        <span>
          <strong>StudyForge AI</strong>
          <small>Content Console</small>
        </span>
      </div>

      <div class="login-copy">
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.desc }}</p>
      </div>

      <dl class="login-stack">
        <div>
          <dt>Web App</dt>
          <dd>Vue 3 / Vite / TypeScript</dd>
        </div>
        <div>
          <dt>Requests</dt>
          <dd>Ajax / Fetch / Axios</dd>
        </div>
        <div>
          <dt>Service</dt>
          <dd>Spring MVC / Service / MyBatis / MySQL</dd>
        </div>
      </dl>
    </section>

    <section class="login-card" aria-labelledby="login-title">
      <div class="section-heading compact">
        <span>Portal</span>
        <h2 id="login-title">{{ copy.panel }}</h2>
      </div>

      <form class="form-stack" @submit.prevent="submit">
        <label>
          <span>{{ copy.account }}</span>
          <input v-model.trim="form.account" name="account" autocomplete="username" required />
        </label>

        <label>
          <span>{{ copy.password }}</span>
          <input v-model="form.password" name="password" type="password" autocomplete="current-password" required />
        </label>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <button class="primary-button full-width" type="submit" :disabled="authStore.loading">
          <LogIn :size="18" />
          <span>{{ authStore.loading ? copy.submitting : copy.submit }}</span>
        </button>
      </form>
    </section>
  </main>
</template>
