<template>
  <div class="loading-state" role="status" aria-live="polite">
    <span class="loading-dot" />
    <span>{{ resolvedLabel }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { usePreferencesStore } from '@/stores/preferences';

const props = defineProps<{
  label?: string;
}>();

const preferencesStore = usePreferencesStore();

const resolvedLabel = computed(() => {
  if (props.label) {
    return props.label;
  }

  return preferencesStore.languageCode === 'en_US' ? 'Loading' : '加载中';
});
</script>
