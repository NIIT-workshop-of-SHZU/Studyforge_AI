<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import {
  ArrowLeft,
  BookMarked,
  Bookmark,
  Bot,
  CalendarClock,
  Flag,
  Flame,
  Languages,
  MessageSquareText,
  PencilLine,
  Send,
  ThumbsUp,
  Volume2
} from '@lucide/vue';
import { generateReviewCards, generateSummary } from '@/api/ai';
import { ApiError } from '@/api/http';
import {
  createComment,
  deleteComment,
  getComments,
  getInteractionState,
  recordPostView,
  reportPost,
  toggleCommentLike,
  toggleFavorite,
  toggleLike
} from '@/api/interactions';
import { getPostDetail } from '@/api/posts';
import { textToSpeech } from '@/api/voice';
import EmptyState from '@/components/EmptyState.vue';
import ForumThreadItem from '@/components/ForumThreadItem.vue';
import LoadingState from '@/components/LoadingState.vue';
import MentionTextarea from '@/components/MentionTextarea.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { AiResult, CommentItem, PostDetail, PostInteractionState, VoiceResult } from '@/types/api';
import { categoryForCode } from '@/i18n/categories';
import { languageLabel } from '@/i18n/labels';
import { formatDateTime, formatShortDateTime } from '@/utils/date';

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
  createdLabel: string;
  replies: ForumThreadNode[];
}

const route = useRoute();
const router = useRouter();
const preferencesStore = usePreferencesStore();
const sessionStore = useSessionStore();
const post = ref<PostDetail | null>(null);
const comments = ref<CommentItem[]>([]);
const interaction = ref<PostInteractionState | null>(null);
const aiSummary = ref<AiResult | null>(null);
const reviewCards = ref<AiResult | null>(null);
const voice = ref<VoiceResult | null>(null);
const newComment = ref('');
const replyComment = ref('');
const replyingToComment = ref<CommentItem | null>(null);
const reportReason = ref('');
const reportSuccess = ref('');
const aiLoading = ref('');
const reportLoading = ref(false);
const actionError = ref('');
const loading = ref(false);
const errorMessage = ref('');
const postId = computed(() => String(route.params.postId));
const contentLanguage = computed(() => (typeof route.query.language === 'string' && route.query.language ? route.query.language : undefined));
const postCreatedTime = computed(() => formatDateTime(post.value?.createdTime, preferencesStore.languageCode));
const postUpdatedTime = computed(() => formatDateTime(post.value?.updatedTime, preferencesStore.languageCode));
const commentTree = computed(() => buildCommentTree(comments.value));
const categoryName = computed(() => (post.value ? categoryForCode(preferencesStore.languageCode, post.value.categoryCode).name : ''));
const alternateLanguages = computed(() => {
  const available = post.value?.availableLanguages ?? [];
  if (!available.length) {
    return [];
  }
  return available.filter((language) => language !== post.value?.languageCode);
});

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      back: 'Back to feed',
      loading: 'Opening article',
      loadErrorTitle: 'Article unavailable',
      loadErrorFallback: 'This article could not be loaded.',
      views: 'views',
      edit: 'Edit',
      liked: 'Liked',
      like: 'Like',
      favorited: 'Saved',
      favorite: 'Save',
      voiceLoading: 'Generating',
      voice: 'Read summary',
      sessionExpired: 'Your session expired. You can keep reading, but sign in again to interact.',
      loginRequired: 'Please sign in to use this feature.',
      actionFailed: 'The action did not complete successfully.',
      deleteCommentConfirm: 'Delete this comment? The floor number and replies will be kept.',
      deletedComment: 'This comment was deleted.',
      reportReasonRequired: 'Please describe the issue so moderators can review it.',
      reportSubmitted: (reportId: number) => `Report submitted. Reference #${reportId}.`,
      discussion: 'Discussion',
      noDiscussionTitle: 'No discussion yet',
      noDiscussionDesc: 'Leave a question or note after reading.',
      commentPlaceholder: 'Share a question, note, or reaction. Type @ to mention a friend.',
      replyPlaceholder: 'Write a reply. Type @username to notify someone.',
      submitReply: 'Send reply',
      send: 'Send',
      aiAssistant: 'AI study helper',
      summaryLoading: 'Summarizing',
      summary: 'Generate summary',
      cardsLoading: 'Generating',
      cards: 'Generate review cards',
      articleInfo: 'Article info',
      semanticTags: 'Smart tags',
      semanticTagsHint: 'AI-extracted topics for matching with your MEMORY.md',
      author: 'Author',
      postId: 'ID',
      published: 'Published',
      updated: 'Updated',
      comments: 'Comments',
      reportTitle: 'Report article',
      reportPlaceholder: 'Describe the issue, such as spam, misleading content, harassment, or other community violations.',
      reportLoading: 'Submitting',
      report: 'Submit report',
      afterReadingTitle: 'After reading',
      afterReadingText: 'Write three sentences with the core takeaway, then one question you still want to ask.',
      articleLanguage: 'Article language',
      readIn: (language: string) => `Read in ${languageLabel(language)}`
    };
  }

  return {
    back: '返回知识流',
    loading: '正在打开文章',
    loadErrorTitle: '文章暂时打不开',
    loadErrorFallback: '这篇内容暂时没取到',
    views: '次阅读',
    edit: '编辑文章',
    liked: '已赞',
    like: '点赞',
    favorited: '已收藏',
    favorite: '收藏',
    voiceLoading: '生成中',
    voice: '朗读摘要',
    sessionExpired: '登录状态已过期，文章可以继续阅读，互动功能请重新登录后使用。',
    loginRequired: '请先登录再使用这个功能',
    actionFailed: '操作暂时没有成功',
    deleteCommentConfirm: '确定删除这条评论吗？删除后会保留楼层和回复关系。',
    deletedComment: '这条评论已删除',
    reportReasonRequired: '请写下举报原因，方便管理员判断',
    reportSubmitted: (reportId: number) => `举报已提交，编号 #${reportId}`,
    discussion: '讨论',
    noDiscussionTitle: '还没有讨论',
    noDiscussionDesc: '读完后可以留下一个问题或补充。',
    commentPlaceholder: '写下你的问题、补充或读后想法，输入 @ 可以选择好友',
    replyPlaceholder: '写下回复，输入 @用户名 可以提醒对方',
    submitReply: '发送回复',
    send: '发送',
    aiAssistant: 'AI 学习助手',
    summaryLoading: '正在整理',
    summary: '生成摘要',
    cardsLoading: '正在生成',
    cards: '生成复习卡片',
    articleInfo: '文章信息',
    semanticTags: '智能标签',
    semanticTagsHint: '系统从正文切割的主题标签，用于和 MEMORY.md 做模糊匹配',
    author: '作者',
    postId: '编号',
    published: '发布',
    updated: '更新',
    comments: '评论',
    reportTitle: '举报文章',
    reportPlaceholder: '说明哪里可能有问题，例如广告、误导、攻击或其他不适合社区的内容',
    reportLoading: '提交中',
    report: '提交举报',
    afterReadingTitle: '读完可以做',
    afterReadingText: '用三句话写下核心结论，再列出一个还想追问的问题。',
    articleLanguage: '文章语言',
    readIn: (language: string) => `阅读${languageLabel(language)}版`
  };
});

