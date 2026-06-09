<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { CheckCircle2, Clock3, RefreshCw, XCircle } from '@lucide/vue';
import { getHomepageReviews, reviewHomepage } from '@/api/community';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { AdminHomepageReview } from '@/types/api';

const preferencesStore = usePreferencesStore();
const items = ref<AdminHomepageReview[]>([]);
const loading = ref(false);
const saving = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const status = ref('PENDING_REVIEW');
const remark = ref('');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      eyebrow: 'Moderation',
      title: 'Homepage Review Queue',
      refresh: 'Refresh',
      queue: 'Current Queue',
      overdue: 'Overdue',
      status: 'Status',
      pending: 'Pending Review',
      approved: 'Approved',
      rejected: 'Rejected',
      all: 'All',
      remark: 'Review remark',
      remarkPlaceholder: 'Add an admin decision note',
      loading: 'Loading homepage review queue',
      unavailable: 'Homepage review queue is unavailable',
      approvedMsg: 'Homepage approved.',
      rejectedMsg: 'Homepage rejected.',
      failed: 'Homepage review failed.',
      summaryFallback: 'This review record does not have an additional summary.',
      overdue48: 'Over 48 hours',
      approve: 'Approve',
      reject: 'Reject',
      empty: 'No homepage review records',
      emptyDesc: 'There are no homepage review items for the current filter.'
    };
  }
  return {
    eyebrow: 'Moderation',
    title: '主页审核台',
    refresh: '刷新',
    queue: '当前队列',
    overdue: '超时待处理',
    status: '状态',
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    all: '全部',
    remark: '审核备注',
    remarkPlaceholder: '填写管理员审核意见',
    loading: '正在读取主页审核队列',
    unavailable: '主页审核队列暂时不可用',
    approvedMsg: '主页已审核通过。',
    rejectedMsg: '主页已驳回。',
    failed: '主页审核失败。',
    summaryFallback: '这条主页审核记录没有附加描述。',
    overdue48: '已超 48 小时',
    approve: '通过',
    reject: '驳回',
    empty: '没有主页审核记录',
    emptyDesc: '当前筛选条件下没有需要处理的主页审核项。'
  };
});

const overdueCount = computed(() => items.value.filter((item) => item.overdue).length);

async function loadReviews() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    items.value = await getHomepageReviews(status.value, 40);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

async function handleReview(item: AdminHomepageReview, decision: 'APPROVE' | 'REJECT') {
  saving.value = `${item.targetType}-${item.targetId}-${decision}`;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await reviewHomepage(item.targetId, item.targetType, decision, remark.value);
    successMessage.value = decision === 'APPROVE' ? copy.value.approvedMsg : copy.value.rejectedMsg;
    await loadReviews();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.failed;
  } finally {
    saving.value = '';
  }
}

function busy(item: AdminHomepageReview, decision: 'APPROVE' | 'REJECT') {
  return saving.value === `${item.targetType}-${item.targetId}-${decision}`;
}

onMounted(loadReviews);
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <div class="section-heading">
        <span>{{ copy.eyebrow }}</span>
        <h1>{{ copy.title }}</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadReviews">
        <RefreshCw :size="17" />
        <span>{{ copy.refresh }}</span>
      </button>
    </div>

    <div class="metric-grid">
      <div class="metric-card">
        <Clock3 :size="20" />
        <span>{{ copy.queue }}</span>
        <strong>{{ items.length }}</strong>
      </div>
      <div class="metric-card">
        <XCircle :size="20" />
        <span>{{ copy.overdue }}</span>
        <strong>{{ overdueCount }}</strong>
      </div>
    </div>

    <div class="management-toolbar">
      <label class="select-field">
        <span>{{ copy.status }}</span>
        <select v-model="status" @change="loadReviews">
          <option value="PENDING_REVIEW">{{ copy.pending }}</option>
          <option value="APPROVED">{{ copy.approved }}</option>
          <option value="REJECTED">{{ copy.rejected }}</option>
          <option value="ALL">{{ copy.all }}</option>
        </select>
      </label>
    </div>

    <label class="moderation-remark">
      <span>{{ copy.remark }}</span>
      <input v-model.trim="remark" type="text" :placeholder="copy.remarkPlaceholder" />
    </label>

    <LoadingState v-if="loading" :label="copy.loading" />
    <p v-else-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    <p v-else-if="successMessage" class="form-success">{{ successMessage }}</p>

    <div v-else class="homepage-design-grid">
      <article v-for="item in items" :key="`${item.targetType}-${item.targetId}`" class="homepage-design-card">
        <div class="homepage-design-meta">
          <strong>{{ item.title }}</strong>
          <span>{{ item.ownerName }} · {{ item.targetType }}</span>
        </div>
        <p>{{ item.summary || copy.summaryFallback }}</p>
        <div class="homepage-design-stats">
          <span>{{ item.layoutMode }}</span>
          <span>{{ item.reviewStatus }}</span>
          <span v-if="item.overdue">{{ copy.overdue48 }}</span>
        </div>
        <pre class="homepage-code-dump">{{ item.layoutMode === 'code' ? item.customCode : item.mediaLayout }}</pre>
        <div class="table-actions">
          <button class="secondary-button" type="button" :disabled="item.reviewStatus !== 'PENDING_REVIEW' || busy(item, 'APPROVE')" @click="handleReview(item, 'APPROVE')">
            <CheckCircle2 :size="16" />
            <span>{{ copy.approve }}</span>
          </button>
          <button class="secondary-button danger-action" type="button" :disabled="item.reviewStatus !== 'PENDING_REVIEW' || busy(item, 'REJECT')" @click="handleReview(item, 'REJECT')">
            <XCircle :size="16" />
            <span>{{ copy.reject }}</span>
          </button>
        </div>
      </article>
      <EmptyState v-if="items.length === 0" :title="copy.empty" :description="copy.emptyDesc" />
    </div>
  </section>
</template>
