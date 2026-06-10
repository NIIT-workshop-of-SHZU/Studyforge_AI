<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { Heart, MessageSquareReply, Trash2, UserRound } from '@lucide/vue';
import MentionTextarea from '@/components/MentionTextarea.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';

interface ForumThreadNode {
  id: number;
  userId: number;
  authorUsername: string;
  authorName: string;
  authorAvatarUrl: string;
  parentAuthorName: string;
  content: string;
  floorNo: number;
  likeCount: number;
  likedByViewer: boolean;
  canDelete: boolean;
  deleted: boolean;
  accepted?: boolean;
  createdLabel: string;
  replies: ForumThreadNode[];
}

const props = withDefaults(
  defineProps<{
    node: ForumThreadNode;
    replyingToId: number | null;
    replyText: string;
    depth?: number;
    submitLabel?: string;
    replyPlaceholder?: string;
  }>(),
  {
    depth: 0
  }
);

const emit = defineEmits<{
  reply: [node: ForumThreadNode];
  cancelReply: [];
  updateReplyText: [value: string];
  submitReply: [];
  like: [node: ForumThreadNode];
  delete: [node: ForumThreadNode];
}>();

const preferencesStore = usePreferencesStore();
const depthClass = computed(() => `depth-${Math.min(props.depth, 3)}`);

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      floor: 'Floor',
      replyTo: 'Reply to',
      accepted: 'Accepted',
      reply: 'Reply',
      delete: 'Delete',
      cancel: 'Cancel',
      submitReply: 'Send reply',
      replyPlaceholder: 'Write a reply. Type @username to notify someone.'
    };
  }

  return {
    floor: '楼',
    replyTo: '回复',
    accepted: '已采纳',
    reply: '回复',
    delete: '删除',
    cancel: '取消',
    submitReply: '发送回复',
    replyPlaceholder: '写下回复，输入 @用户名 可以提醒对方'
  };
});

const resolvedSubmitLabel = computed(() => props.submitLabel ?? copy.value.submitReply);
const resolvedReplyPlaceholder = computed(() => props.replyPlaceholder ?? copy.value.replyPlaceholder);
</script>

<template>
  <article class="forum-comment" :class="[depthClass, { deleted: node.deleted, accepted: node.accepted }]">
    <RouterLink class="forum-comment-avatar" :to="`/users/${node.userId}`" :aria-label="node.authorName">
      <img v-if="node.authorAvatarUrl" :src="node.authorAvatarUrl" alt="" />
      <UserRound v-else :size="20" />
    </RouterLink>

    <div class="forum-comment-body">
      <header class="forum-comment-head">
        <div class="forum-comment-author">
          <RouterLink :to="`/users/${node.userId}`">{{ node.authorName }}</RouterLink>
          <span>@{{ node.authorUsername }}</span>
        </div>
        <div class="forum-comment-meta">
          <span>{{ node.floorNo }} {{ copy.floor }}</span>
          <span v-if="node.parentAuthorName">{{ copy.replyTo }} {{ node.parentAuthorName }}</span>
          <span v-if="node.accepted" class="accepted-pill">{{ copy.accepted }}</span>
          <time v-if="node.createdLabel">{{ node.createdLabel }}</time>
        </div>
      </header>

      <MarkdownRenderer class="comment-markdown forum-comment-markdown" :content="node.content" />

      <footer v-if="!node.deleted" class="forum-comment-actions">
        <button class="ghost-button" :class="{ active: node.likedByViewer }" type="button" @click="emit('like', node)">
          <Heart :size="15" />
          <span>{{ node.likeCount }}</span>
        </button>
        <button class="ghost-button" type="button" @click="emit('reply', node)">
          <MessageSquareReply :size="15" />
          <span>{{ copy.reply }}</span>
        </button>
        <button v-if="node.canDelete" class="ghost-button danger" type="button" @click="emit('delete', node)">
          <Trash2 :size="15" />
          <span>{{ copy.delete }}</span>
        </button>
      </footer>

      <form v-if="replyingToId === node.id" class="comment-form forum-reply-form" @submit.prevent="emit('submitReply')">
        <MentionTextarea
          :model-value="replyText"
          rows="3"
          :placeholder="resolvedReplyPlaceholder"
          @update:model-value="emit('updateReplyText', $event)"
        />
        <div class="forum-reply-actions">
          <button class="secondary-button" type="button" @click="emit('cancelReply')">{{ copy.cancel }}</button>
          <button class="primary-button" type="submit">{{ resolvedSubmitLabel }}</button>
        </div>
      </form>

      <div v-if="node.replies.length" class="forum-replies">
        <ForumThreadItem
          v-for="reply in node.replies"
          :key="reply.id"
          :node="reply"
          :replying-to-id="replyingToId"
          :reply-text="replyText"
          :depth="depth + 1"
          :submit-label="resolvedSubmitLabel"
          :reply-placeholder="resolvedReplyPlaceholder"
          @reply="emit('reply', $event)"
          @cancel-reply="emit('cancelReply')"
          @update-reply-text="emit('updateReplyText', $event)"
          @submit-reply="emit('submitReply')"
          @like="emit('like', $event)"
          @delete="emit('delete', $event)"
        />
      </div>
    </div>
  </article>
</template>
