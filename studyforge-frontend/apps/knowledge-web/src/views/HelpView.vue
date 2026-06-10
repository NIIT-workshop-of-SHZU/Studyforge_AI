<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { CalendarClock, CircleHelp, MessageSquarePlus, RefreshCw, Send } from '@lucide/vue';
import { createHelpAnswer, createHelpRequest, deleteHelpAnswer, getHelpAnswers, getHelpRequests, toggleHelpAnswerLike } from '@/api/help';
import EmptyState from '@/components/EmptyState.vue';
import ForumThreadItem from '@/components/ForumThreadItem.vue';
import LoadingState from '@/components/LoadingState.vue';
import MentionTextarea from '@/components/MentionTextarea.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { HelpAnswer, HelpRequest } from '@/types/api';
import { formatShortDateTime, toDate } from '@/utils/date';

interface ForumThreadNode {
  id: number;
  parentId: number | null;
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

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const route = useRoute();
const helps = ref<HelpRequest[]>([]);
const answers = ref<Record<number, HelpAnswer[]>>({});
const activeHelpId = ref<number | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const answerText = ref('');
const replyAnswerText = ref('');
const replyingToAnswer = ref<HelpAnswer | null>(null);

const form = reactive({
  title: '',
  description: '',
  categoryId: 1,
  rewardPoints: 0
});

const titleInput = ref<HTMLInputElement | null>(null);

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      kicker: 'Help Desk',
      title: 'Help threads',
      subtitle: 'Describe the context, what you tried, and what outcome you need so others can reply with steps, references, or debugging direction.',
      ask: 'Ask a question',
      refresh: 'Refresh list',
      statsAria: 'Help desk statistics',
      allQuestions: 'All questions',
      waiting: 'Waiting',
      solved: 'Resolved',
      rewardPoints: 'Reward points',
      recentTitle: 'Recent questions',
      recentDesc: 'Pick a question to read the context, answers, and follow-up suggestions.',
      loading: 'Loading help threads',
      loadErrorTitle: 'Unable to load',
      loadErrorFallback: 'Help threads could not be loaded.',
      emptyTitle: 'No help threads yet',
      emptyDesc: 'When you get stuck, start a question here.',
      points: 'pts',
      asker: (id: number) => `Asker #${id}`,
      answers: (count: number) => `${count} answers`,
      reward: (points: number) => `${points} points`,
      answersTitle: 'Answers and suggestions',
      noAnswers: 'No answers yet. You can start with a debugging direction, reference, or actionable step.',
      answerPlaceholder: 'Share your suggestion, steps, or references. Markdown is supported. Type @ to mention a friend.',
      submitAnswer: 'Submit answer',
      composeTitle: 'Ask a question',
      titleLabel: 'Title',
      titlePlaceholder: 'Summarize where you are stuck',
      backgroundLabel: 'Background',
      backgroundPlaceholder: 'Describe what you tried and what help you need. Markdown is supported.',
      rewardLabel: 'Reward points',
      publish: 'Publish question',
      guideTitle: 'Get better answers',
      guideItems: [
        'Include environment, error messages, and methods you already tried.',
        'Break the expected outcome into concrete questions so others can locate the issue.',
        'Answers can use Markdown so code, links, and steps stay clear.'
      ],
      loginToAsk: 'Please sign in before publishing a help thread.',
      publishFailed: 'The help thread could not be published.',
      loginToAnswer: 'Please sign in before answering.',
      answerFailed: 'The answer could not be sent.',
      loginToReply: 'Please sign in before replying.',
      replyFailed: 'The reply could not be sent.',
      loginToLike: 'Please sign in before liking.',
      likeFailed: 'The like action did not complete.',
      loginToDelete: 'Please sign in before deleting.',
      deleteFailed: 'The answer could not be deleted.',
      deleteConfirm: 'Delete this answer? The floor number and replies will be kept.',
      deletedAnswer: 'This answer was deleted.',
      statusOpen: 'Open',
      statusSolved: 'Solved',
      statusClosed: 'Closed',
      statusPending: 'Pending'
    };
  }

  return {
    kicker: 'Help Desk',
    title: '求助讨论',
    subtitle: '把问题背景、尝试过的办法和期望结果写清楚，社区成员可以直接给出步骤、资料或排查方向。',
    ask: '提出问题',
    refresh: '刷新列表',
    statsAria: '求助讨论统计',
    allQuestions: '全部问题',
    waiting: '等待讨论',
    solved: '已有结论',
    rewardPoints: '积分奖励',
    recentTitle: '最近的问题',
    recentDesc: '选择一个问题查看背景、回答和补充建议',
    loading: '正在加载求助',
    loadErrorTitle: '暂时无法加载',
    loadErrorFallback: '求助内容暂时没取到',
    emptyTitle: '还没有求助',
    emptyDesc: '遇到卡点时，可以从这里发起一个问题。',
    points: '分',
    asker: (id: number) => `提问者 #${id}`,
    answers: (count: number) => `${count} 条回答`,
    reward: (points: number) => `${points} 积分`,
    answersTitle: '回答与建议',
    noAnswers: '还没有回答。可以先给出一个排查方向、参考资料或可执行步骤。',
    answerPlaceholder: '写下你的建议、步骤或参考资料，支持 Markdown；输入 @ 可以选择好友',
    submitAnswer: '提交回答',
    composeTitle: '提出问题',
    titleLabel: '标题',
    titlePlaceholder: '一句话说清卡在哪里',
    backgroundLabel: '背景',
    backgroundPlaceholder: '写下你已经试过什么、希望得到什么帮助，支持 Markdown',
    rewardLabel: '奖励积分',
    publish: '发布求助',
    guideTitle: '更容易得到帮助',
    guideItems: [
      '写清楚环境、报错信息和已经尝试过的方法。',
      '把期望结果拆成具体问题，别人更容易定位。',
      '回答可以使用 Markdown，代码、链接和步骤会更清晰。'
    ],
    loginToAsk: '请先登录再发布求助',
    publishFailed: '暂时发布不了求助',
    loginToAnswer: '请先登录再回答',
    answerFailed: '暂时发送不了回答',
    loginToReply: '请先登录再回复',
    replyFailed: '暂时发送不了回复',
    loginToLike: '请先登录再点赞',
    likeFailed: '暂时点赞不了',
    loginToDelete: '请先登录再删除',
    deleteFailed: '暂时删除不了',
    deleteConfirm: '确定删除这条回答吗？删除后会保留楼层和回复关系。',
    deletedAnswer: '这条回答已删除',
    statusOpen: '进行中',
    statusSolved: '已解决',
    statusClosed: '已关闭',
    statusPending: '待处理'
  };
});

