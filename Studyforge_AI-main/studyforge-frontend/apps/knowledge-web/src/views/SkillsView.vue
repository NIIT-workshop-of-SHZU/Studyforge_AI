<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { BookOpen, Compass, GraduationCap, Loader, Search, Star, TrendingUp, Users } from '@lucide/vue';
import { getSkills } from '@/api/skills';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { SkillSummary } from '@/api/skills';

const preferencesStore = usePreferencesStore();
const skills = ref<SkillSummary[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const keyword = ref('');
const activeDifficulty = ref('ALL');

const difficulties = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return [
      { code: 'ALL', name: 'All Levels' },
      { code: 'BEGINNER', name: 'Beginner' },
      { code: 'INTERMEDIATE', name: 'Intermediate' },
      { code: 'ADVANCED', name: 'Advanced' }
    ];
  }

  return [
    { code: 'ALL', name: '全部难度' },
    { code: 'BEGINNER', name: '入门' },
    { code: 'INTERMEDIATE', name: '进阶' },
    { code: 'ADVANCED', name: '高级' }
  ];
});

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      title: 'Skill Library',
      subtitle: 'Choose a skill to learn, like managing a game library. Track your progress and master new technologies.',
      search: 'Search skills...',
      loading: 'Loading skills',
      emptyTitle: 'No skills found',
      emptyDescription: 'Try a different search or difficulty level.',
      learners: 'learners',
      posts: 'posts',
      rating: 'rating'
    };
  }

  return {
    title: '技能库',
    subtitle: '像管理游戏库一样选择学习技能，追踪进度，掌握新技术。',
    search: '搜索技能...',
    loading: '正在加载技能',
    emptyTitle: '没有找到技能',
    emptyDescription: '试试其他搜索词或难度等级',
    learners: '学习者',
    posts: '篇文章',
    rating: '评分'
  };
});

const filteredSkills = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase();

  return skills.value.filter((skill) => {
    const matchesDifficulty = activeDifficulty.value === 'ALL' || skill.difficulty === activeDifficulty.value;
    const matchesKeyword =
      !normalizedKeyword ||
      skill.name.toLowerCase().includes(normalizedKeyword) ||
      skill.description.toLowerCase().includes(normalizedKeyword);

    return matchesDifficulty && matchesKeyword;
  });
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

async function loadSkills() {
  loading.value = true;
  errorMessage.value = '';

  try {
    skills.value = await getSkills(preferencesStore.languageCode);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载技能列表失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadSkills);
</script>

<template>
  <section class="skills-page">
    <div class="page-heading">
      <span class="section-kicker">
        <GraduationCap :size="18" />
        Skill Library
      </span>
      <h1>{{ copy.title }}</h1>
      <p>{{ copy.subtitle }}</p>
    </div>

    <div class="skills-toolbar">
      <label class="inline-search" for="skills-search">
        <Search :size="17" />
        <input id="skills-search" v-model.trim="keyword" type="search" :placeholder="copy.search" />
      </label>

      <div class="difficulty-filters">
        <button
          v-for="diff in difficulties"
          :key="diff.code"
          type="button"
          class="difficulty-chip"
          :class="{ active: activeDifficulty === diff.code }"
          @click="activeDifficulty = diff.code"
        >
          {{ diff.name }}
        </button>
      </div>
    </div>

    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.emptyTitle" :description="errorMessage" />
    <EmptyState v-else-if="filteredSkills.length === 0" :title="copy.emptyTitle" :description="copy.emptyDescription" />

    <div v-else class="skills-grid">
      <RouterLink
        v-for="skill in filteredSkills"
        :key="skill.skillId"
        :to="`/skills/${skill.skillCode}`"
        class="skill-card"
        :style="{ '--skill-color': difficultyColor(skill.difficulty) }"
      >
        <div class="skill-card-header">
          <div class="skill-icon">
            <BookOpen :size="24" />
          </div>
          <span class="difficulty-badge">{{ difficultyLabel(skill.difficulty) }}</span>
        </div>

        <h2>{{ skill.name }}</h2>
        <p>{{ skill.description }}</p>

        <div class="skill-stats">
          <span class="stat">
            <Users :size="14" />
            {{ skill.learnerCount }} {{ copy.learners }}
          </span>
          <span class="stat">
            <BookOpen :size="14" />
            {{ skill.postCount }} {{ copy.posts }}
          </span>
          <span class="stat">
            <Star :size="14" />
            {{ skill.ratingScore.toFixed(1) }}
          </span>
        </div>

        <div class="skill-card-footer">
          <span class="explore-link">
            <Compass :size="16" />
            {{ preferencesStore.languageCode === 'en_US' ? 'Explore' : '探索' }}
          </span>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.skills-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-heading {
  margin-bottom: 32px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.page-heading h1 {
  font-size: 32px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 8px;
}

.page-heading p {
  font-size: 16px;
  color: var(--text-muted);
  margin: 0;
  max-width: 600px;
}

.skills-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.inline-search {
  display: flex;
  align-items: center;
  min-height: 42px;
  width: min(400px, 100%);
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.inline-search:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.14);
}

.inline-search svg {
  color: var(--text-soft);
  flex-shrink: 0;
}

.inline-search input {
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  padding: 8px 10px;
  font-size: 14px;
  color: var(--text);
}

.difficulty-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.difficulty-chip {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.difficulty-chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.difficulty-chip.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.skill-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  text-decoration: none;
  color: inherit;
  transition: all 0.3s var(--ease);
  cursor: pointer;
}

.skill-card:hover {
  transform: translateY(-4px);
  border-color: var(--skill-color);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.04);
}

.skill-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--skill-color);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.skill-card:hover::before {
  opacity: 1;
}

.skill-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.skill-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--skill-color) 12%, transparent);
  color: var(--skill-color);
}

.difficulty-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  background: color-mix(in srgb, var(--skill-color) 12%, transparent);
  color: var(--skill-color);
}

.skill-card h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 8px;
}

.skill-card p {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 16px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.skill-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-soft);
}

.skill-card-footer {
  margin-top: 16px;
}

.explore-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
}

@media (max-width: 768px) {
  .skills-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .inline-search {
    width: 100%;
  }

  .difficulty-filters {
    justify-content: center;
  }

  .skills-grid {
    grid-template-columns: 1fr;
  }
}
</style>
