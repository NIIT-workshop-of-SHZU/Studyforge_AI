<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { Bell, BookOpen, CircleHelp, Home, Library, LogIn, LogOut, MessageCircle, PenLine, Search, Settings, UserRound } from '@lucide/vue';
import { getUnreadNotificationCount } from '@/api/notifications';
import studyforgeLogo from '@/assets/studyforge-logo-mark.png';
import { usePreferencesStore, type LanguageCode } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const unreadNotifications = ref(0);
const showUserMenu = ref(false);
const userChipRef = ref<HTMLElement | null>(null);

const languageCode = computed({
  get: () => preferencesStore.languageCode,
  set: (value: LanguageCode) => preferencesStore.setLanguageCode(value)
});

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      brandSub: 'Study Space',
      navAria: 'Primary navigation',
      home: 'Home',
      knowledge: 'Knowledge',
      library: 'My Study',
      profile: 'Profile',
      friends: 'Friends',
      notifications: 'Notifications',
      account: 'Account',
      publish: 'Write',
      help: 'Help',
      search: 'Search articles or topics',
      language: 'Language',
      login: 'Log in',
      logout: 'Log out'
    };
  }

  return {
    brandSub: '学习空间',
    navAria: '主导航',
    home: '首页',
    knowledge: '知识流',
    library: '我的学习',
    profile: '我的主页',
    friends: '好友',
    notifications: '通知',
    account: '账号',
    publish: '发布',
    help: '求助',
    search: '搜索文章或主题',
    language: '语言',
    login: '登录',
    logout: '退出登录'
  };
});

async function logout() {
  await sessionStore.logout();
  unreadNotifications.value = 0;
  await router.push('/');
}

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value;
}

function closeUserMenu() {
  showUserMenu.value = false;
}

async function goToProfile() {
  closeUserMenu();
  await router.push('/me');
}

async function goToFriends() {
  closeUserMenu();
  await router.push('/friends');
}

async function handleLogout() {
  closeUserMenu();
  await logout();
}

function handleDocumentClick(event: MouseEvent) {
  if (!showUserMenu.value) {
    return;
  }
  if (userChipRef.value && !userChipRef.value.contains(event.target as Node)) {
    closeUserMenu();
  }
}

async function loadUnreadNotifications() {
  if (!sessionStore.isAuthenticated) {
    unreadNotifications.value = 0;
    return;
  }

  try {
    unreadNotifications.value = await getUnreadNotificationCount();
  } catch {
    unreadNotifications.value = 0;
  }
}

function handleNotificationUpdate() {
  void loadUnreadNotifications();
}

onMounted(() => {
  void loadUnreadNotifications();
  window.addEventListener('studyforge:notifications-updated', handleNotificationUpdate);
  document.addEventListener('click', handleDocumentClick);
});

onBeforeUnmount(() => {
  window.removeEventListener('studyforge:notifications-updated', handleNotificationUpdate);
  document.removeEventListener('click', handleDocumentClick);
});

watch(
  () => [sessionStore.isAuthenticated, router.currentRoute.value.fullPath],
  () => {
    void loadUnreadNotifications();
  }
);
</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <RouterLink class="brand-lockup" to="/" aria-label="StudyForge AI Knowledge">
        <span class="brand-mark">
          <img :src="studyforgeLogo" alt="" />
        </span>
        <span>
          <strong>StudyForge AI</strong>
          <small>{{ copy.brandSub }}</small>
        </span>
      </RouterLink>

      <nav class="main-nav" :aria-label="copy.navAria">
        <RouterLink to="/">
          <Home :size="17" />
          <span>{{ copy.home }}</span>
        </RouterLink>
        <RouterLink to="/knowledge">
          <BookOpen :size="17" />
          <span>{{ copy.knowledge }}</span>
        </RouterLink>
        <RouterLink to="/library">
          <Library :size="17" />
          <span>{{ copy.library }}</span>
        </RouterLink>
        <RouterLink to="/publish">
          <PenLine :size="17" />
          <span>{{ copy.publish }}</span>
        </RouterLink>
        <RouterLink to="/help">
          <CircleHelp :size="17" />
          <span>{{ copy.help }}</span>
        </RouterLink>
      </nav>

      <div class="header-actions">
        <label class="search-shell" for="global-search">
          <Search :size="17" />
          <input id="global-search" type="search" :placeholder="copy.search" />
        </label>

        <label class="select-field" for="knowledge-language">
          <span>{{ copy.language }}</span>
          <select id="knowledge-language" v-model="languageCode">
            <option value="zh_CN">中文</option>
            <option value="en_US">English</option>
          </select>
        </label>

        <div v-if="sessionStore.isAuthenticated" ref="userChipRef" class="user-chip">
          <button
            type="button"
            class="profile-link"
            :aria-expanded="showUserMenu"
            aria-haspopup="menu"
            @click="toggleUserMenu"
          >
            <UserRound :size="17" />
            <span>{{ sessionStore.displayName }}</span>
          </button>
          <div v-if="showUserMenu" class="user-menu" role="menu">
            <button type="button" role="menuitem" @click="goToProfile">
              <UserRound :size="16" />
              <span>{{ copy.profile }}</span>
            </button>
            <button type="button" role="menuitem" @click="goToFriends">
              <MessageCircle :size="16" />
              <span>{{ copy.friends }}</span>
            </button>
            <button type="button" role="menuitem" @click="handleLogout">
              <LogOut :size="16" />
              <span>{{ copy.logout }}</span>
            </button>
          </div>
          <RouterLink class="chip-icon-link notification-chip" to="/notifications" :aria-label="copy.notifications">
            <Bell :size="16" />
            <span v-if="unreadNotifications > 0" class="notification-badge">{{ unreadNotifications > 99 ? '99+' : unreadNotifications }}</span>
          </RouterLink>
          <RouterLink class="chip-icon-link" to="/account" :aria-label="copy.account">
            <Settings :size="16" />
          </RouterLink>
        </div>

        <RouterLink v-else class="primary-button" to="/login">
          <LogIn :size="17" />
          <span>{{ copy.login }}</span>
        </RouterLink>
      </div>
    </header>

    <main class="page-shell">
      <slot />
    </main>
  </div>
</template>
