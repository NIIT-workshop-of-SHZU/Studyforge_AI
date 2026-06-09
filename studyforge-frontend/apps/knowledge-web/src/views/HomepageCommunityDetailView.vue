<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { Copy, UserRound } from '@lucide/vue';
import { cloneCommunityHomepage, getCommunityHomepage } from '@/api/homepages';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { CommunityHomepageDesign } from '@/types/api';

const route = useRoute();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const detail = ref<CommunityHomepageDesign | null>(null);
const loading = ref(false);
const cloning = ref(false);
const errorMessage = ref('');

const designId = computed(() => Number(route.params.designId));

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      loading: 'Loading homepage design details',
      unavailable: 'Homepage design details are unavailable',
      eyebrow: 'Template',
      back: 'Back to gallery',
      cloning: 'Cloning',
      clone: 'Clone into my homepage draft',
      summaryFallback: 'This design does not have a detailed summary yet.',
      clones: 'Clones',
      favorites: 'Favorites',
      likes: 'Likes',
      comments: 'Comments',
      source: 'Source & layout notes',
      loginFirst: 'Please log in first, then clone this design.',
      cloneFailed: 'Cloning the homepage design failed.'
    };
  }

  return {
    loading: '正在读取主页设计详情',
    unavailable: '主页设计详情暂时不可用',
    eyebrow: 'Template',
    back: '返回列表',
    cloning: '克隆中',
    clone: '克隆到我的主页草稿',
    summaryFallback: '这套主页设计还没有填写详细说明。',
    clones: '克隆',
    favorites: '收藏',
    likes: '点赞',
    comments: '评论',
    source: '源码与布局说明',
    loginFirst: '请先登录，再克隆这套主页设计。',
    cloneFailed: '克隆主页设计失败。'
  };
});

async function loadDetail() {
  loading.value = true;
  errorMessage.value = '';
  try {
    detail.value = await getCommunityHomepage(designId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

async function cloneDesign() {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginFirst;
    return;
  }
  cloning.value = true;
  errorMessage.value = '';
  try {
    await cloneCommunityHomepage(designId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.cloneFailed;
  } finally {
    cloning.value = false;
  }
}

onMounted(loadDetail);
</script>

<template>
  <section class="page-section">
    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage && !detail" :title="copy.unavailable" :description="errorMessage" />

    <div v-else-if="detail" class="homepage-detail-layout">
      <div class="page-header">
        <div class="section-heading">
          <span>{{ copy.eyebrow }}</span>
          <h1>{{ detail.title }}</h1>
        </div>
        <div class="table-actions">
          <RouterLink class="secondary-button" to="/homepages">{{ copy.back }}</RouterLink>
          <button class="primary-button" type="button" :disabled="cloning || !detail.canClone" @click="cloneDesign">
            <Copy :size="16" />
            <span>{{ cloning ? copy.cloning : copy.clone }}</span>
          </button>
        </div>
      </div>

      <section class="profile-hero">
        <div class="profile-identity">
          <div class="profile-avatar">
            <img v-if="detail.authorAvatarUrl" :src="detail.authorAvatarUrl" alt="" />
            <UserRound v-else :size="42" />
          </div>
          <div>
            <div class="profile-title-row">
              <h1>{{ detail.authorName }}</h1>
              <span class="level-badge">{{ detail.layoutMode }}</span>
            </div>
            <p class="profile-bio">{{ detail.summary || copy.summaryFallback }}</p>
          </div>
        </div>
      </section>

      <section class="profile-stats homepage-template-stats">
        <div><strong>{{ detail.cloneCount }}</strong><span>{{ copy.clones }}</span></div>
        <div><strong>{{ detail.favoriteCount }}</strong><span>{{ copy.favorites }}</span></div>
        <div><strong>{{ detail.likeCount }}</strong><span>{{ copy.likes }}</span></div>
        <div><strong>{{ detail.commentCount }}</strong><span>{{ copy.comments }}</span></div>
      </section>

      <section class="account-panel">
        <div class="panel-title"><span>{{ copy.source }}</span></div>
        <pre class="homepage-code-dump">{{ detail.layoutMode === 'code' ? detail.customCode : detail.mediaLayout }}</pre>
      </section>
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    </div>
  </section>
</template>
