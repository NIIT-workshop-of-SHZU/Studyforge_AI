<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import {
  BookOpen,
  BookmarkCheck,
  CircleHelp,
  Eye,
  Heart,
  MessageCircle,
  MessageSquareReply,
  PenLine,
  Settings,
  Star,
  UserRound,
  Users
} from '@lucide/vue';
import { ApiError } from '@/api/http';
import { getUserHomepage } from '@/api/homepages';
import {
  followUser,
  getFriends,
  getMyProfile,
  getUserActivities,
  getUserPosts,
  getUserProfile,
  reviewFriendRequest,
  sendFriendRequest,
  unfollowUser
} from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import HomepageHero from '@/components/HomepageHero.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { PostSummary, SocialUser, TopicCategory, UserActivity, UserHomepage, UserProfile } from '@/types/api';
import { formatShortDateTime } from '@/utils/date';

type ProfileTab = 'activity' | 'posts' | 'friends';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const sessionStore = useSessionStore();

const profile = ref<UserProfile | null>(null);
const homepage = ref<UserHomepage | null>(null);
const posts = ref<PostSummary[]>([]);
const activities = ref<UserActivity[]>([]);
const friends = ref<SocialUser[]>([]);
const loading = ref(false);
const actionLoading = ref(false);
const errorMessage = ref('');
const activeTab = ref<ProfileTab>('activity');

const isMeRoute = computed(() => route.name === 'me');
const targetUserId = computed(() => (isMeRoute.value ? sessionStore.userId : Number(route.params.userId)));
const publishedPreview = computed(() => route.query.preview === 'published');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      loginTitle: 'Sign in to view your profile',
      loginDesc: 'Your activity, friends, favorites, and homepage customization live here.',
      login: 'Log in',
      loading: 'Opening profile',
      unavailable: 'Profile is unavailable',
      sessionExpired: 'Your session expired. Please sign in again to open the profile.',
      followFirst: 'Please sign in first, then follow this user.',
      friendFirst: 'Please sign in first, then send a friend request.',
      followFailed: 'Follow action did not complete successfully.',
      friendFailed: 'Friend action did not complete successfully.',
      publishCenter: 'Creator Center',
      publishCenterDesc: 'Publish a new learning post',
      favorites: 'Collections',
      favoritesDesc: 'Organize saved posts by theme',
      friends: 'Friends',
      friendsDesc: 'Handle requests and messages',
      account: 'Account Settings',
      accountDesc: 'Update avatar, name, and password',
      homepageStudio: 'Homepage Studio',
      homepageStudioDesc: 'Default, code, and media-driven homepage design',
      tabsAria: 'Profile sections',
      activity: 'Activity',
      posts: 'Posts',
      friendsTab: 'Friends',
      communitySignals: 'Community Signals',
      publishedPosts: 'Published Posts',
      receivedLikes: 'Received Likes',
      discussions: 'Discussions',
      reputation: 'Reputation',
      open: 'Open',
      viewHelp: 'View help thread',
      noActivity: 'No activity yet',
      noActivityDesc: 'Publishing, answering, commenting, liking, and saving will show up here.',
      noPosts: 'No posts yet',
      noPostsDesc: 'Published articles will appear here.',
      noFriends: 'No friends yet',
      noFriendsDesc: 'Friends appear here after requests are accepted. Followers stay separate.',
      profileLink: 'Profile',
      friendRequestMessage: 'Hi, I would love to connect and keep learning together.',
      updatedActivity: 'updated activity',
      publishedActivity: 'published a post',
      askedHelp: 'asked a question',
      answeredHelp: 'answered a question',
      commented: 'commented on a post',
      liked: 'liked a post',
      favorited: 'saved a post',
      openContent: 'Open content'
    };
  }

  return {
    loginTitle: '登录后查看个人主页',
    loginDesc: '你的动态、好友、收藏和主页设计都会放在这里。',
    login: '登录',
    loading: '正在打开个人主页',
    unavailable: '个人主页暂时不可用',
    sessionExpired: '登录状态已过期，请重新登录后查看个人主页。',
    followFirst: '请先登录后再关注这个用户。',
    friendFirst: '请先登录后再发送好友申请。',
    followFailed: '关注操作暂时没有成功。',
    friendFailed: '好友操作暂时没有成功。',
    publishCenter: '创作中心',
    publishCenterDesc: '发布新的学习内容',
    favorites: '收藏夹',
    favoritesDesc: '按主题整理收藏内容',
    friends: '好友',
    friendsDesc: '处理申请和消息',
    account: '账号设置',
    accountDesc: '修改头像、名字和密码',
    homepageStudio: '主页设计',
    homepageStudioDesc: '默认模板、代码主页和媒体主页',
    tabsAria: '个人主页内容',
    activity: '动态',
    posts: '投稿',
    friendsTab: '好友',
    communitySignals: '社区表现',
    publishedPosts: '发布文章',
    receivedLikes: '收到点赞',
    discussions: '参与讨论',
    reputation: '声望',
    open: '打开',
    viewHelp: '查看求助',
    noActivity: '还没有动态',
    noActivityDesc: '发帖、提问、回答、评论、点赞和收藏后，这里会形成动态。',
    noPosts: '还没有投稿',
    noPostsDesc: '发布后的文章会展示在这里。',
    noFriends: '还没有好友',
    noFriendsDesc: '通过好友申请后，好友会显示在这里。关注和粉丝不会混到好友列表里。',
    profileLink: '主页',
    friendRequestMessage: '你好，我想和你成为好友，之后可以继续交流学习内容。',
    updatedActivity: '更新了动态',
    publishedActivity: '发布了帖子',
    askedHelp: '提出了问题',
    answeredHelp: '回答了问题',
    commented: '评论了帖子',
    liked: '点赞了帖子',
    favorited: '收藏了帖子',
    openContent: '打开'
  };
});

