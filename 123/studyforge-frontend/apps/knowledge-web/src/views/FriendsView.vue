<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowLeft, Check, ChevronRight, Compass, Gift, Image, Inbox, MessageCircle, Mic, MoreVertical, Phone, Plus, RefreshCw, Search, Send, Smile, UserRound, Users, Video, X, FileImage } from '@lucide/vue';
import {
  getFriendMessages,
  getIncomingFriendRequests,
  getMyFriends,
  getOutgoingFriendRequests,
  reviewFriendRequest,
  sendFriendMessage,
  searchUsersByEmail,
  sendFriendRequest
} from '@/api/users';
import { uploadImage } from '@/api/uploads';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { FriendMessage, FriendRequest, SocialUser } from '@/types/api';
import { formatShortDateTime } from '@/utils/date';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const router = useRouter();
const friends = ref<SocialUser[]>([]);
const incoming = ref<FriendRequest[]>([]);
const outgoing = ref<FriendRequest[]>([]);
const messages = ref<FriendMessage[]>([]);
const activeFriend = ref<SocialUser | null>(null);
const draftMessage = ref('');
const activeTab = ref<'chat' | 'contacts' | 'discover'>('chat');
const activePanel = ref<'messages' | 'requests'>('messages');
const loading = ref(false);
const actionLoading = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const showSearch = ref(false);
const searchQuery = ref('');
const showRequests = ref(false);
const showAddFriendModal = ref(false);
const filteredFriends = ref<SocialUser[]>([]);
const searchResults = ref<SocialUser[]>([]);
const isSearching = ref(false);
const friendEmail = ref('');
const searchByEmailResults = ref<SocialUser[]>([]);
const showEmojiPicker = ref(false);
const uploadingImage = ref(false);
const chatBackgroundImage = ref(localStorage.getItem('chatBackground') || '');

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      searchPlaceholder: 'Search friends by email or name',
      searching: 'Searching...',
      chat: 'Chat',
      contacts: 'Contacts',
      discover: 'Discover',
      noMessages: 'No messages yet',
      noChats: 'No chats yet',
      startChatting: 'Start chatting with friends',
      searchResults: 'Search Results',
      adding: 'Adding...',
      addFriend: 'Add Friend',
      commonContacts: 'Common Contacts',
      friendRequests: 'Friend Requests',
      newFriends: 'New Friends',
      moments: 'Moments',
      changeBackground: 'Change background',
      resetBackground: 'Reset background',
      noMessagesYet: 'No messages yet',
      sendFirstMessage: 'Send the first message to start chatting',
      imageAlt: 'Image',
      typeMessage: 'Type a message...',
      friendRequestsTitle: 'Friend Requests',
      wantToAddYou: 'Wants to add you as a friend',
      accept: 'Accept',
      reject: 'Reject',
      noFriendRequests: 'No friend requests',
      willShowHere: 'It will show here when someone adds you',
      addFriendTitle: 'Add Friend',
      friendEmailLabel: 'Friend email address',
      enterFriendEmail: 'Enter friend email',
      search: 'Search',
      searchingEmail: 'Searching...',
      addFriendBtn: 'Add Friend',
      addingFriend: 'Adding...',
      send: 'Send',
      sending: 'Sending...',
      uploadBackground: 'Change background',
      uploadCover: 'Change cover',
      resetCover: 'Reset cover',
      friendsLoadError: 'Failed to load friends',
      searchError: 'Search failed',
      friendRequestSent: 'Friend request sent',
      friendRequestFailed: 'Failed to send friend request',
      invalidEmail: 'Please enter a valid email address',
      userNotFound: 'User not found',
      checkEmail: 'Please check the email address',
      messagesLoadError: 'Failed to load messages',
      requestAccepted: 'Friend request accepted',
      requestRejected: 'Friend request rejected',
      requestFailed: 'Failed to process friend request',
      messageSendFailed: 'Failed to send message',
      imageOnly: 'Only image files allowed',
      imageTooLarge: 'Image size cannot exceed 10MB',
      imageUploadFailed: 'Image upload failed',
      friendRequestDefault: 'I want to add you as a friend',
      wechatFeatureTitle: 'Sign in to use social features',
      wechatFeatureDesc: 'Please sign in to use chat, moments and other social features',
      signIn: 'Sign in',
      userNotFoundTitle: 'User not found',
      userNotFoundDesc: 'Please check the email address'
    };
  }
  return {
    searchPlaceholder: '搜索好友邮箱或名称',
    searching: '搜索中...',
    chat: '聊天',
    contacts: '通讯录',
    discover: '发现',
    noMessages: '暂无消息',
    noChats: '暂无聊天',
    startChatting: '开始和好友聊天吧',
    searchResults: '搜索结果',
    adding: '发送中...',
    addFriend: '添加好友',
    commonContacts: '常用联系人',
    friendRequests: '好友申请',
    newFriends: '新的朋友',
    moments: '朋友圈',
    changeBackground: '更换背景',
    resetBackground: '重置背景',
    noMessagesYet: '还没有消息',
    sendFirstMessage: '发送第一条消息开始交流吧',
    imageAlt: '图片',
    typeMessage: '输入消息...',
    friendRequestsTitle: '好友申请',
    wantToAddYou: '想添加你为好友',
    accept: '接受',
    reject: '拒绝',
    noFriendRequests: '暂无好友申请',
    willShowHere: '有人添加你时会在这里显示',
    addFriendTitle: '添加好友',
    friendEmailLabel: '好友邮箱地址',
    enterFriendEmail: '请输入好友邮箱',
    search: '搜索',
    searchingEmail: '搜索中...',
    addFriendBtn: '添加好友',
    addingFriend: '发送中...',
    send: '发送',
    sending: '发送中...',
    uploadBackground: '更换背景',
    uploadCover: '更换封面',
    resetCover: '重置封面',
    friendsLoadError: '好友数据暂时没取到',
    searchError: '搜索失败',
    friendRequestSent: '好友申请已发送',
    friendRequestFailed: '好友申请发送失败',
    invalidEmail: '请输入有效的邮箱地址',
    userNotFound: '未找到该用户',
    checkEmail: '请检查邮箱地址是否正确',
    messagesLoadError: '消息暂时没取到',
    requestAccepted: '已通过好友申请',
    requestRejected: '已拒绝好友申请',
    requestFailed: '好友申请处理失败',
    messageSendFailed: '消息没有发送成功',
    imageOnly: '只能上传图片文件',
    imageTooLarge: '图片大小不能超过10MB',
    imageUploadFailed: '图片上传失败',
    friendRequestDefault: '我想添加你为好友',
    wechatFeatureTitle: '登录后使用微信功能',
    wechatFeatureDesc: '请登录账号以使用好友聊天、朋友圈等功能',
    signIn: '立即登录',
    userNotFoundTitle: '未找到用户',
    userNotFoundDesc: '请检查邮箱地址是否正确'
  };
});