const sortedHelps = computed(() =>
  helps.value
    .map((help, index) => ({ help, index }))
    .sort(
      (first, second) =>
        helpLanguageRank(first.help) - helpLanguageRank(second.help) ||
        helpTimestamp(second.help) - helpTimestamp(first.help) ||
        first.index - second.index
    )
    .map(({ help }) => help)
);
const activeHelp = computed(() => sortedHelps.value.find((item) => item.helpId === activeHelpId.value) ?? sortedHelps.value[0] ?? null);
const activeAnswers = computed(() => (activeHelp.value ? answers.value[activeHelp.value.helpId] ?? [] : []));
const activeAnswerTree = computed(() => buildAnswerTree(activeAnswers.value));
const openHelpCount = computed(() => helps.value.filter((item) => normalizeStatus(item.status) === 'OPEN').length);
const solvedHelpCount = computed(() => helps.value.filter((item) => ['SOLVED', 'RESOLVED', 'CLOSED'].includes(normalizeStatus(item.status))).length);
const totalRewardPoints = computed(() => helps.value.reduce((total, item) => total + Number(item.rewardPoints || 0), 0));

function routeHelpId() {
  const value = Number(route.query.helpId);
  return Number.isFinite(value) && value > 0 ? value : null;
}

function normalizeStatus(status: string) {
  return (status || '').toUpperCase();
}

function inferHelpLanguage(help: HelpRequest) {
  const text = `${help.title} ${help.description}`;
  return /[\u3400-\u9fff]/.test(text) ? 'zh_CN' : 'en_US';
}

function helpLanguageRank(help: HelpRequest) {
  return inferHelpLanguage(help) === preferencesStore.languageCode ? 0 : 1;
}

function helpTimestamp(help: HelpRequest) {
  return toDate(help.createdTime)?.getTime() ?? 0;
}

