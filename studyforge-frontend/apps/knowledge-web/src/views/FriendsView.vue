<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import {
  ArrowLeft,
  Check,
  ChevronLeft,
  ChevronRight,
  Image,
  Inbox,
  MessageCircle,
  Mic,
  Plus,
  RefreshCw,
  Send,
  Smile,
  Trash2,
  UserRound,
  X
} from '@lucide/vue';
import { uploadImage } from '@/api/uploads';
import {
  addSticker,
  deleteSticker,
  getFriendMessages,
  getIncomingFriendRequests,
  getMyFriends,
  getMyStickers,
  getOutgoingFriendRequests,
  reorderStickers,
  reviewFriendRequest,
  sendFriendMessage
} from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { FriendMessage, FriendRequest, SocialUser, Sticker } from '@/types/api';
import { formatShortDateTime } from '@/utils/date';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const friends = ref<SocialUser[]>([]);
const incoming = ref<FriendRequest[]>([]);
const outgoing = ref<FriendRequest[]>([]);
const messages = ref<FriendMessage[]>([]);
const stickers = ref<Sticker[]>([]);
const activeFriend = ref<SocialUser | null>(null);
const draftMessage = ref('');
const activePanel = ref<'messages' | 'requests'>('messages');
const emojiPanelOpen = ref(false);
const plusMenuOpen = ref(false);
const stickerPanelTab = ref<'emoji' | 'stickers'>('emoji');
const contextMenu = ref<{ imageUrl: string; x: number; y: number } | null>(null);
const loading = ref(false);
const actionLoading = ref('');
const errorMessage = ref('');
const successMessage = ref('');

const stickerUploadInput = ref<HTMLInputElement | null>(null);
const imageUploadInput = ref<HTMLInputElement | null>(null);
const composeRef = ref<HTMLElement | null>(null);

const emojiList = [
  '😀', '😁', '😂', '🤣', '😊', '😍', '🥰', '😘', '😎', '🤔',
  '😅', '😭', '😡', '👍', '👎', '👏', '🙏', '💪', '🔥', '✨',
  '❤️', '💯', '🎉', '🙌', '😴', '🤗', '😇', '🥳', '🤩', '😬'
];

const pendingIncoming = computed(() => incoming.value.filter((item) => item.status === 'PENDING'));
const pendingOutgoing = computed(() => outgoing.value.filter((item) => item.status === 'PENDING'));