// 常用表情包
const commonEmojis = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
  '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩',
  '😘', '😗', '😚', '😙', '🥲', '😋', '😛', '😜',
  '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐',
  '👍', '👎', '👏', '🙌', '👋', '🤝', '💪', '❤️'
];

const pendingIncoming = computed(() => incoming.value.filter((item) => item.status === 'PENDING'));
const pendingOutgoing = computed(() => outgoing.value.filter((item) => item.status === 'PENDING'));

async function loadFriends() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const [friendData, incomingData, outgoingData] = await Promise.all([
      getMyFriends(),
      getIncomingFriendRequests('ALL'),
      getOutgoingFriendRequests('ALL')
    ]);
    friends.value = friendData.map(friend => ({
      ...friend,
      unreadCount: 0,
      lastMessage: '',
      lastMessageTime: ''
    }));
    filteredFriends.value = [...friends.value];
    incoming.value = incomingData;
    outgoing.value = outgoingData;
    if (activeFriend.value) {
      const stillFriend = friendData.find((friend) => friend.userId === activeFriend.value?.userId);
      activeFriend.value = stillFriend ?? null;
      if (stillFriend) {
        await loadMessages(stillFriend.userId);
      } else {
        messages.value = [];
      }
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.friendsLoadError;
  } finally {
    loading.value = false;
  }
}

async function filterFriends() {
  if (!searchQuery.value.trim()) {
    filteredFriends.value = [...friends.value];
    searchResults.value = [];
    return;
  }
  
  // 如果包含@符号，按邮箱搜索
  if (searchQuery.value.includes('@')) {
    isSearching.value = true;
    try {
      const users = await searchUsersByEmail(searchQuery.value);
      searchResults.value = users;
      filteredFriends.value = [];
    } catch (error) {
      searchResults.value = [];
      errorMessage.value = error instanceof Error ? error.message : copy.searchError;
    } finally {
      isSearching.value = false;
    }
  } else {
    // 按用户名和昵称搜索
    const query = searchQuery.value.toLowerCase();
    filteredFriends.value = friends.value.filter(friend => 
      friend.displayName.toLowerCase().includes(query) ||
      friend.username.toLowerCase().includes(query)
    );
    searchResults.value = [];
  }
}