function switchContentLanguage(language: string) {
  void router.replace({
    query: {
      ...route.query,
      language
    }
  });
}

async function loadDetail() {
  loading.value = true;
  errorMessage.value = '';
  actionError.value = '';

  try {
    post.value = await getPostDetail(postId.value, contentLanguage.value);
    comments.value = await getComments(postId.value);
    await loadInteractionState();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadErrorFallback;
  } finally {
    loading.value = false;
  }
}

async function loadInteractionState() {
  if (!sessionStore.isAuthenticated) {
    interaction.value = null;
    return;
  }

  try {
    interaction.value = await getInteractionState(postId.value);
    await recordPostView(postId.value);
  } catch (error) {
    interaction.value = null;

    if (error instanceof ApiError && error.code === 4010) {
      actionError.value = copy.value.sessionExpired;
      void sessionStore.logout().catch(() => undefined);
    }
  }
}

onMounted(loadDetail);

watch(
  () => [postId.value, contentLanguage.value],
  () => loadDetail()
);

async function runAuthenticated<T>(task: () => Promise<T>) {
  actionError.value = '';
  if (!sessionStore.isAuthenticated) {
    actionError.value = copy.value.loginRequired;
    return null;
  }

  try {
    return await task();
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : copy.value.actionFailed;
    return null;
  }
}

async function likePost() {
  const next = await runAuthenticated(() => toggleLike(postId.value));
  if (next) {
    interaction.value = next;
  }
}

