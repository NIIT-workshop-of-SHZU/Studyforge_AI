import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import AccountSettingsView from '@/views/AccountSettingsView.vue';
import FavoritesView from '@/views/FavoritesView.vue';
import FriendsView from '@/views/FriendsView.vue';
import LandingView from '@/views/LandingView.vue';
import LibraryView from '@/views/LibraryView.vue';
import LoginView from '@/views/LoginView.vue';
import NotificationsView from '@/views/NotificationsView.vue';
import PostDetailView from '@/views/PostDetailView.vue';
import ProfileView from '@/views/ProfileView.vue';
import PublishView from '@/views/PublishView.vue';
import HelpView from '@/views/HelpView.vue';
import SkillsView from '@/views/SkillsView.vue';
import SkillDetailView from '@/views/SkillDetailView.vue';
import RoadmapDetailView from '@/views/RoadmapDetailView.vue';

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
      path: '/posts/:postId/edit',
      name: 'post-edit',
      component: PublishView
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
      path: '/favorites',
      name: 'favorites',
      component: FavoritesView
    },
    {
      path: '/friends',
      name: 'friends',
      component: FriendsView
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: NotificationsView
    },
    {
      path: '/account',
      name: 'account-settings',
      component: AccountSettingsView
    },
    {
      path: '/me',
      name: 'me',
      component: ProfileView
    },
    {
      path: '/users/:userId',
      name: 'user-profile',
      component: ProfileView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/skills',
      name: 'skills',
      component: SkillsView
    },
    {
      path: '/skills/:skillCode',
      name: 'skill-detail',
      component: SkillDetailView
    },
    {
      path: '/roadmaps/:roadmapId',
      name: 'roadmap-detail',
      component: RoadmapDetailView
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});
