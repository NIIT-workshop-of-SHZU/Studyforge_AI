<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowLeft, Image, KeyRound, RefreshCw, Save, UserRound } from '@lucide/vue';
import { uploadImage } from '@/api/uploads';
import { getMyProfile, updateMyPassword, updateMyProfile } from '@/api/users';
import EmptyState from '@/components/EmptyState.vue';
import LoadingState from '@/components/LoadingState.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { UserProfile } from '@/types/api';

const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const profile = ref<UserProfile | null>(null);
const loading = ref(false);
const saving = ref('');
const errorMessage = ref('');
const successMessage = ref('');

const profileForm = reactive({
  username: '',
  email: '',
  displayName: '',
  bio: '',
  avatarUrl: '',
  bannerUrl: ''
});

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        loginTitle: 'Sign in to edit your account',
        loginDesc: 'Update your avatar, profile, account email, and password after signing in.',
        login: 'Log in',
        back: 'Back to profile',
        title: 'Account Settings',
        refresh: 'Refresh',
        loading: 'Loading account profile',
        unavailable: 'Account profile is unavailable',
        profilePanel: 'Profile Details',
        username: 'Username',
        email: 'Email',
        displayName: 'Display Name',
        bio: 'Bio',
        avatarUrl: 'Avatar URL',
        uploadAvatar: 'Upload Avatar',
        uploadingAvatar: 'Uploading Avatar',
        bannerUrl: 'Homepage Banner URL',
        uploadBanner: 'Upload Banner',
        uploadingBanner: 'Uploading Banner',
        saveProfile: 'Save Profile',
        savingProfile: 'Saving Profile',
        passwordPanel: 'Change Password',
        currentPassword: 'Current Password',
        newPassword: 'New Password',
        confirmPassword: 'Confirm New Password',
        savePassword: 'Update Password',
        savingPassword: 'Updating Password',
        loadFailed: 'Account profile could not be loaded right now.',
        saveFailed: 'Profile could not be saved.',
        saveSuccess: 'Profile saved.',
        passwordMismatch: 'The new passwords do not match.',
        passwordFailed: 'Password could not be updated.',
        passwordSuccess: 'Password updated.',
        avatarUploaded: 'Avatar uploaded. Remember to save your profile.',
        bannerUploaded: 'Homepage banner uploaded. Remember to save your profile.',
        uploadFailed: 'Image upload failed.'
      }
    : {
        loginTitle: '登录后编辑账号',
        loginDesc: '登录后可以修改头像、资料、账号邮箱和密码。',
        login: '登录',
        back: '返回主页',
        title: '账号设置',
        refresh: '刷新',
        loading: '正在读取账号资料',
        unavailable: '账号资料暂时打不开',
        profilePanel: '个人资料',
        username: '用户名',
        email: '邮箱',
        displayName: '显示名称',
        bio: '个人签名',
        avatarUrl: '头像地址',
        uploadAvatar: '上传头像',
        uploadingAvatar: '头像上传中',
        bannerUrl: '主页背景地址',
        uploadBanner: '上传主页背景',
        uploadingBanner: '背景上传中',
        saveProfile: '保存资料',
        savingProfile: '保存中',
        passwordPanel: '修改密码',
        currentPassword: '当前密码',
        newPassword: '新密码',
        confirmPassword: '确认新密码',
        savePassword: '更新密码',
        savingPassword: '更新中',
        loadFailed: '账号资料暂时没取到',
        saveFailed: '资料没有保存成功',
        saveSuccess: '资料已保存',
        passwordMismatch: '两次输入的新密码不一致',
        passwordFailed: '密码没有更新成功',
        passwordSuccess: '密码已更新',
        avatarUploaded: '头像已上传，记得保存资料',
        bannerUploaded: '主页背景已上传，记得保存资料',
        uploadFailed: '图片上传失败'
      }
);

async function loadProfile() {
  if (!sessionStore.isAuthenticated) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const data = await getMyProfile();
    profile.value = data;
    profileForm.username = data.username;
    profileForm.email = data.email;
    profileForm.displayName = data.displayName;
    profileForm.bio = data.bio;
    profileForm.avatarUrl = data.avatarUrl;
    profileForm.bannerUrl = data.bannerUrl;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadFailed;
  } finally {
    loading.value = false;
  }
}

async function saveProfile() {
  saving.value = 'profile';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const next = await updateMyProfile({ ...profileForm });
    profile.value = next;
    sessionStore.updateFromProfile(next);
    successMessage.value = copy.value.saveSuccess;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.saveFailed;
  } finally {
    saving.value = '';
  }
}