const categories = computed<Record<string, TopicCategory>>(() => ({
  TECHNOLOGY: { code: 'TECHNOLOGY', name: preferencesStore.languageCode === 'en_US' ? 'Technology' : '技术实践', description: '', accent: '#2563eb' },
  BUSINESS: { code: 'BUSINESS', name: preferencesStore.languageCode === 'en_US' ? 'Business' : '商业观察', description: '', accent: '#7c3aed' },
  PRODUCTIVITY: { code: 'PRODUCTIVITY', name: preferencesStore.languageCode === 'en_US' ? 'Productivity' : '效率方法', description: '', accent: '#b45309' },
  CAREER: { code: 'CAREER', name: preferencesStore.languageCode === 'en_US' ? 'Career' : '职业成长', description: '', accent: '#15803d' },
  FINANCE: { code: 'FINANCE', name: preferencesStore.languageCode === 'en_US' ? 'Finance' : '财务入门', description: '', accent: '#0891b2' }
}));

function categoryFor(post: PostSummary): TopicCategory {
  return categories.value[post.categoryCode] ?? { code: post.categoryCode, name: post.categoryCode, description: '', accent: '#0f766e' };
}

async function loadProfile() {
  if (isMeRoute.value && !sessionStore.isAuthenticated) {
    profile.value = null;
    homepage.value = null;
    posts.value = [];
    activities.value = [];
    friends.value = [];
    return;
  }
  if (!targetUserId.value) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const profileData = isMeRoute.value ? await getMyProfile() : await getUserProfile(targetUserId.value);
    profile.value = profileData;
    const [homepageData, postData, friendData, activityData] = await Promise.all([
      getUserHomepage(profileData.userId),
      getUserPosts(profileData.userId, preferencesStore.languageCode),
      getFriends(profileData.userId),
      getUserActivities(profileData.userId, preferencesStore.languageCode)
    ]);
    homepage.value = homepageData;
    posts.value = postData;
    friends.value = friendData;
    activities.value = activityData;
  } catch (error) {
    if (error instanceof ApiError && error.code === 4010) {
      await sessionStore.logout();
      errorMessage.value = copy.value.sessionExpired;
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

function formatActivityTime(value: UserActivity['createdTime']) {
  return formatShortDateTime(value, preferencesStore.languageCode);
}

function activityMeta(activity: UserActivity) {
  const map: Record<string, { label: string; icon: typeof PenLine; tone: string }> = {
    POST_PUBLISHED: { label: copy.value.publishedActivity, icon: PenLine, tone: 'publish' },
    HELP_ASKED: { label: copy.value.askedHelp, icon: CircleHelp, tone: 'help' },
    HELP_ANSWERED: { label: copy.value.answeredHelp, icon: MessageSquareReply, tone: 'answer' },
    COMMENTED: { label: copy.value.commented, icon: MessageCircle, tone: 'comment' },
    LIKED_POST: { label: copy.value.liked, icon: Heart, tone: 'like' },
    FAVORITED_POST: { label: copy.value.favorited, icon: Star, tone: 'favorite' }
  };
  return map[activity.activityType] ?? { label: copy.value.updatedActivity, icon: BookOpen, tone: 'default' };
}

function activityTitle(activity: UserActivity) {
  return activity.title || (activity.targetType === 'HELP' ? copy.value.viewHelp : copy.value.openContent);
}

function categoryLabel(code: string) {
  return code ? categories.value[code]?.name ?? code : '';
}

function languageLabel(code: string) {
  return { zh_CN: '中文', en_US: 'English' }[code] ?? code;
}

function activityChipText(activity: UserActivity) {
  return [categoryLabel(activity.categoryCode), languageLabel(activity.languageCode)].filter(Boolean).join(' · ');
}

function activityTarget(activity: UserActivity) {
  if (activity.postId) {
    return { path: `/posts/${activity.postId}`, query: { language: activity.languageCode || preferencesStore.languageCode } };
  }
  if (activity.helpId) {
    return { path: '/help', query: { helpId: activity.helpId } };
  }
  return { path: '/knowledge' };
}

async function toggleFollow() {
  if (!profile.value || profile.value.self || actionLoading.value) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.followFirst;
    return;
  }

  actionLoading.value = true;
  try {
    profile.value = profile.value.followedByViewer ? await unfollowUser(profile.value.userId) : await followUser(profile.value.userId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.followFailed;
  } finally {
    actionLoading.value = false;
  }
}

async function requestFriendship() {
  if (!profile.value || profile.value.self || actionLoading.value) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.friendFirst;
    return;
  }

  actionLoading.value = true;
  try {
    if (profile.value.friendStatus === 'PENDING_RECEIVED' && profile.value.friendRequestId) {
      await reviewFriendRequest(profile.value.friendRequestId, 'ACCEPT');
    } else if (profile.value.friendStatus === 'NONE') {
      await sendFriendRequest(profile.value.userId, copy.value.friendRequestMessage);
    }
    profile.value = await getUserProfile(profile.value.userId);
    friends.value = await getFriends(profile.value.userId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.friendFailed;
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadProfile);

watch(() => [route.fullPath, sessionStore.isAuthenticated, preferencesStore.languageCode], () => loadProfile());
</script>

<template>
  <section class="profile-page">
    <div v-if="isMeRoute && !sessionStore.isAuthenticated" class="login-required">
      <UserRound :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">{{ copy.login }}</RouterLink>
    </div>

    <LoadingState v-else-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.unavailable" :description="errorMessage" />

    <template v-else-if="profile">
      <HomepageHero
        :profile="profile"
        :homepage="homepage"
        :loading="loading"
        :action-loading="actionLoading"
        :self="profile.self"
        :published-preview="publishedPreview"
        @refresh="loadProfile"
        @follow="toggleFollow"
        @friend="requestFriendship"
      />

      <section v-if="profile.self" class="profile-shortcuts">
        <RouterLink class="profile-shortcut-card" to="/publish">
          <PenLine :size="24" />
          <strong>{{ copy.publishCenter }}</strong>
          <span>{{ copy.publishCenterDesc }}</span>
        </RouterLink>
        <RouterLink class="profile-shortcut-card" to="/favorites">
          <BookmarkCheck :size="24" />
          <strong>{{ copy.favorites }}</strong>
          <span>{{ copy.favoritesDesc }}</span>
        </RouterLink>
        <RouterLink class="profile-shortcut-card" to="/friends">
          <MessageCircle :size="24" />
          <strong>{{ copy.friends }}</strong>
          <span>{{ copy.friendsDesc }}</span>
        </RouterLink>
        <RouterLink class="profile-shortcut-card" to="/account">
          <Settings :size="24" />
          <strong>{{ copy.account }}</strong>
          <span>{{ copy.accountDesc }}</span>
        </RouterLink>
        <RouterLink class="profile-shortcut-card" to="/homepage-studio">
          <PenLine :size="24" />
          <strong>{{ copy.homepageStudio }}</strong>
          <span>{{ copy.homepageStudioDesc }}</span>
        </RouterLink>
      </section>

      <nav class="profile-tabs" :aria-label="copy.tabsAria">
        <button type="button" :class="{ active: activeTab === 'activity' }" @click="activeTab = 'activity'">{{ copy.activity }}</button>
        <button type="button" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'">{{ copy.posts }}</button>
        <button type="button" :class="{ active: activeTab === 'friends' }" @click="activeTab = 'friends'">{{ copy.friendsTab }}</button>
      </nav>

      <section v-if="activeTab === 'activity'" class="profile-content-grid">
        <div class="activity-timeline">
          <article v-for="activity in activities" :key="activity.activityKey" class="timeline-item" :class="`tone-${activityMeta(activity).tone}`">
            <div class="timeline-rail">
              <component :is="activityMeta(activity).icon" :size="18" />
            </div>
            <div class="timeline-card">
              <header class="timeline-header">
                <div class="activity-author">
                  <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="" />
                  <UserRound v-else :size="18" />
                  <div>
                    <strong>{{ profile.displayName }}</strong>
                    <span>{{ activityMeta(activity).label }} · {{ formatActivityTime(activity.createdTime) }}</span>
                  </div>
                </div>
                <span v-if="activityChipText(activity)" class="timeline-chip">{{ activityChipText(activity) }}</span>
              </header>

              <RouterLink class="timeline-title" :to="activityTarget(activity)">
                {{ activityTitle(activity) }}
              </RouterLink>

              <div v-if="activity.content" class="timeline-quote">
                <MarkdownRenderer :content="activity.content" />
              </div>
              <p v-else class="timeline-summary">{{ activity.summary }}</p>

              <img v-if="activity.coverImageUrl" class="activity-cover" :src="activity.coverImageUrl" alt="" loading="lazy" />

              <footer v-if="activity.postId" class="timeline-stats">
                <span><Heart :size="15" />{{ activity.likeCount }}</span>
                <span><BookmarkCheck :size="15" />{{ activity.favoriteCount }}</span>
                <span><MessageCircle :size="15" />{{ activity.commentCount }}</span>
                <span><Eye :size="15" />{{ activity.viewCount }}</span>
                <RouterLink :to="activityTarget(activity)">{{ copy.open }}</RouterLink>
              </footer>
              <footer v-else class="timeline-stats">
                <RouterLink :to="activityTarget(activity)">{{ copy.viewHelp }}</RouterLink>
              </footer>
            </div>
          </article>
          <EmptyState v-if="activities.length === 0" :title="copy.noActivity" :description="copy.noActivityDesc" />
        </div>

        <aside class="profile-side-panel">
          <h2>{{ copy.communitySignals }}</h2>
          <dl>
            <div><dt>{{ copy.publishedPosts }}</dt><dd>{{ profile.postCount }}</dd></div>
            <div><dt>{{ copy.receivedLikes }}</dt><dd>{{ profile.receivedLikeCount }}</dd></div>
            <div><dt>{{ copy.discussions }}</dt><dd>{{ profile.commentCount }}</dd></div>
            <div><dt>{{ copy.reputation }}</dt><dd>{{ profile.reputationScore }}</dd></div>
          </dl>
        </aside>
      </section>

      <section v-else-if="activeTab === 'posts'" class="knowledge-grid compact-grid">
        <KnowledgeCard v-for="(post, index) in posts" :key="post.postId" :post="post" :category="categoryFor(post)" :index="index" />
        <EmptyState v-if="posts.length === 0" :title="copy.noPosts" :description="copy.noPostsDesc" />
      </section>

      <section v-else class="social-grid">
        <article v-for="user in friends" :key="user.userId" class="social-card">
          <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
          <UserRound v-else :size="24" />
          <div>
            <strong>{{ user.displayName }}</strong>
            <span>@{{ user.username }} · Lv.{{ user.communityLevel }}</span>
            <p>{{ user.bio }}</p>
          </div>
          <RouterLink class="secondary-button stable-action" :to="`/users/${user.userId}`">
            <Users :size="16" />
            <span>{{ copy.profileLink }}</span>
          </RouterLink>
        </article>
        <EmptyState v-if="friends.length === 0" :title="copy.noFriends" :description="copy.noFriendsDesc" />
      </section>
    </template>
  </section>
</template>
