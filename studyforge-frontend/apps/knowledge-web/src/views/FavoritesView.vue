<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { BookmarkCheck, FolderPlus, LogIn, NotebookTabs, Pin, RefreshCw, Settings, Trash2 } from '@lucide/vue';
import {
  createCollection,
  getCollectionPosts,
  getMyCollections,
  patchCollectionPost,
  removePostFromCollection
} from '@/api/collections';
import { getLearningMemory } from '@/api/learningMemory';
import { ApiError } from '@/api/http';
import EmptyState from '@/components/EmptyState.vue';
import KnowledgeCard from '@/components/KnowledgeCard.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { FavoriteCollection, InterestTag, PostSummary, TopicCategory } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const collections = ref<FavoriteCollection[]>([]);
const posts = ref<PostSummary[]>([]);
const interestTags = ref<InterestTag[]>([]);
const activeCollectionId = ref<number | null>(null);
const sortMode = ref<'importance' | 'recent'>('importance');
const activeTag = ref('');
const loading = ref(false);
const postLoading = ref(false);
const errorMessage = ref('');
const expandedReasons = ref<number | null>(null);
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
        memory: 'MEMORY.md',
        sortImportance: 'Important to me',
        sortRecent: 'Recently saved',
        pinned: 'Pinned',
        matchPrefix: 'Match',
        showReasons: 'Why ranked here',
        hideReasons: 'Hide reasons',
        pin: 'Pin',
        unpin: 'Unpin',
        studyOverview: 'My Study',
        collectionsNav: 'Collections',
        memoryNav: 'MEMORY.md',
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
        memory: 'MEMORY.md',
        sortImportance: '对我更重要',
        sortRecent: '最近收藏',
        pinned: '已置顶',
        matchPrefix: '匹配度',
        showReasons: '为何靠前',
        hideReasons: '收起推荐原因',
        pin: '置顶',
        unpin: '取消置顶',
        studyOverview: '我的学习',
        collectionsNav: '收藏夹',
        memoryNav: 'MEMORY.md',
        expired: '登录状态已过期，请重新登录后整理收藏夹。',
        loadFailed: '收藏夹暂时没取到',
        openFailed: '这个收藏夹暂时打不开',
        createFailed: '收藏夹暂时创建不了'
      }
);

const activeCollection = computed(() => collections.value.find((collection) => collection.collectionId === activeCollectionId.value) ?? collections.value[0] ?? null);
const totalSaved = computed(() => collections.value.reduce((total, collection) => total + collection.itemCount, 0));
const tagFilters = computed(() => interestTags.value.slice(0, 8));

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

let collectionsRequestId = 0;
let postsRequestId = 0;

function isCurrentCollectionsRequest(requestId: number, userId: number) {
  return requestId === collectionsRequestId && sessionStore.isAuthenticated && sessionStore.userId === userId;
}

function isCurrentPostsRequest(requestId: number, collectionRequestId: number, collectionId: number, userId: number) {
  return (
    requestId === postsRequestId &&
    collectionRequestId === collectionsRequestId &&
    sessionStore.isAuthenticated &&
    sessionStore.userId === userId &&
    activeCollection.value?.collectionId === collectionId
  );
}

function categoryFor(post: PostSummary): TopicCategory {
  return categories.value[post.categoryCode] ?? { code: post.categoryCode, name: post.categoryCode, description: '', accent: '#0f766e' };
}

function resetCollections() {
  postsRequestId += 1;
  collections.value = [];
  posts.value = [];
  interestTags.value = [];
  activeCollectionId.value = null;
  activeTag.value = '';
  expandedReasons.value = null;
  errorMessage.value = '';
  loading.value = false;
  postLoading.value = false;
  form.name = '';
  form.description = '';
  form.visibility = 'PRIVATE';
}

async function loadLearningMemory(requestId: number, userId: number) {
  try {
    const memory = await getLearningMemory();
    if (isCurrentCollectionsRequest(requestId, userId)) {
      interestTags.value = memory.interestTags ?? [];
    }
  } catch {
    if (isCurrentCollectionsRequest(requestId, userId)) {
      interestTags.value = [];
    }
  }
}