async function savePassword() {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errorMessage.value = copy.value.passwordMismatch;
    return;
  }

  saving.value = 'password';
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const next = await updateMyPassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    });
    sessionStore.updateFromProfile(next);
    passwordForm.currentPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    successMessage.value = copy.value.passwordSuccess;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.passwordFailed;
  } finally {
    saving.value = '';
  }
}

async function uploadProfileImage(event: Event, field: 'avatarUrl' | 'bannerUrl') {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  saving.value = field;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const uploaded = await uploadImage(file);
    profileForm[field] = uploaded.url;
    successMessage.value = field === 'avatarUrl' ? copy.value.avatarUploaded : copy.value.bannerUploaded;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.uploadFailed;
  } finally {
    saving.value = '';
    input.value = '';
  }
}

onMounted(loadProfile);
</script>

<template>
  <section class="account-page">
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
          <span>Account</span>
          <h1>{{ copy.title }}</h1>
        </div>
        <button class="secondary-button" type="button" :disabled="loading" @click="loadProfile">
          <RefreshCw :size="17" />
          <span>{{ copy.refresh }}</span>
        </button>
      </div>

      <LoadingState v-if="loading" :label="copy.loading" />
      <EmptyState v-else-if="errorMessage && !profile" :title="copy.unavailable" :description="errorMessage" />

      <div v-else class="account-layout">
        <section class="account-panel">
          <div class="panel-title">
            <UserRound :size="18" />
            <span>{{ copy.profilePanel }}</span>
          </div>

          <div class="profile-preview-strip" :style="{ '--profile-banner': profileForm.bannerUrl ? `url(${profileForm.bannerUrl})` : 'none' }">
            <div class="profile-avatar preview-avatar">
              <img v-if="profileForm.avatarUrl" :src="profileForm.avatarUrl" alt="" />
              <UserRound v-else :size="36" />
            </div>
            <div>
              <strong>{{ profileForm.displayName || profileForm.username }}</strong>
              <span>@{{ profileForm.username }}</span>
            </div>
          </div>

          <form class="account-form" @submit.prevent="saveProfile">
            <label>
              <span>{{ copy.username }}</span>
              <input v-model.trim="profileForm.username" type="text" autocomplete="username" maxlength="50" required />
            </label>
            <label>
              <span>{{ copy.email }}</span>
              <input v-model.trim="profileForm.email" type="email" autocomplete="email" maxlength="100" required />
            </label>
            <label>
              <span>{{ copy.displayName }}</span>
              <input v-model.trim="profileForm.displayName" type="text" maxlength="80" required />
            </label>
            <label>
              <span>{{ copy.bio }}</span>
              <textarea v-model.trim="profileForm.bio" rows="4" maxlength="300" />
            </label>
            <label>
              <span>{{ copy.avatarUrl }}</span>
              <input v-model.trim="profileForm.avatarUrl" type="text" maxlength="512" />
            </label>
            <label class="file-upload-line">
              <Image :size="17" />
              <span>{{ saving === 'avatarUrl' ? copy.uploadingAvatar : copy.uploadAvatar }}</span>
              <input type="file" accept="image/*" :disabled="saving === 'avatarUrl'" @change="uploadProfileImage($event, 'avatarUrl')" />
            </label>
            <label>
              <span>{{ copy.bannerUrl }}</span>
              <input v-model.trim="profileForm.bannerUrl" type="text" maxlength="512" />
            </label>
            <label class="file-upload-line">
              <Image :size="17" />
              <span>{{ saving === 'bannerUrl' ? copy.uploadingBanner : copy.uploadBanner }}</span>
              <input type="file" accept="image/*" :disabled="saving === 'bannerUrl'" @change="uploadProfileImage($event, 'bannerUrl')" />
            </label>
            <button class="primary-button" type="submit" :disabled="saving === 'profile'">
              <Save :size="17" />
              <span>{{ saving === 'profile' ? copy.savingProfile : copy.saveProfile }}</span>
            </button>
          </form>
        </section>

        <section class="account-panel">
          <div class="panel-title">
            <KeyRound :size="18" />
            <span>{{ copy.passwordPanel }}</span>
          </div>
          <form class="account-form" @submit.prevent="savePassword">
            <label>
              <span>{{ copy.currentPassword }}</span>
              <input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" required />
            </label>
            <label>
              <span>{{ copy.newPassword }}</span>
              <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" minlength="8" required />
            </label>
            <label>
              <span>{{ copy.confirmPassword }}</span>
              <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" minlength="8" required />
            </label>
            <button class="secondary-button" type="submit" :disabled="saving === 'password'">
              <KeyRound :size="17" />
              <span>{{ saving === 'password' ? copy.savingPassword : copy.savePassword }}</span>
            </button>
          </form>

          <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        </section>
      </div>
    </template>
  </section>
</template>
