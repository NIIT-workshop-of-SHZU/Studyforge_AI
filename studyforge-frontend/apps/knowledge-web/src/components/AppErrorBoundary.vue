<script setup lang="ts">
import { computed, onErrorCaptured, ref } from 'vue';
import { AlertTriangle, RefreshCw } from '@lucide/vue';
import { usePreferencesStore } from '@/stores/preferences';

const preferencesStore = usePreferencesStore();
const errorMessage = ref('');

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        title: 'This page is temporarily unavailable.',
        reload: 'Reload'
      }
    : {
        title: '页面暂时打不开',
        reload: '重新加载'
      }
);

onErrorCaptured((error) => {
  errorMessage.value = error instanceof Error ? error.message : copy.value.title;
  return false;
});

function reload() {
  window.location.reload();
}
</script>

<template>
  <div v-if="errorMessage" class="error-boundary">
    <AlertTriangle :size="28" />
    <div>
      <h2>{{ copy.title }}</h2>
      <p>{{ errorMessage }}</p>
    </div>
    <button class="secondary-button" type="button" @click="reload">
      <RefreshCw :size="17" />
      <span>{{ copy.reload }}</span>
    </button>
  </div>

  <slot v-else />
</template>