function clearSearch() {
  searchQuery.value = '';
  filterFriends();
  showSearch.value = false;
}

async function sendFriendRequestByEmail(user: SocialUser) {
  actionLoading.value = `add-${user.userId}`;
  try {
    await sendFriendRequest(user.userId, copy.friendRequestDefault);
    successMessage.value = copy.friendRequestSent;
    await loadFriends();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.friendRequestFailed;
  } finally {
    actionLoading.value = '';
  }
}

async function searchUserByEmail() {
  if (!friendEmail.value.trim() || !friendEmail.value.includes('@')) {
    errorMessage.value = copy.invalidEmail;
    return;
  }
  
  isSearching.value = true;
  try {
    const users = await searchUsersByEmail(friendEmail.value);
    searchByEmailResults.value = users;
    if (users.length === 0) {
      successMessage.value = copy.userNotFound;
    }
  } catch (error) {
    searchByEmailResults.value = [];
    errorMessage.value = error instanceof Error ? error.message : copy.searchError;
  } finally {
    isSearching.value = false;
  }
}

function closeAddFriendModal() {
  showAddFriendModal.value = false;
  friendEmail.value = '';
  searchByEmailResults.value = [];
  errorMessage.value = '';
  successMessage.value = '';
}

function closeChat() {
  activeFriend.value = null;
  messages.value = [];
  draftMessage.value = '';
}

async function openChat(friend: SocialUser) {
  activePanel.value = 'messages';
  activeFriend.value = friend;
  await loadMessages(friend.userId);
}

async function loadMessages(friendId: number) {
  try {
    messages.value = await getFriendMessages(friendId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.messagesLoadError;
  }
}

async function handleRequest(request: FriendRequest, decision: 'ACCEPT' | 'REJECT') {
  actionLoading.value = `${decision}-${request.requestId}`;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    await reviewFriendRequest(request.requestId, decision);
    successMessage.value = decision === 'ACCEPT' ? copy.requestAccepted : copy.requestRejected;
    await loadFriends();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.requestFailed;
  } finally {
    actionLoading.value = '';
  }
}

async function sendMessage() {
  const content = draftMessage.value.trim();
  if (!activeFriend.value || !content) {
    return;
  }

  actionLoading.value = 'message';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const message = await sendFriendMessage(activeFriend.value.userId, content);
    messages.value = [...messages.value, message];
    draftMessage.value = '';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.messageSendFailed;
  } finally {
    actionLoading.value = '';
  }
}

// 添加表情到输入框
function insertEmoji(emoji: string) {
  draftMessage.value = draftMessage.value + emoji;
  showEmojiPicker.value = false;
}

// 处理图片上传
async function handleImageUpload(event: Event) {
  const target = event.target as HTMLInputElement;
  if (!target.files || target.files.length === 0) {
    return;
  }

  const file = target.files[0];
  if (!file.type.startsWith('image/')) {
    errorMessage.value = copy.imageOnly;
    return;
  }

  if (file.size > 10 * 1024 * 1024) {
    errorMessage.value = copy.imageTooLarge;
    return;
  }

  uploadingImage.value = true;
  errorMessage.value = '';

  try {
    const uploaded = await uploadImage(file);
    const message = await sendFriendMessage(activeFriend.value!.userId, `![image](${uploaded.url})`);
    messages.value = [...messages.value, message];
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.imageUploadFailed;
  } finally {
    uploadingImage.value = false;
    target.value = '';
  }
}

// 触发图片选择
function triggerImageUpload() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.addEventListener('change', handleImageUpload);
  input.click();
}

function formatDate(value: unknown) {
  return formatShortDateTime(value, preferencesStore.languageCode);
}

function goToMoments() {
  router.push('/knowledge/moments');
}

// 选择聊天背景
function selectChatBackground() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.addEventListener('change', handleChatBackgroundUpload);
  input.click();
}

