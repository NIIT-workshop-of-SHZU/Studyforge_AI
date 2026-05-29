<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { ArrowLeft, BookOpen, Calendar, Check, Clock, Loader, MapPin, Play, Star, Users } from '@lucide/vue';
import { getRoadmapDetail, startRoadmap, completeNode, getRoadmapProgress } from '@/api/skills';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { RoadmapDetail, RoadmapNode, UserRoadmapProgress } from '@/api/skills';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const sessionStore = useSessionStore();
const roadmap = ref<RoadmapDetail | null>(null);
const progress = ref<UserRoadmapProgress | null>(null);
const loading = ref(false);
const starting = ref(false);
const completingNodeId = ref<number | null>(null);
const errorMessage = ref('');

const roadmapId = computed(() => {
  const id = route.params.roadmapId;
  return typeof id === 'string' ? parseInt(id, 10) : 0;
});

const completedNodeIds = computed(() => {
  if (!progress.value) {
    return new Set<number>();
  }
  return new Set(progress.value.nodes.filter((_, index) => index < progress.value!.completedNodes).map(n => n.nodeId));
});

const isStarted = computed(() => progress.value !== null);
const isCompleted = computed(() => progress.value?.status === 'COMPLETED');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      back: 'Back',
      loading: 'Loading roadmap',
      error: 'Roadmap not found',
      skill: 'Skill',
      difficulty: 'Difficulty',
      days: 'days',
      estimatedTime: 'Estimated time',
      minutes: 'min',
      learners: 'learners',
      rating: 'rating',
      nodes: 'Learning Nodes',
      startRoadmap: 'Start Learning',
      continueRoadmap: 'Continue Learning',
      markComplete: 'Mark as Complete',
      completed: 'Completed',
      progress: 'Progress',
      loginRequired: 'Please login to start learning'
    };
  }

  return {
    back: '返回',
    loading: '正在加载路线',
    error: '路线不存在',
    skill: '技能',
    difficulty: '难度',
    days: '天',
    estimatedTime: '预计时间',
    minutes: '分钟',
    learners: '学习者',
    rating: '评分',
    nodes: '学习节点',
    startRoadmap: '开始学习',
    continueRoadmap: '继续学习',
    markComplete: '标记完成',
    completed: '已完成',
    progress: '进度',
    loginRequired: '请先登录再开始学习'
  };
});

const difficultyLabel = (difficulty: string) => {
  const labels: Record<string, { zh: string; en: string }> = {
    BEGINNER: { zh: '入门', en: 'Beginner' },
    INTERMEDIATE: { zh: '进阶', en: 'Intermediate' },
    ADVANCED: { zh: '高级', en: 'Advanced' }
  };
  const label = labels[difficulty] || labels.BEGINNER;
  return preferencesStore.languageCode === 'en_US' ? label.en : label.zh;
};

const difficultyColor = (difficulty: string) => {
  const colors: Record<string, string> = {
    BEGINNER: '#15803d',
    INTERMEDIATE: '#2563eb',
    ADVANCED: '#7c3aed'
  };
  return colors[difficulty] || '#0f766e';
};

const totalEstimatedMinutes = computed(() => {
  if (!roadmap.value) return 0;
  return roadmap.value.nodes.reduce((sum, node) => sum + node.estimatedMinutes, 0);
});

const progressPercent = computed(() => {
  if (!progress.value || !roadmap.value) return 0;
  return Math.round(progress.value.progressPercent);
});

async function loadRoadmap() {
  if (!roadmapId.value) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    roadmap.value = await getRoadmapDetail(roadmapId.value, preferencesStore.languageCode);

    if (sessionStore.isAuthenticated) {
      try {
        progress.value = await getRoadmapProgress(roadmapId.value);
      } catch {
        progress.value = null;
      }
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载路线详情失败';
  } finally {
    loading.value = false;
  }
}

async function handleStart() {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginRequired;
    return;
  }

  starting.value = true;
  errorMessage.value = '';

  try {
    await startRoadmap(roadmapId.value);
    progress.value = await getRoadmapProgress(roadmapId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '开始学习失败';
  } finally {
    starting.value = false;
  }
}

async function handleCompleteNode(nodeId: number) {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginRequired;
    return;
  }

  completingNodeId.value = nodeId;
  errorMessage.value = '';

  try {
    progress.value = await completeNode(nodeId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '标记完成失败';
  } finally {
    completingNodeId.value = null;
  }
}

onMounted(loadRoadmap);
</script>

