<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft, Heart, MessageCircle, UserRound, Image as ImageIcon, Plus, Smile, RefreshCw } from '@lucide/vue';
import { useSessionStore } from '@/stores/session';
import { usePreferencesStore } from '@/stores/preferences';
import { uploadImage } from '@/api/uploads';
import { getMyFriends } from '@/api/users';
import { formatShortDateTime } from '@/utils/date';

interface Moment {
  id: number;
  userId: number;
  username: string;
  displayName: string;
  avatarUrl: string;
  content: string;
  images: string[];
  likeCount: number;
  likedByViewer: boolean;
  comments: MomentComment[];
  createdTime: string;
}

interface MomentComment {
  id: number;
  userId: number;
  username: string;
  displayName: string;
  content: string;
  createdTime: string;
}

const router = useRouter();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();

const moments = ref<Moment[]>([]);
const loading = ref(false);
const showPostModal = ref(false);
const postContent = ref('');
const postImages = ref<string[]>([]);
const uploading = ref(false);
const commentingMomentId = ref<number | null>(null);
const commentContent = ref('');
const friendIds = ref<number[]>([]);
const momentsCoverImage = ref(localStorage.getItem('momentsCover') || '');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      moments: 'Moments',
      changeCover: 'Change cover',
      resetCover: 'Reset cover',
      post: 'Post',
      whatAreYouThinking: 'What are you thinking?',
      addImage: 'Add image',
      publish: 'Publish',
      publishing: 'Publishing...',
      cancel: 'Cancel',
      noMoments: 'No moments yet',
      beTheFirst: 'Be the first to post!',
      delete: 'Delete',
      like: 'Like',
      comment: 'Comment',
      writeComment: 'Write a comment...',
      send: 'Send',
      imageAlt: 'Image',
      uploadCover: 'Change cover',
      resetCoverBtn: 'Reset cover',
      loadFriendsError: 'Failed to load friends',
      uploadFailed: 'Upload failed',
      coverUploadFailed: 'Cover upload failed',
      user: 'User',
      me: 'Me',
      loading: 'Loading...',
      peopleLiked: 'people liked',
      postMoment: 'Post Moment'
    };
  }
  return {
    moments: '朋友圈',
    changeCover: '更换封面',
    resetCover: '重置封面',
    post: '发布',
    whatAreYouThinking: '说说你在想什么...',
    addImage: '添加图片',
    publish: '发表',
    publishing: '发表中...',
    cancel: '取消',
    noMoments: '还没有朋友圈',
    beTheFirst: '成为第一个发布的人吧！',
    delete: '删除',
    like: '点赞',
    comment: '评论',
    writeComment: '写评论...',
    send: '发送',
    imageAlt: '图片',
    uploadCover: '更换封面',
    resetCoverBtn: '重置封面',
    loadFriendsError: '加载好友失败',
    uploadFailed: '上传失败',
    coverUploadFailed: '封面上传失败',
    user: '用户',
    me: '我',
    loading: '加载中...',
    peopleLiked: '人觉得很赞',
    postMoment: '发表朋友圈'
  };
});

const STORAGE_KEY = 'studyforge_moments';

function saveMomentsToStorage(momentsList: Moment[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(momentsList));
}

function loadMomentsFromStorage(): Moment[] {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored ? JSON.parse(stored) : [];
}

async function loadFriends() {
  try {
    const friends = await getMyFriends();
    friendIds.value = friends.map(f => f.userId);
  } catch (e) {
    console.error(copy.loadFriendsError, e);
  }
}

async function loadMoments() {
  loading.value = true;
  await loadFriends();
  
  const storedMoments = loadMomentsFromStorage();
  
  // 只显示自己和好友的朋友圈
  moments.value = storedMoments.filter(moment => 
    moment.userId === sessionStore.userId || 
    friendIds.value.includes(moment.userId)
  ).sort((a, b) => new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime());
  
  loading.value = false;
}