async function handleChatBackgroundUpload(event: Event) {
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
    chatBackgroundImage.value = uploaded.url;
    localStorage.setItem('chatBackground', uploaded.url);
  } catch (error) {
    console.error('背景上传失败', error);
  } finally {
    target.value = '';
  }
}

// 重置聊天背景
function resetChatBackground() {
  chatBackgroundImage.value = '';
  localStorage.removeItem('chatBackground');
}

onMounted(loadFriends);
</script>

<template>
  <section class="wechat-friends">
    <!-- 微信风格顶部导航 -->
    <header class="wechat-header">
      <div class="header-left">
      </div>
      <div class="header-right">
        <button class="icon-btn" @click="showSearch = !showSearch">
          <Search :size="20" />
        </button>
        <button class="icon-btn" @click="showAddFriendModal = true">
          <Plus :size="20" />
        </button>
      </div>
    </header>

    <!-- 搜索框 -->
    <div v-if="showSearch" class="search-bar">
      <input type="text" :placeholder="copy.searchPlaceholder" v-model="searchQuery" @input="filterFriends" />
      <X :size="16" @click="clearSearch" />
      <span v-if="isSearching" class="search-loading">{{ copy.searching }}</span>
    </div>

    <!-- 主内容区域 -->
    <div class="wechat-container">
      <!-- 左侧导航栏 -->
      <aside class="wechat-sidebar">
        <div class="nav-item" :class="{ active: activeTab === 'chat' }" @click="activeTab = 'chat'">
          <MessageCircle :size="22" />
          <span>{{ copy.chat }}</span>
        </div>
        <div class="nav-item" :class="{ active: activeTab === 'contacts' }" @click="activeTab = 'contacts'">
          <Users :size="22" />
          <span>{{ copy.contacts }}</span>
        </div>
        <div class="nav-item" :class="{ active: activeTab === 'discover' }" @click="activeTab = 'discover'">
          <Compass :size="22" />
          <span>{{ copy.discover }}</span>
        </div>
      </aside>

      <!-- 聊天列表 -->
      <main v-if="activeTab === 'chat'" class="chat-list-container">
        <div class="chat-list">
          <div
            v-for="friend in filteredFriends"
            :key="friend.userId"
            class="chat-item"
            :class="{ active: activeFriend?.userId === friend.userId }"
            @click="openChat(friend)"
          >
            <div class="chat-avatar">
              <img v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="" class="avatar" />
              <UserRound v-else :size="40" class="avatar-placeholder" />
              <span v-if="friend.unreadCount > 0" class="unread-badge">{{ friend.unreadCount }}</span>
            </div>
            <div class="chat-content">
              <div class="chat-header">
                <span class="friend-name">{{ friend.displayName }}</span>
                <span class="chat-time">{{ friend.lastMessageTime }}</span>
              </div>
              <div class="chat-preview">
                <span class="last-message">{{ friend.lastMessage || copy.noMessages }}</span>
              </div>
            </div>
          </div>
          <EmptyState v-if="filteredFriends.length === 0 && searchResults.length === 0" :title="copy.noChats" :description="copy.startChatting" />

          <!-- 邮箱搜索结果 -->
          <div v-if="searchResults.length > 0" class="search-results">
            <div class="section-title">{{ copy.searchResults }}</div>
            <div
              v-for="user in searchResults"
              :key="user.userId"
              class="search-result-item"
            >
              <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" class="avatar" />
              <UserRound v-else :size="40" class="avatar-placeholder" />
              <div class="user-info">
                <span class="user-name">{{ user.displayName }}</span>
                <span class="user-email">{{ user.email }}</span>
                <span class="user-username">@{{ user.username }}</span>
              </div>
              <button 
                class="add-friend-btn"
                @click="sendFriendRequestByEmail(user)"
                :disabled="actionLoading === `add-${user.userId}`"
              >
                {{ actionLoading === `add-${user.userId}` ? copy.adding : copy.addFriend }}
              </button>
            </div>
          </div>
        </div>
      </main>

      <!-- 通讯录 -->
      <main v-else-if="activeTab === 'contacts'" class="contacts-container">
        <div class="contacts-header">
          <h2>{{ copy.contacts }}</h2>
        </div>
        <div class="contacts-list">
          <div class="contact-section">
            <div class="section-title">{{ copy.commonContacts }}</div>
            <div
              v-for="friend in friends"
              :key="friend.userId"
              class="contact-item"
              @click="openChat(friend)"
            >
              <img v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="" class="avatar" />
              <UserRound v-else :size="36" class="avatar-placeholder" />
              <span class="contact-name">{{ friend.displayName }}</span>
            </div>
          </div>
          <div class="contact-section">
            <div class="section-title">{{ copy.friendRequests }}</div>
            <div class="contact-item" @click="showRequests = true">
              <Inbox :size="36" />
              <span class="contact-name">{{ copy.newFriends }}</span>
              <span v-if="pendingIncoming.length > 0" class="unread-badge">{{ pendingIncoming.length }}</span>
            </div>
          </div>
        </div>
      </main>

      <!-- 发现页 -->
      <main v-else-if="activeTab === 'discover'" class="discover-container">
        <div class="discover-header">
          <h2>{{ copy.discover }}</h2>
        </div>
        <div class="discover-menu">
          <div class="menu-item" @click="goToMoments">
            <Image :size="36" />
            <span>{{ copy.moments }}</span>
            <ChevronRight :size="20" />
          </div>
        </div>
      </main>
    </div>

    <!-- 聊天窗口 -->
    <div v-if="activeFriend" class="chat-window" :style="chatBackgroundImage ? { backgroundImage: `url(${chatBackgroundImage})` } : {}">
      <div class="chat-header-bar">
        <button class="back-btn" @click="closeChat">
          <ArrowLeft :size="20" />
        </button>
        <div class="chat-friend-info">
          <img v-if="activeFriend.avatarUrl" :src="activeFriend.avatarUrl" alt="" class="small-avatar" />
          <UserRound v-else :size="24" class="small-avatar-placeholder" />
          <span class="friend-name">{{ activeFriend.displayName }}</span>
        </div>
        <div class="chat-actions">
          <button class="icon-btn" @click="selectChatBackground" :title="copy.changeBackground">
            <Image :size="20" />
          </button>
          <button v-if="chatBackgroundImage" class="icon-btn" @click="resetChatBackground" :title="copy.resetBackground">
            <RefreshCw :size="20" />
          </button>
          <button class="icon-btn">
            <Phone :size="20" />
          </button>
          <button class="icon-btn">
            <Video :size="20" />
          </button>
          <button class="icon-btn">
            <MoreVertical :size="20" />
          </button>
        </div>
      </div>

      <div class="message-container">
        <div class="message-list">
          <div
            v-for="message in messages"
            :key="message.messageId"
            class="message-item"
            :class="{ sent: message.senderId === sessionStore.userId, received: message.senderId !== sessionStore.userId }"
          >
            <img v-if="message.senderId !== sessionStore.userId && activeFriend.avatarUrl" :src="activeFriend.avatarUrl" alt="" class="message-avatar" />
            <div class="message-bubble">
              <!-- 检查是否是图片消息 -->
              <template v-if="message.content.startsWith('![image](') && message.content.endsWith(')')">
                <img :src="message.content.slice(8, -1)" :alt="copy.imageAlt" class="message-image" />
              </template>
              <template v-else>
                <p class="message-content">{{ message.content }}</p>
              </template>
              <span class="message-time">{{ formatDate(message.createdTime) }}</span>
            </div>
            <img v-if="message.senderId === sessionStore.userId && sessionStore.user?.avatarUrl" :src="sessionStore.user.avatarUrl" alt="" class="message-avatar" />
          </div>
          <EmptyState v-if="messages.length === 0" :title="copy.noMessagesYet" :description="copy.sendFirstMessage" />
        </div>

        <div class="message-input-area">
          <!-- 表情选择器 -->
          <div v-if="showEmojiPicker" class="emoji-picker">
            <div class="emoji-grid">
              <button
                v-for="emoji in commonEmojis"
                :key="emoji"
                class="emoji-btn"
                @click="insertEmoji(emoji)"
              >
                {{ emoji }}
              </button>
            </div>
          </div>

          <div class="input-toolbar">
            <button class="toolbar-btn" @click="showEmojiPicker = !showEmojiPicker">
              <Smile :size="20" />
            </button>
            <button class="toolbar-btn" @click="triggerImageUpload" :disabled="uploadingImage">
              <FileImage :size="20" />
            </button>
            <button class="toolbar-btn">
              <Mic :size="20" />
            </button>
          </div>
          <form class="message-form" @submit.prevent="sendMessage">
            <textarea
              v-model.trim="draftMessage"
              rows="1"
              maxlength="2000"
              :placeholder="copy.typeMessage"
              class="message-input"
            />
            <button 
              class="send-btn" 
              type="submit" 
              :disabled="!draftMessage.trim() || actionLoading === 'message' || uploadingImage"
            >
              <Send :size="18" />
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- 好友申请弹窗 -->
    <div v-if="showRequests" class="requests-modal">
      <div class="modal-header">
        <h2>{{ copy.friendRequestsTitle }}</h2>
        <button class="close-btn" @click="showRequests = false">
          <X :size="20" />
        </button>
      </div>
      <div class="requests-list">
        <div v-for="request in pendingIncoming" :key="request.requestId" class="request-item">
          <img v-if="request.requester.avatarUrl" :src="request.requester.avatarUrl" alt="" class="avatar" />
          <UserRound v-else :size="40" class="avatar-placeholder" />
          <div class="request-info">
            <div class="requester-name">{{ request.requester.displayName }}</div>
            <div class="request-message">{{ request.message || copy.wantToAddYou }}</div>
          </div>
          <div class="request-actions">
            <button class="accept-btn" @click="handleRequest(request, 'ACCEPT')" :disabled="actionLoading === `ACCEPT-${request.requestId}`">
              {{ copy.accept }}
            </button>
            <button class="reject-btn" @click="handleRequest(request, 'REJECT')" :disabled="actionLoading === `REJECT-${request.requestId}`">
              {{ copy.reject }}
            </button>
          </div>
        </div>
        <EmptyState v-if="pendingIncoming.length === 0" :title="copy.noFriendRequests" :description="copy.willShowHere" />
      </div>
    </div>

    <!-- 添加好友弹窗 -->
    <div v-if="showAddFriendModal" class="add-friend-modal">
      <div class="modal-overlay" @click="closeAddFriendModal"></div>
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ copy.addFriendTitle }}</h2>
          <button class="close-btn" @click="closeAddFriendModal">
            <X :size="20" />
          </button>
        </div>
        <div class="modal-body">
          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
          
          <div class="email-input-section">
            <label>{{ copy.friendEmailLabel }}</label>
            <input 
              type="email" 
              v-model="friendEmail" 
              :placeholder="copy.enterFriendEmail"
              @keyup.enter="searchUserByEmail"
            />
            <button 
              class="search-btn"
              @click="searchUserByEmail"
              :disabled="isSearching || !friendEmail.trim()"
            >
              {{ isSearching ? copy.searchingEmail : copy.search }}
            </button>
          </div>
          
          <!-- 搜索结果 -->
          <div v-if="searchByEmailResults.length > 0" class="search-results-section">
            <h3>{{ copy.searchResults }}</h3>
            <div
              v-for="user in searchByEmailResults"
              :key="user.userId"
              class="search-result-item"
            >
              <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" class="avatar" />
              <UserRound v-else :size="40" class="avatar-placeholder" />
              <div class="user-info">
                <span class="user-name">{{ user.displayName }}</span>
                <span class="user-email">{{ user.email }}</span>
                <span class="user-username">@{{ user.username }}</span>
              </div>
              <button 
                class="add-friend-btn"
                @click="sendFriendRequestByEmail(user)"
                :disabled="actionLoading === `add-${user.userId}`"
              >
                {{ actionLoading === `add-${user.userId}` ? copy.addingFriend : copy.addFriendBtn }}
              </button>
            </div>
          </div>
          
          <div v-if="searchByEmailResults.length === 0 && friendEmail && !isSearching" class="no-results">
            <EmptyState :title="copy.userNotFoundTitle" :description="copy.userNotFoundDesc" />
          </div>
        </div>
      </div>
    </div>

    <!-- 登录提示 -->
    <div v-if="!sessionStore.isAuthenticated" class="login-prompt">
      <UserRound :size="60" />
      <h2>{{ copy.wechatFeatureTitle }}</h2>
      <p>{{ copy.wechatFeatureDesc }}</p>
      <RouterLink class="login-btn" to="/login">{{ copy.signIn }}</RouterLink>
    </div>
  </section>
