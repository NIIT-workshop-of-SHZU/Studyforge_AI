<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CircleHelp, MessageSquarePlus, RefreshCw, Send } from '@lucide/vue';
import { createHelpAnswer, createHelpRequest, getHelpAnswers, getHelpRequests } from '@/api/help';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { useSessionStore } from '@/stores/session';
import type { HelpAnswer, HelpRequest } from '@/types/api';

const sessionStore = useSessionStore();
const helps = ref<HelpRequest[]>([]);
const answers = ref<Record<number, HelpAnswer[]>>({});
const activeHelpId = ref<number | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const answerText = ref('');

const form = reactive({
  title: '',
  description: '',
  categoryId: 1,
  rewardPoints: 0
});

const activeHelp = computed(() => helps.value.find((item) => item.helpId === activeHelpId.value) ?? helps.value[0] ?? null);
const activeAnswers = computed(() => (activeHelp.value ? answers.value[activeHelp.value.helpId] ?? [] : []));

async function loadHelps() {
  loading.value = true;
  errorMessage.value = '';

  try {
    helps.value = await getHelpRequests();
    activeHelpId.value = activeHelpId.value ?? helps.value[0]?.helpId ?? null;
    if (activeHelpId.value) {
      await loadAnswers(activeHelpId.value);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '求助内容暂时没取到';
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
    errorMessage.value = '请先登录再发布求助';
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
    errorMessage.value = error instanceof Error ? error.message : '暂时发布不了求助';
  }
}

async function sendAnswer() {
  if (!activeHelp.value || !answerText.value.trim()) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = '请先登录再回答';
    return;
  }

  try {
    await createHelpAnswer(activeHelp.value.helpId, answerText.value);
    answerText.value = '';
    await loadAnswers(activeHelp.value.helpId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '暂时发送不了回答';
  }
}

async function selectHelp(helpId: number) {
  activeHelpId.value = helpId;
  if (!answers.value[helpId]) {
    await loadAnswers(helpId);
  }
}

onMounted(loadHelps);
</script>

<template>
  <section class="help-page">
    <div class="page-heading">
      <span class="section-kicker">Help Desk</span>
      <h1>求助讨论</h1>
      <p>把卡住的背景讲清楚，别人更容易给到能直接尝试的建议。</p>
    </div>

    <div class="help-layout">
      <aside class="help-compose">
        <section class="side-panel">
          <div class="panel-title">
            <MessageSquarePlus :size="18" />
            <span>提出问题</span>
          </div>
          <form class="compact-form" @submit.prevent="publishHelp">
            <label>
              <span>标题</span>
              <input v-model.trim="form.title" required placeholder="一句话说清卡在哪里" />
            </label>
            <label>
              <span>背景</span>
              <textarea v-model.trim="form.description" required rows="6" placeholder="写下你已经试过什么、希望得到什么帮助" />
            </label>
            <label>
              <span>奖励积分</span>
              <input v-model.number="form.rewardPoints" min="0" type="number" />
            </label>
            <button class="primary-button full-width" type="submit">
              <Send :size="17" />
              <span>发布求助</span>
            </button>
          </form>
        </section>
      </aside>

      <section class="help-list">
        <div class="feed-toolbar">
          <strong>最近的问题</strong>
          <button class="secondary-button" type="button" :disabled="loading" @click="loadHelps">
            <RefreshCw :size="17" />
            <span>刷新</span>
          </button>
        </div>

        <LoadingState v-if="loading" label="正在加载求助" />
        <EmptyState v-else-if="errorMessage" title="暂时无法加载" :description="errorMessage" />
        <EmptyState v-else-if="helps.length === 0" title="还没有求助" description="遇到卡点时，可以从这里发起一个问题。" />

        <div v-else class="help-items">
          <button
            v-for="help in helps"
            :key="help.helpId"
            class="help-item"
            :class="{ active: help.helpId === activeHelp?.helpId }"
            type="button"
            @click="selectHelp(help.helpId)"
          >
            <span>{{ help.status }}</span>
            <strong>{{ help.title }}</strong>
            <small>{{ help.description }}</small>
          </button>
        </div>
      </section>

      <aside class="help-detail">
        <section v-if="activeHelp" class="side-panel">
          <div class="panel-title">
            <CircleHelp :size="18" />
            <span>问题详情</span>
          </div>
          <h2>{{ activeHelp.title }}</h2>
          <p>{{ activeHelp.description }}</p>
          <div class="post-meta">
            <span>#{{ activeHelp.userId }}</span>
            <span>{{ activeHelp.rewardPoints }} 分</span>
            <span>{{ activeHelp.status }}</span>
          </div>
        </section>

        <section v-if="activeHelp" class="side-panel">
          <div class="panel-title">
            <span>回答</span>
          </div>
          <div v-if="activeAnswers.length" class="comment-list">
            <article v-for="answer in activeAnswers" :key="answer.answerId" class="comment-item">
              <strong>#{{ answer.userId }}</strong>
              <p>{{ answer.content }}</p>
            </article>
          </div>
          <p v-else>还没有回答，可以先给出一个可尝试的方向。</p>
          <form class="compact-form" @submit.prevent="sendAnswer">
            <textarea v-model.trim="answerText" rows="4" placeholder="写下你的建议、步骤或参考资料" />
            <button class="secondary-button full-width" type="submit">提交回答</button>
          </form>
        </section>
      </aside>
    </div>
  </section>
</template>
