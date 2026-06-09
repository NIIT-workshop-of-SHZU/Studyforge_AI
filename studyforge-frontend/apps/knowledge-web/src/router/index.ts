import { createRouter, createWebHistory } from 'vue-router';
import { useSessionStore } from '@/stores/session';
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
import HomepageCommunityDetailView from '@/views/HomepageCommunityDetailView.vue';
import HomepageCommunityView from '@/views/HomepageCommunityView.vue';
import HomepageStudioView from '@/views/HomepageStudioView.vue';

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      path: '/homepage-studio',
      name: 'homepage-studio',
      component: HomepageStudioView
    },
    {
      path: '/homepages',
      name: 'homepage-community',
      component: HomepageCommunityView
    },
    {
      path: '/homepages/:designId',
      name: 'homepage-community-detail',
      component: HomepageCommunityDetailView
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
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to) => {
  const sessionStore = useSessionStore();
  sessionStore.hydrate();
  sessionStore.syncFromStorage();

  if (to.name === 'login' && sessionStore.isAuthenticated) {
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/knowledge';
  }

  return true;
});