async function loadCollections() {
  const requestId = ++collectionsRequestId;
  const userId = sessionStore.userId;

  if (!sessionStore.isAuthenticated || userId === null) {
    resetCollections();
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    await loadLearningMemory(requestId, userId);
    const collectionData = await getMyCollections();

    if (!isCurrentCollectionsRequest(requestId, userId)) {
      return;
    }

    collections.value = collectionData;
    if (!collectionData.some((collection) => collection.collectionId === activeCollectionId.value)) {
      activeCollectionId.value = collectionData[0]?.collectionId ?? null;
    }
    await loadPosts(requestId);
  } catch (error) {
    if (!isCurrentCollectionsRequest(requestId, userId)) {
      return;
    }
    if (error instanceof ApiError && error.code === 4010) {
      await sessionStore.logout();
      errorMessage.value = copy.value.expired;
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadFailed;
  } finally {
    if (isCurrentCollectionsRequest(requestId, userId)) {
      loading.value = false;
    }
  }
}

async function loadPosts(collectionRequestId = collectionsRequestId) {
  const requestId = ++postsRequestId;
  const userId = sessionStore.userId;

  if (!sessionStore.isAuthenticated || userId === null) {
    resetCollections();
    return;
  }

  if (collectionRequestId !== collectionsRequestId) {
    return;
  }

  if (!activeCollection.value) {
    posts.value = [];
    postLoading.value = false;
    return;
  }

  postLoading.value = true;
  const collectionId = activeCollection.value.collectionId;
  try {
    const postData = await getCollectionPosts(collectionId, preferencesStore.languageCode, {
      sort: sortMode.value,
      tag: activeTag.value || undefined
    });

    if (!isCurrentPostsRequest(requestId, collectionRequestId, collectionId, userId)) {
      return;
    }

    posts.value = postData;
  } catch (error) {
    if (isCurrentPostsRequest(requestId, collectionRequestId, collectionId, userId)) {
      errorMessage.value = error instanceof Error ? error.message : copy.value.openFailed;
    }
  } finally {
    if (isCurrentPostsRequest(requestId, collectionRequestId, collectionId, userId)) {
      postLoading.value = false;
    }
  }
}

async function selectCollection(collectionId: number) {
  activeCollectionId.value = collectionId;
  await loadPosts();
}

async function changeSort(mode: 'importance' | 'recent') {
  sortMode.value = mode;
  await loadPosts();
}

async function changeTag(tag: string) {
  activeTag.value = activeTag.value === tag ? '' : tag;
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

async function togglePin(post: PostSummary) {
  if (!activeCollection.value) {
    return;
  }
  const nextPinned = !post.pinned;
  await patchCollectionPost(activeCollection.value.collectionId, post.postId, preferencesStore.languageCode, {
    pinned: nextPinned
  });
  await loadPosts();
}

function toggleReasons(postId: number) {
  expandedReasons.value = expandedReasons.value === postId ? null : postId;
}

onMounted(loadCollections);

watch(
  () => [sessionStore.isAuthenticated, sessionStore.userId, preferencesStore.languageCode],
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

    <nav class="study-subnav" :aria-label="copy.title">
      <RouterLink to="/library">{{ copy.studyOverview }}</RouterLink>
      <RouterLink to="/favorites">{{ copy.collectionsNav }}</RouterLink>
      <RouterLink to="/memory">{{ copy.memoryNav }}</RouterLink>
    </nav>

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
        <RouterLink class="secondary-button" :to="{ path: '/memory', query: { from: 'favorites' } }">
          <Settings :size="17" />
          <span>{{ copy.memory }}</span>
        </RouterLink>
        <RouterLink class="secondary-button" to="/library">
          <NotebookTabs :size="17" />
          <span>{{ copy.studyOverview }}</span>
        </RouterLink>
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

          <div class="favorites-sort-bar">
            <button class="secondary-button" :class="{ active: sortMode === 'importance' }" type="button" @click="changeSort('importance')">
              {{ copy.sortImportance }}
            </button>
            <button class="secondary-button" :class="{ active: sortMode === 'recent' }" type="button" @click="changeSort('recent')">
              {{ copy.sortRecent }}
            </button>
          </div>

          <div v-if="tagFilters.length" class="favorites-tag-bar">
            <button
              v-for="tag in tagFilters"
              :key="tag.tag"
              class="secondary-button tag-chip"
              :class="{ active: activeTag === tag.tag }"
              type="button"
              @click="changeTag(tag.tag)"
            >
              {{ tag.tag }}
            </button>
          </div>

          <LoadingState v-if="postLoading" :label="copy.openLoading" />
          <div v-else-if="posts.length" class="collection-post-list">
            <article v-for="(post, index) in posts" :key="post.postId" class="collection-post-card">
              <div v-if="post.pinned" class="pin-badge">{{ copy.pinned }}</div>
              <div v-if="sortMode === 'importance' && post.importanceScore != null" class="importance-badge">
                {{ copy.matchPrefix }} {{ Math.round(post.importanceScore * 100) }}%
              </div>
              <KnowledgeCard :post="post" :category="categoryFor(post)" :index="index" />
              <div v-if="post.rankReasons?.length" class="rank-reasons">
                <button class="text-button" type="button" @click="toggleReasons(post.postId)">
                  {{ expandedReasons === post.postId ? copy.hideReasons : copy.showReasons }}
                </button>
                <ul v-if="expandedReasons === post.postId">
                  <li v-for="reason in post.rankReasons" :key="reason">{{ reason }}</li>
                </ul>
              </div>
              <div class="collection-post-actions">
                <button class="secondary-button" type="button" @click="togglePin(post)">
                  <Pin :size="16" />
                  <span>{{ post.pinned ? copy.unpin : copy.pin }}</span>
                </button>
                <button class="secondary-button remove-button" type="button" @click="removePost(post.postId)">
                  <Trash2 :size="16" />
                  <span>{{ copy.remove }}</span>
                </button>
              </div>
            </article>
          </div>
          <EmptyState v-else :title="copy.emptyCollection" :description="copy.emptyCollectionDesc" />
        </main>
      </section>
    </template>
  </section>
</template>

<style scoped>
.favorites-sort-bar,
.favorites-tag-bar,
.collection-post-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.favorites-sort-bar,
.favorites-tag-bar {
  margin-bottom: 1rem;
}

.secondary-button.active {
  border-color: #0f766e;
  color: #0f766e;
}

.tag-chip {
  font-size: 0.85rem;
}

.pin-badge,
.importance-badge {
  display: inline-block;
  margin-bottom: 0.5rem;
  margin-right: 0.35rem;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
}

.pin-badge {
  background: #ecfdf5;
  color: #0f766e;
}

.importance-badge {
  background: #eff6ff;
  color: #1d4ed8;
}

.rank-reasons {
  margin: 0.5rem 0;
  font-size: 0.85rem;
  color: #475569;
}

.rank-reasons ul {
  margin: 0.35rem 0 0;
  padding-left: 1.1rem;
}

.text-button {
  border: none;
  background: none;
  color: #0f766e;
  cursor: pointer;
  padding: 0;
}
</style>
