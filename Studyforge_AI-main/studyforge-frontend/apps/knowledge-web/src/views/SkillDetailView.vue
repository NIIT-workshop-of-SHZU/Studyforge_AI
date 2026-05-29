<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { ArrowLeft, BookOpen, Calendar, Clock, Loader, MapPin, Star, TrendingUp, Users } from '@lucide/vue';
import { getSkillDetail } from '@/api/skills';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { SkillDetail, RoadmapSummary } from '@/api/skills';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const skill = ref<SkillDetail | null>(null);
const loading = ref(false);
const errorMessage = ref('');

const skillCode = computed(() => (typeof route.params.skillCode === 'string' ? route.params.skillCode : ''));

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      back: 'Back to Skills',
      loading: 'Loading skill details',
      error: 'Skill not found',
      learners: 'Learners',
      posts: 'Posts',
      rating: 'Rating',
      roadmaps: 'Learning Roadmaps',
      noRoadmaps: 'No roadmaps available yet',
      days: 'days',
      nodes: 'nodes',
      startLearning: 'Start Learning',
      viewRoadmap: 'View Roadmap'
    };
  }

  return {
    back: '返回技能库',
    loading: '正在加载技能详情',
    error: '技能不存在',
    learners: '学习者',
    posts: '篇文章',
    rating: '评分',
    roadmaps: '学习路线',
    noRoadmaps: '暂无学习路线',
    days: '天',
    nodes: '个节点',
    startLearning: '开始学习',
    viewRoadmap: '查看路线'
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

async function loadSkill() {
  if (!skillCode.value) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    skill.value = await getSkillDetail(skillCode.value, preferencesStore.languageCode);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载技能详情失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadSkill);
</script>

<template>
  <section class="skill-detail-page">
    <RouterLink class="back-link" to="/skills">
      <ArrowLeft :size="18" />
      <span>{{ copy.back }}</span>
    </RouterLink>

    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.error" :description="errorMessage" />

    <template v-else-if="skill">
      <div class="skill-hero" :style="{ '--skill-color': difficultyColor(skill.difficulty) }">
        <div class="hero-content">
          <div class="hero-icon">
            <BookOpen :size="32" />
          </div>
          <div class="hero-info">
            <span class="difficulty-tag">{{ difficultyLabel(skill.difficulty) }}</span>
            <h1>{{ skill.name }}</h1>
            <p>{{ skill.description }}</p>
          </div>
        </div>

        <div class="hero-stats">
          <div class="stat-item">
            <Users :size="20" />
            <div>
              <strong>{{ skill.learnerCount }}</strong>
              <span>{{ copy.learners }}</span>
            </div>
          </div>
          <div class="stat-item">
            <BookOpen :size="20" />
            <div>
              <strong>{{ skill.postCount }}</strong>
              <span>{{ copy.posts }}</span>
            </div>
          </div>
          <div class="stat-item">
            <Star :size="20" />
            <div>
              <strong>{{ skill.ratingScore.toFixed(1) }}</strong>
              <span>{{ copy.rating }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="roadmaps-section">
        <h2>
          <MapPin :size="20" />
          {{ copy.roadmaps }}
        </h2>

        <div v-if="skill.roadmaps.length === 0" class="empty-roadmaps">
          <p>{{ copy.noRoadmaps }}</p>
        </div>

        <div v-else class="roadmaps-grid">
          <RouterLink
            v-for="roadmap in skill.roadmaps"
            :key="roadmap.roadmapId"
            :to="`/roadmaps/${roadmap.roadmapId}`"
            class="roadmap-card"
          >
            <div class="roadmap-header">
              <h3>{{ roadmap.title }}</h3>
              <span class="difficulty-mini" :style="{ color: difficultyColor(roadmap.difficulty) }">
                {{ difficultyLabel(roadmap.difficulty) }}
              </span>
            </div>

            <p v-if="roadmap.summary" class="roadmap-summary">{{ roadmap.summary }}</p>

            <div class="roadmap-meta">
              <span class="meta-item">
                <Calendar :size="14" />
                {{ roadmap.estimatedDays }} {{ copy.days }}
              </span>
              <span class="meta-item">
                <MapPin :size="14" />
                {{ roadmap.nodeCount }} {{ copy.nodes }}
              </span>
              <span class="meta-item">
                <Users :size="14" />
                {{ roadmap.learnerCount }}
              </span>
              <span class="meta-item">
                <Star :size="14" />
                {{ roadmap.ratingScore.toFixed(1) }}
              </span>
            </div>

            <div class="roadmap-footer">
              <span class="view-link">{{ copy.viewRoadmap }}</span>
            </div>
          </RouterLink>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.skill-detail-page {
  max-width: 1000px;
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

.skill-hero {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 32px;
  margin-bottom: 32px;
}

.hero-content {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.hero-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--skill-color) 12%, transparent);
  color: var(--skill-color);
  flex-shrink: 0;
}

.hero-info {
  flex: 1;
}

.difficulty-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  background: color-mix(in srgb, var(--skill-color) 12%, transparent);
  color: var(--skill-color);
  margin-bottom: 8px;
}

.hero-info h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 8px;
}

.hero-info p {
  font-size: 16px;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.6;
}

.hero-stats {
  display: flex;
  gap: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-item svg {
  color: var(--skill-color);
}

.stat-item strong {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

.stat-item span {
  font-size: 13px;
  color: var(--text-muted);
}

.roadmaps-section {
  margin-top: 40px;
}

.roadmaps-section h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 20px;
}

.empty-roadmaps {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.roadmaps-grid {
  display: grid;
  gap: 16px;
}

.roadmap-card {
  display: flex;
  flex-direction: column;
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  text-decoration: none;
  color: inherit;
  transition: all 0.2s var(--ease);
}

.roadmap-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
}

.roadmap-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.roadmap-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.difficulty-mini {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.roadmap-summary {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 16px;
  line-height: 1.6;
}

.roadmap-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-soft);
}

.roadmap-footer {
  margin-top: 16px;
}

.view-link {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
}

@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .hero-stats {
    justify-content: center;
  }

  .roadmap-header {
    flex-direction: column;
  }
}
</style>