function preferredHelpId() {
  return sortedHelps.value[0]?.helpId ?? null;
}

function statusLabel(status: string) {
  const normalized = normalizeStatus(status);
  if (normalized === 'OPEN') {
    return copy.value.statusOpen;
  }
  if (normalized === 'SOLVED' || normalized === 'RESOLVED') {
    return copy.value.statusSolved;
  }
  if (normalized === 'CLOSED') {
    return copy.value.statusClosed;
  }
  return status || copy.value.statusPending;
}

function focusCompose() {
  titleInput.value?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  titleInput.value?.focus();
}

async function loadHelps() {
  loading.value = true;
  errorMessage.value = '';

  try {
    helps.value = await getHelpRequests();
    const targetHelpId = routeHelpId();
    const currentHelpId = activeHelpId.value && helps.value.some((item) => item.helpId === activeHelpId.value) ? activeHelpId.value : null;
    activeHelpId.value = targetHelpId && helps.value.some((item) => item.helpId === targetHelpId) ? targetHelpId : (currentHelpId ?? preferredHelpId());
    if (activeHelpId.value) {
      await loadAnswers(activeHelpId.value);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadErrorFallback;
  } finally {
    loading.value = false;
  }
}

async function loadAnswers(helpId: number) {
  answers.value = {
    ...answers.value,
    [helpId]: await getHelpAnswers(helpId)
  };
}

async function publishHelp() {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToAsk;
    return;
  }

  try {
    const helpId = await createHelpRequest({ ...form });
    form.title = '';
    form.description = '';
    form.rewardPoints = 0;
    await loadHelps();
    activeHelpId.value = helpId;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.publishFailed;
  }
}

async function sendAnswer() {
  if (!activeHelp.value || !answerText.value.trim()) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToAnswer;
    return;
  }

  try {
    await createHelpAnswer(activeHelp.value.helpId, answerText.value);
    answerText.value = '';
    await loadAnswers(activeHelp.value.helpId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.answerFailed;
  }
}

async function sendAnswerReply() {
  if (!activeHelp.value || !replyingToAnswer.value || !replyAnswerText.value.trim()) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToReply;
    return;
  }

  try {
    const created = await createHelpAnswer(activeHelp.value.helpId, replyAnswerText.value, replyingToAnswer.value.answerId);
    upsertAnswer(activeHelp.value.helpId, created);
    replyAnswerText.value = '';
    replyingToAnswer.value = null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.replyFailed;
  }
}

function startAnswerReply(node: ForumThreadNode) {
  const source = activeAnswers.value.find((item) => item.answerId === node.id);
  if (!source || source.deleted) {
    return;
  }
  replyingToAnswer.value = source;
  replyAnswerText.value = source.authorUsername ? `@${source.authorUsername} ` : '';
}

function cancelAnswerReply() {
  replyingToAnswer.value = null;
  replyAnswerText.value = '';
}

async function likeAnswer(node: ForumThreadNode) {
  if (!activeHelp.value) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToLike;
    return;
  }

  try {
    const updated = await toggleHelpAnswerLike(activeHelp.value.helpId, node.id);
    upsertAnswer(activeHelp.value.helpId, updated);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.likeFailed;
  }
}

async function removeAnswer(node: ForumThreadNode) {
  if (!activeHelp.value || !window.confirm(copy.value.deleteConfirm)) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToDelete;
    return;
  }

  try {
    await deleteHelpAnswer(activeHelp.value.helpId, node.id);
    answers.value = {
      ...answers.value,
      [activeHelp.value.helpId]: activeAnswers.value.map((answer) =>
        answer.answerId === node.id
          ? { ...answer, status: 'DELETED', deleted: true, canDelete: false, likedByViewer: false, content: copy.value.deletedAnswer }
          : answer
      )
    };
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.deleteFailed;
  }
}

function upsertAnswer(helpId: number, answer: HelpAnswer) {
  const current = answers.value[helpId] ?? [];
  answers.value = {
    ...answers.value,
    [helpId]: current.some((item) => item.answerId === answer.answerId)
      ? current.map((item) => (item.answerId === answer.answerId ? answer : item))
      : [...current, answer].sort((a, b) => (a.floorNo || 0) - (b.floorNo || 0))
  };
}

