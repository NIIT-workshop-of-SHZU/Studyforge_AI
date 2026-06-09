<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { Copy, Search, Sparkles } from '@lucide/vue';
import { cloneCommunityHomepage, listCommunityHomepages } from '@/api/homepages';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { CommunityHomepageDesign } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const items = ref<CommunityHomepageDesign[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const keyword = ref('');
const cloningId = ref<number | null>(null);

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      eyebrow: 'Community',
      title: 'Homepage Gallery',
      designMine: 'Open my homepage studio',
      search: 'Search',
      searchPlaceholder: 'Search by title or author',
      loading: 'Loading community homepage designs',
      unavailable: 'Community homepage designs are unavailable',
      untitledSummary: 'This design does not have a summary yet.',
      clones: 'clones',
      favorites: 'favorites',
      details: 'View details',
      cloning: 'Cloning',
      cloneToDraft: 'Clone into my draft',
      loginFirst: 'Please log in first, then clone this design into your own homepage draft.',
      cloneFailed: 'Cloning the homepage design failed.'
    };
  }

  return {
    eyebrow: 'Community',
    title: '主页设计专区',
    designMine: '去设计我的主页',
    search: '搜索',
    searchPlaceholder: '按模板标题或作者搜索',
    loading: '正在读取社区主页设计',
    unavailable: '社区主页设计暂时不可用',
    untitledSummary: '这套主页还没有补充说明。',
    clones: '次克隆',
    favorites: '收藏',
    details: '查看详情',
    cloning: '克隆中',
    cloneToDraft: '克隆为我的草稿',
    loginFirst: '请先登录，再把这套主页克隆到自己的主页草稿。',
    cloneFailed: '克隆主页设计失败。'
  };
});

const emptyDescription = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return keyword.value
      ? 'Try another keyword, or publish the first approved homepage design.'
      : 'Homepage designs approved by community admins appear here.';
  }
  return keyword.value ? '换个关键词试试，或者先发布第一套通过审核的主页设计。' : '社区管理员审核通过的主页设计会在这里出现。';
});

async function loadList() {
  loading.value = true;
  errorMessage.value = '';
  try {
    items.value = await listCommunityHomepages(keyword.value, 30);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

async function cloneDesign(designId: number) {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginFirst;
    return;
  }
  cloningId.value = designId;
  errorMessage.value = '';
  try {
    await cloneCommunityHomepage(designId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.cloneFailed;
  } finally {
    cloningId.value = null;
  }
}

onMounted(loadList);
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <div class="section-heading">
        <span>{{ copy.eyebrow }}</span>
        <h1>{{ copy.title }}</h1>
      </div>
      <RouterLink class="secondary-button" to="/homepage-studio">
        <Sparkles :size="17" />
        <span>{{ copy.designMine }}</span>
      </RouterLink>
    </div>

    <label class="inline-input wide-input">
      <Search :size="17" />
      <span>{{ copy.search }}</span>
      <input v-model.trim="keyword" type="search" :placeholder="copy.searchPlaceholder" @keyup.enter="loadList" />
    </label>

    <LoadingState v-if="loading" :label="copy.loading" />
    <p v-else-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div v-else class="homepage-design-grid">
      <article v-for="item in items" :key="item.designId" class="homepage-design-card">
        <div class="homepage-design-meta">
          <strong>{{ item.title }}</strong>
          <span>by {{ item.authorName }}</span>
        </div>
        <p>{{ item.summary || copy.untitledSummary }}</p>
        <div class="homepage-design-stats">
          <span>{{ item.layoutMode }}</span>
          <span>{{ item.cloneCount }} {{ copy.clones }}</span>
          <span>{{ item.favoriteCount }} {{ copy.favorites }}</span>
        </div>
        <div class="table-actions">
          <RouterLink class="secondary-button" :to="`/homepages/${item.designId}`">{{ copy.details }}</RouterLink>
          <button class="primary-button" type="button" :disabled="cloningId === item.designId || !item.canClone" @click="cloneDesign(item.designId)">
            <Copy :size="16" />
            <span>{{ cloningId === item.designId ? copy.cloning : copy.cloneToDraft }}</span>
          </button>
        </div>
      </article>
      <EmptyState v-if="items.length === 0" :title="copy.title" :description="emptyDescription" />
    </div>
  </section>
</template>
