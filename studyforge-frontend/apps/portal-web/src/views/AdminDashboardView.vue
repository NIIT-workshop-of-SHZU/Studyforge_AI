<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Activity, Database, Layers3, RefreshCw, ServerCog } from '@lucide/vue';
import { getHealth } from '@/api/health';
import LoadingState from '@/components/LoadingState.vue';
import type { HealthStatus } from '@/types/api';

const health = ref<HealthStatus | null>(null);
const loading = ref(false);
const errorMessage = ref('');

const modules = [
  { name: '账号与权限', owner: '数据库账号、角色、登录令牌', status: 'live', label: '已接入' },
  { name: '内容库', owner: '文章、分类、原始语言内容', status: 'live', label: '已接入' },
  { name: '学习记录', owner: '收藏、浏览记录、复习资料', status: 'live', label: '已接入' },
  { name: 'AI 助手', owner: '摘要、问答、学习卡片、Markdown 排版', status: 'live', label: '已接入' },
  { name: '语音学习', owner: '文本朗读与调用记录', status: 'live', label: '已接入' },
  { name: '求助讨论', owner: '提问、回答、采纳状态', status: 'live', label: '已接入' },
  { name: '系统设置', owner: 'AI 与语音供应商配置', status: 'live', label: '已接入' }
];

async function loadHealth() {
  loading.value = true;
  errorMessage.value = '';

  try {
    health.value = await getHealth();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '服务状态暂时没取到';
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
        <span>Operations</span>
        <h1>运营看板</h1>
      </div>

      <button class="secondary-button" type="button" :disabled="loading" @click="loadHealth">
        <RefreshCw :size="17" />
        <span>刷新</span>
      </button>
    </div>

    <div class="metric-grid">
      <div class="metric-card">
        <ServerCog :size="20" />
        <span>Web API</span>
        <strong>{{ health?.service || 'studyforge-webapi' }}</strong>
      </div>
      <div class="metric-card">
        <Activity :size="20" />
        <span>状态</span>
        <strong>{{ health?.status || (loading ? 'CHECKING' : 'UNKNOWN') }}</strong>
      </div>
      <div class="metric-card">
        <Database :size="20" />
        <span>数据库</span>
        <strong>test_studyforge_ai_v2</strong>
      </div>
      <div class="metric-card">
        <Layers3 :size="20" />
        <span>控制台</span>
        <strong>portal-web</strong>
      </div>
    </div>

    <LoadingState v-if="loading" label="正在查看服务状态" />
    <p v-else-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-surface">
      <table>
        <thead>
          <tr>
            <th>区域</th>
            <th>可以管理</th>
            <th>状态</th>
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