async function favoritePost() {
  const next = await runAuthenticated(() => toggleFavorite(postId.value));
  if (next) {
    interaction.value = next;
  }
}

async function submitComment() {
  const content = newComment.value.trim();
  if (!content) {
    return;
  }

  const comment = await runAuthenticated(() => createComment(postId.value, content, post.value?.languageCode ?? preferencesStore.languageCode));
  if (comment) {
    upsertComment(comment);
    newComment.value = '';
    interaction.value = interaction.value
      ? { ...interaction.value, commentCount: interaction.value.commentCount + 1 }
      : interaction.value;
  }
}

async function submitReply() {
  const parent = replyingToComment.value;
  const content = replyComment.value.trim();
  if (!parent || !content) {
    return;
  }

  const comment = await runAuthenticated(() =>
    createComment(postId.value, content, post.value?.languageCode ?? preferencesStore.languageCode, parent.commentId)
  );
  if (comment) {
    upsertComment(comment);
    replyComment.value = '';
    replyingToComment.value = null;
    interaction.value = interaction.value
      ? { ...interaction.value, commentCount: interaction.value.commentCount + 1 }
      : interaction.value;
  }
}

function startReply(node: ForumThreadNode) {
  const source = comments.value.find((item) => item.commentId === node.id);
  if (!source || source.deleted) {
    return;
  }
  replyingToComment.value = source;
  replyComment.value = source.authorUsername ? `@${source.authorUsername} ` : '';
}

function cancelReply() {
  replyingToComment.value = null;
  replyComment.value = '';
}

async function likeComment(node: ForumThreadNode) {
  const updated = await runAuthenticated(() => toggleCommentLike(postId.value, node.id));
  if (updated) {
    upsertComment(updated);
  }
}

async function removeComment(node: ForumThreadNode): Promise<void> {
  if (!window.confirm(copy.value.deleteCommentConfirm)) {
    return;
  }
  const result = await runAuthenticated(() => deleteComment(postId.value, node.id));
  if (result !== null) {
    comments.value = comments.value.map((comment) =>
      comment.commentId === node.id
        ? { ...comment, status: 'DELETED', deleted: true, canDelete: false, likedByViewer: false, content: copy.value.deletedComment }
        : comment
    );
    interaction.value = interaction.value
      ? { ...interaction.value, commentCount: Math.max(0, interaction.value.commentCount - 1) }
      : interaction.value;
  }
}

function upsertComment(comment: CommentItem) {
  if (comments.value.some((item) => item.commentId === comment.commentId)) {
    comments.value = comments.value.map((item) => (item.commentId === comment.commentId ? comment : item));
  } else {
    comments.value = [...comments.value, comment].sort((a, b) => (a.floorNo || 0) - (b.floorNo || 0));
  }
}

async function submitReport() {
  const reason = reportReason.value.trim();
  if (!reason) {
    actionError.value = copy.value.reportReasonRequired;
    return;
  }

  reportLoading.value = true;
  reportSuccess.value = '';
  const result = await runAuthenticated(() => reportPost(postId.value, reason));
  if (result) {
    reportReason.value = '';
    reportSuccess.value = copy.value.reportSubmitted(result.reportId);
  }
  reportLoading.value = false;
}

async function createSummary() {
  aiLoading.value = 'summary';
  const result = await runAuthenticated(() =>
    generateSummary(postId.value, post.value?.languageCode ?? post.value?.originalLanguage ?? 'zh_CN', preferencesStore.languageCode)
  );
  if (result) {
    aiSummary.value = result;
  }
  aiLoading.value = '';
}

async function createReviewCards() {
  aiLoading.value = 'cards';
  const result = await runAuthenticated(() =>
    generateReviewCards(postId.value, post.value?.languageCode ?? post.value?.originalLanguage ?? 'zh_CN', preferencesStore.languageCode)
  );
  if (result) {
    reviewCards.value = result;
  }
  aiLoading.value = '';
}

async function playVoice() {
  if (!post.value) {
    return;
  }
  aiLoading.value = 'voice';
  const result = await runAuthenticated(() => textToSpeech(postId.value, `${post.value?.title}\n${post.value?.summary}`, post.value?.languageCode ?? 'zh_CN'));
  if (result) {
    voice.value = result;
    setTimeout(() => {
      const audio = document.querySelector<HTMLAudioElement>('#post-audio-player');
      audio?.play();
    }, 50);
  }
  aiLoading.value = '';
}

