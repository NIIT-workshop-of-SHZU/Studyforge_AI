<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { BookOpen, BookmarkCheck, Clock3, LogIn, NotebookTabs, RefreshCw } from '@lucide/vue';
import { getMyReviewCards } from '@/api/ai';
import { getFavoritePosts, getHistoryPosts } from '@/api/posts';
import EmptyState from '@/components/EmptyState.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { AiLogItem, PostSummary, TopicCategory } from '@/types/api';
import { toDate } from '@/utils/date';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const favorites = ref<PostSummary[]>([]);
const history = ref<PostSummary[]>([]);
const reviewCards = ref<AiLogItem[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const localizedFavorites = computed(() => prioritizePosts(favorites.value));
const localizedHistory = computed(() => prioritizePosts(history.value));
const latestFavorite = computed(() => localizedFavorites.value[0] ?? null);
const latestHistory = computed(() => localizedHistory.value[0] ?? null);
const continuePost = computed(() => prioritizePosts([...history.value, ...favorites.value])[0] ?? null);
const latestReviewCard = computed(() => reviewCards.value[0] ?? null);
const totalSavedInteractions = computed(() => favorites.value.reduce((sum, post) => sum + post.favoriteCount + post.commentCount, 0));

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        title: 'My Study',
        loginTitle: 'Sign in to view your study space',
        loginDesc: 'Saved posts, recent reading, and review cards all land here.',
        login: 'Log in',
        continueTitle: 'Continue',
        continueFallbackTitle: 'Pick a post from the feed to get started',
        continueFallbackDesc: 'Read posts, save them, and generate review cards. Everything will be gathered here.',
        continueReading: 'Continue Reading',
        organizeCollections: 'Organize Collections',
        favorites: 'Saved Posts',
        history: 'Recent Reads',
        reviewCards: 'Review Cards',
        boardTitle: 'Study Board',
        refresh: 'Refresh',
        latestFavorite: 'Latest Favorite',
        noFavorite: 'No saved posts yet',
        noFavoriteDesc: 'Save a post from the article page and it will show up here.',
        latestHistory: 'Recently Read',
        noHistory: 'No reading history yet',
        noHistoryDesc: 'Open a post detail page and it will be tracked here automatically.',
        interactionTitle: 'Signals from your saved content',
        interactionDesc: 'Use favorites and discussion counts to spot which posts are worth revisiting.',
        reviewPanel: 'Review Cards',
        noReviewPanel: 'Generate review cards on a post detail page and the latest one will appear here.',
        loading: 'Loading your study records',
        unavailable: 'Unable to load study records right now',
        savedSection: 'Saved Posts',
        savedEmpty: 'No saved posts yet',
        savedEmptyDesc: 'Save a post from the article page and it will appear here.',
        historySection: 'Recently Read',
        historyEmpty: 'No reading history yet',
        historyEmptyDesc: 'Open a post detail page and recent reads will be recorded automatically.',
        reviewSection: 'Review Cards',
        reviewEmpty: 'No review cards yet',
        reviewEmptyDesc: 'Generate review cards from a post detail page and they will be stored here.',
        studyOverview: 'My Study',
        collectionsNav: 'Collections',
        memoryNav: 'MEMORY.md'
      }
    : {
        title: '我的学习',
        loginTitle: '登录后查看你的学习',
        loginDesc: '保存的文章、最近读过的内容和复习卡片都会放在这里。',
        login: '登录',
        continueTitle: 'Continue',
        continueFallbackTitle: '从知识流里挑一篇文章开始',
        continueFallbackDesc: '阅读过的内容、收藏的文章和生成的复习卡片会在这里汇总。',
        continueReading: '继续阅读',
        organizeCollections: '整理收藏夹',
        favorites: '收藏文章',
        history: '最近读过',
        reviewCards: '复习卡片',
        boardTitle: '学习看板',
        refresh: '刷新',
        latestFavorite: '最新收藏',
        noFavorite: '还没有收藏文章',
        noFavoriteDesc: '在文章页点击收藏后，会出现在这里。',
        latestHistory: '最近阅读',
        noHistory: '还没有阅读记录',
        noHistoryDesc: '打开文章详情后，会自动记录最近读过。',
        interactionTitle: '保存内容带来的互动',
        interactionDesc: '来自你收藏文章的收藏数和讨论数，方便判断哪些内容值得回看。',
        reviewPanel: '复习卡片',
        noReviewPanel: '在文章详情里生成复习卡片后，最新的一张会显示在这里。',
        loading: '正在整理学习记录',
        unavailable: '暂时无法加载',
        savedSection: '收藏的文章',
        savedEmpty: '还没有收藏',
        savedEmptyDesc: '在文章页点击收藏后，会出现在这里。',
        historySection: '最近读过',
        historyEmpty: '还没有阅读记录',
        historyEmptyDesc: '打开文章详情后，最近读过会自动记录。',
        reviewSection: '复习卡片',
        reviewEmpty: '还没有复习卡片',
        reviewEmptyDesc: '在文章详情页生成复习卡片后，会保存到这里。',
        studyOverview: '我的学习',
        collectionsNav: '收藏夹',
        memoryNav: 'MEMORY.md'
      }
);