const copy = computed(() => {
  if (preferencesStore.languageCode === 'en_US') {
    return {
      loginTitle: 'Sign in to use friends',
      loginDesc: 'Friend requests and direct messages require an account.',
      login: 'Log in',
      back: 'Back to profile',
      kicker: 'Friends',
      title: 'Friends',
      refresh: 'Refresh',
      loading: 'Loading friends and messages',
      loadErrorFallback: 'Friend data could not be loaded.',
      messagesError: 'Messages could not be loaded.',
      acceptSuccess: 'Friend request accepted.',
      rejectSuccess: 'Friend request rejected.',
      requestFailed: 'Friend request could not be processed.',
      messageFailed: 'Message could not be sent.',
      messages: 'Messages',
      requests: (count: number) => `Requests ${count}`,
      noFriendsTitle: 'No friends yet',
      noFriendsDesc: 'After a friend request is accepted, they will appear here.',
      defaultRequestMessage: 'Would like to add you as a friend.',
      accept: 'Accept',
      reject: 'Reject',
      sent: 'Sent',
      pending: 'Pending',
      noOutgoing: 'No outgoing requests',
      waitingOutgoing: (message: string) => `Waiting for approval: ${message}`,
      sentRequestDefault: 'Friend request sent.',
      noPendingTitle: 'No pending requests',
      noPendingDesc: 'New friend requests will appear here.',
      profileLink: 'Profile',
      noMessagesTitle: 'No messages yet',
      noMessagesDesc: 'Send the first message to start chatting.',
      pickFriendTitle: 'Choose a friend',
      pickFriendDesc: 'After a request is accepted, you can message them here.',
      pickFriend: 'Pick a friend to start chatting',
      messagePlaceholder: 'Write a message',
      send: 'Send',
      emojiTitle: 'Emoji',
      moreTitle: 'More',
      emojiTab: 'Emoji',
      stickersTab: 'Stickers',
      upload: 'Upload',
      moveLeft: 'Move left',
      moveRight: 'Move right',
      deleteSticker: 'Delete',
      voiceInput: 'Voice input',
      inDevelopment: 'Coming soon',
      image: 'Image',
      stickerLoadFailed: 'Stickers could not be loaded.',
      stickerAdded: 'Sticker added.',
      stickerUploadFailed: 'Sticker upload failed.',
      imageUploadFailed: 'Image could not be sent.',
      stickerDeleteFailed: 'Sticker could not be deleted.',
      stickerReorderFailed: 'Sticker order could not be updated.',
      stickerSaved: 'Saved as sticker.',
      stickerSaveFailed: 'Sticker could not be saved.',
      saveImageAsSticker: 'Save as sticker'
    };
  }

  return {
    loginTitle: '登录后使用好友',
    loginDesc: '好友申请和私信需要登录后使用。',
    login: '登录',
    back: '返回主页',
    kicker: 'Friends',
    title: '好友',
    refresh: '刷新',
    loading: '正在读取好友和消息',
    loadErrorFallback: '好友数据暂时没取到',
    messagesError: '消息暂时没取到',
    acceptSuccess: '已通过好友申请',
    rejectSuccess: '已拒绝好友申请',
    requestFailed: '好友申请处理失败',
    messageFailed: '消息没有发送成功',
    messages: '消息',
    requests: (count: number) => `申请 ${count}`,
    noFriendsTitle: '还没有好友',
    noFriendsDesc: '在用户主页发送好友申请，通过后会出现在这里。',
    defaultRequestMessage: '想添加你为好友。',
    accept: '通过',
    reject: '拒绝',
    sent: '已发送',
    pending: '待处理',
    noOutgoing: '没有发出的申请',
    waitingOutgoing: (message: string) => `等待对方通过：${message}`,
    sentRequestDefault: '已发送好友申请。',
    noPendingTitle: '没有待处理申请',
    noPendingDesc: '新的好友申请会出现在这里。',
    profileLink: '主页',
    noMessagesTitle: '还没有消息',
    noMessagesDesc: '发送第一条消息开始交流。',
    pickFriendTitle: '选择一位好友',
    pickFriendDesc: '通过好友申请后，就可以在这里发送消息。',
    pickFriend: '选择一个好友开始聊天',
    messagePlaceholder: '写一条消息',
    send: '发送',
    emojiTitle: '表情',
    moreTitle: '更多',
    emojiTab: 'Emoji',
    stickersTab: '表情包',
    upload: '上传',
    moveLeft: '左移',
    moveRight: '右移',
    deleteSticker: '删除',
    voiceInput: '语音输入',
    inDevelopment: '开发中',
    image: '图片',
    stickerLoadFailed: '表情包暂时没取到',
    stickerAdded: '表情包已添加',
    stickerUploadFailed: '表情包上传失败',
    imageUploadFailed: '图片发送失败',
    stickerDeleteFailed: '表情包删除失败',
    stickerReorderFailed: '表情包排序失败',
    stickerSaved: '已添加为表情包',
    stickerSaveFailed: '添加表情包失败',
    saveImageAsSticker: '添加为表情包'
  };
});

function closePanels() {
  emojiPanelOpen.value = false;
  plusMenuOpen.value = false;
  contextMenu.value = null;
}

function toggleEmojiPanel() {
  plusMenuOpen.value = false;
  emojiPanelOpen.value = !emojiPanelOpen.value;
}

function togglePlusMenu() {
  emojiPanelOpen.value = false;
  plusMenuOpen.value = !plusMenuOpen.value;
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node | null;
  if (composeRef.value && target && !composeRef.value.contains(target)) {
    closePanels();
  }
}

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
    friends.value = friendData;
    incoming.value = incomingData;
    outgoing.value = outgoingData;
    if (!activeFriend.value && friendData.length) {
      await openChat(friendData[0]);
    } else if (activeFriend.value) {
      const stillFriend = friendData.find((friend) => friend.userId === activeFriend.value?.userId);
      activeFriend.value = stillFriend ?? null;
      if (stillFriend) {
        await loadMessages(stillFriend.userId);
      } else {
        messages.value = [];
      }
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadErrorFallback;
  } finally {
    loading.value = false;
  }
}

async function loadStickers() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  try {
    stickers.value = await getMyStickers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.stickerLoadFailed;
  }
}

async function openChat(friend: SocialUser) {
  activePanel.value = 'messages';
  activeFriend.value = friend;
  closePanels();
  await Promise.all([loadMessages(friend.userId), loadStickers()]);
}