</template>

<style scoped>
/* 微信主题色 */
:root {
  --wechat-green: #07c160;
  --wechat-green-light: #e7f8ee;
  --wechat-gray: #f1f1f1;
  --wechat-gray-dark: #888;
  --wechat-gray-darker: #333;
  --wechat-white: #ffffff;
  --wechat-red: #ff4d4f;
  --wechat-blue: #1677ff;
}

.wechat-friends {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--wechat-gray);
}

/* 顶部导航栏 */
.wechat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--wechat-green);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.wechat-header h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 20px;
}

.icon-btn {
  background: #ff4d4f;
  border: 2px solid #ff7875;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background-color: #ff7875;
  border-color: #ff4d4f;
}

/* 搜索框 */
.search-bar {
  padding: 12px 16px;
  background-color: white;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--wechat-gray);
}

.search-bar input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  padding: 8px 0;
}

.search-loading {
  font-size: 12px;
  color: var(--wechat-green);
  font-weight: 500;
}

/* 搜索结果 */
.search-results {
  padding: 16px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--wechat-gray-darker);
}

.user-email {
  font-size: 14px;
  color: var(--wechat-green);
}

.user-username {
  font-size: 12px;
  color: var(--wechat-gray-dark);
}

.add-friend-btn {
  background-color: var(--wechat-green);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.add-friend-btn:hover:not(:disabled) {
  background-color: #06ad56;
}

.add-friend-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 添加好友弹窗 */
.add-friend-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 400;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.modal-content {
  position: relative;
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--wechat-gray);
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--wechat-gray-darker);
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--wechat-gray-dark);
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background-color: var(--wechat-gray);
  color: var(--wechat-gray-darker);
}