const category = computed<TopicCategory>(() => ({
  code: 'STUDY',
  name: preferencesStore.languageCode === 'en_US' ? 'My Study' : '我的内容',
  description: preferencesStore.languageCode === 'en_US' ? 'Saved content' : '已保存',
  accent: '#0f766e'
}));

let libraryRequestId = 0;

function isCurrentLibraryRequest(requestId: number, userId: number) {
  return requestId === libraryRequestId && sessionStore.isAuthenticated && sessionStore.userId === userId;
}

function resetLibrary() {
  favorites.value = [];
  history.value = [];
  reviewCards.value = [];
  errorMessage.value = '';
  loading.value = false;
}

function isPreferredLanguage(post: PostSummary) {
  return post.languageCode === preferencesStore.languageCode;
}

function prioritizePosts(posts: PostSummary[]) {
  return posts
    .map((post, index) => ({ post, index }))
    .sort(
      (first, second) =>
        Number(!isPreferredLanguage(first.post)) - Number(!isPreferredLanguage(second.post)) ||
        postTimestamp(second.post) - postTimestamp(first.post) ||
        first.index - second.index
    )
    .map(({ post }) => post);
}

function postTimestamp(post: PostSummary) {
  return toDate(post.activityTime ?? post.createdTime)?.getTime() ?? 0;
}

async function loadLibrary() {
  const requestId = ++libraryRequestId;
  const userId = sessionStore.userId;

  if (!sessionStore.isAuthenticated || userId === null) {
    resetLibrary();
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

    if (!isCurrentLibraryRequest(requestId, userId)) {
      return;
    }

    favorites.value = favoriteData;
    history.value = historyData;
    reviewCards.value = cardData;
  } catch (error) {
    if (isCurrentLibraryRequest(requestId, userId)) {
      errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
    }
  } finally {
    if (isCurrentLibraryRequest(requestId, userId)) {
      loading.value = false;
    }
  }
}

onMounted(loadLibrary);

watch(
  () => [sessionStore.isAuthenticated, sessionStore.userId, preferencesStore.languageCode],
  () => loadLibrary()
);
</script>

