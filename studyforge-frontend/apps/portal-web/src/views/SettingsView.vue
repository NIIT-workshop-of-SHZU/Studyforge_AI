<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ImagePlus, KeyRound, RefreshCw, Save, Volume2 } from '@lucide/vue';
import { getIntegrationSettings, saveIntegrationSettings } from '@/api/settings';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import type { IntegrationSetting } from '@/types/api';

const preferencesStore = usePreferencesStore();
const settings = ref<IntegrationSetting[]>([]);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const savedMessage = ref('');

const aiSettings = computed(() => settings.value.filter((item) => item.settingKey.startsWith('ai.')));
const voiceSettings = computed(() => settings.value.filter((item) => item.settingKey.startsWith('voice.')));
const imageSettings = computed(() => settings.value.filter((item) => item.settingKey.startsWith('image.')));

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      eyebrow: 'Integrations',
      title: 'AI, Voice, and Image Settings',
      refresh: 'Refresh',
      save: 'Save',
      saving: 'Saving',
      loading: 'Loading settings',
      unavailable: 'Settings are unavailable',
      saved: 'Saved.',
      aiPanel: 'Text AI',
      aiNote: 'Used for summaries, review cards, Q&A, and AI-assisted composition.',
      voicePanel: 'Voice Service',
      voiceNote: 'Used for speech input, recordings, and audio transcription.',
      imagePanel: 'Cover Image Generation',
      imageNote: 'Used for article cover generation based on title, summary, and body content.'
    };
  }
  return {
    eyebrow: 'Integrations',
    title: 'AI、语音与生图设置',
    refresh: '刷新',
    save: '保存',
    saving: '保存中',
    loading: '正在读取设置',
    unavailable: '设置暂时不可用',
    saved: '已保存。',
    aiPanel: '文本 AI',
    aiNote: '用于 AI 摘要、复习卡片、问答和 AI 排版。',
    voicePanel: '语音服务',
    voiceNote: '用于语音输入、录音和学习内容转写。',
    imagePanel: '封面生图',
    imageNote: '用于根据标题、摘要和正文自动生成博客风格封面图。'
  };
});

async function loadSettings() {
  loading.value = true;
  errorMessage.value = '';
  savedMessage.value = '';

  try {
    settings.value = await getIntegrationSettings();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    loading.value = false;
  }
}

async function saveSettings() {
  saving.value = true;
  errorMessage.value = '';
  savedMessage.value = '';

  try {
    await saveIntegrationSettings(settings.value);
    savedMessage.value = copy.value.saved;
    await loadSettings();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.unavailable;
  } finally {
    saving.value = false;
  }
}

function label(key: string) {
  const labels: Record<string, { zh: string; en: string }> = {
    'ai.base_url': { zh: 'AI Base URL', en: 'AI Base URL' },
    'ai.api_key': { zh: 'AI API Key', en: 'AI API Key' },
    'ai.chat_model': { zh: '文本模型', en: 'Chat Model' },
    'voice.base_url': { zh: '语音 Base URL', en: 'Voice Base URL' },
    'voice.api_key': { zh: '语音 API Key', en: 'Voice API Key' },
    'voice.model': { zh: '语音模型', en: 'Voice Model' },
    'voice.name': { zh: '语音音色', en: 'Voice Persona' },
    'image.base_url': { zh: '生图 Base URL', en: 'Image Base URL' },
    'image.api_key': { zh: '生图 API Key', en: 'Image API Key' },
    'image.model': { zh: '生图模型', en: 'Image Model' },
    'image.size': { zh: '封面尺寸', en: 'Cover Size' }
  };
  const localized = labels[key];
  if (!localized) {
    return key;
  }
  return preferencesStore.languageCode === 'en_US' ? localized.en : localized.zh;
}

onMounted(loadSettings);
</script>

<template>
  <section class="page-section">
    <div class="page-header">
      <div class="section-heading">
        <span>{{ copy.eyebrow }}</span>
        <h1>{{ copy.title }}</h1>
      </div>

      <div class="toolbar">
        <button class="secondary-button" type="button" :disabled="loading" @click="loadSettings">
          <RefreshCw :size="17" />
          <span>{{ copy.refresh }}</span>
        </button>
        <button class="primary-button" type="button" :disabled="saving" @click="saveSettings">
          <Save :size="17" />
          <span>{{ saving ? copy.saving : copy.save }}</span>
        </button>
      </div>
    </div>

    <LoadingState v-if="loading" :label="copy.loading" />
    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    <p v-if="savedMessage" class="form-success">{{ savedMessage }}</p>

    <div v-if="!loading" class="settings-grid">
      <section class="settings-panel">
        <div class="settings-panel-title">
          <KeyRound :size="19" />
          <span>{{ copy.aiPanel }}</span>
        </div>
        <p class="settings-panel-note">{{ copy.aiNote }}</p>
        <label v-for="setting in aiSettings" :key="setting.settingKey">
          <span>{{ label(setting.settingKey) }}</span>
          <input v-model.trim="setting.settingValue" :type="setting.secretFlag ? 'password' : 'text'" />
        </label>
      </section>

      <section class="settings-panel">
        <div class="settings-panel-title">
          <Volume2 :size="19" />
          <span>{{ copy.voicePanel }}</span>
        </div>
        <p class="settings-panel-note">{{ copy.voiceNote }}</p>
        <label v-for="setting in voiceSettings" :key="setting.settingKey">
          <span>{{ label(setting.settingKey) }}</span>
          <input v-model.trim="setting.settingValue" :type="setting.secretFlag ? 'password' : 'text'" />
        </label>
      </section>

      <section class="settings-panel">
        <div class="settings-panel-title">
          <ImagePlus :size="19" />
          <span>{{ copy.imagePanel }}</span>
        </div>
        <p class="settings-panel-note">{{ copy.imageNote }}</p>
        <label v-for="setting in imageSettings" :key="setting.settingKey">
          <span>{{ label(setting.settingKey) }}</span>
          <input v-model.trim="setting.settingValue" :type="setting.secretFlag ? 'password' : 'text'" />
        </label>
      </section>
    </div>
  </section>
</template>
