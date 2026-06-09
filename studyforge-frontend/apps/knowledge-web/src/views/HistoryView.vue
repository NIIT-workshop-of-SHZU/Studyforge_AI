<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowLeft, Clock3, RefreshCw } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { getHistoryPosts } from '@/api/posts';
import EmptyState from '@/components/EmptyState.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { PostSummary, TopicCategory } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const posts = ref<PostSummary[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const categories: Record<string, TopicCategory> = {
  TECHNOLOGY: { code: 'TECHNOLOGY', name: '技术实践', description: '前端、后端、工具', accent: '#2563eb' },
  BUSINESS: { code: 'BUSINESS', name: '商业观察', description: '案例、决策、市场', accent: '#7c3aed' },
  PRODUCTIVITY: { code: 'PRODUCTIVITY', name: '效率方法', description: '笔记、复盘、计划', accent: '#b45309' },
  CAREER: { code: 'CAREER', name: '职业成长', description: '求职、面试、成长', accent: '#15803d' },
  FINANCE: { code: 'FINANCE', name: '财务入门', description: '预算、风险、常识', accent: '#0891b2' }
};

function categoryFor(post: PostSummary): TopicCategory {
  return categories[post.categoryCode] ?? { code: post.categoryCode, name: post.categoryCode, description: '', accent: '#0f766e' };
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
      errorMessage.value = '登录状态已过期，请重新登录后查看浏览历史。';
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : '浏览历史暂时打不开';
  } finally {
    loading.value = false;
  }
}

onMounted(loadHistory);

watch(
  () => [sessionStore.isAuthenticated, preferencesStore.languageCode],
  () => loadHistory()
);
</script>

<template>
  <section class="history-page">
    <div class="page-heading with-actions">
      <div>
        <RouterLink class="secondary-button return-link" to="/me">
          <ArrowLeft :size="17" />
          <span>返回主页</span>
        </RouterLink>
        <span class="section-kicker">History</span>
        <h1>历史浏览</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadHistory">
        <RefreshCw :size="17" />
        <span>刷新</span>
      </button>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <Clock3 :size="42" />
      <h2>登录后查看浏览历史</h2>
      <p>你最近读过的文章会显示在这里。</p>
      <RouterLink class="primary-button" to="/login">登录</RouterLink>
    </div>

    <template v-else>
      <LoadingState v-if="loading" label="正在读取浏览历史" />
      <EmptyState v-else-if="errorMessage" title="暂时无法加载" :description="errorMessage" />

      <section v-else-if="posts.length" class="knowledge-grid compact-grid">
        <KnowledgeCard
          v-for="(post, index) in posts"
          :key="`history-${post.postId}-${index}`"
          :post="post"
          :category="categoryFor(post)"
          :index="index"
        />
      </section>
      <EmptyState v-else title="还没有浏览记录" description="打开文章详情后，最近读过会自动记录在这里。" />
    </template>
  </section>
</template>