.modal-body {
  padding: 24px;
}

.email-input-section {
  margin-bottom: 24px;
}

.email-input-section label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--wechat-gray-darker);
  margin-bottom: 8px;
}

.email-input-section input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--wechat-gray);
  border-radius: 8px;
  font-size: 15px;
  margin-bottom: 12px;
  outline: none;
}

.email-input-section input:focus {
  border-color: var(--wechat-green);
}

.search-btn {
  width: 100%;
  background-color: var(--wechat-green);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-btn:hover:not(:disabled) {
  background-color: #06ad56;
}

.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-results-section {
  margin-top: 24px;
}

.search-results-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--wechat-gray-darker);
  margin-bottom: 16px;
}

/* 添加好友弹窗中的搜索结果项 */
.search-results-section .search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  width: 100%;
}

.search-results-section .search-result-item .add-friend-btn {
  background-color: #ff4d4f;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.search-results-section .search-result-item .add-friend-btn:hover:not(:disabled) {
  background-color: #ff7875;
}

.search-results-section .search-result-item .add-friend-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.no-results {
  margin-top: 24px;
  text-align: center;
}

/* 主容器 */
.wechat-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧导航栏 */
.wechat-sidebar {
  width: 60px;
  background-color: white;
  border-right: 1px solid var(--wechat-gray);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  padding: 20px 0;
  gap: 40px;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
  color: var(--wechat-gray-dark);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  font-size: 12px;
}

