<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowLeft, RefreshCw, UserCheck, UserRound, Users } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { getFollowers, getFollowing, unfollowUser } from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { useSessionStore } from '@/stores/session';
import type { SocialUser } from '@/types/api';

const sessionStore = useSessionStore();
const router = useRouter();

const users = ref<SocialUser[]>([]);
const mutualIds = ref<Set<number>>(new Set());
const loading = ref(false);
const actionLoading = ref<number | null>(null);
const errorMessage = ref('');

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
      errorMessage.value = '登录状态已过期，请重新登录后查看关注列表。';
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : '关注列表暂时打不开';
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
  if (!window.confirm(`确定不再关注 ${user.displayName} 吗？`)) {
    return;
  }
  actionLoading.value = user.userId;
  try {
    await unfollowUser(user.userId);
    await loadFollowing();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '取关操作暂时没有成功';
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
          <span>返回主页</span>
        </RouterLink>
        <span class="section-kicker">Following</span>
        <h1>关注</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadFollowing">
        <RefreshCw :size="17" />
        <span>刷新</span>
      </button>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <UserRound :size="42" />
      <h2>登录后查看关注</h2>
      <p>你关注的人会显示在这里。</p>
      <RouterLink class="primary-button" to="/login">登录</RouterLink>
    </div>

    <template v-else>
      <LoadingState v-if="loading" label="正在读取关注列表" />
      <EmptyState v-else-if="errorMessage" title="暂时无法加载" :description="errorMessage" />

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
            <span>{{ isMutual(user.userId) ? '互相关注' : '已关注' }}</span>
          </button>
        </article>
        <EmptyState v-if="users.length === 0" title="还没有关注任何人" description="在用户主页点击关注后，对方会显示在这里。" />
      </section>
    </template>
  </section>
</template>