function formatDate(value: unknown) {
  return formatShortDateTime(value, preferencesStore.languageCode);
}

async function handleImageUpload(event: Event) {
  const target = event.target as HTMLInputElement;
  if (!target.files || target.files.length === 0) {
    return;
  }

  const file = target.files[0];
  if (!file.type.startsWith('image/')) {
    return;
  }

  uploading.value = true;

  try {
    const uploaded = await uploadImage(file);
    postImages.value = [...postImages.value, uploaded.url];
  } catch (error) {
    console.error(copy.uploadFailed, error);
  } finally {
    uploading.value = false;
    target.value = '';
  }
}

function triggerImageUpload() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.addEventListener('change', handleImageUpload);
  input.click();
}

function removeImage(index: number) {
  postImages.value = postImages.value.filter((_, i) => i !== index);
}

function publishMoment() {
  if (!postContent.value.trim() && postImages.value.length === 0) {
    return;
  }

  const newMoment: Moment = {
    id: Date.now(),
    userId: sessionStore.userId!,
    username: sessionStore.user?.username || 'me',
    displayName: sessionStore.user?.displayName || copy.me,
    avatarUrl: sessionStore.user?.avatarUrl || '',
    content: postContent.value,
    images: postImages.value,
    likeCount: 0,
    likedByViewer: false,
    comments: [],
    createdTime: new Date().toISOString()
  };

  const storedMoments = loadMomentsFromStorage();
  storedMoments.unshift(newMoment);
  saveMomentsToStorage(storedMoments);

  moments.value = [newMoment, ...moments.value];
  postContent.value = '';
  postImages.value = [];
  showPostModal.value = false;
}

function toggleLike(moment: Moment) {
  moment.likedByViewer = !moment.likedByViewer;
  moment.likeCount += moment.likedByViewer ? 1 : -1;

  const storedMoments = loadMomentsFromStorage();
  const index = storedMoments.findIndex(m => m.id === moment.id);
  if (index !== -1) {
    storedMoments[index] = moment;
    saveMomentsToStorage(storedMoments);
  }
}

function startComment(momentId: number) {
  commentingMomentId.value = momentId;
  commentContent.value = '';
}

function cancelComment() {
  commentingMomentId.value = null;
  commentContent.value = '';
}

function submitComment(moment: Moment) {
  if (!commentContent.value.trim()) {
    return;
  }

  const newComment: MomentComment = {
    id: Date.now(),
    userId: sessionStore.userId!,
    username: sessionStore.user?.username || 'me',
    displayName: sessionStore.user?.displayName || copy.me,
    content: commentContent.value,
    createdTime: new Date().toISOString()
  };

  moment.comments = [...moment.comments, newComment];
  cancelComment();

  const storedMoments = loadMomentsFromStorage();
  const index = storedMoments.findIndex(m => m.id === moment.id);
  if (index !== -1) {
    storedMoments[index] = moment;
    saveMomentsToStorage(storedMoments);
  }
}

// 选择朋友圈封面
function selectMomentsCover() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.addEventListener('change', handleMomentsCoverUpload);
  input.click();
}

async function handleMomentsCoverUpload(event: Event) {
  const target = event.target as HTMLInputElement;
  if (!target.files || target.files.length === 0) {
    return;
  }

  const file = target.files[0];
  if (!file.type.startsWith('image/')) {
    return;
  }

  try {
    const uploaded = await uploadImage(file);
    momentsCoverImage.value = uploaded.url;
    localStorage.setItem('momentsCover', uploaded.url);
  } catch (error) {
    console.error(copy.coverUploadFailed, error);
  } finally {
    target.value = '';
  }
}

// 重置朋友圈封面
function resetMomentsCover() {
  momentsCoverImage.value = '';
  localStorage.removeItem('momentsCover');
}

onMounted(loadMoments);
</script>

