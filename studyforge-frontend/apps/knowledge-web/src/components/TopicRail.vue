<script setup lang="ts">
import { computed } from 'vue';
import { BookOpenCheck, Layers3 } from '@lucide/vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { TopicCategory } from '@/types/api';

defineProps<{
  categories: TopicCategory[];
  activeCode: string;
}>();

defineEmits<{
  select: [code: string];
}>();

const preferencesStore = usePreferencesStore();
const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        aria: 'Browse by topic',
        title: 'Browse by Topic'
      }
    : {
        aria: '按主题浏览',
        title: '按主题浏览'
      }
);
</script>

<template>
  <aside class="topic-rail" :aria-label="copy.aria">
    <div class="rail-title">
      <Layers3 :size="18" />
      <span>{{ copy.title }}</span>
    </div>

    <button
      v-for="category in categories"
      :key="category.code"
      class="topic-button"
      :class="{ active: category.code === activeCode }"
      type="button"
      :style="{ '--category-color': category.accent }"
      @click="$emit('select', category.code)"
    >
      <BookOpenCheck :size="17" />
      <span>
        <strong>{{ category.name }}</strong>
        <small>{{ category.description }}</small>
      </span>
    </button>
  </aside>
</template>
