<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowLeft, RefreshCw, UserPlus, UserRound, Users } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { followUser, getFollowers, unfollowUser } from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { SocialUser } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const router = useRouter();

const users = ref<SocialUser[]>([]);
const loading = ref(false);
const actionLoading = ref<number | null>(null);
const errorMessage = ref('');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      back: 'Back to profile',
      kicker: 'Followers',
      title: 'Followers',
      refresh: 'Refresh',
      loginTitle: 'Sign in to view followers',
      loginDesc: 'People who follow you will appear here.',
      login: 'Log in',
      loading: 'Loading followers',
      loadErrorTitle: 'Unable to load',
      loadErrorFallback: 'Followers list could not be loaded.',
      sessionExpired: 'Your session expired. Please sign in again to view followers.',
      unfollowConfirm: (name: string) => `Stop following ${name}?`,
      followFailed: 'Follow action did not complete successfully.',
      mutual: 'Mutual',
      followBack: 'Follow back',
      emptyTitle: 'No followers yet',
      emptyDesc: 'When someone follows you, they will appear here.'
    };
  }

  return {
    back: '返回主页',
    kicker: 'Followers',
    title: '粉丝',
    refresh: '刷新',
    loginTitle: '登录后查看粉丝',
    loginDesc: '关注你的人会显示在这里。',
    login: '登录',
    loading: '正在读取粉丝列表',
    loadErrorTitle: '暂时无法加载',
    loadErrorFallback: '粉丝列表暂时打不开',
    sessionExpired: '登录状态已过期，请重新登录后查看粉丝列表。',
    unfollowConfirm: (name: string) => `确定不再关注 ${name} 吗？`,
    followFailed: '关注操作暂时没有成功',
    mutual: '互相关注',
    followBack: '回关',
    emptyTitle: '还没有粉丝',
    emptyDesc: '当有人关注你时，会显示在这里。'
  };
});

async function loadFollowers() {
  if (!sessionStore.isAuthenticated || !sessionStore.userId) {
    users.value = [];
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    users.value = await getFollowers(sessionStore.userId);
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

function openProfile(userId: number) {
  void router.push(`/users/${userId}`);
}

async function toggleFollow(user: SocialUser) {
  if (actionLoading.value) {
    return;
  }
  if (user.followedByViewer && !window.confirm(copy.value.unfollowConfirm(user.displayName))) {
    return;
  }
  actionLoading.value = user.userId;
  try {
    if (user.followedByViewer) {
      await unfollowUser(user.userId);
    } else {
      await followUser(user.userId);
    }
    user.followedByViewer = !user.followedByViewer;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.followFailed;
  } finally {
    actionLoading.value = null;
  }
}

onMounted(loadFollowers);

watch(
  () => sessionStore.isAuthenticated,
  () => loadFollowers()
);
</script>

<template>
  <section class="followers-page">
    <div class="page-heading with-actions">
      <div>
        <RouterLink class="secondary-button return-link" to="/me">
          <ArrowLeft :size="17" />
          <span>{{ copy.back }}</span>
        </RouterLink>
        <span class="section-kicker">{{ copy.kicker }}</span>
        <h1>{{ copy.title }}</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadFollowers">
        <RefreshCw :size="17" />
        <span>{{ copy.refresh }}</span>
      </button>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <UserRound :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">{{ copy.login }}</RouterLink>
    </div>

    <template v-else>
      <LoadingState v-if="loading" :label="copy.loading" />
      <EmptyState v-else-if="errorMessage" :title="copy.loadErrorTitle" :description="errorMessage" />

      <section v-else class="social-grid">
        <article
          v-for="user in users"
          :key="user.userId"
          class="social-card is-clickable"
          role="link"
          tabindex="0"
          @click="openProfile(user.userId)"
          @keydown.enter="openProfile(user.userId)"
        >
          <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
          <UserRound v-else :size="24" />
          <div>
            <strong>{{ user.displayName }}</strong>
            <span>@{{ user.username }} · Lv.{{ user.communityLevel }}</span>
            <p>{{ user.bio }}</p>
          </div>
          <button
            class="secondary-button"
            :class="user.followedByViewer ? 'follow-done' : 'follow-todo'"
            type="button"
            :disabled="actionLoading === user.userId"
            @click.stop="toggleFollow(user)"
          >
            <component :is="user.followedByViewer ? Users : UserPlus" :size="16" />
            <span>{{ user.followedByViewer ? copy.mutual : copy.followBack }}</span>
          </button>
        </article>
        <EmptyState v-if="users.length === 0" :title="copy.emptyTitle" :description="copy.emptyDesc" />
      </section>
    </template>
  </section>
</template>
