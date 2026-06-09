import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from '@/App.vue';
import { router } from '@/router';
import { useAuthStore } from '@/stores/auth';
import '@/assets/base.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const authStore = useAuthStore(pinia);
authStore.hydrate();

if (typeof window !== 'undefined') {
  const syncSession = () => authStore.syncFromStorage();
  window.addEventListener('storage', syncSession);
  window.addEventListener('studyforge:portal-auth-changed', syncSession as EventListener);
}

app.use(router);
app.mount('#app');
