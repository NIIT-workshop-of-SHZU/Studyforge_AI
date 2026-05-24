<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { BookmarkCheck, Clock3, LogIn, NotebookTabs, RefreshCw } from '@lucide/vue';
import { getMyReviewCards } from '@/api/ai';
import { getFavoritePosts, getHistoryPosts } from '@/api/posts';
import EmptyState from '@/components/EmptyState.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { AiLogItem, PostSummary, TopicCategory } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const favorites = ref<PostSummary[]>([]);
const history = ref<PostSummary[]>([]);
const reviewCards = ref<AiLogItem[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const category: TopicCategory = {
  code: 'STUDY',
  name: '我的内容',
  description: '已保存',
  accent: '#0f766e'
};

async function loadLibrary() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';

  try {
    const [favoriteData, historyData, cardData] = await Promise.all([
      getFavoritePosts(preferencesStore.languageCode),
      getHistoryPosts(preferencesStore.languageCode),
      getMyReviewCards()
    ]);
    favorites.value = favoriteData;
    history.value = historyData;
    reviewCards.value = cardData;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '学习记录暂时没取到';
  } finally {
    loading.value = false;
  }
}

onMounted(loadLibrary);

watch(
  () => [sessionStore.isAuthenticated, preferencesStore.languageCode],
  () => loadLibrary()
);
</script>

<template>
  <section class="library-page">
    <div class="page-heading">
      <span class="section-kicker">My Study</span>
      <h1>我的学习</h1>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <NotebookTabs :size="42" />
      <h2>登录后查看你的学习</h2>
      <p>保存的文章、最近读过的内容和复习卡片都会放在这里。</p>
      <RouterLink class="primary-button" to="/login">
        <LogIn :size="17" />
        <span>登录</span>
      </RouterLink>
    </div>

    <div v-else class="library-grid">
      <section class="metric-card">
        <BookmarkCheck :size="22" />
        <span>已收藏</span>
        <strong>{{ favorites.length }}</strong>
      </section>
      <section class="metric-card">
        <Clock3 :size="22" />
        <span>最近读过</span>
        <strong>{{ history.length }}</strong>
      </section>
      <section class="metric-card">
        <NotebookTabs :size="22" />
        <span>复习卡片</span>
        <strong>{{ reviewCards.length }}</strong>
      </section>
    </div>

    <div v-if="sessionStore.isAuthenticated" class="feed-toolbar">
      <strong>学习内容</strong>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadLibrary">
        <RefreshCw :size="17" />
        <span>刷新</span>
      </button>
    </div>

    <LoadingState v-if="sessionStore.isAuthenticated && loading" label="正在整理学习记录" />
    <EmptyState v-else-if="sessionStore.isAuthenticated && errorMessage" title="暂时无法加载" :description="errorMessage" />

    <section v-if="sessionStore.isAuthenticated && !loading" class="library-sections">
      <div class="library-section">
        <h2>收藏的文章</h2>
        <div v-if="favorites.length" class="knowledge-grid compact-grid">
          <KnowledgeCard v-for="(post, index) in favorites" :key="post.postId" :post="post" :category="category" :index="index" />
        </div>
        <EmptyState v-else title="还没有收藏" description="在文章页点击收藏后，会出现在这里。" />
      </div>

      <div class="library-section">
        <h2>最近读过</h2>
        <div v-if="history.length" class="knowledge-grid compact-grid">
          <KnowledgeCard v-for="(post, index) in history" :key="`history-${post.postId}-${index}`" :post="post" :category="category" :index="index" />
        </div>
        <EmptyState v-else title="还没有阅读记录" description="打开文章详情后，最近读过会自动记录。" />
      </div>

      <div class="library-section">
        <h2>复习卡片</h2>
        <div v-if="reviewCards.length" class="review-card-list">
          <article v-for="card in reviewCards" :key="card.logId" class="review-card-item">
            <span>#{{ card.postId }}</span>
            <pre>{{ card.responseText }}</pre>
          </article>
        </div>
        <EmptyState v-else title="还没有复习卡片" description="在文章详情页生成复习卡片后，会保存到这里。" />
      </div>
    </section>
  </section>
</template>
