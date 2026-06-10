<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowLeft, RefreshCw, UserCheck, UserRound, Users } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { getFollowers, getFollowing, unfollowUser } from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { SocialUser } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const router = useRouter();

const users = ref<SocialUser[]>([]);
const mutualIds = ref<Set<number>>(new Set());
const loading = ref(false);
const actionLoading = ref<number | null>(null);
const errorMessage = ref('');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      back: 'Back to profile',
      kicker: 'Following',
      title: 'Following',
      refresh: 'Refresh',
      loginTitle: 'Sign in to view following',
      loginDesc: 'People you follow will appear here.',
      login: 'Log in',
      loading: 'Loading following list',
      loadErrorTitle: 'Unable to load',
      loadErrorFallback: 'Following list could not be loaded.',
      sessionExpired: 'Your session expired. Please sign in again to view following.',
      unfollowConfirm: (name: string) => `Stop following ${name}?`,
      unfollowFailed: 'Unfollow did not complete successfully.',
      mutual: 'Mutual',
      following: 'Following',
      emptyTitle: 'Not following anyone yet',
      emptyDesc: 'After you follow someone from their profile, they will appear here.'
    };
  }

  return {
    back: '返回主页',
    kicker: 'Following',
    title: '关注',
    refresh: '刷新',
    loginTitle: '登录后查看关注',
    loginDesc: '你关注的人会显示在这里。',
    login: '登录',
    loading: '正在读取关注列表',
    loadErrorTitle: '暂时无法加载',
    loadErrorFallback: '关注列表暂时打不开',
    sessionExpired: '登录状态已过期，请重新登录后查看关注列表。',
    unfollowConfirm: (name: string) => `确定不再关注 ${name} 吗？`,
    unfollowFailed: '取关操作暂时没有成功',
    mutual: '互相关注',
    following: '已关注',
    emptyTitle: '还没有关注任何人',
    emptyDesc: '在用户主页点击关注后，对方会显示在这里。'
  };
});

function isMutual(userId: number) {
  return mutualIds.value.has(userId);
}

async function loadFollowing() {
  if (!sessionStore.isAuthenticated || !sessionStore.userId) {
    users.value = [];
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const [followingData, followerData] = await Promise.all([
      getFollowing(sessionStore.userId),
      getFollowers(sessionStore.userId)
    ]);
    users.value = followingData;
    mutualIds.value = new Set(followerData.map((follower) => follower.userId));
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

async function unfollow(user: SocialUser) {
  if (actionLoading.value) {
    return;
  }
  if (!window.confirm(copy.value.unfollowConfirm(user.displayName))) {
    return;
  }
  actionLoading.value = user.userId;
  try {
    await unfollowUser(user.userId);
    await loadFollowing();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unfollowFailed;
  } finally {
    actionLoading.value = null;
  }
}

onMounted(loadFollowing);

watch(
  () => sessionStore.isAuthenticated,
  () => loadFollowing()
);
</script>

<template>
  <section class="following-page">
    <div class="page-heading with-actions">
      <div>
        <RouterLink class="secondary-button return-link" to="/me">
          <ArrowLeft :size="17" />
          <span>{{ copy.back }}</span>
        </RouterLink>
        <span class="section-kicker">{{ copy.kicker }}</span>
        <h1>{{ copy.title }}</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadFollowing">
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
          <button class="secondary-button follow-done" type="button" :disabled="actionLoading === user.userId" @click.stop="unfollow(user)">
            <component :is="isMutual(user.userId) ? Users : UserCheck" :size="16" />
            <span>{{ isMutual(user.userId) ? copy.mutual : copy.following }}</span>
          </button>
        </article>
        <EmptyState v-if="users.length === 0" :title="copy.emptyTitle" :description="copy.emptyDesc" />
      </section>
    </template>
  </section>
</template>
