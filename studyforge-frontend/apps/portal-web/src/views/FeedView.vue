<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { Flame, RefreshCw, Search } from '@lucide/vue';
import { getTrendingPosts } from '@/api/posts';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { PostSummary } from '@/types/api';

const preferencesStore = usePreferencesStore();
const posts = ref<PostSummary[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const limit = ref(10);

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      eyebrow: 'Content Library',
      title: 'Content Feed',
      limit: 'Limit',
      refresh: 'Refresh',
      loading: 'Loading content',
      unavailable: 'Content is unavailable',
      empty: 'No content yet',
      emptyDesc: 'No articles are available in the current language.',
      open: 'Open'
    };
  }
  return {
    eyebrow: 'Content Library',
    title: '内容流',
    limit: '显示数量',
    refresh: '刷新',
    loading: '正在整理内容',
    unavailable: '内容暂时不可用',
    empty: '暂无内容',
    emptyDesc: '当前语言下还没有可展示的文章。',
    open: '查看'
  };
});

async function loadPosts() {
  loading.value = true;
  errorMessage.value = '';

  try {
    posts.value = await getTrendingPosts({
      languageCode: preferencesStore.languageCode,
      limit: limit.value
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

onMounted(loadPosts);

watch(
  () => preferencesStore.languageCode,
  () => loadPosts()
);
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <div class="section-heading">
        <span>{{ copy.eyebrow }}</span>
        <h1>{{ copy.title }}</h1>
      </div>

      <div class="toolbar">
        <label class="inline-input">
          <Search :size="17" />
          <span>{{ copy.limit }}</span>
          <input v-model.number="limit" type="number" min="1" max="20" @change="loadPosts" />
        </label>
        <button class="secondary-button" type="button" :disabled="loading" @click="loadPosts">
          <RefreshCw :size="17" />
          <span>{{ copy.refresh }}</span>
        </button>
      </div>
    </div>

    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.unavailable" :description="errorMessage" />
    <EmptyState v-else-if="posts.length === 0" :title="copy.empty" :description="copy.emptyDesc" />

    <div v-else class="post-list">
      <article v-for="(post, index) in posts" :key="`${post.postId}-${index}`" class="post-item">
        <div class="post-main">
          <div class="post-meta">
            <span class="score-pill">
              <Flame :size="15" />
              {{ post.hotScore.toFixed(1) }}
            </span>
            <span>{{ post.languageCode }}</span>
          </div>
          <h2>{{ post.title }}</h2>
          <p>{{ post.summary }}</p>
        </div>

        <RouterLink class="secondary-button stable-action" :to="`/posts/${post.postId}`">
          {{ copy.open }}
        </RouterLink>
      </article>
    </div>
  </section>
</template>
