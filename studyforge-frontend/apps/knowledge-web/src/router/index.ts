import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import LandingView from '@/views/LandingView.vue';
import LibraryView from '@/views/LibraryView.vue';
import LoginView from '@/views/LoginView.vue';
import PostDetailView from '@/views/PostDetailView.vue';
import PublishView from '@/views/PublishView.vue';
import HelpView from '@/views/HelpView.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: LandingView
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: HomeView
    },
    {
      path: '/posts/:postId',
      name: 'post-detail',
      component: PostDetailView
    },
    {
      path: '/publish',
      name: 'publish',
      component: PublishView
    },
    {
      path: '/help',
      name: 'help',
      component: HelpView
    },
    {
      path: '/library',
      name: 'library',
      component: LibraryView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});
