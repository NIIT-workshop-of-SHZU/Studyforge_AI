<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { BookmarkCheck, FolderPlus, LogIn, RefreshCw, Trash2 } from '@lucide/vue';
import { createCollection, getCollectionPosts, getMyCollections, removePostFromCollection } from '@/api/collections';
import { ApiError } from '@/api/http';
import EmptyState from '@/components/EmptyState.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { FavoriteCollection, PostSummary, TopicCategory } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const collections = ref<FavoriteCollection[]>([]);
const posts = ref<PostSummary[]>([]);
const activeCollectionId = ref<number | null>(null);
const loading = ref(false);
const postLoading = ref(false);
const errorMessage = ref('');
const form = reactive({
  name: '',
  description: '',
  visibility: 'PRIVATE' as 'PUBLIC' | 'PRIVATE'
});

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        title: 'Collections',
        desc: 'Organize the posts you want to revisit so they are easy to find later.',
        loginTitle: 'Sign in to organize collections',
        loginDesc: 'Create themed collections to group posts by project, course, or review plan.',
        login: 'Log in',
        collectionCount: 'Collections',
        savedCount: 'Saved Posts',
        refresh: 'Refresh',
        loading: 'Loading collections',
        unavailable: 'Unable to load collections right now',
        myCollections: 'My Collections',
        createCollection: 'New Collection',
        namePlaceholder: 'Collection name',
        descPlaceholder: 'Add a short note about what this collection is for',
        private: 'Private',
        public: 'Public',
        create: 'Create',
        posts: 'posts',
        untitled: 'Collection',
        openLoading: 'Opening collection',
        remove: 'Remove',
        emptyCollection: 'This collection has no posts yet',
        emptyCollectionDesc: 'Saved posts first go to the default collection. You can create more themed collections here.',
        defaultCollectionDesc: 'No description yet.',
        expired: 'Your session expired. Please sign in again to manage collections.',
        loadFailed: 'Collections could not be loaded.',
        openFailed: 'This collection could not be opened.',
        createFailed: 'Collection could not be created.'
      }
    : {
        title: '收藏夹',
        desc: '把值得反复看的文章按主题收好，读完之后也能找得回来。',
        loginTitle: '登录后整理收藏夹',
        loginDesc: '你可以创建不同主题的收藏夹，把文章按项目、课程或复习计划整理起来。',
        login: '登录',
        collectionCount: '收藏夹',
        savedCount: '已整理文章',
        refresh: '刷新',
        loading: '正在整理收藏夹',
        unavailable: '暂时无法加载',
        myCollections: '我的收藏夹',
        createCollection: '新建收藏夹',
        namePlaceholder: '收藏夹名称',
        descPlaceholder: '简单说明这个收藏夹适合放什么',
        private: '私密',
        public: '公开',
        create: '创建',
        posts: '篇',
        untitled: '收藏夹',
        openLoading: '正在打开收藏夹',
        remove: '移出',
        emptyCollection: '这个收藏夹还没有文章',
        emptyCollectionDesc: '在文章详情页点收藏后，会先进入默认收藏；你也可以在这里继续新建主题收藏夹。',
        defaultCollectionDesc: '还没有说明。',
        expired: '登录状态已过期，请重新登录后整理收藏夹。',
        loadFailed: '收藏夹暂时没取到',
        openFailed: '这个收藏夹暂时打不开',
        createFailed: '收藏夹暂时创建不了'
      }
);

const activeCollection = computed(() => collections.value.find((collection) => collection.collectionId === activeCollectionId.value) ?? collections.value[0] ?? null);
const totalSaved = computed(() => collections.value.reduce((total, collection) => total + collection.itemCount, 0));

const categories = computed<Record<string, TopicCategory>>(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        TECHNOLOGY: { code: 'TECHNOLOGY', name: 'Technology', description: 'Frontend, backend, tools', accent: '#2563eb' },
        BUSINESS: { code: 'BUSINESS', name: 'Business', description: 'Cases, decisions, markets', accent: '#7c3aed' },
        PRODUCTIVITY: { code: 'PRODUCTIVITY', name: 'Productivity', description: 'Notes, reviews, planning', accent: '#b45309' },
        CAREER: { code: 'CAREER', name: 'Career', description: 'Interviews, growth, job search', accent: '#15803d' },
        FINANCE: { code: 'FINANCE', name: 'Finance', description: 'Budget, risk, basics', accent: '#0891b2' }
      }
    : {
        TECHNOLOGY: { code: 'TECHNOLOGY', name: '技术实践', description: '前端、后端、工具', accent: '#2563eb' },
        BUSINESS: { code: 'BUSINESS', name: '商业观察', description: '案例、决策、市场', accent: '#7c3aed' },
        PRODUCTIVITY: { code: 'PRODUCTIVITY', name: '效率方法', description: '笔记、复盘、计划', accent: '#b45309' },
        CAREER: { code: 'CAREER', name: '职业成长', description: '求职、面试、成长', accent: '#15803d' },
        FINANCE: { code: 'FINANCE', name: '财务入门', description: '预算、风险、常识', accent: '#0891b2' }
      }
);

function categoryFor(post: PostSummary): TopicCategory {
  return categories.value[post.categoryCode] ?? { code: post.categoryCode, name: post.categoryCode, description: '', accent: '#0f766e' };
}

