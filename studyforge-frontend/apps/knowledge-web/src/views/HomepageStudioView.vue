<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import {
  ArrowLeft,
  Code2,
  Eye,
  Film,
  ImagePlus,
  LayoutTemplate,
  RefreshCw,
  Save,
  Send,
  Share2,
  Sparkles,
  UserRound
} from '@lucide/vue';
import {
  getMyHomepage,
  publishHomepageCommunity,
  saveHomepageDraft,
  submitHomepageReview,
  uploadHomepageMedia
} from '@/api/homepages';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { UserHomepage } from '@/types/api';

type LayoutMode = 'default' | 'code' | 'media';
type MediaItem = { type: 'gif' | 'video'; url: string; title: string; caption: string };

type CodeTemplate = {
  key: string;
  label: { zh: string; en: string };
  summary: { zh: string; en: string };
  themeConfig: string;
  customCodeDraft: string;
};

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const homepage = ref<UserHomepage | null>(null);
const loading = ref(false);
const saving = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const publishTitle = ref('');
const publishSummary = ref('');

const codeTemplates: CodeTemplate[] = [
  {
    key: 'minimal',
    label: { zh: '简约', en: 'Minimal' },
    summary: { zh: '留白克制，突出内容和身份信息。', en: 'Quiet spacing and content-first hierarchy.' },
    themeConfig: JSON.stringify({ accent: '#0f766e', surface: '#f8fafc' }, null, 2),
    customCodeDraft: `.hero {
  min-height: 280px;
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}
.hero::before {
  content: "Minimal identity";
  display: inline-flex;
  margin-bottom: 18px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero::after {
  content: "A calm profile layout focused on writing, projects, and trust signals.";
  display: block;
  max-width: 520px;
  font-size: 17px;
  line-height: 1.8;
}`
  },
  {
    key: 'tech',
    label: { zh: '科技', en: 'Tech' },
    summary: { zh: '网格、数据感和冷色高对比。', en: 'Grid-driven, data-forward, and high contrast.' },
    themeConfig: JSON.stringify({ accent: '#38bdf8', surface: '#08111f' }, null, 2),
    customCodeDraft: `.hero {
  position: relative;
  min-height: 300px;
  padding: 32px;
  border: 1px solid rgba(56, 189, 248, 0.35);
  border-radius: 28px;
  background:
    linear-gradient(90deg, rgba(56, 189, 248, 0.14) 1px, transparent 1px),
    linear-gradient(180deg, rgba(56, 189, 248, 0.12) 1px, transparent 1px),
    linear-gradient(145deg, rgba(8, 17, 31, 0.96), rgba(15, 23, 42, 0.96));
  background-size: 28px 28px, 28px 28px, auto;
}
.hero::before {
  content: "Tech Mode / Portfolio Matrix";
  display: block;
  color: #7dd3fc;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.04em;
}
.hero::after {
  content: "Show current systems, stack focus, and measurable contribution in a compact hero.";
  display: block;
  max-width: 560px;
  margin-top: 18px;
  color: #cbd5e1;
  font-size: 16px;
  line-height: 1.8;
}`
  },
  {
    key: 'art',
    label: { zh: '艺术', en: 'Art' },
    summary: { zh: '更自由的层次、渐变和展示性排版。', en: 'Expressive layers, gradients, and visual storytelling.' },
    themeConfig: JSON.stringify({ accent: '#f97316', surface: '#241437' }, null, 2),
    customCodeDraft: `.hero {
  min-height: 320px;
  padding: 34px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.55), transparent 26%),
    radial-gradient(circle at bottom right, rgba(168, 85, 247, 0.42), transparent 30%),
    linear-gradient(145deg, rgba(36, 20, 55, 0.96), rgba(88, 28, 135, 0.92));
  box-shadow: 0 28px 72px rgba(88, 28, 135, 0.28);
}
.hero::before {
  content: "Artful presence";
  display: block;
  margin-bottom: 14px;
  color: #fde68a;
  font-size: 30px;
  font-weight: 800;
}
.hero::after {
  content: "Blend motion, media, and personality into a homepage that feels curated rather than templated.";
  display: block;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.86);
  font-size: 16px;
  line-height: 1.85;
}`
  }
];

const form = reactive({
  templateType: 'GITHUB_DEFAULT',
  layoutMode: 'default' as LayoutMode,
  themeConfig: JSON.stringify({ accent: '#22c55e', surface: '#0f172a' }, null, 2),
  customCodeDraft: codeTemplates[0].customCodeDraft,
  mediaLayoutDraft: JSON.stringify([], null, 2)
});