.nav-item:hover {
  background-color: var(--wechat-green-light);
  color: var(--wechat-green);
}

.nav-item.active {
  background-color: var(--wechat-green-light);
  color: var(--wechat-green);
}

/* 聊天列表 */
.chat-list-container {
  flex: 1;
  overflow-y: auto;
  background-color: white;
}

.chat-list {
  padding: 8px 0;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid var(--wechat-gray);
}

.chat-item:hover {
  background-color: var(--wechat-gray);
}

.chat-item.active {
  background-color: var(--wechat-green-light);
}

.chat-avatar {
  position: relative;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
}

.avatar-placeholder {
  color: var(--wechat-gray-dark);
}

.unread-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background-color: var(--wechat-red);
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 12px;
  min-width: 18px;
  text-align: center;
}

.chat-content {
  flex: 1;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.friend-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--wechat-gray-darker);
}

.chat-time {
  font-size: 12px;
  color: var(--wechat-gray-dark);
}

.chat-preview {
  display: flex;
  align-items: center;
}

.last-message {
  font-size: 14px;
  color: var(--wechat-gray-dark);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 通讯录 */
.contacts-container {
  flex: 1;
  overflow-y: auto;
  background-color: white;
}

.contacts-header {
  padding: 16px;
  border-bottom: 1px solid var(--wechat-gray);
}

.contacts-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: var(--wechat-gray-darker);
}

.contacts-list {
  padding: 16px;
}

.contact-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--wechat-gray-dark);
  margin-bottom: 16px;
  padding-left: 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 8px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.contact-item:hover {
  background-color: var(--wechat-gray);
}

.contact-name {
  font-size: 16px;
  color: var(--wechat-gray-darker);
}

