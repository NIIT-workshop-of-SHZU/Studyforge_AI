<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowLeft, Check, Inbox, MessageCircle, RefreshCw, Send, UserRound, X } from '@lucide/vue';
import {
  getFriendMessages,
  getIncomingFriendRequests,
  getMyFriends,
  getOutgoingFriendRequests,
  reviewFriendRequest,
  sendFriendMessage
} from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { FriendMessage, FriendRequest, SocialUser } from '@/types/api';
import { formatShortDateTime } from '@/utils/date';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const friends = ref<SocialUser[]>([]);
const incoming = ref<FriendRequest[]>([]);
const outgoing = ref<FriendRequest[]>([]);
const messages = ref<FriendMessage[]>([]);
const activeFriend = ref<SocialUser | null>(null);
const draftMessage = ref('');
const activePanel = ref<'messages' | 'requests'>('messages');
const loading = ref(false);
const actionLoading = ref('');
const errorMessage = ref('');
const successMessage = ref('');

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
      send: 'Send'
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
    send: '发送'
  };
});

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

async function openChat(friend: SocialUser) {
  activePanel.value = 'messages';
  activeFriend.value = friend;
  await loadMessages(friend.userId);
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
    errorMessage.value = error instanceof Error ? error.message : copy.value.messageFailed;
  } finally {
    actionLoading.value = '';
  }
}

function formatDate(value: unknown) {
  return formatShortDateTime(value, preferencesStore.languageCode);
}

onMounted(loadFriends);
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
              <article v-for="message in messages" :key="message.messageId" class="message-bubble" :class="{ mine: message.senderId === sessionStore.userId }">
                <p>{{ message.content }}</p>
                <span>{{ formatDate(message.createdTime) }}</span>
              </article>
              <EmptyState v-if="messages.length === 0" :title="copy.noMessagesTitle" :description="copy.noMessagesDesc" />
            </div>

            <form class="message-compose" @submit.prevent="sendMessage">
              <textarea v-model.trim="draftMessage" rows="3" maxlength="2000" :placeholder="copy.messagePlaceholder" />
              <button class="primary-button" type="submit" :disabled="actionLoading === 'message'">
                <Send :size="17" />
                <span>{{ copy.send }}</span>
              </button>
            </form>
          </template>

          <EmptyState v-else :title="copy.pickFriendTitle" :description="copy.pickFriendDesc" />
        </section>
      </div>
    </template>
  </section>
</template>