const mediaItems = computed<MediaItem[]>(() => {
  try {
    const parsed = JSON.parse(form.mediaLayoutDraft);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
});

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      loginTitle: 'Design your homepage after signing in',
      loginDesc: 'Choose the default template, a coded layout, or a media-driven profile experience.',
      login: 'Log in',
      back: 'Back to profile',
      eyebrow: 'Homepage',
      title: 'Homepage Studio',
      refresh: 'Refresh',
      loading: 'Loading homepage configuration',
      unavailable: 'Homepage configuration is unavailable',
      designPath: 'Design Path',
      templateType: 'Template ID',
      layoutMode: 'Layout mode',
      defaultMode: 'Default template',
      codeMode: 'Code-based homepage',
      mediaMode: 'Media-based homepage',
      themeConfig: 'Theme config JSON',
      codeTemplates: 'Code templates',
      replaceHint: 'Selecting a template replaces the current code draft and theme config.',
      codeSnippet: 'HTML / CSS / JS snippet',
      uploadMedia: 'Upload GIF / MP4 / WebM',
      uploadingMedia: 'Uploading media',
      mediaLayout: 'Media layout JSON',
      saveDraft: 'Save draft',
      savingDraft: 'Saving',
      submitReview: 'Submit for admin review',
      submittingReview: 'Submitting',
      releasePreview: 'Release Preview',
      previewArea: 'Preview & community publish',
      reviewStatus: 'Review status',
      deadlinePrefix: 'Admin review deadline:',
      livePreview: 'Live Preview',
      mediaEmptyTitle: 'No media blocks yet',
      mediaEmptyDesc: 'Upload GIF, MP4, or WebM to build a media-based homepage preview.',
      defaultPreview: 'The default template keeps the platform’s classic profile layout.',
      communityTitle: 'Community design title',
      communityTitlePlaceholder: 'For example: Aurora Developer Homepage',
      communitySummary: 'Community design summary',
      communitySummaryPlaceholder: 'Describe the intended vibe, audience, and layout language.',
      publishCommunity: 'Publish to community gallery',
      publishingCommunity: 'Submitting',
      browseCommunity: 'Browse community homepage designs'
    };
  }

  return {
    loginTitle: '登录后设计主页',
    loginDesc: '可以选择平台默认模板、代码式主页，或通过视频与动图打造媒体式主页。',
    login: '登录',
    back: '返回主页',
    eyebrow: 'Homepage',
    title: '主页设计中心',
    refresh: '刷新',
    loading: '正在读取主页配置',
    unavailable: '主页配置暂时不可用',
    designPath: '设计路径',
    templateType: '模板标识',
    layoutMode: '主页模式',
    defaultMode: '默认模板',
    codeMode: '代码式主页',
    mediaMode: '媒体式主页',
    themeConfig: '主题配置 JSON',
    codeTemplates: '代码模板',
    replaceHint: '点击模板会直接替换当前代码草稿和主题配置，请先保存需要保留的内容。',
    codeSnippet: 'HTML / CSS / JS 代码片段',
    uploadMedia: '上传 GIF / MP4 / WebM',
    uploadingMedia: '媒体上传中',
    mediaLayout: '媒体布局 JSON',
    saveDraft: '保存草稿',
    savingDraft: '保存中',
    submitReview: '提交管理员审核',
    submittingReview: '提交中',
    releasePreview: '发布态预览',
    previewArea: '预览与社区发布',
    reviewStatus: '审核状态',
    deadlinePrefix: '管理员最晚审核时间：',
    livePreview: '实时预览',
    mediaEmptyTitle: '还没有媒体区块',
    mediaEmptyDesc: '上传 GIF、MP4 或 WebM 后，这里会形成媒体式主页预览。',
    defaultPreview: '默认模板会继续使用平台的经典个人主页布局。',
    communityTitle: '社区设计标题',
    communityTitlePlaceholder: '例如：Aurora Developer Homepage',
    communitySummary: '社区设计简介',
    communitySummaryPlaceholder: '说明这套主页适合什么风格、适合谁来使用。',
    publishCommunity: '发布到社区模板专区',
    publishingCommunity: '提交中',
    browseCommunity: '浏览社区主页设计'
  };
});

const selectedTemplateKey = computed(() => {
  return codeTemplates.find((item) => item.customCodeDraft === form.customCodeDraft)?.key ?? '';
});

async function loadHomepage() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const data = await getMyHomepage();
    homepage.value = data;
    form.templateType = data.templateType || 'GITHUB_DEFAULT';
    form.layoutMode = (data.layoutMode as LayoutMode) || 'default';
    form.themeConfig = data.themeConfig || '{}';
    form.customCodeDraft = data.customCodeDraft || codeTemplates[0].customCodeDraft;
    form.mediaLayoutDraft = data.mediaLayoutDraft || '[]';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