function commentTime(comment: CommentItem) {
  return formatShortDateTime(comment.createdTime, preferencesStore.languageCode);
}

function buildCommentTree(items: CommentItem[]): ForumThreadNode[] {
  const nodes = new Map<number, ForumThreadNode>();
  const roots: ForumThreadNode[] = [];

  for (const comment of [...items].sort((a, b) => (a.floorNo || 0) - (b.floorNo || 0))) {
    const node: ForumThreadNode = {
      id: comment.commentId,
      userId: comment.userId,
      authorUsername: comment.authorUsername || `user_${comment.userId}`,
      authorName: comment.authorName || comment.authorUsername || `#${comment.userId}`,
      authorAvatarUrl: comment.authorAvatarUrl,
      parentAuthorName: comment.parentAuthorName,
      content: comment.content,
      floorNo: comment.floorNo,
      likeCount: comment.likeCount,
      likedByViewer: comment.likedByViewer,
      canDelete: comment.canDelete,
      deleted: comment.deleted,
      createdLabel: commentTime(comment),
      replies: []
    };
    nodes.set(comment.commentId, node);
  }

  // 构建树形结构（内部使用 parentId，但不暴露给外部）
  for (const comment of items) {
    const node = nodes.get(comment.commentId);
    if (!node) continue;
    
    if (comment.parentCommentId && nodes.has(comment.parentCommentId)) {
      nodes.get(comment.parentCommentId)?.replies.push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
}
</script>

<template>
  <section class="detail-page">
    <RouterLink class="secondary-button return-link" to="/knowledge">
      <ArrowLeft :size="17" />
      <span>{{ copy.back }}</span>
    </RouterLink>

    <LoadingState v-if="loading" :label="copy.loading" />
    <EmptyState v-else-if="errorMessage" :title="copy.loadErrorTitle" :description="errorMessage" />

    <article v-else-if="post" class="article-layout">
      <div class="article-main">
        <div class="article-meta">
          <span class="score-pill">
            <Flame :size="15" />
            {{ post.hotScore.toFixed(1) }}
          </span>
          <span>
            <Languages :size="15" />
            {{ languageLabel(post.languageCode) }}
          </span>
          <span v-if="postCreatedTime">
            <CalendarClock :size="15" />
            {{ postCreatedTime }}
          </span>
          <span>{{ categoryName }}</span>
          <span>{{ interaction?.viewCount ?? post.viewCount }} {{ copy.views }}</span>
        </div>

        <div v-if="alternateLanguages.length" class="article-language-switch">
          <span>{{ copy.articleLanguage }}</span>
          <button
            v-for="language in alternateLanguages"
            :key="language"
            class="secondary-button"
            type="button"
            @click="switchContentLanguage(language)"
          >
            {{ copy.readIn(language) }}
          </button>
        </div>

        <h1>{{ post.title }}</h1>
        <p class="article-summary">{{ post.summary }}</p>

        <div v-if="post.semanticTags?.length" class="semantic-tag-panel">
          <span class="semantic-tag-label">{{ copy.semanticTags }}</span>
          <div class="semantic-tag-list">
            <span v-for="tag in post.semanticTags" :key="tag" class="semantic-tag-chip">{{ tag }}</span>
          </div>
          <p class="semantic-tag-hint">{{ copy.semanticTagsHint }}</p>
        </div>

        <img v-if="post.coverImageUrl" class="article-cover" :src="post.coverImageUrl" alt="" />

        <div class="article-content">
          <MarkdownRenderer :content="post.content" />
        </div>

        <div class="article-actions">
          <RouterLink v-if="sessionStore.isAuthenticated && post.authorId === sessionStore.userId" class="secondary-button" :to="`/posts/${post.postId}/edit`">
            <PencilLine :size="17" />
            <span>{{ copy.edit }}</span>
          </RouterLink>
          <button class="secondary-button" type="button" @click="likePost">
            <ThumbsUp :size="17" />
            <span>{{ interaction?.liked ? copy.liked : copy.like }} {{ interaction?.likeCount ?? post.likeCount }}</span>
          </button>
          <button class="secondary-button" type="button" @click="favoritePost">
            <Bookmark :size="17" />
            <span>{{ interaction?.favorited ? copy.favorited : copy.favorite }} {{ interaction?.favoriteCount ?? post.favoriteCount }}</span>
          </button>
          <button class="secondary-button" type="button" :disabled="aiLoading === 'voice'" @click="playVoice">
            <Volume2 :size="17" />
            <span>{{ aiLoading === 'voice' ? copy.voiceLoading : copy.voice }}</span>
          </button>
        </div>

        <audio v-if="voice" id="post-audio-player" class="audio-player" controls :src="voice.audioDataUrl" />
        <p v-if="actionError" class="form-error">{{ actionError }}</p>

        <section class="comment-section">
          <div class="panel-title">
            <MessageSquareText :size="18" />
            <span>{{ copy.discussion }}</span>
          </div>
          <div v-if="commentTree.length" class="comment-list forum-thread-list">
            <ForumThreadItem
              v-for="comment in commentTree"
              :key="comment.id"
              :node="comment"
              :replying-to-id="replyingToComment?.commentId ?? null"
              :reply-text="replyComment"
              :reply-placeholder="copy.replyPlaceholder"
              :submit-label="copy.submitReply"
              @reply="startReply"
              @cancel-reply="cancelReply"
              @update-reply-text="replyComment = $event"
              @submit-reply="submitReply"
              @like="likeComment"
              @delete="removeComment"
            />
          </div>
          <EmptyState v-else :title="copy.noDiscussionTitle" :description="copy.noDiscussionDesc" />
          <form class="comment-form" @submit.prevent="submitComment">
            <MentionTextarea v-model="newComment" rows="3" :placeholder="copy.commentPlaceholder" />
            <button class="primary-button" type="submit">
              <Send :size="17" />
              <span>{{ copy.send }}</span>
            </button>
          </form>
        </section>
      </div>

      <aside class="article-side">
        <section class="side-panel assistant-tools">
          <div class="panel-title">
            <Bot :size="18" />
            <span>{{ copy.aiAssistant }}</span>
          </div>
          <button class="primary-button full-width" type="button" :disabled="aiLoading === 'summary'" @click="createSummary">
            {{ aiLoading === 'summary' ? copy.summaryLoading : copy.summary }}
          </button>
          <button class="secondary-button full-width" type="button" :disabled="aiLoading === 'cards'" @click="createReviewCards">
            {{ aiLoading === 'cards' ? copy.cardsLoading : copy.cards }}
          </button>
          <MarkdownRenderer v-if="aiSummary" class="ai-output" :content="aiSummary.text" />
          <MarkdownRenderer v-if="reviewCards" class="ai-output" :content="reviewCards.text" />
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <BookMarked :size="18" />
            <span>{{ copy.articleInfo }}</span>
          </div>
          <dl class="info-list">
            <div>
              <dt>{{ copy.author }}</dt>
              <dd>
                <RouterLink class="text-link" :to="`/users/${post.authorId}`">
                  {{ post.authorName || `#${post.authorId}` }}
                </RouterLink>
              </dd>
            </div>
            <div>
              <dt>{{ copy.postId }}</dt>
              <dd>#{{ post.postId }}</dd>
            </div>
            <div>
              <dt>{{ copy.published }}</dt>
              <dd>{{ postCreatedTime || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.updated }}</dt>
              <dd>{{ postUpdatedTime || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.comments }}</dt>
              <dd>{{ interaction?.commentCount ?? post.commentCount }}</dd>
            </div>
          </dl>
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <Flag :size="18" />
            <span>{{ copy.reportTitle }}</span>
          </div>
          <form class="compact-form" @submit.prevent="submitReport">
            <textarea v-model.trim="reportReason" rows="4" :placeholder="copy.reportPlaceholder" />
            <button class="secondary-button full-width" type="submit" :disabled="reportLoading">
              {{ reportLoading ? copy.reportLoading : copy.report }}
            </button>
            <p v-if="reportSuccess" class="form-success">{{ reportSuccess }}</p>
          </form>
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <MessageSquareText :size="18" />
            <span>{{ copy.afterReadingTitle }}</span>
          </div>
          <p>{{ copy.afterReadingText }}</p>
        </section>
      </aside>
    </article>
  </section>
</template>

<style scoped>
.semantic-tag-panel {
  margin: 1rem 0 1.25rem;
  padding: 0.85rem 1rem;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fbff;
}

.semantic-tag-label {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  color: #1d4ed8;
  margin-bottom: 0.55rem;
}

.semantic-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.semantic-tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  background: #eff6ff;
  color: #1e3a8a;
  font-size: 0.82rem;
}

.semantic-tag-hint {
  margin: 0.55rem 0 0;
  font-size: 0.78rem;
  color: #64748b;
}
</style>
