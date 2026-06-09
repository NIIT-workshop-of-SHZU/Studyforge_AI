<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowLeft, Brain, Eye, LogIn, PencilLine, RefreshCw, Save } from '@lucide/vue';
import { getLearningMemory, refreshLearningMemory, updateLearningMemory } from '@/api/learningMemory';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { InterestTag, LearningMemory } from '@/types/api';
import { formatDateTime } from '@/utils/date';

type ViewMode = 'read' | 'edit';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const loading = ref(false);
const saving = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const mode = ref<ViewMode>('read');
const memory = ref<LearningMemory | null>(null);

const form = reactive({
  memoryMd: '',
  aiMemoryEnabled: true,
  manualTags: ''
});

const autoTags = computed(() => (memory.value?.interestTags ?? []).filter((tag) => tag.source !== 'manual'));
const manualTags = computed(() => (memory.value?.interestTags ?? []).filter((tag) => tag.source === 'manual'));
const lastRefreshedLabel = computed(() => {
  if (!memory.value?.lastRefreshedAt) {
    return '尚未刷新';
  }
  return formatDateTime(memory.value.lastRefreshedAt, preferencesStore.languageCode);
});

const emptyTemplate = `## 近期学习重点
- 例如：主攻 Java 后端、Spring Boot

## 常看主题
- 例如：技术实践、面试准备

## 备注
- 写下你希望自己优先看到的内容
`;

function applyMemory(data: LearningMemory) {
  memory.value = data;
  form.memoryMd = data.memoryMd ?? '';
  form.aiMemoryEnabled = data.aiMemoryEnabled;
  form.manualTags = (data.interestTags ?? [])
    .filter((tag) => tag.source === 'manual')
    .map((tag) => tag.tag)
    .join(', ');
}

async function loadMemory() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    applyMemory(await getLearningMemory());
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '学习记忆暂时打不开';
  } finally {
    loading.value = false;
  }
}

function startEdit() {
  mode.value = 'edit';
  if (!form.memoryMd.trim()) {
    form.memoryMd = emptyTemplate;
  }
}

function cancelEdit() {
  if (memory.value) {
    applyMemory(memory.value);
  }
  mode.value = 'read';
  errorMessage.value = '';
  successMessage.value = '';
}

async function saveMemory() {
  saving.value = 'save';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const nextManualTags: InterestTag[] = form.manualTags
      .split(/[,，]/)
      .map((tag) => tag.trim())
      .filter(Boolean)
      .map((tag) => ({ tag, weight: 0.9, source: 'manual' }));

    await updateLearningMemory(
      {
        memoryMd: form.memoryMd.trim(),
        aiMemoryEnabled: form.aiMemoryEnabled,
        interestTags: nextManualTags
      },
      preferencesStore.languageCode
    );
    applyMemory(await getLearningMemory());
    mode.value = 'read';
    successMessage.value = 'MEMORY.md 已保存';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败，请稍后再试';
  } finally {
    saving.value = '';
  }
}

async function refreshMemory() {
  if (memory.value?.memoryManuallyEdited) {
    const confirmed = window.confirm('自动刷新会用系统归纳的内容覆盖你手写的 MEMORY.md，确定继续吗？');
    if (!confirmed) {
      return;
    }
  }

  saving.value = 'refresh';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const updated = await refreshLearningMemory(preferencesStore.languageCode);
    applyMemory(updated);
    mode.value = 'read';
    successMessage.value = '已根据你的收藏与学习行为重新生成';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '刷新失败，请稍后再试';
  } finally {
    saving.value = '';
  }
}

onMounted(loadMemory);
</script>