<template>
  <section class="moments-page">
    <!-- 顶部栏 -->
    <header class="moments-header">
      <button class="back-btn" @click="router.back()">
        <ArrowLeft :size="20" />
      </button>
      <h1>{{ copy.moments }}</h1>
      <button class="add-btn" @click="showPostModal = true">
        <ImageIcon :size="20" />
      </button>
    </header>

    <!-- 封面区域 -->
    <div class="moments-cover">
      <div 
        class="cover-bg" 
        :style="momentsCoverImage ? { backgroundImage: `url(${momentsCoverImage})` } : {}"
      ></div>
      <div class="user-info">
        <span class="user-name">{{ sessionStore.user?.displayName || copy.user }}</span>
        <img v-if="sessionStore.user?.avatarUrl" :src="sessionStore.user.avatarUrl" alt="" class="user-avatar" />
        <UserRound v-else :size="50" class="user-avatar-placeholder" />
      </div>
      <div class="cover-actions">
        <button class="cover-btn" @click="selectMomentsCover" :title="copy.changeCover">
          <ImageIcon :size="20" />
        </button>
        <button v-if="momentsCoverImage" class="cover-btn" @click="resetMomentsCover" :title="copy.resetCover">
          <RefreshCw :size="20" />
        </button>
      </div>
    </div>

    <!-- 朋友圈列表 -->
    <div class="moments-list">
      <div v-if="loading" class="loading-state">{{ copy.loading }}</div>
      <div v-else-if="moments.length === 0" class="empty-state">{{ copy.noMoments }}</div>
      <div v-else>
        <div v-for="moment in moments" :key="moment.id" class="moment-item">
          <img v-if="moment.avatarUrl" :src="moment.avatarUrl" alt="" class="moment-avatar" />
          <UserRound v-else :size="40" class="moment-avatar-placeholder" />
          <div class="moment-content">
            <span class="moment-author">{{ moment.displayName }}</span>
            <p v-if="moment.content" class="moment-text">{{ moment.content }}</p>
            <div v-if="moment.images.length > 0" class="moment-images">
              <img
                v-for="(img, index) in moment.images"
                :key="index"
                :src="img"
                :alt="copy.imageAlt"
                class="moment-image"
              />
            </div>
            <div class="moment-meta">
              <span class="moment-time">{{ formatDate(moment.createdTime) }}</span>
              <div class="moment-actions">
                <button class="action-btn" @click="toggleLike(moment)">
                  <Heart :size="16" :fill="moment.likedByViewer ? '#ff4d4f' : 'transparent'" />
                </button>
                <button class="action-btn" @click="startComment(moment.id)">
                  <MessageCircle :size="16" />
                </button>
              </div>
            </div>
            <div v-if="moment.likeCount > 0" class="moment-likes">
              <Heart :size="12" fill="#ff4d4f" />
              <span>{{ moment.likeCount }} {{ copy.peopleLiked }}</span>
            </div>
            <div v-if="moment.comments.length > 0" class="moment-comments">
              <div v-for="comment in moment.comments" :key="comment.id" class="comment-item">
                <span class="comment-author">{{ comment.displayName }}:</span>
                <span class="comment-text">{{ comment.content }}</span>
              </div>
            </div>
            <div v-if="commentingMomentId === moment.id" class="comment-input">
              <input
                v-model="commentContent"
                :placeholder="copy.writeComment"
                @keyup.enter="submitComment(moment)"
              />
              <button @click="submitComment(moment)">{{ copy.send }}</button>
              <button @click="cancelComment()">{{ copy.cancel }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布朋友圈弹窗 -->
    <div v-if="showPostModal" class="post-modal">
      <div class="modal-overlay" @click="showPostModal = false"></div>
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ copy.postMoment }}</h2>
          <button @click="showPostModal = false">
            <ArrowLeft :size="20" />
          </button>
        </div>
        <div class="modal-body">
          <textarea
            v-model="postContent"
            :placeholder="copy.whatAreYouThinking"
            rows="5"
          />
          <div v-if="postImages.length > 0" class="post-images-preview">
            <div v-for="(img, index) in postImages" :key="index" class="image-preview-item">
              <img :src="img" :alt="copy.imageAlt" />
              <button class="remove-btn" @click="removeImage(index)">×</button>
            </div>
          </div>
          <div class="post-toolbar">
            <button class="toolbar-btn" @click="triggerImageUpload" :disabled="uploading">
              <ImageIcon :size="24" />
            </button>
            <button class="toolbar-btn">
              <Smile :size="24" />
            </button>
          </div>
          <button class="publish-btn" @click="publishMoment" :disabled="uploading">
            {{ copy.publish }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.moments-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.moments-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: linear-gradient(to bottom, rgba(0,0,0,0.3), transparent);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  z-index: 100;
}

