<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Activity, AlertTriangle, BarChart3, CheckCircle, TrendingUp, Users } from '@lucide/vue';
import { getAiUsageOverview, getAiUsageTrend, type AiUsageOverview, type AiDailyStats } from '@/api/ai-stats';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';

const preferencesStore = usePreferencesStore();

const overview = ref<AiUsageOverview | null>(null);
const trendData = ref<AiDailyStats[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      heading: 'AI Usage Dashboard',
      loading: 'Loading statistics...',
      loadFailed: 'Failed to load data. Please refresh and try again.',
      loadStatsFailed: 'Failed to load statistics',
      loadTrendFailed: 'Failed to load trend data',
      todayCalls: "Today's Calls",
      successRate: 'Success Rate',
      successfulCalls: 'Successful Calls',
      failed: 'Failed',
      calls: 'calls',
      uniqueUsers: 'Unique Users',
      activeUsersToday: 'Active users today',
      monthCalls: 'Monthly Total Calls',
      estimatedCost: 'Estimated Cost',
      featureDistribution: 'Feature Distribution (Last 30 Days)',
      trend7Days: '7-Day Call Trend',
    };
  }
  return {
    heading: 'AI 使用监控面板',
    loading: '正在加载统计数据',
    loadFailed: '加载数据失败，请刷新重试',
    loadStatsFailed: '加载统计数据失败',
    loadTrendFailed: '加载趋势数据失败',
    todayCalls: '今日调用次数',
    successRate: '成功率',
    successfulCalls: '成功调用',
    failed: '失败',
    calls: '次',
    uniqueUsers: '独立用户数',
    activeUsersToday: '今日活跃用户',
    monthCalls: '本月总调用',
    estimatedCost: '预估费用',
    featureDistribution: '功能调用分布（最近30天）',
    trend7Days: '近7天调用趋势',
  };
});

// 加载概览数据
async function loadOverview() {
  try {
    overview.value = await getAiUsageOverview();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadStatsFailed;
  }
}

// 加载趋势数据
async function loadTrend() {
  try {
    trendData.value = await getAiUsageTrend(7); // 最近7天
  } catch (error) {
    console.error(copy.value.loadTrendFailed, error);
  }
}

// 初始化加载
onMounted(async () => {
  loading.value = true;
  try {
    await Promise.all([loadOverview(), loadTrend()]);
  } catch (error) {
    errorMessage.value = copy.value.loadFailed;
  } finally {
    loading.value = false;
  }
});

// 格式化日期
function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return `${date.getMonth() + 1}/${date.getDate()}`;
}
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <div class="section-heading">
        <span>AI Monitoring</span>
        <h1>{{ copy.heading }}</h1>
      </div>
    </div>

    <LoadingState v-if="loading" :label="copy.loading" />
    
    <div v-if="errorMessage" class="error-alert">
      <AlertTriangle :size="20" />
      <span>{{ errorMessage }}</span>
    </div>

    <div v-if="!loading && overview" class="stats-dashboard">
      <!-- 今日统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon blue">
            <Activity :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-label">{{ copy.todayCalls }}</div>
            <div class="stat-value">{{ overview.todayTotalCalls }}</div>
            <div class="stat-trend">
              <TrendingUp :size="14" />
              <span>{{ copy.successRate }} {{ overview.todaySuccessRate }}%</span>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon green">
            <CheckCircle :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-label">{{ copy.successfulCalls }}</div>
            <div class="stat-value">{{ overview.todaySuccessfulCalls }}</div>
            <div class="stat-sub">{{ copy.failed }} {{ overview.todayFailedCalls }} {{ copy.calls }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon purple">
            <Users :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-label">{{ copy.uniqueUsers }}</div>
            <div class="stat-value">{{ overview.todayUniqueUsers }}</div>
            <div class="stat-sub">{{ copy.activeUsersToday }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon orange">
            <BarChart3 :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-label">{{ copy.monthCalls }}</div>
            <div class="stat-value">{{ overview.monthTotalCalls }}</div>
            <div class="stat-sub">{{ copy.estimatedCost }} ¥{{ overview.estimatedCost }}</div>
          </div>
        </div>
      </div>

      <!-- 功能分布 -->
      <div class="chart-section">
        <h2>{{ copy.featureDistribution }}</h2>
        <div class="distribution-list">
          <div 
            v-for="item in overview.featureDistribution" 
            :key="item.featureType"
            class="distribution-item"
          >
            <div class="dist-info">
              <span class="dist-type">{{ item.featureType }}</span>
              <span class="dist-count">{{ item.callCount }} {{ copy.calls }}</span>
            </div>
            <div class="dist-bar">
              <div 
                class="dist-fill" 
                :style="{ width: item.percentage + '%' }"
              ></div>
            </div>
            <span class="dist-percent">{{ item.percentage }}%</span>
          </div>
        </div>
      </div>

      <!-- 调用趋势图 -->
      <div class="chart-section">
        <h2>{{ copy.trend7Days }}</h2>
        <div class="trend-chart">
          <div v-for="day in trendData" :key="day.statDate" class="trend-bar-wrapper">
            <div 
              class="trend-bar"
              :style="{ height: (() => { const max = Math.max(...trendData.map(d => d.totalCalls)); return max > 0 ? (day.totalCalls / max * 100) + '%' : '4px'; })() }"
            ></div>
            <div class="trend-date">{{ formatDate(day.statDate) }}</div>
            <div class="trend-value">{{ day.totalCalls }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-section {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.section-heading span {
  font-size: 13px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.section-heading h1 {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin-top: 4px;
}

.error-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #991b1b;
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 16px;
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.blue { background: #dbeafe; color: #2563eb; }
.stat-icon.green { background: #d1fae5; color: #059669; }
.stat-icon.purple { background: #e9d5ff; color: #7c3aed; }
.stat-icon.orange { background: #fed7aa; color: #ea580c; }

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #059669;
}

.stat-sub {
  font-size: 13px;
  color: #9ca3af;
}

.chart-section {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.chart-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 20px;
}

.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dist-info {
  min-width: 180px;
  display: flex;
  justify-content: space-between;
}

.dist-type {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.dist-count {
  font-size: 14px;
  color: #6b7280;
}

.dist-bar {
  flex: 1;
  height: 8px;
  background: #f3f4f6;
  border-radius: 4px;
  overflow: hidden;
}

.dist-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.dist-percent {
  min-width: 50px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 200px;
  padding-top: 20px;
  gap: 12px;
}

.trend-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.trend-bar {
  width: 100%;
  max-width: 60px;
  background: linear-gradient(180deg, #3b82f6, #60a5fa);
  border-radius: 6px 6px 0 0;
  min-height: 4px;
  transition: all 0.3s ease;
}

.trend-bar:hover {
  opacity: 0.8;
}

.trend-date {
  font-size: 12px;
  color: #6b7280;
}

.trend-value {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}
</style>
