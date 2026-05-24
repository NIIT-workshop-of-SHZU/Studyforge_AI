<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { BookOpen, CircleHelp, Home, Library, LogIn, LogOut, PenLine, Search, UserRound } from '@lucide/vue';
import studyforgeLogo from '@/assets/studyforge-logo-mark.png';
import { usePreferencesStore, type LanguageCode } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const languageCode = computed({
  get: () => preferencesStore.languageCode,
  set: (value: LanguageCode) => preferencesStore.setLanguageCode(value)
});

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      brandSub: 'Study Space',
      home: 'Home',
      knowledge: 'Knowledge',
      library: 'My Study',
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
    home: '首页',
    knowledge: '知识流',
    library: '我的学习',
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
  await router.push('/');
}
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

      <nav class="main-nav" aria-label="主导航">
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

        <div v-if="sessionStore.isAuthenticated" class="user-chip">
          <UserRound :size="17" />
          <span>{{ sessionStore.username }}</span>
          <button type="button" :aria-label="copy.logout" @click="logout">
            <LogOut :size="16" />
          </button>
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
