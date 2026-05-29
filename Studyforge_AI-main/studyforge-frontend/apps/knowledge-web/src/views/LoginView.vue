<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LogIn } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const errorMessage = ref('');
const form = reactive({
  account: 'chen_jiayi',
  password: 'StudyForge@2026'
});

async function submit() {
  errorMessage.value = '';

  try {
    await sessionStore.login(form);
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/knowledge';
    await router.push(redirect);
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败';
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-copy">
      <span class="section-kicker">StudyForge Account</span>
      <h1>登录后同步你的学习</h1>
      <p>收藏、阅读记录和复习卡片会跟着账号走，换个设备也能接着看。</p>
    </div>

    <form class="auth-card" @submit.prevent="submit">
      <label>
        <span>账号</span>
        <input v-model.trim="form.account" autocomplete="username" required />
      </label>

      <label>
        <span>密码</span>
        <input v-model="form.password" type="password" autocomplete="current-password" required />
      </label>

      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <button class="primary-button full-width" type="submit" :disabled="sessionStore.loading">
        <LogIn :size="18" />
        <span>{{ sessionStore.loading ? '登录中' : '登录' }}</span>
      </button>
    </form>
  </section>
</template>