async function loadMessages(friendId: number) {
  try {
    messages.value = await getFriendMessages(friendId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.messagesError;
  }
}

async function handleRequest(request: FriendRequest, decision: 'ACCEPT' | 'REJECT') {
  actionLoading.value = `${decision}-${request.requestId}`;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    await reviewFriendRequest(request.requestId, decision);
    successMessage.value = decision === 'ACCEPT' ? copy.value.acceptSuccess : copy.value.rejectSuccess;
    await loadFriends();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.requestFailed;
  } finally {
    actionLoading.value = '';
  }
}

async function sendMessage(messageType: 'TEXT' | 'IMAGE' | 'STICKER' = 'TEXT', content = draftMessage.value.trim()) {
  if (!activeFriend.value || !content) {
    return;
  }

  actionLoading.value = 'message';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const message = await sendFriendMessage(activeFriend.value.userId, content, messageType);
    messages.value = [...messages.value, message];
    if (messageType === 'TEXT') {
      draftMessage.value = '';
    }
    closePanels();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.messageFailed;
  } finally {
    actionLoading.value = '';
  }
}

function insertEmoji(emoji: string) {
  draftMessage.value += emoji;
}

async function sendStickerMessage(imageUrl: string) {
  await sendMessage('STICKER', imageUrl);
}

async function sendImageMessage(imageUrl: string) {
  await sendMessage('IMAGE', imageUrl);
}

async function handleStickerUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }

  actionLoading.value = 'sticker-upload';
  errorMessage.value = '';
  try {
    const uploaded = await uploadImage(file);
    await addSticker(uploaded.url);
    stickers.value = await getMyStickers();
    successMessage.value = copy.value.stickerAdded;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.stickerUploadFailed;
  } finally {
    actionLoading.value = '';
  }
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }

  actionLoading.value = 'image-upload';
  errorMessage.value = '';
  try {
    const uploaded = await uploadImage(file);
    await sendImageMessage(uploaded.url);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.imageUploadFailed;
  } finally {
    actionLoading.value = '';
  }
}

async function removeSticker(stickerId: number) {
  actionLoading.value = `sticker-delete-${stickerId}`;
  errorMessage.value = '';
  try {
    await deleteSticker(stickerId);
    stickers.value = await getMyStickers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.stickerDeleteFailed;
  } finally {
    actionLoading.value = '';
  }
}

async function moveSticker(index: number, direction: -1 | 1) {
  const nextIndex = index + direction;
  if (nextIndex < 0 || nextIndex >= stickers.value.length) {
    return;
  }
  const next = [...stickers.value];
  const [item] = next.splice(index, 1);
  next.splice(nextIndex, 0, item);
  actionLoading.value = 'sticker-reorder';
  errorMessage.value = '';
  try {
    stickers.value = await reorderStickers(next.map((sticker) => sticker.stickerId));
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.stickerReorderFailed;
  } finally {
    actionLoading.value = '';
  }
}

function openImageContextMenu(event: MouseEvent, message: FriendMessage) {
  if (message.senderId === sessionStore.userId || message.messageType !== 'IMAGE') {
    return;
  }
  contextMenu.value = {
    imageUrl: message.content,
    x: event.clientX,
    y: event.clientY
  };
}

async function saveImageAsSticker() {
  if (!contextMenu.value) {
    return;
  }
  actionLoading.value = 'sticker-save';
  errorMessage.value = '';
  try {
    await addSticker(contextMenu.value.imageUrl);
    stickers.value = await getMyStickers();
    successMessage.value = copy.value.stickerSaved;
    contextMenu.value = null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.stickerSaveFailed;
  } finally {
    actionLoading.value = '';
  }
}

function isMediaMessage(message: FriendMessage) {
  return message.messageType === 'IMAGE' || message.messageType === 'STICKER';
}

function formatDate(value: unknown) {
  return formatShortDateTime(value, preferencesStore.languageCode);
}

onMounted(() => {
  loadFriends();
  document.addEventListener('click', handleDocumentClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick);
});
</script>

