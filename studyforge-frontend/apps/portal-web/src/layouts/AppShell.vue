<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import {
  Activity,
  BookOpen,
  Database,
  LayoutDashboard,
  LayoutTemplate,
  LogOut,
  Server,
  Settings,
  ShieldCheck,
  UserRound
} from '@lucide/vue';
import studyforgeLogo from '@/assets/studyforge-logo-mark.png';
import { useAuthStore } from '@/stores/auth';
import { usePreferencesStore } from '@/stores/preferences';

const router = useRouter();
const authStore = useAuthStore();
const preferencesStore = usePreferencesStore();

const currentLanguage = computed({
  get: () => preferencesStore.languageCode,
  set: (value: 'zh_CN' | 'en_US') => preferencesStore.setLanguageCode(value)
});

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        brandSub: 'Content Console',
        navAria: 'Primary navigation',
        content: 'Content Feed',
        dashboard: 'Operations',
        community: 'Community',
        homepageReviews: 'Homepage Reviews',
        settings: 'AI Settings',
        apiStack: 'API: Spring MVC JSON',
        dataStack: 'Data: MyBatis + MySQL',
        workspace: 'Local Workspace',
        language: 'Language',
        logout: 'Log out'
      }
    : {
        brandSub: '内容工作台',
        navAria: '主导航',
        content: '内容流',
        dashboard: '运营看板',
        community: '社区管理',
        homepageReviews: '主页审核',
        settings: 'AI 与模型设置',
        apiStack: '接口：Spring MVC JSON',
        dataStack: '数据：MyBatis + MySQL',
        workspace: '本地工作台',
        language: '语言',
        logout: '退出登录'
      }
);

async function handleLogout() {
  await authStore.logout();
  await router.push('/login');
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/feed" class="brand-lockup" aria-label="StudyForge AI">
        <span class="brand-mark">
          <img :src="studyforgeLogo" alt="" />
        </span>
        <span>
          <strong>StudyForge AI</strong>
          <small>{{ copy.brandSub }}</small>
        </span>
      </RouterLink>

      <nav class="side-nav" :aria-label="copy.navAria">
        <RouterLink to="/feed">
          <BookOpen :size="18" />
          <span>{{ copy.content }}</span>
        </RouterLink>
        <RouterLink to="/admin">
          <LayoutDashboard :size="18" />
          <span>{{ copy.dashboard }}</span>
        </RouterLink>
        <RouterLink to="/community">
          <ShieldCheck :size="18" />
          <span>{{ copy.community }}</span>
        </RouterLink>
        <RouterLink to="/homepage-reviews">
          <LayoutTemplate :size="18" />
          <span>{{ copy.homepageReviews }}</span>
        </RouterLink>
        <RouterLink to="/ai-dashboard">
          <Activity :size="18" />
          <span>AI 监控</span>
        </RouterLink>
        <RouterLink to="/settings">
          <Settings :size="18" />
          <span>{{ copy.settings }}</span>
        </RouterLink>
      </nav>

      <div class="stack-panel">
        <div class="stack-row">
          <Server :size="16" />
          <span>{{ copy.apiStack }}</span>
        </div>
        <div class="stack-row">
          <Database :size="16" />
          <span>{{ copy.dataStack }}</span>
        </div>
      </div>
    </aside>

    <div class="main-panel">
      <header class="topbar">
        <div class="topbar-title">
          <span class="environment-dot" />
          <span>{{ copy.workspace }}</span>
        </div>

        <div class="topbar-actions">
          <label class="select-field" for="language-select">
            <span>{{ copy.language }}</span>
            <select id="language-select" v-model="currentLanguage">
              <option value="zh_CN">中文</option>
              <option value="en_US">English</option>
            </select>
          </label>

          <div class="user-chip">
            <UserRound :size="17" />
            <span>{{ authStore.username }}</span>
            <small>{{ authStore.role }}</small>
          </div>

          <button class="icon-text-button" type="button" @click="handleLogout">
            <LogOut :size="17" />
            <span>{{ copy.logout }}</span>
          </button>
        </div>
      </header>

      <main class="content-area">
        <slot />
      </main>
    </div>
  </div>
</template>