/* 发现页 */
.discover-container {
  flex: 1;
  overflow-y: auto;
  background-color: white;
}

.discover-header {
  padding: 16px;
  border-bottom: 1px solid var(--wechat-gray);
}

.discover-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: var(--wechat-gray-darker);
}

.discover-menu {
  padding: 16px;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.menu-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.menu-item span:nth-child(2) {
  font-size: 16px;
  color: var(--wechat-gray-darker);
  flex: 1;
  margin-left: 16px;
}

/* 聊天窗口 */
.chat-window {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #e5ddd5;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  z-index: 200;
}

.chat-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--wechat-green);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.back-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.chat-friend-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  justify-content: center;
}

.small-avatar {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  object-fit: cover;
}

.small-avatar-placeholder {
  color: white;
}

.chat-actions {
  display: flex;
  gap: 16px;
}

/* 消息容器 */
.message-container {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 16px;
  gap: 12px;
}

.message-item.sent {
  justify-content: flex-end;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  align-self: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 18px;
  position: relative;
}

.message-item.received .message-bubble {
  background-color: white;
  border-bottom-left-radius: 4px;
}

.message-item.sent .message-bubble {
  background-color: var(--wechat-green);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-content {
  margin: 0 0 6px 0;
  font-size: 15px;
  line-height: 1.4;
  word-break: break-all;
}

.message-image {
  max-width: 250px;
  max-height: 250px;
  border-radius: 8px;
  margin-bottom: 6px;
  object-fit: contain;
}

.message-time {
  font-size: 12px;
  color: var(--wechat-gray-dark);
  display: block;
  text-align: right;
}

.message-item.sent .message-time {
  color: rgba(255, 255, 255, 0.7);
}

/* 表情选择器 */
.emoji-picker {
  background-color: white;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

.emoji-btn {
  background: transparent;
  border: none;
  font-size: 24px;
  padding: 8px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-btn:hover {
  background-color: var(--wechat-gray);
}

/* 消息输入区域 */
.message-input-area {
  padding: 12px 16px;
  background-color: white;
  border-top: 1px solid var(--wechat-gray);
}

.input-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.toolbar-btn {
  background: transparent;
  border: none;
  color: var(--wechat-gray-dark);
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background-color: var(--wechat-gray);
  color: var(--wechat-green);
}

.message-form {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--wechat-gray);
  border-radius: 24px;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.4;
  min-height: 44px;
  max-height: 120px;
}

.message-input:focus {
  border-color: var(--wechat-green);
}

.send-btn {
  background-color: #ff4d4f;
  color: white;
  border: none;
  padding: 12px 16px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.send-btn:hover:not(:disabled) {
  background-color: #ff7875;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 好友申请弹窗 */
.requests-modal {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: white;
  z-index: 300;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--wechat-green);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.requests-list {
  padding: 16px;
  max-height: calc(100vh - 60px);
  overflow-y: auto;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.request-info {
  flex: 1;
}

.requester-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--wechat-gray-darker);
  margin-bottom: 8px;
}

.request-message {
  font-size: 14px;
  color: var(--wechat-gray-dark);
  line-height: 1.4;
}

.request-actions {
  display: flex;
  gap: 12px;
}

.accept-btn {
  background-color: var(--wechat-green);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.accept-btn:hover:not(:disabled) {
  background-color: #06ad56;
}

.reject-btn {
  background-color: white;
  color: var(--wechat-gray-dark);
  border: 1px solid var(--wechat-gray);
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.reject-btn:hover:not(:disabled) {
  border-color: var(--wechat-gray-dark);
  color: var(--wechat-gray-darker);
}

.accept-btn:disabled, .reject-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 登录提示 */
.login-prompt {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 24px;
  background-color: white;
  padding: 40px;
  text-align: center;
  z-index: 50;
}

.login-prompt h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--wechat-gray-darker);
}

.login-prompt p {
  font-size: 16px;
  color: var(--wechat-gray-dark);
  margin: 0;
  max-width: 320px;
  line-height: 1.6;
}

.login-btn {
  background-color: var(--wechat-green);
  color: white;
  padding: 12px 32px;
  border-radius: 24px;
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.login-btn:hover {
  background-color: #06ad56;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: var(--wechat-gray);
}

::-webkit-scrollbar-thumb {
  background: var(--wechat-gray-dark);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--wechat-green);
}
</style>
