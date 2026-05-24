<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowUpRight, Eye, Flame, Heart, Languages, MessageCircle } from '@lucide/vue';
import topicAiUrl from '@/assets/topic-ai.svg';
import topicHelpUrl from '@/assets/topic-help.svg';
import topicLearningUrl from '@/assets/topic-learning.svg';
import topicSystemsUrl from '@/assets/topic-systems.svg';
import topicWritingUrl from '@/assets/topic-writing.svg';
import type { PostSummary, TopicCategory } from '@/types/api';

const props = defineProps<{
  post: PostSummary;
  category: TopicCategory;
  index: number;
}>();

const coverUrl = computed(() => {
  const covers: Record<string, string> = {
    AI: topicAiUrl,
    HELP: topicHelpUrl,
    LANGUAGE: topicWritingUrl,
    TECHNOLOGY: topicSystemsUrl
  };

  return props.post.coverImageUrl || covers[props.category.code] || (props.index % 2 === 0 ? topicLearningUrl : topicSystemsUrl);
});
</script>

<template>
  <article class="knowledge-card" :style="{ '--category-color': category.accent, '--card-delay': `${index * 55}ms` }">
    <img class="knowledge-cover" :src="coverUrl" alt="" loading="lazy" />

    <div class="knowledge-card-body">
      <div class="post-meta">
        <span class="score-pill">
          <Flame :size="15" />
          {{ post.hotScore.toFixed(1) }}
        </span>
        <span class="category-pill">
          {{ category.name }}
        </span>
        <span>
          <Languages :size="15" />
          {{ post.languageCode }}
        </span>
      </div>

      <h2>{{ post.title }}</h2>
      <p>{{ post.summary }}</p>

      <div class="post-card-footer">
        <div class="post-stats">
          <span>
            <Heart :size="15" />
            {{ post.favoriteCount }}
          </span>
          <span>
            <MessageCircle :size="15" />
            {{ post.commentCount }}
          </span>
          <span>
            <Eye :size="15" />
            {{ post.viewCount }}
          </span>
        </div>

        <RouterLink class="card-link" :to="{ path: `/posts/${post.postId}`, query: { language: post.languageCode } }">
          <span>阅读全文</span>
          <ArrowUpRight :size="17" />
        </RouterLink>
      </div>
    </div>
  </article>
</template>
