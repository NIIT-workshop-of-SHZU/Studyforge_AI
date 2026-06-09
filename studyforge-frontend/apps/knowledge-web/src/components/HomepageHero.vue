<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import {
  CircleHelp,
  MessageCircle,
  PenLine,
  RefreshCw,
  Settings,
  UserCheck,
  UserPlus,
  UserRound
} from '@lucide/vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { UserHomepage, UserProfile } from '@/types/api';

const props = defineProps<{
  profile: UserProfile;
  homepage: UserHomepage | null;
  loading?: boolean;
  actionLoading?: boolean;
  self: boolean;
  publishedPreview?: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'follow'): void;
  (e: 'friend'): void;
}>();

const preferencesStore = usePreferencesStore();

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      refresh: 'Refresh',
      messages: 'Friend messages',
      studio: 'Homepage studio',
      editProfile: 'Edit profile',
      following: 'Following',
      follow: 'Follow',
      approveFriend: 'Approve request',
      addFriend: 'Add friend',
      pendingFriend: 'Request sent',
      sendMessage: 'Message',
      noBio: 'This user has not added a bio yet.',
      mediaEdit: 'Edit homepage',
      codeTitleSuffix: '\'s custom homepage',
      codeSubtitle: 'Code-based homepage',
      codeTip: 'Custom code is rendered inside a sandboxed iframe and cannot override the global site shell.',
      experience: 'XP',
      nextLevel: 'XP to next level',
      friends: 'Friends',
      followingCount: 'Following',
      followers: 'Followers',
      favorites: 'Favorites',
      history: 'History'
    };
  }

  return {
    refresh: '刷新',
    messages: '好友消息',
    studio: '主页设计中心',
    editProfile: '编辑资料',
    following: '已关注',
    follow: '关注',
    approveFriend: '通过好友申请',
    addFriend: '加好友',
    pendingFriend: '申请已发送',
    sendMessage: '发消息',
    noBio: '这个用户还没有写签名。',
    mediaEdit: '编辑主页',
    codeTitleSuffix: '的自定义主页',
    codeSubtitle: '代码式主页',
    codeTip: '代码内容会在受控 iframe 中展示，不会覆盖站点导航和社区功能。',
    experience: '经验值',
    nextLevel: '距离下一等级',
    friends: '好友',
    followingCount: '关注',
    followers: '粉丝',
    favorites: '收藏',
    history: '历史浏览'
  };
});

const usePublishedVersion = computed(() => props.self && props.publishedPreview);

const effectiveMode = computed(() => {
  if (usePublishedVersion.value) {
    return props.homepage?.publishedLayoutMode || 'default';
  }
  if (props.self) {
    return props.homepage?.layoutMode || 'default';
  }
  return props.homepage?.publishedLayoutMode || 'default';
});