.back-btn,
.add-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
}

.moments-header h1 {
  font-size: 18px;
  color: white;
  font-weight: 600;
}

.moments-cover {
  height: 300px;
  position: relative;
  background: #000;
  overflow: hidden;
}

.cover-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.user-info {
  position: absolute;
  bottom: 15px;
  right: 15px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-name {
  color: white;
  font-size: 17px;
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

.user-avatar,
.user-avatar-placeholder {
  width: 70px;
  height: 70px;
  border-radius: 6px;
  border: 2px solid white;
  background: white;
}

.cover-actions {
  position: absolute;
  top: 60px;
  right: 15px;
  display: flex;
  gap: 10px;
}

.cover-btn {
  background: rgba(0, 0, 0, 0.5);
  border: none;
  color: white;
  cursor: pointer;
  padding: 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.cover-btn:hover {
  background: rgba(0, 0, 0, 0.7);
}

.moments-list {
  flex: 1;
  padding-bottom: 20px;
}

.loading-state,
.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #888;
}

.moment-item {
  display: flex;
  gap: 12px;
  padding: 15px;
  background: white;
  border-bottom: 1px solid #eee;
}

.moment-avatar,
.moment-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: #eee;
  flex-shrink: 0;
}

.moment-content {
  flex: 1;
  min-width: 0;
}

.moment-author {
  font-size: 15px;
  font-weight: 600;
  color: #576b95;
  display: block;
  margin-bottom: 6px;
}

.moment-text {
  font-size: 15px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 10px;
}

.moment-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 5px;
  margin-bottom: 10px;
}

.moment-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 4px;
}

.moment-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.moment-time {
  font-size: 12px;
  color: #999;
}

.moment-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: #f5f5f5;
  border: none;
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #666;
}

.moment-likes {
  background: #f5f5f5;
  padding: 6px 10px;
  border-radius: 4px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #576b95;
}

.moment-comments {
  background: #f5f5f5;
  padding: 8px 10px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.comment-item {
  font-size: 13px;
  line-height: 1.6;
}

.comment-author {
  color: #576b95;
  font-weight: 600;
}

.comment-text {
  color: #333;
}

.comment-input {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.comment-input input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.comment-input button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.comment-input button:first-of-type {
  background: #07c160;
  color: white;
}

.comment-input button:last-of-type {
  background: #f5f5f5;
  color: #666;
}

.post-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
}

.modal-content {
  position: relative;
  flex: 1;
  background: white;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  font-size: 17px;
  font-weight: 600;
}

.modal-header button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px;
}

.modal-body {
  flex: 1;
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.modal-body textarea {
  width: 100%;
  border: none;
  font-size: 16px;
  resize: none;
  margin-bottom: 15px;
  font-family: inherit;
}

.post-images-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 15px;
}

.image-preview-item {
  position: relative;
  aspect-ratio: 1;
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.7);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.post-toolbar {
  display: flex;
  gap: 15px;
  margin-top: auto;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.toolbar-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  color: #333;
}

.publish-btn {
  margin-top: 20px;
  background: #07c160;
  color: white;
  border: none;
  padding: 12px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  font-weight: 600;
}

.publish-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