<template>
  <section class="friends-page">
    <div v-if="!sessionStore.isAuthenticated" class="login-required">
      <UserRound :size="42" />
      <h2>{{ copy.loginTitle }}</h2>
      <p>{{ copy.loginDesc }}</p>
      <RouterLink class="primary-button" to="/login">{{ copy.login }}</RouterLink>
    </div>

    <template v-else>
      <div class="page-heading with-actions">
        <div>
          <RouterLink class="secondary-button return-link" to="/me">
            <ArrowLeft :size="17" />
            <span>{{ copy.back }}</span>
          </RouterLink>
          <span>{{ copy.kicker }}</span>
          <h1>{{ copy.title }}</h1>
        </div>
        <button class="secondary-button" type="button" :disabled="loading" @click="loadFriends">
          <RefreshCw :size="17" />
          <span>{{ copy.refresh }}</span>
        </button>
      </div>

      <LoadingState v-if="loading" :label="copy.loading" />
      <p v-else-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

      <div class="friend-workspace">
        <aside class="friend-sidebar">
          <div class="friend-tabs">
            <button type="button" :class="{ active: activePanel === 'messages' }" @click="activePanel = 'messages'">
              <MessageCircle :size="17" />
              <span>{{ copy.messages }}</span>
            </button>
            <button type="button" :class="{ active: activePanel === 'requests' }" @click="activePanel = 'requests'">
              <Inbox :size="17" />
              <span>{{ copy.requests(pendingIncoming.length) }}</span>
            </button>
          </div>

          <div v-if="activePanel === 'messages'" class="friend-list">
            <button
              v-for="friend in friends"
              :key="friend.userId"
              type="button"
              :class="{ active: activeFriend?.userId === friend.userId }"
              @click="openChat(friend)"
            >
              <img v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="" />
              <UserRound v-else :size="20" />
              <span>
                <strong>{{ friend.displayName }}</strong>
                <small>@{{ friend.username }} · Lv.{{ friend.communityLevel }}</small>
              </span>
            </button>
            <EmptyState v-if="friends.length === 0" :title="copy.noFriendsTitle" :description="copy.noFriendsDesc" />
          </div>

          <div v-else class="request-list compact">
            <article v-for="request in pendingIncoming" :key="request.requestId" class="request-card">
              <div class="request-user">
                <img v-if="request.requester.avatarUrl" :src="request.requester.avatarUrl" alt="" />
                <UserRound v-else :size="20" />
                <div>
                  <strong>{{ request.requester.displayName }}</strong>
                  <span>@{{ request.requester.username }}</span>
                </div>
              </div>
              <p>{{ request.message || copy.defaultRequestMessage }}</p>
              <div class="request-actions">
                <button class="primary-button" type="button" :disabled="actionLoading === `ACCEPT-${request.requestId}`" @click="handleRequest(request, 'ACCEPT')">
                  <Check :size="16" />
                  <span>{{ copy.accept }}</span>
                </button>
                <button class="secondary-button" type="button" :disabled="actionLoading === `REJECT-${request.requestId}`" @click="handleRequest(request, 'REJECT')">
                  <X :size="16" />
                  <span>{{ copy.reject }}</span>
                </button>
              </div>
            </article>

            <article v-for="request in pendingOutgoing" :key="`out-${request.requestId}`" class="request-card muted">
              <div class="request-user">
                <img v-if="request.addressee.avatarUrl" :src="request.addressee.avatarUrl" alt="" />
                <UserRound v-else :size="20" />
                <div>
                  <strong>{{ request.addressee.displayName }}</strong>
                  <span>@{{ request.addressee.username }}</span>
                </div>
              </div>
              <p>{{ copy.waitingOutgoing(request.message || copy.sentRequestDefault) }}</p>
            </article>

            <EmptyState v-if="pendingIncoming.length === 0 && pendingOutgoing.length === 0" :title="copy.noPendingTitle" :description="copy.noPendingDesc" />
          </div>
        </aside>

        <section class="chat-panel">
          <template v-if="activeFriend">
            <header class="chat-header">
              <div class="request-user">
                <img v-if="activeFriend.avatarUrl" :src="activeFriend.avatarUrl" alt="" />
                <UserRound v-else :size="22" />
                <div>
                  <strong>{{ activeFriend.displayName }}</strong>
                  <span>@{{ activeFriend.username }}</span>
                </div>
              </div>
              <RouterLink class="secondary-button" :to="`/users/${activeFriend.userId}`">{{ copy.profileLink }}</RouterLink>
            </header>

            <div class="message-list">
              <article
                v-for="message in messages"
                :key="message.messageId"
                class="message-bubble"
                :class="{ mine: message.senderId === sessionStore.userId, media: isMediaMessage(message) }"
              >
                <img
                  v-if="isMediaMessage(message)"
                  class="message-media"
                  :class="{ sticker: message.messageType === 'STICKER' }"
                  :src="message.content"
                  alt=""
                  @contextmenu.prevent="openImageContextMenu($event, message)"
                />
                <p v-else>{{ message.content }}</p>
                <span>{{ formatDate(message.createdTime) }}</span>
              </article>
              <EmptyState v-if="messages.length === 0" :title="copy.noMessagesTitle" :description="copy.noMessagesDesc" />
            </div>

            <form ref="composeRef" class="message-compose" @submit.prevent="sendMessage()">
              <div class="compose-main">
                <div class="compose-toolbar">
                  <button class="compose-icon-button" type="button" :title="copy.emojiTitle" @click.stop="toggleEmojiPanel">
                    <Smile :size="18" />
                  </button>
                  <button class="compose-icon-button" type="button" :title="copy.moreTitle" @click.stop="togglePlusMenu">
                    <Plus :size="18" />
                  </button>
                </div>

                <div v-if="emojiPanelOpen" class="compose-popover emoji-popover" @click.stop>
                  <div class="popover-tabs">
                    <button type="button" :class="{ active: stickerPanelTab === 'emoji' }" @click="stickerPanelTab = 'emoji'">{{ copy.emojiTab }}</button>
                    <button type="button" :class="{ active: stickerPanelTab === 'stickers' }" @click="stickerPanelTab = 'stickers'">{{ copy.stickersTab }}</button>
                  </div>

                  <div v-if="stickerPanelTab === 'emoji'" class="emoji-grid">
                    <button v-for="emoji in emojiList" :key="emoji" type="button" @click="insertEmoji(emoji)">{{ emoji }}</button>
                  </div>

                  <div v-else class="sticker-panel">
                    <div class="sticker-panel-header">
                      <button class="secondary-button compact" type="button" :disabled="actionLoading === 'sticker-upload'" @click="stickerUploadInput?.click()">
                        <Plus :size="15" />
                        <span>{{ copy.upload }}</span>
                      </button>
                    </div>
                    <div class="sticker-grid">
                      <div v-for="(sticker, index) in stickers" :key="sticker.stickerId" class="sticker-item">
                        <button type="button" class="sticker-send" @click="sendStickerMessage(sticker.imageUrl)">
                          <img :src="sticker.imageUrl" alt="" />
                        </button>
                        <div class="sticker-actions">
                          <button type="button" :title="copy.moveLeft" :disabled="index === 0 || actionLoading === 'sticker-reorder'" @click="moveSticker(index, -1)">
                            <ChevronLeft :size="14" />
                          </button>
                          <button type="button" :title="copy.moveRight" :disabled="index === stickers.length - 1 || actionLoading === 'sticker-reorder'" @click="moveSticker(index, 1)">
                            <ChevronRight :size="14" />
                          </button>
                          <button type="button" :title="copy.deleteSticker" :disabled="actionLoading === `sticker-delete-${sticker.stickerId}`" @click="removeSticker(sticker.stickerId)">
                            <Trash2 :size="14" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="plusMenuOpen" class="compose-popover plus-popover" @click.stop>
                  <button type="button" class="plus-menu-item" @click="successMessage = `${copy.voiceInput}${copy.inDevelopment}`; plusMenuOpen = false">
                    <Mic :size="16" />
                    <span>{{ copy.voiceInput }}</span>
                    <small>{{ copy.inDevelopment }}</small>
                  </button>
                  <button type="button" class="plus-menu-item" :disabled="actionLoading === 'image-upload'" @click="imageUploadInput?.click()">
                    <Image :size="16" />
                    <span>{{ copy.image }}</span>
                  </button>
                </div>

                <textarea v-model.trim="draftMessage" rows="3" maxlength="2000" :placeholder="copy.messagePlaceholder" />
              </div>
              <button class="primary-button" type="submit" :disabled="actionLoading === 'message'">
                <Send :size="17" />
                <span>{{ copy.send }}</span>
              </button>

              <input ref="stickerUploadInput" class="hidden-file-input" type="file" accept="image/*" @change="handleStickerUpload" />
              <input ref="imageUploadInput" class="hidden-file-input" type="file" accept="image/*" @change="handleImageUpload" />
            </form>
          </template>

          <EmptyState v-else :title="copy.pickFriendTitle" :description="copy.pickFriendDesc" />
        </section>
      </div>

      <div
        v-if="contextMenu"
        class="message-context-menu"
        :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px` }"
        @click.stop
      >
        <button type="button" :disabled="actionLoading === 'sticker-save'" @click="saveImageAsSticker">{{ copy.saveImageAsSticker }}</button>
      </div>
    </template>
  </section>
</template>