const effectiveTheme = computed(() => {
  const raw = usePublishedVersion.value
    ? props.homepage?.publishedThemeConfig
    : props.self
      ? props.homepage?.themeConfig
      : props.homepage?.publishedThemeConfig;
  try {
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
});

const effectiveMedia = computed(() => {
  const raw = usePublishedVersion.value
    ? props.homepage?.publishedMediaLayout
    : props.self
      ? props.homepage?.mediaLayoutDraft
      : props.homepage?.publishedMediaLayout;
  try {
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
});

const effectiveCode = computed(() => {
  if (usePublishedVersion.value) {
    return props.homepage?.publishedCustomCode || '';
  }
  return props.self ? props.homepage?.customCodeDraft || '' : props.homepage?.publishedCustomCode || '';
});

const progressPercent = computed(() => {
  const currentStart = Math.max(0, (props.profile.communityLevel - 1) * 100);
  const next = Math.max(props.profile.nextLevelExperience, currentStart + 100);
  return Math.min(100, Math.round(((props.profile.experiencePoints - currentStart) / (next - currentStart)) * 100));
});

const remainingExperience = computed(() => Math.max(0, props.profile.nextLevelExperience - props.profile.experiencePoints));

const heroStats = computed(() => [
  { value: props.profile.friendCount, label: copy.value.friends },
  { value: props.profile.followingCount, label: copy.value.followingCount },
  { value: props.profile.followerCount, label: copy.value.followers },
  { value: props.profile.favoriteCount, label: copy.value.favorites },
  { value: props.profile.historyCount, label: copy.value.history }
]);

const codeDoc = computed(() => {
  const accent = typeof effectiveTheme.value?.accent === 'string' ? effectiveTheme.value.accent : '#22c55e';
  return `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
      :root { color-scheme: light; }
      body { margin: 0; font-family: "Segoe UI", sans-serif; background: #08111f; color: #f8fafc; }
      .shell { min-height: 100vh; padding: 24px; background:
        radial-gradient(circle at top left, ${accent}55, transparent 28%),
        linear-gradient(145deg, #08111f, #132238);
      }
      ${effectiveCode.value}
    </style>
  </head>
  <body>
    <div class="shell"></div>
  </body>
</html>`;
});
</script>

<template>
  <section
    v-if="effectiveMode === 'default'"
    class="profile-hero"
    :style="{ '--profile-banner': profile.bannerUrl ? `url(${profile.bannerUrl})` : 'none' }"
  >
    <div class="profile-topbar">
      <button class="secondary-button" type="button" :disabled="loading" @click="emit('refresh')">
        <RefreshCw :size="17" />
        <span>{{ copy.refresh }}</span>
      </button>
      <RouterLink v-if="self" class="secondary-button" to="/friends">
        <MessageCircle :size="17" />
        <span>{{ copy.messages }}</span>
      </RouterLink>
      <RouterLink v-if="self" class="secondary-button" to="/homepage-studio">
        <PenLine :size="17" />
        <span>{{ copy.studio }}</span>
      </RouterLink>
      <RouterLink v-if="self" class="primary-button" to="/account">
        <Settings :size="17" />
        <span>{{ copy.editProfile }}</span>
      </RouterLink>
      <button v-if="!self" class="primary-button" type="button" :disabled="actionLoading" @click="emit('follow')">
        <UserPlus :size="17" />
        <span>{{ profile.followedByViewer ? copy.following : copy.follow }}</span>
      </button>
      <button
        v-if="!self && profile.friendStatus !== 'FRIEND' && profile.friendStatus !== 'PENDING_SENT'"
        class="secondary-button"
        type="button"
        :disabled="actionLoading"
        @click="emit('friend')"
      >
        <UserCheck :size="17" />
        <span>{{ profile.friendStatus === 'PENDING_RECEIVED' ? copy.approveFriend : copy.addFriend }}</span>
      </button>
      <RouterLink v-if="!self && profile.friendStatus === 'FRIEND'" class="secondary-button" to="/friends">
        <MessageCircle :size="17" />
        <span>{{ copy.sendMessage }}</span>
      </RouterLink>
      <button v-if="!self && profile.friendStatus === 'PENDING_SENT'" class="secondary-button" type="button" disabled>
        <UserCheck :size="17" />
        <span>{{ copy.pendingFriend }}</span>
      </button>
    </div>

    <div class="profile-identity">
      <div class="profile-avatar">
        <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="" />
        <UserRound v-else :size="42" />
      </div>
      <div>
        <div class="profile-title-row">
          <h1>{{ profile.displayName }}</h1>
          <span class="level-badge">Lv.{{ profile.communityLevel }}</span>
        </div>
        <p class="profile-username">@{{ profile.username }}</p>
        <p class="profile-bio">{{ profile.bio || copy.noBio }}</p>

        <div class="profile-meta-stack">
          <section class="profile-level compact">
            <div>
              <strong>{{ profile.experiencePoints }}</strong>
              <span>{{ copy.experience }}</span>
            </div>
            <meter min="0" max="100" :value="progressPercent" />
            <small>{{ copy.nextLevel }} {{ remainingExperience }} XP</small>
          </section>

          <section class="profile-stats compact">
            <div v-for="item in heroStats" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </section>
        </div>
      </div>
    </div>
  </section>

  <section
    v-else-if="effectiveMode === 'media'"
    class="homepage-media-hero"
    :style="{ '--home-accent': effectiveTheme.accent || '#22c55e', '--home-surface': effectiveTheme.surface || '#0f172a' }"
  >
    <div class="homepage-media-header">
      <div class="profile-avatar">
        <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="" />
        <UserRound v-else :size="42" />
      </div>
      <div>
        <h1>{{ profile.displayName }}</h1>
        <p>@{{ profile.username }} · Lv.{{ profile.communityLevel }}</p>
      </div>
      <RouterLink v-if="self" class="secondary-button" to="/homepage-studio">{{ copy.mediaEdit }}</RouterLink>
    </div>
    <p class="profile-bio">{{ profile.bio || copy.noBio }}</p>
    <div class="homepage-media-grid">
      <article v-for="(item, index) in effectiveMedia" :key="`${item.url}-${index}`" class="homepage-media-card">
        <video v-if="item.type === 'video'" :src="item.url" controls muted playsinline preload="metadata" />
        <img v-else :src="item.url" alt="" />
        <div class="homepage-media-copy">
          <strong>{{ item.title || `Media block ${index + 1}` }}</strong>
          <span>{{ item.caption || 'Show personality with media-first storytelling.' }}</span>
        </div>
      </article>
    </div>
  </section>

  <section
    v-else
    class="homepage-code-hero"
    :style="{ '--home-accent': effectiveTheme.accent || '#38bdf8', '--home-surface': effectiveTheme.surface || '#0f172a' }"
  >
    <div class="homepage-code-meta">
      <div>
        <h1>{{ profile.displayName }}{{ copy.codeTitleSuffix }}</h1>
        <p>@{{ profile.username }} · {{ copy.codeSubtitle }} · Lv.{{ profile.communityLevel }}</p>
      </div>
      <RouterLink v-if="self" class="secondary-button" to="/homepage-studio">{{ copy.mediaEdit }}</RouterLink>
    </div>
    <iframe class="homepage-code-frame" title="homepage-preview" :srcdoc="codeDoc" sandbox="allow-scripts" />
    <div class="homepage-code-tip">
      <CircleHelp :size="16" />
      <span>{{ copy.codeTip }}</span>
    </div>
  </section>
</template>
