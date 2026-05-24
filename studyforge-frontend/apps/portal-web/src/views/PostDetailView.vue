<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { ArrowLeft, BadgeCheck, Flame } from '@lucide/vue';
import { getPostDetail } from '@/api/posts';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { PostDetail } from '@/types/api';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const post = ref<PostDetail | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const postId = computed(() => String(route.params.postId));

async function loadDetail() {
  loading.value = true;
  errorMessage.value = '';

  try {
    post.value = await getPostDetail(postId.value, preferencesStore.languageCode);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '这篇内容暂时没取到';
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);

watch(
  () => [postId.value, preferencesStore.languageCode],
  () => loadDetail()
);
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <RouterLink class="secondary-button" to="/feed">
        <ArrowLeft :size="17" />
        返回
      </RouterLink>
    </div>

    <LoadingState v-if="loading" label="正在打开文章" />
    <EmptyState v-else-if="errorMessage" title="文章暂时打不开" :description="errorMessage" />

    <article v-else-if="post" class="detail-surface">
      <div class="detail-header">
        <div class="post-meta">
          <span class="score-pill">
            <Flame :size="15" />
            {{ post.hotScore.toFixed(1) }}
          </span>
          <span>{{ post.languageCode }}</span>
          <span>{{ post.categoryCode }}</span>
        </div>

        <h1>{{ post.title }}</h1>
        <p>{{ post.summary }}</p>
      </div>

      <div class="detail-body">
        <p>{{ post.content }}</p>
      </div>

      <footer class="detail-footer">
        <span>
          <BadgeCheck :size="16" />
          作者 #{{ post.authorId }}
        </span>
        <span>内容 #{{ post.postId }}</span>
      </footer>
    </article>
  </section>
</template>