async function loadCollections() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';

  try {
    collections.value = await getMyCollections();
    activeCollectionId.value = activeCollectionId.value ?? collections.value[0]?.collectionId ?? null;
    await loadPosts();
  } catch (error) {
    if (error instanceof ApiError && error.code === 4010) {
      await sessionStore.logout();
      errorMessage.value = copy.value.expired;
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadFailed;
  } finally {
    loading.value = false;
  }
}

async function loadPosts() {
  if (!activeCollection.value) {
    posts.value = [];
    return;
  }

  postLoading.value = true;
  try {
    posts.value = await getCollectionPosts(activeCollection.value.collectionId, preferencesStore.languageCode);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.openFailed;
  } finally {
    postLoading.value = false;
  }
}

async function selectCollection(collectionId: number) {
  activeCollectionId.value = collectionId;
  await loadPosts();
}

async function submitCollection() {
  if (!form.name.trim()) {
    return;
  }

  try {
    const collection = await createCollection({
      name: form.name.trim(),
      description: form.description.trim(),
      visibility: form.visibility
    });
    collections.value = [...collections.value, collection];
    activeCollectionId.value = collection.collectionId;
    form.name = '';
    form.description = '';
    form.visibility = 'PRIVATE';
    await loadPosts();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.createFailed;
  }
}

async function removePost(postId: number) {
  if (!activeCollection.value) {
    return;
  }
  await removePostFromCollection(activeCollection.value.collectionId, postId);
  posts.value = posts.value.filter((post) => post.postId !== postId);
  collections.value = collections.value.map((collection) =>
    collection.collectionId === activeCollection.value?.collectionId
      ? { ...collection, itemCount: Math.max(0, collection.itemCount - 1) }
      : collection
  );
}

onMounted(loadCollections);

watch(
  () => [sessionStore.isAuthenticated, preferencesStore.languageCode],
  () => loadCollections()
);
</script>

<template>
  <section class="favorites-page">
    <div class="page-heading">
      <span class="section-kicker">Collections</span>
      <h1>{{ copy.title }}</h1>
      <p>{{ copy.desc }}</p>
    </div>

    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <BookmarkCheck :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">
        <LogIn :size="17" />
        <span>{{ copy.login }}</span>
      </RouterLink>
    </div>

    <template v-else>
      <section class="favorites-summary">
        <div>
          <BookmarkCheck :size="24" />
          <span>{{ copy.collectionCount }}</span>
          <strong>{{ collections.length }}</strong>
        </div>
        <div>
          <BookmarkCheck :size="24" />
          <span>{{ copy.savedCount }}</span>
          <strong>{{ totalSaved }}</strong>
        </div>
        <button class="secondary-button" type="button" :disabled="loading" @click="loadCollections">
          <RefreshCw :size="17" />
          <span>{{ copy.refresh }}</span>
        </button>
      </section>

      <LoadingState v-if="loading" :label="copy.loading" />
      <EmptyState v-else-if="errorMessage" :title="copy.unavailable" :description="errorMessage" />

      <section v-else class="favorites-layout">
        <aside class="collection-sidebar">
          <div class="panel-title">
            <BookmarkCheck :size="18" />
            <span>{{ copy.myCollections }}</span>
          </div>
          <button
            v-for="collection in collections"
            :key="collection.collectionId"
            class="collection-item"
            :class="{ active: collection.collectionId === activeCollection?.collectionId }"
            type="button"
            @click="selectCollection(collection.collectionId)"
          >
            <strong>{{ collection.name }}</strong>
            <span>{{ collection.itemCount }} {{ copy.posts }} · {{ collection.visibility === 'PUBLIC' ? copy.public : copy.private }}</span>
            <small>{{ collection.description || copy.defaultCollectionDesc }}</small>
          </button>

          <form class="collection-form" @submit.prevent="submitCollection">
            <div class="panel-title">
              <FolderPlus :size="18" />
              <span>{{ copy.createCollection }}</span>
            </div>
            <input v-model.trim="form.name" maxlength="80" :placeholder="copy.namePlaceholder" />
            <textarea v-model.trim="form.description" rows="3" maxlength="300" :placeholder="copy.descPlaceholder" />
            <select v-model="form.visibility">
              <option value="PRIVATE">{{ copy.private }}</option>
              <option value="PUBLIC">{{ copy.public }}</option>
            </select>
            <button class="primary-button full-width" type="submit">{{ copy.create }}</button>
          </form>
        </aside>

        <main class="collection-posts">
          <div class="feed-toolbar">
            <strong>{{ activeCollection?.name || copy.untitled }}</strong>
            <span>{{ posts.length }} {{ copy.posts }}</span>
          </div>

          <LoadingState v-if="postLoading" :label="copy.openLoading" />
          <div v-else-if="posts.length" class="collection-post-list">
            <article v-for="(post, index) in posts" :key="post.postId" class="collection-post-card">
              <KnowledgeCard :post="post" :category="categoryFor(post)" :index="index" />
              <button class="secondary-button remove-button" type="button" @click="removePost(post.postId)">
                <Trash2 :size="16" />
                <span>{{ copy.remove }}</span>
              </button>
            </article>
          </div>
          <EmptyState v-else :title="copy.emptyCollection" :description="copy.emptyCollectionDesc" />
        </main>
      </section>
    </template>
  </section>
</template>