<template>
  <section class="library-page">
    <div class="page-heading">
      <span class="section-kicker">My Study</span>
      <h1>{{ copy.title }}</h1>
    </div>

    <nav class="study-subnav" :aria-label="copy.title">
      <RouterLink to="/library">{{ copy.studyOverview }}</RouterLink>
      <RouterLink to="/favorites">{{ copy.collectionsNav }}</RouterLink>
      <RouterLink to="/memory">{{ copy.memoryNav }}</RouterLink>
    </nav>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <NotebookTabs :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">
        <LogIn :size="17" />
        <span>{{ copy.login }}</span>
      </RouterLink>
    </div>

    <template v-else>
      <div class="library-dashboard">
        <section class="library-focus-panel">
          <div class="focus-copy">
            <span>{{ copy.continueTitle }}</span>
            <h2>{{ continuePost?.title || copy.continueFallbackTitle }}</h2>
            <p>{{ continuePost?.summary || copy.continueFallbackDesc }}</p>
          </div>
          <div class="focus-actions">
            <RouterLink
              v-if="continuePost"
              class="primary-button"
              :to="{ path: `/posts/${continuePost.postId}`, query: { language: continuePost.languageCode } }"
            >
              <BookOpen :size="17" />
              <span>{{ copy.continueReading }}</span>
            </RouterLink>
            <RouterLink class="secondary-button" to="/favorites">
              <BookmarkCheck :size="17" />
              <span>{{ copy.organizeCollections }}</span>
            </RouterLink>
            <RouterLink class="secondary-button" :to="{ path: '/memory', query: { from: 'library' } }">
              <NotebookTabs :size="17" />
              <span>MEMORY.md</span>
            </RouterLink>
          </div>
        </section>

        <section class="library-stat-card">
          <BookmarkCheck :size="21" />
          <span>{{ copy.favorites }}</span>
          <strong>{{ favorites.length }}</strong>
        </section>
        <section class="library-stat-card">
          <Clock3 :size="21" />
          <span>{{ copy.history }}</span>
          <strong>{{ history.length }}</strong>
        </section>
        <section class="library-stat-card">
          <NotebookTabs :size="21" />
          <span>{{ copy.reviewCards }}</span>
          <strong>{{ reviewCards.length }}</strong>
        </section>
      </div>

      <div class="library-workspace">
        <section class="library-board-main">
          <div class="board-header">
            <div>
              <span>Reading Queue</span>
              <h2>{{ copy.boardTitle }}</h2>
            </div>
            <button class="secondary-button" type="button" :disabled="loading" @click="loadLibrary">
              <RefreshCw :size="17" />
              <span>{{ copy.refresh }}</span>
            </button>
          </div>
          <div class="library-board-grid">
            <article class="library-board-card">
              <span>{{ copy.latestFavorite }}</span>
              <strong>{{ latestFavorite?.title || copy.noFavorite }}</strong>
              <p>{{ latestFavorite?.summary || copy.noFavoriteDesc }}</p>
            </article>
            <article class="library-board-card">
              <span>{{ copy.latestHistory }}</span>
              <strong>{{ latestHistory?.title || copy.noHistory }}</strong>
              <p>{{ latestHistory?.summary || copy.noHistoryDesc }}</p>
            </article>
            <article class="library-board-card">
              <span>{{ copy.interactionTitle }}</span>
              <strong>{{ totalSavedInteractions }}</strong>
              <p>{{ copy.interactionDesc }}</p>
            </article>
          </div>
        </section>

        <aside class="library-review-panel">
          <div class="panel-title">
            <NotebookTabs :size="18" />
            <span>{{ copy.reviewPanel }}</span>
          </div>
          <MarkdownRenderer
            v-if="latestReviewCard"
            class="review-card-markdown"
            :content="latestReviewCard.responseText"
          />
          <p v-else>{{ copy.noReviewPanel }}</p>
        </aside>
      </div>
    </template>

    <LoadingState v-if="sessionStore.isAuthenticated && loading" :label="copy.loading" />
    <EmptyState v-else-if="sessionStore.isAuthenticated && errorMessage" :title="copy.unavailable" :description="errorMessage" />

    <section v-if="sessionStore.isAuthenticated && !loading" class="library-sections">
      <div class="library-section">
        <h2>{{ copy.savedSection }}</h2>
        <div v-if="localizedFavorites.length" class="knowledge-grid compact-grid">
          <KnowledgeCard v-for="(post, index) in localizedFavorites" :key="post.postId" :post="post" :category="category" :index="index" />
        </div>
        <EmptyState v-else :title="copy.savedEmpty" :description="copy.savedEmptyDesc" />
      </div>

      <div class="library-section">
        <h2>{{ copy.historySection }}</h2>
        <div v-if="localizedHistory.length" class="knowledge-grid compact-grid">
          <KnowledgeCard v-for="(post, index) in localizedHistory" :key="`history-${post.postId}-${index}`" :post="post" :category="category" :index="index" />
        </div>
        <EmptyState v-else :title="copy.historyEmpty" :description="copy.historyEmptyDesc" />
      </div>

      <div class="library-section">
        <h2>{{ copy.reviewSection }}</h2>
        <div v-if="reviewCards.length" class="review-card-list">
          <article v-for="card in reviewCards" :key="card.logId" class="review-card-item">
            <span>#{{ card.postId }}</span>
            <MarkdownRenderer class="review-card-markdown" :content="card.responseText" />
          </article>
        </div>
        <EmptyState v-else :title="copy.reviewEmpty" :description="copy.reviewEmptyDesc" />
      </div>
    </section>
  </section>
</template>
