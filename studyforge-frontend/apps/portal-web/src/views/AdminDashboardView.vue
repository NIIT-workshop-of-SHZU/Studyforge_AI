<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Activity, Database, Flag, Layers3, Pin, RefreshCw, ServerCog, Users } from '@lucide/vue';
import { getAdminOverview } from '@/api/community';
import { getHealth } from '@/api/health';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { AdminOverview, HealthStatus } from '@/types/api';

const preferencesStore = usePreferencesStore();
const health = ref<HealthStatus | null>(null);
const overview = ref<AdminOverview | null>(null);
const loading = ref(false);
const errorMessage = ref('');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      eyebrow: 'Operations',
      title: 'Operations Dashboard',
      refresh: 'Refresh',
      activeAccounts: 'Active Accounts',
      pendingReports: 'Pending Reports',
      featuredPosts: 'Featured Posts',
      apiStatus: 'API Status',
      publishedPosts: 'Published Posts',
      database: 'Database',
      frontend: 'Frontend',
      loading: 'Loading service health',
      unavailable: 'Service health is unavailable',
      area: 'Area',
      responsibility: 'What you can manage',
      status: 'Status',
      manageable: 'Available',
      checking: 'CHECKING',
      unknown: 'UNKNOWN'
    };
  }

  return {
    eyebrow: 'Operations',
    title: '运营看板',
    refresh: '刷新',
    activeAccounts: '活跃账号',
    pendingReports: '待处理举报',
    featuredPosts: '置顶文章',
    apiStatus: '接口状态',
    publishedPosts: '已发布文章',
    database: '数据库',
    frontend: '前端',
    loading: '正在查看服务状态',
    unavailable: '服务状态暂时不可用',
    area: '区域',
    responsibility: '可以管理',
    status: '状态',
    manageable: '可管理',
    checking: '检查中',
    unknown: '未知'
  };
});

const modules = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return [
      { name: 'Post Moderation', owner: 'Handle reports, archive violations, and restore mistaken removals.', status: 'live', label: copy.value.manageable },
      { name: 'Featured Content', owner: 'Pin standout posts so they surface in the most visible positions.', status: 'live', label: copy.value.manageable },
      { name: 'Account Status', owner: 'Review profiles, posting activity, discussions, and community level.', status: 'live', label: copy.value.manageable },
      { name: 'AI Configuration', owner: 'Maintain model settings for text, voice, and cover-image generation.', status: 'live', label: copy.value.manageable }
    ];
  }

  return [
    { name: '帖子审核', owner: '处理举报、下架违规内容、恢复误判帖子。', status: 'live', label: copy.value.manageable },
    { name: '置顶推荐', owner: '把值得阅读的文章固定在更靠前的位置。', status: 'live', label: copy.value.manageable },
    { name: '账号状态', owner: '查看账号资料、发帖数、评论数和社区等级。', status: 'live', label: copy.value.manageable },
    { name: 'AI 与模型设置', owner: '维护文本、语音和封面生成的模型参数。', status: 'live', label: copy.value.manageable }
  ];
});

async function loadHealth() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const [healthStatus, communityOverview] = await Promise.all([getHealth(), getAdminOverview()]);
    health.value = healthStatus;
    overview.value = communityOverview;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

onMounted(loadHealth);
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <div class="section-heading">
        <span>{{ copy.eyebrow }}</span>
        <h1>{{ copy.title }}</h1>
      </div>

      <button class="secondary-button" type="button" :disabled="loading" @click="loadHealth">
        <RefreshCw :size="17" />
        <span>{{ copy.refresh }}</span>
      </button>
    </div>

    <div class="metric-grid">
      <div class="metric-card">
        <ServerCog :size="20" />
        <span>Web API</span>
        <strong>{{ health?.service || 'studyforge-webapi' }}</strong>
      </div>
      <div class="metric-card">
        <Users :size="20" />
        <span>{{ copy.activeAccounts }}</span>
        <strong>{{ overview ? `${overview.activeUsers}/${overview.totalUsers}` : '...' }}</strong>
      </div>
      <div class="metric-card">
        <Flag :size="20" />
        <span>{{ copy.pendingReports }}</span>
        <strong>{{ overview?.pendingReports ?? '...' }}</strong>
      </div>
      <div class="metric-card">
        <Pin :size="20" />
        <span>{{ copy.featuredPosts }}</span>
        <strong>{{ overview?.featuredPosts ?? '...' }}</strong>
      </div>
    </div>

    <div class="metric-grid compact-metrics">
      <div class="metric-card">
        <Activity :size="20" />
        <span>{{ copy.apiStatus }}</span>
        <strong>{{ health?.status || (loading ? copy.checking : copy.unknown) }}</strong>
      </div>
      <div class="metric-card">
        <Layers3 :size="20" />
        <span>{{ copy.publishedPosts }}</span>
        <strong>{{ overview?.publishedPosts ?? '...' }}</strong>
      </div>
      <div class="metric-card">
        <Database :size="20" />
        <span>{{ copy.database }}</span>
        <strong>MySQL / MariaDB</strong>
      </div>
      <div class="metric-card">
        <ServerCog :size="20" />
        <span>{{ copy.frontend }}</span>
        <strong>portal-web</strong>
      </div>
    </div>

    <LoadingState v-if="loading" :label="copy.loading" />
    <p v-else-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-surface">
      <table>
        <thead>
          <tr>
            <th>{{ copy.area }}</th>
            <th>{{ copy.responsibility }}</th>
            <th>{{ copy.status }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in modules" :key="item.name">
            <td>{{ item.name }}</td>
            <td>{{ item.owner }}</td>
            <td>
              <span class="state-badge" :class="`state-${item.status}`">{{ item.label }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