<template>
  <section class="learning-memory-page">
    <div class="page-heading with-actions">
      <div>
        <RouterLink class="secondary-button return-link" to="/favorites">
          <ArrowLeft :size="17" />
          <span>返回收藏夹</span>
        </RouterLink>
        <span class="section-kicker">Learning Memory</span>
        <h1>MEMORY.md</h1>
        <p>这是系统对你的学习画像：收藏排序和 AI 都会参考它。你可以直接阅读，也可以像编辑文档一样修改。</p>
      </div>
      <div v-if="sessionStore.isAuthenticated" class="memory-actions">
        <button
          v-if="mode === 'read'"
          class="secondary-button"
          type="button"
          :disabled="loading || saving === 'refresh'"
          @click="refreshMemory"
        >
          <RefreshCw :size="17" />
          <span>{{ saving === 'refresh' ? '刷新中' : '自动刷新' }}</span>
        </button>
        <button v-if="mode === 'read'" class="primary-button" type="button" @click="startEdit">
          <PencilLine :size="17" />
          <span>编辑</span>
        </button>
        <template v-else>
          <button class="secondary-button" type="button" @click="cancelEdit">取消</button>
          <button class="primary-button" type="button" :disabled="saving === 'save'" @click="saveMemory">
            <Save :size="17" />
            <span>{{ saving === 'save' ? '保存中' : '保存' }}</span>
          </button>
        </template>
      </div>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <Brain :size="42" />
      <h2>登录后查看学习记忆</h2>
      <p>登录后可以阅读、编辑 MEMORY.md，让收藏排序和 AI 更贴合你的学习重点。</p>
      <RouterLink class="primary-button" to="/login">
        <LogIn :size="17" />
        <span>登录</span>
      </RouterLink>
    </div>

    <template v-else>
      <LoadingState v-if="loading" label="正在读取 MEMORY.md" />
      <EmptyState v-else-if="errorMessage && !memory" title="暂时无法加载" :description="errorMessage" />

      <template v-else>
        <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <section class="memory-meta surface-card">
          <div>
            <strong>版本</strong>
            <span>v{{ memory?.profileVersion ?? 1 }}</span>
          </div>
          <div>
            <strong>上次刷新</strong>
            <span>{{ lastRefreshedLabel }}</span>
          </div>
          <label class="memory-toggle">
            <input v-model="form.aiMemoryEnabled" type="checkbox" :disabled="mode === 'read'" />
            <span>AI 使用这份记忆</span>
          </label>
          <div v-if="memory?.memoryManuallyEdited" class="memory-edited-badge">已手写编辑</div>
        </section>

        <section class="memory-tags surface-card">
          <div class="panel-title">
            <Brain :size="18" />
            <span>关注标签</span>
          </div>
          <div v-if="autoTags.length" class="tag-group">
            <span class="tag-group-label">自动归纳</span>
            <div class="tag-list">
              <span v-for="tag in autoTags" :key="`auto-${tag.tag}`" class="tag-chip auto">{{ tag.tag }}</span>
            </div>
          </div>
          <div v-if="manualTags.length || mode === 'edit'" class="tag-group">
            <span class="tag-group-label">你手动添加</span>
            <div v-if="mode === 'read' && manualTags.length" class="tag-list">
              <span v-for="tag in manualTags" :key="`manual-${tag.tag}`" class="tag-chip manual">{{ tag.tag }}</span>
            </div>
            <input
              v-else-if="mode === 'edit'"
              v-model="form.manualTags"
              class="tag-input"
              type="text"
              placeholder="Spring, MyBatis, 面试（逗号分隔）"
            />
          </div>
          <p v-if="!autoTags.length && !manualTags.length && mode === 'read'" class="tag-empty">收藏更多文章后，系统会自动归纳标签。</p>
        </section>

        <section class="memory-document surface-card">
          <div class="memory-doc-header">
            <div class="panel-title">
              <Eye v-if="mode === 'read'" :size="18" />
              <PencilLine v-else :size="18" />
              <span>{{ mode === 'read' ? '阅读' : '编辑' }} · MEMORY.md</span>
            </div>
            <span class="char-count">{{ form.memoryMd.length }} / 800</span>
          </div>

          <div v-if="mode === 'read'" class="memory-read-pane">
            <MarkdownRenderer v-if="form.memoryMd.trim()" :content="form.memoryMd" />
            <EmptyState
              v-else
              title="记忆还是空的"
              description="点右上角「编辑」写下你的学习重点，或先收藏几篇文章再点「自动刷新」。"
            />
          </div>

          <div v-else class="memory-edit-pane">
            <textarea
              v-model="form.memoryMd"
              class="memory-editor"
              rows="18"
              maxlength="800"
              spellcheck="false"
              placeholder="用 Markdown 写下你的学习画像..."
            />
            <p class="editor-hint">支持 Markdown 标题与列表。保存后，收藏「对我更重要」排序和 AI 摘要/复习卡都会参考这份内容。</p>
          </div>
        </section>
      </template>
    </template>
  </section>
</template>

<style scoped>
.learning-memory-page {
  display: grid;
  gap: 18px;
  width: min(920px, 100%);
  margin: 0 auto;
}

.memory-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
}

.memory-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  padding: 1rem 1.1rem;
}

.memory-meta strong {
  display: block;
  font-size: 0.78rem;
  color: #64748b;
  margin-bottom: 0.2rem;
}

.memory-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.92rem;
}

.memory-edited-badge {
  align-self: start;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 0.78rem;
}

.memory-tags,
.memory-document {
  padding: 1.1rem 1.2rem;
}

.tag-group + .tag-group {
  margin-top: 0.85rem;
}

.tag-group-label {
  display: block;
  margin-bottom: 0.45rem;
  font-size: 0.82rem;
  color: #64748b;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  font-size: 0.82rem;
}

.tag-chip.auto {
  background: #eff6ff;
  color: #1d4ed8;
}

.tag-chip.manual {
  background: #ecfdf5;
  color: #0f766e;
}

.tag-input {
  width: 100%;
}

.tag-empty {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}

.memory-doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.85rem;
}

.char-count {
  font-size: 0.82rem;
  color: #64748b;
}

.memory-read-pane {
  padding: 0.35rem 0.15rem;
  border-top: 1px solid #e2e8f0;
}

.memory-edit-pane {
  display: grid;
  gap: 0.65rem;
}

.memory-editor {
  width: 100%;
  min-height: 360px;
  padding: 0.9rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 0.92rem;
  line-height: 1.55;
  resize: vertical;
  background: #f8fafc;
}

.editor-hint {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
}

.form-success,
.form-error {
  margin: 0;
}

@media (max-width: 720px) {
  .memory-meta {
    grid-template-columns: 1fr;
  }
}
</style>
