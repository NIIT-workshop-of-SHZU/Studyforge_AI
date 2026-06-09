<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { CalendarClock, Eye, Heart, Languages, MessageCircle, PencilLine, UserRound } from '@lucide/vue';
import topicAiUrl from '@/assets/topic-ai.svg';
import topicHelpUrl from '@/assets/topic-help.svg';
import topicLearningUrl from '@/assets/topic-learning.svg';
import topicSystemsUrl from '@/assets/topic-systems.svg';
import topicWritingUrl from '@/assets/topic-writing.svg';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { PostSummary, TopicCategory } from '@/types/api';
import { formatRelativeTime } from '@/utils/date';

const props = defineProps<{
  post: PostSummary;
  category: TopicCategory;
  index: number;
}>();

const router = useRouter();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

function openPost() {
  router.push({ path: `/posts/${props.post.postId}`, query: { language: props.post.languageCode } });
}

const coverUrl = computed(() => {
  const covers: Record<string, string> = {
    AI: topicAiUrl,
    HELP: topicHelpUrl,
    LANGUAGE: topicWritingUrl,
    TECHNOLOGY: topicSystemsUrl
  };

  return props.post.coverImageUrl || covers[props.category.code] || (props.index % 2 === 0 ? topicLearningUrl : topicSystemsUrl);
});

const canEdit = computed(() => sessionStore.isAuthenticated && sessionStore.userId === props.post.authorId);
const publishTime = computed(() => formatRelativeTime(props.post.createdTime, preferencesStore.languageCode));
const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        editTitle: 'Edit post',
        edit: 'Edit'
      }
    : {
        editTitle: '编辑帖子',
        edit: '编辑'
      }
);
</script>

<template>
  <article
    class="knowledge-card knowledge-card--clickable"
    :style="{ '--category-color': category.accent, '--card-delay': `${index * 55}ms` }"
    @click="openPost"
  >
    <img class="knowledge-cover" :src="coverUrl" alt="" loading="eager" />

    <div class="knowledge-card-body">
      <div class="post-meta">
        <span class="category-pill">
          {{ category.name }}
        </span>
        <span>
          <Languages :size="15" />
          {{ post.languageCode }}
        </span>
        <span v-if="publishTime">
          <CalendarClock :size="15" />
          {{ publishTime }}
        </span>
      </div>

      <h2>{{ post.title }}</h2>
      <p>{{ post.summary }}</p>

      <RouterLink class="card-author" :to="`/users/${post.authorId}`" @click.stop>
        <img v-if="post.authorAvatarUrl" :src="post.authorAvatarUrl" alt="" loading="lazy" />
        <span v-else>
          <UserRound :size="14" />
        </span>
        <strong>{{ post.authorName || `#${post.authorId}` }}</strong>
      </RouterLink>

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

        <div v-if="canEdit" class="card-actions">
          <RouterLink class="card-edit-link" :to="`/posts/${post.postId}/edit`" :title="copy.editTitle" @click.stop>
            <PencilLine :size="16" />
            <span>{{ copy.edit }}</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </article>
</template>