function applyCodeTemplate(template: CodeTemplate) {
  form.templateType = `CODE_${template.key.toUpperCase()}`;
  form.layoutMode = 'code';
  form.themeConfig = template.themeConfig;
  form.customCodeDraft = template.customCodeDraft;
}

async function saveDraft() {
  saving.value = 'draft';
  errorMessage.value = '';
  successMessage.value = '';
  try {
    homepage.value = await saveHomepageDraft({ ...form });
    successMessage.value =
      preferencesStore.languageCode === 'en_US' ? 'Homepage draft saved.' : '主页草稿已保存。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    saving.value = '';
  }
}

async function submitReview() {
  saving.value = 'review';
  errorMessage.value = '';
  successMessage.value = '';
  try {
    homepage.value = await submitHomepageReview();
    successMessage.value =
      preferencesStore.languageCode === 'en_US'
        ? 'Submitted for community admin review. A decision is expected within 48 hours.'
        : '已提交给社区管理员审核，48 小时内会得到处理结果。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    saving.value = '';
  }
}

async function publishCommunity() {
  saving.value = 'community';
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await publishHomepageCommunity({ title: publishTitle.value, summary: publishSummary.value });
    successMessage.value =
      preferencesStore.languageCode === 'en_US'
        ? 'Submitted to the community gallery. It becomes public and clonable after admin approval.'
        : '已提交到社区模板专区，管理员审核通过后才会公开并允许克隆。';
    publishTitle.value = '';
    publishSummary.value = '';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    saving.value = '';
  }
}

async function uploadMedia(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  saving.value = 'media';
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const uploaded = await uploadHomepageMedia(file);
    const items = [...mediaItems.value];
    items.push({
      type: uploaded.contentType?.startsWith('video/') ? 'video' : 'gif',
      url: uploaded.url,
      title: file.name.replace(/\.[^.]+$/, ''),
      caption:
        preferencesStore.languageCode === 'en_US'
          ? 'Adjust the title, caption, and order in the JSON editor below.'
          : '可以在下方 JSON 中继续调整标题、文案和顺序。'
    });
    form.mediaLayoutDraft = JSON.stringify(items, null, 2);
    successMessage.value =
      preferencesStore.languageCode === 'en_US'
        ? 'Media uploaded into the homepage draft.'
        : '媒体素材已上传到主页草稿。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    saving.value = '';
    input.value = '';
  }
}

onMounted(loadHomepage);
</script>

