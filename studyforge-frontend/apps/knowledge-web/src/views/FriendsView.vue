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
    errorMessage.value = error instanceof Error ? error.message : '好友数据暂时没取到';
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
    errorMessage.value = error instanceof Error ? error.message : '表情包暂时没取到';
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
    errorMessage.value = error instanceof Error ? error.message : '消息暂时没取到';
  }
}

async function handleRequest(request: FriendRequest, decision: 'ACCEPT' | 'REJECT') {
  actionLoading.value = `${decision}-${request.requestId}`;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    await reviewFriendRequest(request.requestId, decision);
    successMessage.value = decision === 'ACCEPT' ? '已通过好友申请' : '已拒绝好友申请';
    await loadFriends();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '好友申请处理失败';
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
    errorMessage.value = error instanceof Error ? error.message : '消息没有发送成功';
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
    successMessage.value = '表情包已添加';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表情包上传失败';
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
    errorMessage.value = error instanceof Error ? error.message : '图片发送失败';
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
    errorMessage.value = error instanceof Error ? error.message : '表情包删除失败';
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
    errorMessage.value = error instanceof Error ? error.message : '表情包排序失败';
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
    successMessage.value = '已添加为表情包';
    contextMenu.value = null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '添加表情包失败';
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
      <h2>登录后使用好友</h2>
      <p>好友申请和私信需要登录后使用。</p>
      <RouterLink class="primary-button" to="/login">登录</RouterLink>
    </div>

    <template v-else>
      <div class="page-heading with-actions">
        <div>
          <RouterLink class="secondary-button return-link" to="/me">
            <ArrowLeft :size="17" />
            <span>返回主页</span>
          </RouterLink>
          <span>Friends</span>
          <h1>好友</h1>
        </div>
        <button class="secondary-button" type="button" :disabled="loading" @click="loadFriends">
          <RefreshCw :size="17" />
          <span>刷新</span>
        </button>
      </div>

      <LoadingState v-if="loading" label="正在读取好友和消息" />
      <p v-else-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

      <div class="friend-workspace">
        <aside class="friend-sidebar">
          <div class="friend-tabs">
            <button type="button" :class="{ active: activePanel === 'messages' }" @click="activePanel = 'messages'">
              <MessageCircle :size="17" />
              <span>消息</span>
            </button>
            <button type="button" :class="{ active: activePanel === 'requests' }" @click="activePanel = 'requests'">
              <Inbox :size="17" />
              <span>申请 {{ pendingIncoming.length }}</span>
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
            <EmptyState v-if="friends.length === 0" title="还没有好友" description="在用户主页发送好友申请，通过后会出现在这里。" />
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
              <p>{{ request.message || '想添加你为好友。' }}</p>
              <div class="request-actions">
                <button class="primary-button" type="button" :disabled="actionLoading === `ACCEPT-${request.requestId}`" @click="handleRequest(request, 'ACCEPT')">
                  <Check :size="16" />
                  <span>通过</span>
                </button>
                <button class="secondary-button" type="button" :disabled="actionLoading === `REJECT-${request.requestId}`" @click="handleRequest(request, 'REJECT')">
                  <X :size="16" />
                  <span>拒绝</span>
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
              <p>等待对方通过：{{ request.message || '已发送好友申请。' }}</p>
            </article>

            <EmptyState v-if="pendingIncoming.length === 0 && pendingOutgoing.length === 0" title="没有待处理申请" description="新的好友申请会出现在这里。" />
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
              <RouterLink class="secondary-button" :to="`/users/${activeFriend.userId}`">主页</RouterLink>
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
              <EmptyState v-if="messages.length === 0" title="还没有消息" description="发送第一条消息开始交流。" />
            </div>

            <form ref="composeRef" class="message-compose" @submit.prevent="sendMessage()">
              <div class="compose-main">
                <div class="compose-toolbar">
                  <button class="compose-icon-button" type="button" title="表情" @click.stop="toggleEmojiPanel">
                    <Smile :size="18" />
                  </button>
                  <button class="compose-icon-button" type="button" title="更多" @click.stop="togglePlusMenu">
                    <Plus :size="18" />
                  </button>
                </div>

                <div v-if="emojiPanelOpen" class="compose-popover emoji-popover" @click.stop>
                  <div class="popover-tabs">
                    <button type="button" :class="{ active: stickerPanelTab === 'emoji' }" @click="stickerPanelTab = 'emoji'">Emoji</button>
                    <button type="button" :class="{ active: stickerPanelTab === 'stickers' }" @click="stickerPanelTab = 'stickers'">表情包</button>
                  </div>

                  <div v-if="stickerPanelTab === 'emoji'" class="emoji-grid">
                    <button v-for="emoji in emojiList" :key="emoji" type="button" @click="insertEmoji(emoji)">{{ emoji }}</button>
                  </div>

                  <div v-else class="sticker-panel">
                    <div class="sticker-panel-header">
                      <button class="secondary-button compact" type="button" :disabled="actionLoading === 'sticker-upload'" @click="stickerUploadInput?.click()">
                        <Plus :size="15" />
                        <span>上传</span>
                      </button>
                    </div>
                    <div class="sticker-grid">
                      <div v-for="(sticker, index) in stickers" :key="sticker.stickerId" class="sticker-item">
                        <button type="button" class="sticker-send" @click="sendStickerMessage(sticker.imageUrl)">
                          <img :src="sticker.imageUrl" alt="" />
                        </button>
                        <div class="sticker-actions">
                          <button type="button" title="左移" :disabled="index === 0 || actionLoading === 'sticker-reorder'" @click="moveSticker(index, -1)">
                            <ChevronLeft :size="14" />
                          </button>
                          <button type="button" title="右移" :disabled="index === stickers.length - 1 || actionLoading === 'sticker-reorder'" @click="moveSticker(index, 1)">
                            <ChevronRight :size="14" />
                          </button>
                          <button type="button" title="删除" :disabled="actionLoading === `sticker-delete-${sticker.stickerId}`" @click="removeSticker(sticker.stickerId)">
                            <Trash2 :size="14" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="plusMenuOpen" class="compose-popover plus-popover" @click.stop>
                  <button type="button" class="plus-menu-item" @click="successMessage = '语音输入开发中'; plusMenuOpen = false">
                    <Mic :size="16" />
                    <span>语音输入</span>
                    <small>开发中</small>
                  </button>
                  <button type="button" class="plus-menu-item" :disabled="actionLoading === 'image-upload'" @click="imageUploadInput?.click()">
                    <Image :size="16" />
                    <span>图片</span>
                  </button>
                </div>

                <textarea v-model.trim="draftMessage" rows="3" maxlength="2000" placeholder="写一条消息" />
              </div>
              <button class="primary-button" type="submit" :disabled="actionLoading === 'message'">
                <Send :size="17" />
                <span>发送</span>
              </button>

              <input ref="stickerUploadInput" class="hidden-file-input" type="file" accept="image/*" @change="handleStickerUpload" />
              <input ref="imageUploadInput" class="hidden-file-input" type="file" accept="image/*" @change="handleImageUpload" />
            </form>
          </template>

          <EmptyState v-else title="选择一位好友" description="通过好友申请后，就可以在这里发送消息。" />
        </section>
      </div>

      <div
        v-if="contextMenu"
        class="message-context-menu"
        :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px` }"
        @click.stop
      >
        <button type="button" :disabled="actionLoading === 'sticker-save'" @click="saveImageAsSticker">添加为表情包</button>
      </div>
    </template>
  </section>
</template>