<template>
  <section class="roadmap-detail-page">
    <RouterLink class="back-link" :to="roadmap ? `/skills/${roadmap.skillCode || ''}` : '/skills'">
      <ArrowLeft :size="18" />
      <span>{{ copy.back }}</span>
    </RouterLink>

    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.error" :description="errorMessage" />

    <template v-else-if="roadmap">
      <div class="roadmap-hero" :style="{ '--roadmap-color': difficultyColor(roadmap.difficulty) }">
        <div class="hero-content">
          <h1>{{ roadmap.title }}</h1>
          <p v-if="roadmap.summary" class="hero-summary">{{ roadmap.summary }}</p>

          <div class="hero-meta">
            <span class="meta-tag">
              <BookOpen :size="14" />
              {{ roadmap.skillName }}
            </span>
            <span class="meta-tag">
              <Calendar :size="14" />
              {{ roadmap.estimatedDays }} {{ copy.days }}
            </span>
            <span class="meta-tag">
              <Clock :size="14" />
              {{ totalEstimatedMinutes }} {{ copy.minutes }}
            </span>
            <span class="meta-tag">
              <Users :size="14" />
              {{ roadmap.learnerCount }} {{ copy.learners }}
            </span>
            <span class="meta-tag">
              <Star :size="14" />
              {{ roadmap.ratingScore.toFixed(1) }}
            </span>
          </div>
        </div>

        <div v-if="isStarted" class="progress-panel">
          <div class="progress-header">
            <span class="progress-label">{{ copy.progress }}</span>
            <span class="progress-value">{{ progressPercent }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <span class="progress-detail">
            {{ progress?.completedNodes || 0 }} / {{ roadmap.nodeCount }} {{ copy.nodes }}
          </span>
        </div>

        <div class="hero-actions">
          <button
            v-if="!isStarted"
            class="start-button"
            type="button"
            :disabled="starting"
            @click="handleStart"
          >
            <Play :size="18" />
            <span>{{ starting ? '...' : copy.startRoadmap }}</span>
          </button>
          <span v-else-if="isCompleted" class="completed-badge">
            <Check :size="18" />
            {{ copy.completed }}
          </span>
        </div>
      </div>

      <div class="nodes-section">
        <h2>
          <MapPin :size="20" />
          {{ copy.nodes }}
        </h2>

        <div class="nodes-timeline">
          <div
            v-for="(node, index) in roadmap.nodes"
            :key="node.nodeId"
            class="node-item"
            :class="{
              completed: completedNodeIds.has(node.nodeId),
              current: progress?.currentNodeId === node.nodeId
            }"
          >
            <div class="node-indicator">
              <div class="node-number">
                <Check v-if="completedNodeIds.has(node.nodeId)" :size="16" />
                <span v-else>{{ index + 1 }}</span>
              </div>
              <div v-if="index < roadmap.nodes.length - 1" class="node-line"></div>
            </div>

            <div class="node-content">
              <div class="node-header">
                <h3>{{ node.title }}</h3>
                <span class="node-time">
                  <Clock :size="14" />
                  {{ node.estimatedMinutes }} {{ copy.minutes }}
                </span>
              </div>

              <p v-if="node.description" class="node-description">{{ node.description }}</p>

              <div class="node-actions">
                <RouterLink
                  v-if="node.relatedPostId"
                  :to="`/posts/${node.relatedPostId}`"
                  class="related-link"
                >
                  <BookOpen :size="14" />
                  {{ preferencesStore.languageCode === 'en_US' ? 'View Article' : '查看文章' }}
                </RouterLink>

                <button
                  v-if="isStarted && !isCompleted && !completedNodeIds.has(node.nodeId)"
                  class="complete-button"
                  type="button"
                  :disabled="completingNodeId === node.nodeId"
                  @click="handleCompleteNode(node.nodeId)"
                >
                  <Check :size="14" />
                  <span>{{ completingNodeId === node.nodeId ? '...' : copy.markComplete }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.roadmap-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 24px;
  transition: color 0.2s;
}

.back-link:hover {
  color: var(--primary);
}

.roadmap-hero {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 32px;
  margin-bottom: 32px;
}

.hero-content {
  margin-bottom: 24px;
}

.hero-content h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 12px;
}

.hero-summary {
  font-size: 16px;
  color: var(--text-muted);
  margin: 0 0 20px;
  line-height: 1.6;
}

.hero-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-soft);
}

.progress-panel {
  background: var(--surface-muted);
  border-radius: var(--radius);
  padding: 16px;
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.progress-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--roadmap-color);
}

.progress-bar {
  height: 8px;
  background: var(--border);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: var(--roadmap-color);
  border-radius: 4px;
  transition: width 0.3s var(--ease);
}

.progress-detail {
  font-size: 13px;
  color: var(--text-muted);
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.start-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 0;
  border-radius: var(--radius);
  background: var(--roadmap-color);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.start-button:hover:not(:disabled) {
  opacity: 0.9;
}

.start-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.completed-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: var(--radius);
  background: var(--success-soft);
  color: var(--success);
  font-size: 16px;
  font-weight: 600;
}

.nodes-section {
  margin-top: 40px;
}

.nodes-section h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 24px;
}

.nodes-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.node-item {
  display: flex;
  gap: 16px;
}

.node-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 40px;
}

.node-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--surface-muted);
  border: 2px solid var(--border);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-muted);
  transition: all 0.2s;
}

.node-item.completed .node-number {
  background: var(--success);
  border-color: var(--success);
  color: white;
}

.node-item.current .node-number {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.node-line {
  width: 2px;
  flex: 1;
  min-height: 24px;
  background: var(--border);
}

.node-item.completed .node-line {
  background: var(--success);
}

.node-content {
  flex: 1;
  padding-bottom: 24px;
}

.node-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.node-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.node-item.completed .node-header h3 {
  color: var(--text-muted);
}

.node-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-soft);
  white-space: nowrap;
}

.node-description {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 12px;
  line-height: 1.6;
}

.node-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.related-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--primary);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
}

.related-link:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.complete-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.complete-button:hover:not(:disabled) {
  opacity: 0.9;
}

.complete-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .hero-content h1 {
    font-size: 24px;
  }

  .hero-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