async function selectHelp(helpId: number) {
  activeHelpId.value = helpId;
  if (!answers.value[helpId]) {
    await loadAnswers(helpId);
  }
}

function helpTime(help: HelpRequest) {
  return formatShortDateTime(help.createdTime, preferencesStore.languageCode);
}

function answerTime(answer: HelpAnswer) {
  return formatShortDateTime(answer.createdTime, preferencesStore.languageCode);
}

function buildAnswerTree(items: HelpAnswer[]): ForumThreadNode[] {
  const nodes = new Map<number, ForumThreadNode>();
  const roots: ForumThreadNode[] = [];

  for (const answer of [...items].sort((a, b) => (a.floorNo || 0) - (b.floorNo || 0))) {
    nodes.set(answer.answerId, {
      id: answer.answerId,
      parentId: answer.parentAnswerId,
      userId: answer.userId,
      authorUsername: answer.authorUsername || `user_${answer.userId}`,
      authorName: answer.authorName || answer.authorUsername || `#${answer.userId}`,
      authorAvatarUrl: answer.authorAvatarUrl,
      parentAuthorName: answer.parentAuthorName,
      content: answer.content,
      floorNo: answer.floorNo,
      likeCount: answer.likeCount,
      likedByViewer: answer.likedByViewer,
      canDelete: answer.canDelete,
      deleted: answer.deleted,
      accepted: Number(answer.accepted) === 1,
      createdLabel: answerTime(answer),
      replies: []
    });
  }

  for (const node of nodes.values()) {
    if (node.parentId && nodes.has(node.parentId)) {
      nodes.get(node.parentId)?.replies.push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
}

onMounted(loadHelps);

watch(
  () => route.query.helpId,
  async () => {
    const targetHelpId = routeHelpId();
    if (!targetHelpId || !helps.value.some((item) => item.helpId === targetHelpId)) {
      return;
    }
    await selectHelp(targetHelpId);
  }
);

watch(
  () => preferencesStore.languageCode,
  async () => {
    const current = helps.value.find((item) => item.helpId === activeHelpId.value);
    if (!current || helpLanguageRank(current) > 0) {
      activeHelpId.value = preferredHelpId();
    }
    if (activeHelpId.value && !answers.value[activeHelpId.value]) {
      await loadAnswers(activeHelpId.value);
    }
  }
);
</script>

<template>
  <section class="help-page">
    <div class="help-hero">
      <div class="help-hero-copy">
        <span class="section-kicker">{{ copy.kicker }}</span>
        <h1>{{ copy.title }}</h1>
        <p>{{ copy.subtitle }}</p>
        <div class="help-hero-actions">
          <button class="primary-button" type="button" @click="focusCompose">
            <MessageSquarePlus :size="18" />
            <span>{{ copy.ask }}</span>
          </button>
          <button class="secondary-button" type="button" :disabled="loading" @click="loadHelps">
            <RefreshCw :size="17" />
            <span>{{ copy.refresh }}</span>
          </button>
        </div>
      </div>
      <div class="help-hero-stats" :aria-label="copy.statsAria">
        <div>
          <strong>{{ helps.length }}</strong>
          <span>{{ copy.allQuestions }}</span>
        </div>
        <div>
          <strong>{{ openHelpCount }}</strong>
          <span>{{ copy.waiting }}</span>
        </div>
        <div>
          <strong>{{ solvedHelpCount }}</strong>
          <span>{{ copy.solved }}</span>
        </div>
        <div>
          <strong>{{ totalRewardPoints }}</strong>
          <span>{{ copy.rewardPoints }}</span>
        </div>
      </div>
    </div>

    <div class="help-layout">
      <main class="help-main">
        <section class="help-list">
          <div class="feed-toolbar help-section-toolbar">
            <div>
              <strong>{{ copy.recentTitle }}</strong>
              <small>{{ copy.recentDesc }}</small>
            </div>
          </div>

          <LoadingState v-if="loading" :label="copy.loading" />
          <EmptyState v-else-if="errorMessage" :title="copy.loadErrorTitle" :description="errorMessage" />
          <EmptyState v-else-if="helps.length === 0" :title="copy.emptyTitle" :description="copy.emptyDesc" />

          <div v-else class="help-items">
            <button
              v-for="help in sortedHelps"
              :key="help.helpId"
              class="help-item"
              :class="{ active: help.helpId === activeHelp?.helpId }"
              type="button"
              @click="selectHelp(help.helpId)"
            >
              <span class="help-item-topline">
                <span class="help-status-pill" :data-status="normalizeStatus(help.status)">{{ statusLabel(help.status) }}</span>
                <span v-if="help.rewardPoints > 0" class="help-reward-pill">{{ help.rewardPoints }} {{ copy.points }}</span>
              </span>
              <strong>{{ help.title }}</strong>
              <small>{{ help.description }}</small>
              <span class="help-item-footer">
                <span>{{ copy.asker(help.userId) }}</span>
                <span v-if="helpTime(help)" class="time-line">
                  <CalendarClock :size="14" />
                  {{ helpTime(help) }}
                </span>
              </span>
            </button>
          </div>
        </section>

        <div class="help-detail-stack">
          <section v-if="activeHelp" class="help-detail">
            <div class="help-detail-topline">
              <span class="help-status-pill" :data-status="normalizeStatus(activeHelp.status)">{{ statusLabel(activeHelp.status) }}</span>
              <span>{{ copy.asker(activeHelp.userId) }}</span>
              <span v-if="activeHelp.rewardPoints > 0">{{ copy.reward(activeHelp.rewardPoints) }}</span>
              <span>{{ copy.answers(activeAnswers.length) }}</span>
              <span v-if="helpTime(activeHelp)">
                <CalendarClock :size="15" />
                {{ helpTime(activeHelp) }}
              </span>
            </div>
            <h2>{{ activeHelp.title }}</h2>
            <MarkdownRenderer class="help-markdown" :content="activeHelp.description" />
          </section>

          <section v-if="activeHelp" class="help-answer-panel">
            <div class="panel-title">
              <CircleHelp :size="18" />
              <span>{{ copy.answersTitle }}</span>
            </div>
            <div v-if="activeAnswerTree.length" class="comment-list help-answer-list forum-thread-list">
              <ForumThreadItem
                v-for="answer in activeAnswerTree"
                :key="answer.id"
                :node="answer"
                :replying-to-id="replyingToAnswer?.answerId ?? null"
                :reply-text="replyAnswerText"
                @reply="startAnswerReply"
                @cancel-reply="cancelAnswerReply"
                @update-reply-text="replyAnswerText = $event"
                @submit-reply="sendAnswerReply"
                @like="likeAnswer"
                @delete="removeAnswer"
              />
            </div>
            <p v-else>{{ copy.noAnswers }}</p>
            <form class="compact-form help-answer-form" @submit.prevent="sendAnswer">
              <MentionTextarea v-model="answerText" rows="5" :placeholder="copy.answerPlaceholder" />
              <button class="secondary-button full-width" type="submit">{{ copy.submitAnswer }}</button>
            </form>
          </section>
        </div>
      </main>

      <aside class="help-aside">
        <section class="side-panel help-compose-panel">
          <div class="panel-title">
            <MessageSquarePlus :size="18" />
            <span>{{ copy.composeTitle }}</span>
          </div>
          <form class="compact-form" @submit.prevent="publishHelp">
            <label>
              <span>{{ copy.titleLabel }}</span>
              <input id="help-title" ref="titleInput" v-model.trim="form.title" required :placeholder="copy.titlePlaceholder" />
            </label>
            <label>
              <span>{{ copy.backgroundLabel }}</span>
              <textarea v-model.trim="form.description" required rows="8" :placeholder="copy.backgroundPlaceholder" />
            </label>
            <label>
              <span>{{ copy.rewardLabel }}</span>
              <input v-model.number="form.rewardPoints" min="0" type="number" />
            </label>
            <button class="primary-button full-width" type="submit">
              <Send :size="17" />
              <span>{{ copy.publish }}</span>
            </button>
          </form>
        </section>

        <section class="side-panel help-guide-panel">
          <div class="panel-title">
            <CircleHelp :size="18" />
            <span>{{ copy.guideTitle }}</span>
          </div>
          <ul>
            <li v-for="item in copy.guideItems" :key="item">{{ item }}</li>
          </ul>
        </section>
      </aside>
    </div>
  </section>
</template>