<template>
  <section class="account-page">
    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <UserRound :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">{{ copy.login }}</RouterLink>
    </div>

    <template v-else>
      <div class="page-heading with-actions">
        <div>
          <RouterLink class="secondary-button return-link" to="/me">
            <ArrowLeft :size="17" />
            <span>{{ copy.back }}</span>
          </RouterLink>
          <span>{{ copy.eyebrow }}</span>
          <h1>{{ copy.title }}</h1>
        </div>
        <button class="secondary-button" type="button" :disabled="loading" @click="loadHomepage">
          <RefreshCw :size="17" />
          <span>{{ copy.refresh }}</span>
        </button>
      </div>

      <LoadingState v-if="loading" :label="copy.loading" />
      <EmptyState v-else-if="errorMessage && !homepage" :title="copy.unavailable" :description="errorMessage" />

      <div v-else class="account-layout homepage-studio-layout">
        <section class="account-panel">
          <div class="panel-title">
            <LayoutTemplate :size="18" />
            <span>{{ copy.designPath }}</span>
          </div>

          <form class="account-form" @submit.prevent="saveDraft">
            <label>
              <span>{{ copy.templateType }}</span>
              <input v-model.trim="form.templateType" type="text" maxlength="40" />
            </label>

            <label>
              <span>{{ copy.layoutMode }}</span>
              <select v-model="form.layoutMode">
                <option value="default">{{ copy.defaultMode }}</option>
                <option value="code">{{ copy.codeMode }}</option>
                <option value="media">{{ copy.mediaMode }}</option>
              </select>
            </label>

            <label>
              <span>{{ copy.themeConfig }}</span>
              <textarea v-model="form.themeConfig" rows="7" />
            </label>

            <template v-if="form.layoutMode === 'code'">
              <div class="panel-title compact-panel-title">
                <Sparkles :size="16" />
                <span>{{ copy.codeTemplates }}</span>
              </div>
              <p class="settings-panel-note">{{ copy.replaceHint }}</p>
              <div class="homepage-design-grid homepage-template-selector">
                <button
                  v-for="template in codeTemplates"
                  :key="template.key"
                  class="homepage-design-card template-button"
                  type="button"
                  :data-active="selectedTemplateKey === template.key"
                  @click="applyCodeTemplate(template)"
                >
                  <div class="homepage-design-meta">
                    <strong>{{ preferencesStore.languageCode === 'en_US' ? template.label.en : template.label.zh }}</strong>
                    <span>{{ preferencesStore.languageCode === 'en_US' ? template.summary.en : template.summary.zh }}</span>
                  </div>
                </button>
              </div>
              <label>
                <span>{{ copy.codeSnippet }}</span>
                <textarea v-model="form.customCodeDraft" rows="14" spellcheck="false" />
              </label>
            </template>

            <template v-if="form.layoutMode === 'media'">
              <label class="file-upload-line">
                <ImagePlus :size="17" />
                <span>{{ saving === 'media' ? copy.uploadingMedia : copy.uploadMedia }}</span>
                <input type="file" accept="image/gif,video/mp4,video/webm" :disabled="saving === 'media'" @change="uploadMedia" />
              </label>
              <label>
                <span>{{ copy.mediaLayout }}</span>
                <textarea v-model="form.mediaLayoutDraft" rows="14" spellcheck="false" />
              </label>
            </template>

            <div class="form-actions composer-actions">
              <button class="primary-button" type="submit" :disabled="saving === 'draft'">
                <Save :size="17" />
                <span>{{ saving === 'draft' ? copy.savingDraft : copy.saveDraft }}</span>
              </button>
              <button class="secondary-button" type="button" :disabled="saving === 'review'" @click="submitReview">
                <Send :size="17" />
                <span>{{ saving === 'review' ? copy.submittingReview : copy.submitReview }}</span>
              </button>
              <RouterLink
                class="secondary-button"
                :to="{ path: '/me', query: { preview: 'published' } }"
              >
                <Eye :size="17" />
                <span>{{ copy.releasePreview }}</span>
              </RouterLink>
            </div>
          </form>
        </section>

        <section class="account-panel">
          <div class="panel-title">
            <Code2 v-if="form.layoutMode === 'code'" :size="18" />
            <Film v-else :size="18" />
            <span>{{ copy.previewArea }}</span>
          </div>

          <div class="homepage-preview-stack">
            <div class="homepage-status-card">
              <strong>{{ copy.reviewStatus }}</strong>
              <span>{{ homepage?.reviewStatus || 'DRAFT' }}</span>
              <small v-if="homepage?.reviewDeadlineAt">{{ copy.deadlinePrefix }}{{ homepage.reviewDeadlineAt }}</small>
              <small v-if="homepage?.reviewComment">{{ homepage.reviewComment }}</small>
            </div>

            <div class="homepage-status-card">
              <strong>{{ copy.livePreview }}</strong>
            </div>

            <div v-if="form.layoutMode === 'code'" class="homepage-code-hero">
              <iframe
                class="homepage-code-frame"
                title="code-preview"
                :srcdoc="`<style>${form.customCodeDraft}</style><div class='hero'></div>`"
                sandbox="allow-scripts"
              />
            </div>

            <div v-else-if="form.layoutMode === 'media'" class="homepage-media-grid">
              <article v-for="(item, index) in mediaItems" :key="`${item.url}-${index}`" class="homepage-media-card">
                <video v-if="item.type === 'video'" :src="item.url" controls muted playsinline />
                <img v-else :src="item.url" alt="" />
                <div class="homepage-media-copy">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.caption }}</span>
                </div>
              </article>
              <EmptyState v-if="mediaItems.length === 0" :title="copy.mediaEmptyTitle" :description="copy.mediaEmptyDesc" />
            </div>

            <div v-else class="profile-preview-strip">
              <strong>{{ copy.defaultPreview }}</strong>
            </div>

            <form class="account-form" @submit.prevent="publishCommunity">
              <label>
                <span>{{ copy.communityTitle }}</span>
                <input v-model.trim="publishTitle" type="text" maxlength="120" :placeholder="copy.communityTitlePlaceholder" />
              </label>
              <label>
                <span>{{ copy.communitySummary }}</span>
                <textarea
                  v-model.trim="publishSummary"
                  rows="4"
                  maxlength="300"
                  :placeholder="copy.communitySummaryPlaceholder"
                />
              </label>
              <button class="secondary-button" type="submit" :disabled="saving === 'community'">
                <Share2 :size="17" />
                <span>{{ saving === 'community' ? copy.publishingCommunity : copy.publishCommunity }}</span>
              </button>
            </form>

            <RouterLink class="secondary-button stable-action" to="/homepages">
              {{ copy.browseCommunity }}
            </RouterLink>
          </div>

          <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        </section>
      </div>
    </template>
  </section>
</template>
