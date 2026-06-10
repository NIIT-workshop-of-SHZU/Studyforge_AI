<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowLeft, Clock3, RefreshCw } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { getHistoryPosts } from '@/api/posts';
import EmptyState from '@/components/EmptyState.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import { categoryForCode } from '@/i18n/categories';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { PostSummary } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const posts = ref<PostSummary[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      back: 'Back to profile',
      kicker: 'History',
      title: 'Reading history',
      refresh: 'Refresh',
      loginTitle: 'Sign in to view history',
      loginDesc: 'Posts you opened recently will appear here.',
      login: 'Log in',
      loading: 'Loading history',
      loadErrorTitle: 'History unavailable',
      loadErrorFallback: 'Reading history could not be loaded.',
      sessionExpired: 'Your session expired. Please sign in again to view history.',
      emptyTitle: 'No history yet',
      emptyDesc: 'Posts you open will be recorded here.'
    };
  }

  return {
    back: '返回主页',
    kicker: 'History',
    title: '历史浏览',
    refresh: '刷新',
    loginTitle: '登录后查看历史',
    loginDesc: '你最近打开过的文章会显示在这里。',
    login: '登录',
    loading: '正在读取浏览历史',
    loadErrorTitle: '浏览历史暂时打不开',
    loadErrorFallback: '浏览历史暂时打不开',
    sessionExpired: '登录状态已过期，请重新登录后查看浏览历史。',
    emptyTitle: '还没有浏览记录',
    emptyDesc: '打开文章后会自动记录在这里。'
  };
});

function categoryFor(post: PostSummary) {
  return categoryForCode(preferencesStore.languageCode, post.categoryCode);
}

async function loadHistory() {
  if (!sessionStore.isAuthenticated) {
    posts.value = [];
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    posts.value = await getHistoryPosts(preferencesStore.languageCode);
  } catch (error) {
    if (error instanceof ApiError && error.code === 4010) {
      await sessionStore.logout();
      errorMessage.value = copy.value.sessionExpired;
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadErrorFallback;
  } finally {
    loading.value = false;
  }
}

onMounted(loadHistory);

watch(
  () => sessionStore.isAuthenticated,
  () => loadHistory()
);
</script>

<template>
  <section class="history-page">
    <div class="page-heading with-actions">
      <div>
        <RouterLink class="secondary-button return-link" to="/me">
          <ArrowLeft :size="17" />
          <span>{{ copy.back }}</span>
        </RouterLink>
        <span class="section-kicker">{{ copy.kicker }}</span>
        <h1>{{ copy.title }}</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadHistory">
        <RefreshCw :size="17" />
        <span>{{ copy.refresh }}</span>
      </button>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <Clock3 :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">{{ copy.login }}</RouterLink>
    </div>

    <template v-else>
      <LoadingState v-if="loading" :label="copy.loading" />
      <EmptyState v-else-if="errorMessage" :title="copy.loadErrorTitle" :description="errorMessage" />
      <section v-else class="knowledge-grid compact-grid">
        <KnowledgeCard v-for="(post, index) in posts" :key="post.postId" :post="post" :category="categoryFor(post)" :index="index" />
        <EmptyState v-if="posts.length === 0" :title="copy.emptyTitle" :description="copy.emptyDesc" />
      </section>
    </template>
  </section>
</template>
