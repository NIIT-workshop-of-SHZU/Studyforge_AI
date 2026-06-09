<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { Archive, ArchiveRestore, ArrowLeft, BadgeCheck, Flame, Pin } from '@lucide/vue';
import { getAdminPostDetail, updatePostFeatured, updatePostStatus } from '@/api/community';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { AdminPost } from '@/types/api';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const post = ref<AdminPost | null>(null);
const loading = ref(false);
const saving = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const actionRemark = ref('');
const postId = computed(() => String(route.params.postId));

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        back: 'Back to Community',
        loading: 'Opening post',
        unavailable: 'This post is unavailable',
        remark: 'Moderation Note',
        remarkPlaceholder: 'Write why this post is featured, archived, or restored',
        unfeature: 'Remove Feature',
        feature: 'Feature Post',
        archive: 'Archive',
        restore: 'Restore Publication',
        featured: 'Featured',
        successFeature: 'Post is now featured.',
        successUnfeature: 'Post is no longer featured.',
        successArchive: 'Post archived.',
        successRestore: 'Post restored to published state.',
        failedLoad: 'This content could not be loaded.',
        failedFeature: 'Featured state could not be updated.',
        failedStatus: 'Post status could not be updated.',
        author: 'Author',
        contentId: 'Content'
      }
    : {
        back: '返回社区管理',
        loading: '正在打开文章',
        unavailable: '文章暂时打不开',
        remark: '处理备注',
        remarkPlaceholder: '写下置顶、下架或恢复的原因',
        unfeature: '取消置顶',
        feature: '置顶文章',
        archive: '下架',
        restore: '恢复发布',
        featured: '置顶',
        successFeature: '文章已置顶',
        successUnfeature: '文章已取消置顶',
        successArchive: '文章已下架',
        successRestore: '文章已恢复发布',
        failedLoad: '这篇内容暂时没取到',
        failedFeature: '置顶状态没有更新成功',
        failedStatus: '文章状态没有更新成功',
        author: '作者',
        contentId: '内容'
      }
);

async function loadDetail() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    post.value = await getAdminPostDetail(postId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.failedLoad;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);

watch(
  () => postId.value,
  () => loadDetail()
);

async function setFeatured() {
  if (!post.value) {
    return;
  }

  saving.value = 'featured';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    post.value = await updatePostFeatured(post.value.postId, !post.value.featured, actionRemark.value);
    successMessage.value = post.value.featured ? copy.value.successFeature : copy.value.successUnfeature;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.failedFeature;
  } finally {
    saving.value = '';
  }
}

async function setStatus(status: string) {
  if (!post.value) {
    return;
  }

  saving.value = status;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    post.value = await updatePostStatus(post.value.postId, status, actionRemark.value);
    successMessage.value = status === 'ARCHIVED' ? copy.value.successArchive : copy.value.successRestore;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.failedStatus;
  } finally {
    saving.value = '';
  }
}

function statusClass(status: string) {
  return `state-${status.toLowerCase()}`;
}
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <RouterLink class="secondary-button" to="/community">
        <ArrowLeft :size="17" />
        {{ copy.back }}
      </RouterLink>
    </div>

    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.unavailable" :description="errorMessage" />

    <article v-else-if="post" class="detail-surface">
      <div class="detail-header">
        <div class="post-meta">
          <span class="score-pill">
            <Flame :size="15" />
            {{ post.hotScore.toFixed(1) }}
          </span>
          <span class="state-badge" :class="statusClass(post.status)">{{ post.status }}</span>
          <span v-if="post.featured" class="state-badge state-featured">{{ copy.featured }}</span>
          <span>{{ post.languageCode }}</span>
          <span>{{ post.categoryCode }}</span>
        </div>

        <h1>{{ post.title }}</h1>
        <p>{{ post.summary }}</p>

        <div class="detail-actions">
          <label class="moderation-remark compact">
            <span>{{ copy.remark }}</span>
            <input v-model.trim="actionRemark" type="text" :placeholder="copy.remarkPlaceholder" />
          </label>
          <button class="secondary-button" type="button" :disabled="saving === 'featured'" @click="setFeatured">
            <Pin :size="16" />
            <span>{{ post.featured ? copy.unfeature : copy.feature }}</span>
          </button>
          <button class="secondary-button danger-action" type="button" :disabled="post.status === 'ARCHIVED' || saving === 'ARCHIVED'" @click="setStatus('ARCHIVED')">
            <Archive :size="16" />
            <span>{{ copy.archive }}</span>
          </button>
          <button class="secondary-button" type="button" :disabled="post.status === 'PUBLISHED' || saving === 'PUBLISHED'" @click="setStatus('PUBLISHED')">
            <ArchiveRestore :size="16" />
            <span>{{ copy.restore }}</span>
          </button>
        </div>
        <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
      </div>

      <div class="detail-body">
        <MarkdownRenderer :content="post.content" />
      </div>

      <footer class="detail-footer">
        <span>
          <BadgeCheck :size="16" />
          {{ post.authorName }} · {{ copy.author }} #{{ post.authorId }}
        </span>
        <span>{{ copy.contentId }} #{{ post.postId }}</span>
      </footer>
    </article>
  </section>
</template>
