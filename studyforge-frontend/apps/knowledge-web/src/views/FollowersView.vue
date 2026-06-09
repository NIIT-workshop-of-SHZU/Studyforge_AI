<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowLeft, RefreshCw, UserPlus, UserRound, Users } from '@lucide/vue';
import { ApiError } from '@/api/http';
import { followUser, getFollowers, unfollowUser } from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { useSessionStore } from '@/stores/session';
import type { SocialUser } from '@/types/api';

const sessionStore = useSessionStore();
const router = useRouter();

const users = ref<SocialUser[]>([]);
const loading = ref(false);
const actionLoading = ref<number | null>(null);
const errorMessage = ref('');

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
      errorMessage.value = '登录状态已过期，请重新登录后查看粉丝列表。';
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : '粉丝列表暂时打不开';
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
  if (user.followedByViewer && !window.confirm(`确定不再关注 ${user.displayName} 吗？`)) {
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
    errorMessage.value = error instanceof Error ? error.message : '关注操作暂时没有成功';
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
          <span>返回主页</span>
        </RouterLink>
        <span class="section-kicker">Followers</span>
        <h1>粉丝</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadFollowers">
        <RefreshCw :size="17" />
        <span>刷新</span>
      </button>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <UserRound :size="42" />
      <h2>登录后查看粉丝</h2>
      <p>关注你的人会显示在这里。</p>
      <RouterLink class="primary-button" to="/login">登录</RouterLink>
    </div>

    <template v-else>
      <LoadingState v-if="loading" label="正在读取粉丝列表" />
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
          <button
            class="secondary-button"
            :class="user.followedByViewer ? 'follow-done' : 'follow-todo'"
            type="button"
            :disabled="actionLoading === user.userId"
            @click.stop="toggleFollow(user)"
          >
            <component :is="user.followedByViewer ? Users : UserPlus" :size="16" />
            <span>{{ user.followedByViewer ? '互相关注' : '回关' }}</span>
          </button>
        </article>
        <EmptyState v-if="users.length === 0" title="还没有粉丝" description="当有人关注你时，会显示在这里。" />
      </section>
    </template>
  </section>
</template>
