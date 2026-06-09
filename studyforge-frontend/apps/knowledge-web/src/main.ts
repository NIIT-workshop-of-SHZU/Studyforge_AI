import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from '@/App.vue';
import { router } from '@/router';
import { useSessionStore } from '@/stores/session';
import '@/assets/base.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const sessionStore = useSessionStore(pinia);
sessionStore.hydrate();

if (typeof window !== 'undefined') {
  const syncSession = () => sessionStore.syncFromStorage();
  window.addEventListener('storage', syncSession);
  window.addEventListener('studyforge:knowledge-session-changed', syncSession as EventListener);
}

app.use(router);
app.mount('#app');
