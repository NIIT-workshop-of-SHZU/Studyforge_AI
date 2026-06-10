<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  Bold,
  Code2,
  Eye,
  FileText,
  Heading2,
  ImagePlus,
  Images,
  Languages,
  Link,
  ListChecks,
  ListOrdered,
  PenLine,
  Quote,
  Save,
  Send,
  SplitSquareHorizontal,
  Sparkles,
  Table2,
  Text,
  X
} from '@lucide/vue';
import { formatMarkdown as requestMarkdownFormat, generateCover } from '@/api/ai';
import { createPost, getPostDetail, updatePost } from '@/api/posts';
import { uploadImage } from '@/api/uploads';
import LoadingState from '@/components/LoadingState.vue';
import MentionTextarea from '@/components/MentionTextarea.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { topicCategories } from '@/i18n/categories';
import { languageLabel } from '@/i18n/labels';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';

type EditorMode = 'write' | 'preview' | 'split';
type MentionTextareaExpose = {
  focus: () => void;
  setSelectionRange: (start: number, end: number) => void;
  readonly selectionStart: number;
  readonly selectionEnd: number;
};

const DRAFT_KEY = 'studyforge.publish.markdown.draft';
const MAX_IMAGE_SIZE = 8 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const preferencesStore = usePreferencesStore();
const loading = ref(false);
const loadingPost = ref(false);
const uploading = ref(false);
const formatting = ref(false);
const generatingCover = ref(false);
const errorMessage = ref('');
const savedMessage = ref('');
const coverGeneratedMessage = ref('');
const mode = ref<EditorMode>('split');
const editorRef = ref<MentionTextareaExpose | null>(null);
const coverDragActive = ref(false);
const editorDragActive = ref(false);
const restoring = ref(false);

const form = reactive({
  title: '',
  summary: '',
  content: '',
  coverImageUrl: '',
  categoryId: 1,
  originalLanguage: preferencesStore.languageCode
});

const categories = computed(() =>
  topicCategories(preferencesStore.languageCode, false).map((category, index) => ({
    id: index + 1,
    label: category.name
  }))
);

const copy = computed(() =>
  preferencesStore.languageCode === 'en_US'
    ? {
        kicker: 'Markdown Post',
        pageTitleNew: 'Write a post worth revisiting',
        pageTitleEdit: 'Edit this post',
        pageDescNew:
          'Write in Markdown and use the toolbar for headings, links, images, code blocks, and tables. The preview shows how the post will look when published.',
        pageDescEdit:
          'Update the body, summary, cover, and topic. Views, saves, and discussion history stay intact.',
        loadingPost: 'Loading post',
        title: 'Title',
        titlePlaceholder: 'e.g. How to structure a Markdown study note',
        summary: 'Summary',
        summaryPlaceholder: 'In one or two sentences, explain what problem this post helps readers solve',
        removeCover: 'Remove cover',
        replaceCover: 'Replace cover',
        uploadCover: 'Upload cover',
        coverDropHint: 'Click to choose, or drag JPG, PNG, WebP, or GIF here',
        generatingCover: 'Generating',
        aiCover: 'AI cover',
        aiCoverHint: 'Generate a blog-style cover from the title, summary, and body',
        formatting: 'Formatting',
        aiFormat: 'AI format',
        toolbarLabel: 'Markdown toolbar',
        aiFormatTitle: 'AI format',
        headingTitle: 'Heading',
        boldTitle: 'Bold',
        quoteTitle: 'Quote',
        linkTitle: 'Link',
        codeTitle: 'Code block',
        taskListTitle: 'Task list',
        orderedListTitle: 'Ordered list',
        tableTitle: 'Table',
        imageLinkTitle: 'Image link',
        uploadImageTitle: 'Upload image',
        modeWrite: 'Write',
        modeSplit: 'Split',
        modePreview: 'Preview',
        bodyLabel: 'Body (Markdown)',
        bodyPlaceholder: 'Write Markdown directly, or insert formatting from the toolbar above.',
        previewHead: 'Publish preview',
        previewStats: (count: number, minutes: number) => `${count} chars · ~${minutes} min read`,
        untitled: 'Untitled post',
        previewEmpty: 'Start writing the body here.',
        saveDraft: 'Save draft',
        publishing: 'Publishing',
        saving: 'Saving',
        uploading: 'Uploading',
        generatingCoverAction: 'Generating cover',
        formattingAction: 'Formatting',
        saveChanges: 'Save changes',
        publish: 'Publish to feed',
        publishSettings: 'Publish settings',
        topic: 'Topic',
        contentLanguage: 'Content language',
        presets: 'Article presets',
        languageRules: 'Language rules',
        languageRulesText:
          'Posts are shown in the language you write in. When you switch the site language, readers see the original text if no translation exists.',
        draftSaved: 'Draft saved',
        text: 'text',
        emphasis: 'emphasis',
        heading: 'Heading',
        quote: 'Quote text',
        linkText: 'Link text',
        linkTextPrompt: 'Link text',
        linkUrlPrompt: 'URL',
        imageUrlPrompt: 'Image URL',
        imageAltPrompt: 'Alt text',
        image: 'Image',
        imageTooLarge: 'Images must be 8MB or smaller',
        imageTypeInvalid: 'Only JPG, PNG, WebP, and GIF images are supported',
        loginToUpload: 'Please sign in before uploading images',
        uploadFailed: 'Image upload failed',
        loginToFormat: 'Please sign in before using AI formatting',
        writeBeforeFormat: 'Write some body text before using AI formatting',
        formatTooLong: 'AI formatting supports up to 12,000 characters at a time',
        formatFailed: 'AI formatting did not complete',
        loginToCover: 'Please sign in before generating a cover',
        writeBeforeCover: 'Add a title, summary, or body before generating a cover',
        coverTooLong: 'Cover generation supports up to 12,000 characters of reference text',
        coverGenerated: 'Cover generated',
        coverGeneratedWith: (brief: string) => `Cover generated: ${brief}`,
        coverFailed: 'Cover generation did not complete',
        editForbidden: 'Only the author can edit this post.',
        loadFailed: 'This post could not be loaded for editing',
        saveFailed: 'Save failed. Please try again.',
        publishFailed: 'Publish failed. Please try again.',
        templates: [
          {
            name: 'Method notes',
            description: 'Steps, experiments, and retrospectives',
            content:
              '## Problem I ran into\n\n\n## What I tried\n\n1. \n2. \n3. \n\n## Most useful takeaway\n\n> \n\n## Reusable checklist\n\n- [ ] \n- [ ] \n'
          },
          {
            name: 'Technical article',
            description: 'Code, architecture, and tool practice',
            content:
              '## Background\n\n\n## Approach\n\n\n```java\n// key code here\n```\n\n## Why this works\n\n\n## Caveats\n\n- \n'
          },
          {
            name: 'Reading card',
            description: 'Books, courses, and papers',
            content:
              '## What this covered\n\n\n## Three takeaways\n\n- \n- \n- \n\n## Questions to explore\n\n1. \n2. \n\n## Review prompts\n\n'
          }
        ],
        taskTodo: 'Todo',
        taskNext: 'Next step',
        step1: 'Step 1',
        step2: 'Step 2',
        step3: 'Step 3',
        tableProject: 'Item',
        tableDesc: 'Notes'
      }
    : {
        kicker: 'Markdown 帖子',
        pageTitleNew: '写一篇可以沉淀的学习帖子',
        pageTitleEdit: '编辑这篇帖子',
        pageDescNew:
          '用 Markdown 写正文，也可以通过工具栏插入标题、链接、图片、代码块和表格。右侧预览就是发布后的文章效果。',
        pageDescEdit: '修改正文、摘要、封面和主题后保存，文章会继续保留原有的阅读、收藏和讨论记录。',
        loadingPost: '正在读取帖子内容',
        title: '标题',
        titlePlaceholder: '例如：一次把 Markdown 学习笔记写清楚',
        summary: '摘要',
        summaryPlaceholder: '用一两句话说明这篇内容能帮读者解决什么问题',
        removeCover: '移除封面',
        replaceCover: '更换封面',
        uploadCover: '上传封面',
        coverDropHint: '点击选择，或把 JPG、PNG、WebP、GIF 拖到这里',
        generatingCover: '生成中',
        aiCover: 'AI 生成封面',
        aiCoverHint: '根据标题、摘要和正文生成博客风格封面',
        formatting: '排版中',
        aiFormat: 'AI 排版',
        toolbarLabel: 'Markdown 工具栏',
        aiFormatTitle: 'AI 排版',
        headingTitle: '二级标题',
        boldTitle: '加粗',
        quoteTitle: '引用',
        linkTitle: '链接',
        codeTitle: '代码块',
        taskListTitle: '任务列表',
        orderedListTitle: '有序列表',
        tableTitle: '表格',
        imageLinkTitle: '图片链接',
        uploadImageTitle: '上传并插入图片',
        modeWrite: '编辑',
        modeSplit: '分屏',
        modePreview: '预览',
        bodyLabel: '正文 Markdown',
        bodyPlaceholder: '可以直接写 Markdown，也可以用上方工具栏插入格式。',
        previewHead: '发布预览',
        previewStats: (count: number, minutes: number) => `${count} 字 · 约 ${minutes} 分钟读完`,
        untitled: '未命名帖子',
        previewEmpty: '从这里开始写正文。',
        saveDraft: '保存草稿',
        publishing: '正在发布',
        saving: '正在保存',
        uploading: '正在上传',
        generatingCoverAction: '正在生成封面',
        formattingAction: '正在排版',
        saveChanges: '保存修改',
        publish: '发布到知识流',
        publishSettings: '发布设置',
        topic: '主题',
        contentLanguage: '内容语言',
        presets: '文章预设',
        languageRules: '语言规则',
        languageRulesText: '帖子展示以你写作的语言为准。切换站点语言时，如果没有对应版本，会展示原文，不会自动翻译你的内容。',
        draftSaved: '草稿已保存',
        text: '文本',
        emphasis: '重点',
        heading: '标题',
        quote: '引用内容',
        linkText: '链接文本',
        linkTextPrompt: '链接文字',
        linkUrlPrompt: '链接地址',
        imageUrlPrompt: '图片地址',
        imageAltPrompt: '图片说明',
        image: '图片',
        imageTooLarge: '图片不能超过 8MB',
        imageTypeInvalid: '只支持 JPG、PNG、WebP 和 GIF 图片',
        loginToUpload: '请先登录再上传图片',
        uploadFailed: '图片暂时上传不了',
        loginToFormat: '请先登录再使用 AI 排版',
        writeBeforeFormat: '先写入一段正文，再使用 AI 排版',
        formatTooLong: 'AI 排版一次最多处理 12000 个字符',
        formatFailed: 'AI 排版暂时没有成功',
        loginToCover: '请先登录再生成封面',
        writeBeforeCover: '先写标题、摘要或正文，再生成封面',
        coverTooLong: '生成封面一次最多参考 12000 个字符',
        coverGenerated: '封面已生成',
        coverGeneratedWith: (brief: string) => `封面已生成：${brief}`,
        coverFailed: 'AI 封面暂时没有生成成功',
        editForbidden: '只有作者本人可以编辑这篇帖子。',
        loadFailed: '这篇帖子暂时无法编辑',
        saveFailed: '暂时保存不了，请稍后再试',
        publishFailed: '暂时发布不了，请稍后再试',
        templates: [
          {
            name: '方法笔记',
            description: '适合整理步骤、经验和复盘',
            content:
              '## 我遇到的问题\n\n\n## 我尝试的方法\n\n1. \n2. \n3. \n\n## 最有用的结论\n\n> \n\n## 下次可以直接复用的清单\n\n- [ ] \n- [ ] \n'
          },
          {
            name: '技术文章',
            description: '适合写代码、架构和工具实践',
            content:
              '## 背景\n\n\n## 方案\n\n\n```java\n// 在这里放关键代码\n```\n\n## 为什么这样做\n\n\n## 注意点\n\n- \n'
          },
          {
            name: '阅读卡片',
            description: '适合沉淀书籍、课程、论文',
            content:
              '## 这篇内容讲了什么\n\n\n## 三个要点\n\n- \n- \n- \n\n## 我想继续追问\n\n1. \n2. \n\n## 复习提示\n\n'
          }
        ],
        taskTodo: '待完成',
        taskNext: '下一步',
        step1: '第一步',
        step2: '第二步',
        step3: '第三步',
        tableProject: '项目',
        tableDesc: '说明'
      }
);

const templates = computed(() => copy.value.templates);

const wordCount = computed(() => form.content.replace(/\s+/g, '').length);
const readingMinutes = computed(() => Math.max(1, Math.ceil(wordCount.value / 450)));
const previewStats = computed(() => copy.value.previewStats(wordCount.value, readingMinutes.value));
const hasDraft = computed(() => Boolean(form.title || form.summary || form.content || form.coverImageUrl));
const isEditMode = computed(() => route.name === 'post-edit');
const editPostId = computed(() => (typeof route.params.postId === 'string' ? route.params.postId : ''));
const currentDraftKey = computed(() => (isEditMode.value && editPostId.value ? `${DRAFT_KEY}.${editPostId.value}` : DRAFT_KEY));
const pageTitle = computed(() => (isEditMode.value ? copy.value.pageTitleEdit : copy.value.pageTitleNew));
const pageDescription = computed(() => (isEditMode.value ? copy.value.pageDescEdit : copy.value.pageDescNew));
const submitLabel = computed(() => {
  if (loading.value) {
    return isEditMode.value ? copy.value.saving : copy.value.publishing;
  }
  if (uploading.value) {
    return copy.value.uploading;
  }
  if (generatingCover.value) {
    return copy.value.generatingCoverAction;
  }
  if (formatting.value) {
    return copy.value.formattingAction;
  }
  return isEditMode.value ? copy.value.saveChanges : copy.value.publish;
});

function categoryIdFromCode(categoryCode: string) {
  const map: Record<string, number> = {
    TECHNOLOGY: 1,
    BUSINESS: 2,
    PRODUCTIVITY: 3,
    CAREER: 4,
    FINANCE: 5
  };
  return map[categoryCode] ?? 1;
}

function saveDraft() {
  localStorage.setItem(currentDraftKey.value, JSON.stringify(form));
  savedMessage.value = copy.value.draftSaved;
  window.setTimeout(() => {
    savedMessage.value = '';
  }, 1400);
}

function clearDraft() {
  localStorage.removeItem(currentDraftKey.value);
}

function loadDraft() {
  const raw = localStorage.getItem(currentDraftKey.value);
  if (!raw) {
    return;
  }

  try {
    const draft = JSON.parse(raw) as Partial<typeof form>;
    form.title = draft.title ?? '';
    form.summary = draft.summary ?? '';
    form.content = draft.content ?? '';
    form.coverImageUrl = draft.coverImageUrl ?? '';
    form.categoryId = draft.categoryId ?? 1;
    form.originalLanguage = draft.originalLanguage ?? preferencesStore.languageCode;
  } catch {
    clearDraft();
  }
}

async function loadEditablePost() {
  if (!editPostId.value) {
    return;
  }
  if (!sessionStore.isAuthenticated) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  loadingPost.value = true;
  restoring.value = true;
  errorMessage.value = '';

  try {
    const detail = await getPostDetail(editPostId.value, preferencesStore.languageCode);
    if (detail.authorId !== sessionStore.userId) {
      errorMessage.value = copy.value.editForbidden;
      return;
    }
    form.title = detail.title;
    form.summary = detail.summary;
    form.content = detail.content;
    form.coverImageUrl = detail.coverImageUrl ?? '';
    form.categoryId = categoryIdFromCode(detail.categoryCode);
    form.originalLanguage = detail.languageCode;
    loadDraft();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.loadFailed;
  } finally {
    restoring.value = false;
    loadingPost.value = false;
  }
}

async function loadInitialState() {
  restoring.value = true;
  errorMessage.value = '';
  if (isEditMode.value) {
    restoring.value = false;
    await loadEditablePost();
    return;
  }
  form.title = '';
  form.summary = '';
  form.content = '';
  form.coverImageUrl = '';
  form.categoryId = 1;
  form.originalLanguage = preferencesStore.languageCode;
  loadDraft();
  restoring.value = false;
}

function insertMarkdown(before: string, after = '', placeholder = copy.value.text) {
  const textarea = editorRef.value;
  const start = textarea?.selectionStart ?? form.content.length;
  const end = textarea?.selectionEnd ?? form.content.length;
  const selected = form.content.slice(start, end) || placeholder;
  const next = `${before}${selected}${after}`;
  form.content = form.content.slice(0, start) + next + form.content.slice(end);

  requestAnimationFrame(() => {
    editorRef.value?.focus();
    const cursor = start + before.length + selected.length;
    editorRef.value?.setSelectionRange(cursor, cursor);
  });
}

function selectedText() {
  const textarea = editorRef.value;
  if (!textarea) {
    return '';
  }
  return form.content.slice(textarea.selectionStart, textarea.selectionEnd);
}

function insertBlock(block: string) {
  const prefix = form.content && !form.content.endsWith('\n') ? '\n\n' : '';
  form.content += `${prefix}${block}`;
  requestAnimationFrame(() => editorRef.value?.focus());
}

function insertCodeBlock() {
  insertBlock('```java\n// code\n```');
}

function insertTaskList() {
  insertBlock(`- [ ] ${copy.value.taskTodo}\n- [ ] ${copy.value.taskNext}`);
}

function insertOrderedList() {
  insertBlock(`1. ${copy.value.step1}\n2. ${copy.value.step2}\n3. ${copy.value.step3}`);
}

function insertTable() {
  insertBlock(`| ${copy.value.tableProject} | ${copy.value.tableDesc} |\n| --- | --- |\n|  |  |`);
}

function insertLink() {
  const text = window.prompt(copy.value.linkTextPrompt, selectedText() || copy.value.linkText);
  if (text === null) {
    return;
  }
  const url = window.prompt(copy.value.linkUrlPrompt, 'https://');
  if (!url) {
    return;
  }
  insertMarkdown('[', `](${url.trim()})`, text.trim() || copy.value.linkText);
}

function insertImageByUrl() {
  const url = window.prompt(copy.value.imageUrlPrompt, 'https://');
  if (!url) {
    return;
  }
  const alt = window.prompt(copy.value.imageAltPrompt, copy.value.image) || copy.value.image;
  insertBlock(`![${alt.trim() || copy.value.image}](${url.trim()})`);
}

function applyTemplate(content: string) {
  const prefix = form.content.trim() ? '\n\n---\n\n' : '';
  form.content += `${prefix}${content}`;
}

async function handleCoverUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) {
    return;
  }
  await uploadAndUse(file, 'cover');
  (event.target as HTMLInputElement).value = '';
}

async function handleInlineImageUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) {
    return;
  }
  await uploadAndUse(file, 'inline');
  (event.target as HTMLInputElement).value = '';
}

async function handleCoverDrop(event: DragEvent) {
  coverDragActive.value = false;
  const file = firstImage(event.dataTransfer?.files);
  if (file) {
    await uploadAndUse(file, 'cover');
  }
}

async function handleEditorDrop(event: DragEvent) {
  editorDragActive.value = false;
  const file = firstImage(event.dataTransfer?.files);
  if (file) {
    await uploadAndUse(file, 'inline');
  }
}

async function handleEditorPaste(event: ClipboardEvent) {
  const file = firstImage(event.clipboardData?.files);
  if (!file) {
    return;
  }
  event.preventDefault();
  await uploadAndUse(file, 'inline');
}

function handleEditorKeydown(event: KeyboardEvent) {
  if (!event.metaKey && !event.ctrlKey) {
    return;
  }

  const key = event.key.toLowerCase();
  if (key === 'b') {
    event.preventDefault();
    insertMarkdown('**', '**', copy.value.emphasis);
  }
  if (key === 'k') {
    event.preventDefault();
    insertLink();
  }
}

function firstImage(files?: FileList | null) {
  if (!files) {
    return null;
  }
  return Array.from(files).find((file) => file.type.startsWith('image/')) ?? null;
}

function imageValidationMessage(file: File) {
  if (file.size > MAX_IMAGE_SIZE) {
    return copy.value.imageTooLarge;
  }
  if (file.type && !ALLOWED_IMAGE_TYPES.has(file.type)) {
    return copy.value.imageTypeInvalid;
  }
  return '';
}

async function uploadAndUse(file: File, target: 'cover' | 'inline') {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToUpload;
    return;
  }

  const validationMessage = imageValidationMessage(file);
  if (validationMessage) {
    errorMessage.value = validationMessage;
    return;
  }

  uploading.value = true;
  errorMessage.value = '';

  try {
    const uploaded = await uploadImage(file);
    if (target === 'cover') {
      form.coverImageUrl = uploaded.url;
    } else {
      insertBlock(`![${file.name}](${uploaded.url})`);
    }
    saveDraft();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.uploadFailed;
  } finally {
    uploading.value = false;
  }
}

function removeCover() {
  form.coverImageUrl = '';
  saveDraft();
}

async function formatWithAi() {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToFormat;
    return;
  }

  const source = form.content.trim();
  if (!source) {
    errorMessage.value = copy.value.writeBeforeFormat;
    return;
  }
  if (source.length > 12000) {
    errorMessage.value = copy.value.formatTooLong;
    return;
  }

  formatting.value = true;
  errorMessage.value = '';

  try {
    const result = await requestMarkdownFormat(source, form.originalLanguage, preferencesStore.languageCode);
    form.content = result.text.trim();
    mode.value = 'split';
    saveDraft();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.formatFailed;
  } finally {
    formatting.value = false;
  }
}

async function generateCoverWithAi() {
  if (!sessionStore.isAuthenticated) {
    errorMessage.value = copy.value.loginToCover;
    return;
  }

  const title = form.title.trim();
  const summary = form.summary.trim();
  const content = form.content.trim();
  if (!title && !summary && !content) {
    errorMessage.value = copy.value.writeBeforeCover;
    return;
  }
  if (content.length > 12000) {
    errorMessage.value = copy.value.coverTooLong;
    return;
  }

  generatingCover.value = true;
  coverGeneratedMessage.value = '';
  errorMessage.value = '';

  try {
    const result = await generateCover({
      title,
      summary,
      content,
      languageCode: form.originalLanguage,
      promptLanguageCode: preferencesStore.languageCode
    });
    form.coverImageUrl = result.coverImageUrl;
    coverGeneratedMessage.value = result.visualBrief
      ? copy.value.coverGeneratedWith(result.visualBrief)
      : copy.value.coverGenerated;
    saveDraft();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : copy.value.coverFailed;
  } finally {
    generatingCover.value = false;
  }
}

async function submit() {
  if (!sessionStore.isAuthenticated) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const payload = {
      categoryId: Number(form.categoryId),
      originalLanguage: form.originalLanguage,
      coverImageUrl: form.coverImageUrl || null,
      title: form.title.trim(),
      summary: form.summary.trim(),
      content: form.content.trim()
    };
    const postId = isEditMode.value && editPostId.value ? await updatePost(editPostId.value, payload) : await createPost(payload);
    clearDraft();
    await router.push(`/posts/${postId}`);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : isEditMode.value ? copy.value.saveFailed : copy.value.publishFailed;
  } finally {
    loading.value = false;
  }
}

onMounted(loadInitialState);

watch(
  () => [route.name, route.params.postId],
  () => loadInitialState()
);

watch(
  form,
  () => {
    if (!restoring.value) {
      localStorage.setItem(currentDraftKey.value, JSON.stringify(form));
    }
  },
  { deep: true }
);
</script>

<template>
  <section class="editor-page markdown-editor-page">
    <div class="page-heading">
      <span class="section-kicker">{{ copy.kicker }}</span>
      <h1>{{ pageTitle }}</h1>
      <p>{{ pageDescription }}</p>
    </div>

    <LoadingState v-if="loadingPost" :label="copy.loadingPost" />

    <form v-else class="editor-layout markdown-editor-layout" @submit.prevent="submit">
      <div class="editor-main markdown-editor-main">
        <div class="post-composer-head">
          <label>
            <span>{{ copy.title }}</span>
            <input v-model="form.title" required maxlength="120" :placeholder="copy.titlePlaceholder" />
          </label>

          <label>
            <span>{{ copy.summary }}</span>
            <textarea v-model="form.summary" rows="3" maxlength="300" :placeholder="copy.summaryPlaceholder" />
          </label>
        </div>

        <div class="cover-uploader">
          <div v-if="form.coverImageUrl" class="cover-preview">
            <img :src="form.coverImageUrl" alt="" />
            <button type="button" :title="copy.removeCover" @click="removeCover">
              <X :size="16" />
            </button>
          </div>
          <div class="cover-actions">
            <label
              class="upload-drop"
              :class="{ active: coverDragActive }"
              @dragenter.prevent="coverDragActive = true"
              @dragover.prevent="coverDragActive = true"
              @dragleave.prevent="coverDragActive = false"
              @drop.prevent="handleCoverDrop"
            >
              <ImagePlus :size="20" />
              <strong>{{ form.coverImageUrl ? copy.replaceCover : copy.uploadCover }}</strong>
              <span>{{ copy.coverDropHint }}</span>
              <input accept="image/*" type="file" @change="handleCoverUpload" />
            </label>
            <button class="ai-cover-button" type="button" :disabled="generatingCover || uploading || formatting" @click="generateCoverWithAi">
              <Images :size="19" />
              <strong>{{ generatingCover ? copy.generatingCover : copy.aiCover }}</strong>
              <span>{{ copy.aiCoverHint }}</span>
            </button>
          </div>
        </div>

        <div class="editor-toolbar" :aria-label="copy.toolbarLabel">
          <button class="ai-format-button" type="button" :title="copy.aiFormatTitle" :disabled="formatting || uploading || generatingCover" @click="formatWithAi">
            <Sparkles :size="17" />
            <span>{{ formatting ? copy.formatting : copy.aiFormat }}</span>
          </button>
          <button type="button" :title="copy.headingTitle" @click="insertMarkdown('## ', '', copy.heading)">
            <Heading2 :size="17" />
          </button>
          <button type="button" :title="copy.boldTitle" @click="insertMarkdown('**', '**', copy.emphasis)">
            <Bold :size="17" />
          </button>
          <button type="button" :title="copy.quoteTitle" @click="insertMarkdown('> ', '', copy.quote)">
            <Quote :size="17" />
          </button>
          <button type="button" :title="copy.linkTitle" @click="insertLink">
            <Link :size="17" />
          </button>
          <button type="button" :title="copy.codeTitle" @click="insertCodeBlock">
            <Code2 :size="17" />
          </button>
          <button type="button" :title="copy.taskListTitle" @click="insertTaskList">
            <ListChecks :size="17" />
          </button>
          <button type="button" :title="copy.orderedListTitle" @click="insertOrderedList">
            <ListOrdered :size="17" />
          </button>
          <button type="button" :title="copy.tableTitle" @click="insertTable">
            <Table2 :size="17" />
          </button>
          <button type="button" :title="copy.imageLinkTitle" @click="insertImageByUrl">
            <ImagePlus :size="17" />
          </button>
          <label class="toolbar-upload" :title="copy.uploadImageTitle">
            <ImagePlus :size="17" />
            <input accept="image/*" type="file" @change="handleInlineImageUpload" />
          </label>
        </div>

        <div class="editor-mode-tabs">
          <button type="button" :class="{ active: mode === 'write' }" @click="mode = 'write'">
            <Text :size="16" />
            <span>{{ copy.modeWrite }}</span>
          </button>
          <button type="button" :class="{ active: mode === 'split' }" @click="mode = 'split'">
            <SplitSquareHorizontal :size="16" />
            <span>{{ copy.modeSplit }}</span>
          </button>
          <button type="button" :class="{ active: mode === 'preview' }" @click="mode = 'preview'">
            <Eye :size="16" />
            <span>{{ copy.modePreview }}</span>
          </button>
        </div>

        <div class="markdown-workbench" :class="`mode-${mode}`">
          <label v-show="mode !== 'preview'" class="markdown-source">
            <span>{{ copy.bodyLabel }}</span>
            <MentionTextarea
              ref="editorRef"
              v-model="form.content"
              required
              spellcheck="false"
              :class="{ 'drag-active': editorDragActive }"
              :placeholder="copy.bodyPlaceholder"
              @dragenter.prevent="editorDragActive = true"
              @dragover.prevent="editorDragActive = true"
              @dragleave.prevent="editorDragActive = false"
              @drop.prevent="handleEditorDrop"
              @paste="handleEditorPaste"
              @keydown="handleEditorKeydown"
            />
          </label>

          <section v-show="mode !== 'write'" class="markdown-preview-panel">
            <div class="preview-head">
              <span>{{ copy.previewHead }}</span>
              <small>{{ previewStats }}</small>
            </div>
            <article class="preview-document">
              <img v-if="form.coverImageUrl" class="preview-cover" :src="form.coverImageUrl" alt="" />
              <h1>{{ form.title || copy.untitled }}</h1>
              <p v-if="form.summary" class="article-summary">{{ form.summary }}</p>
              <MarkdownRenderer :content="form.content || copy.previewEmpty" />
            </article>
          </section>
        </div>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <p v-if="coverGeneratedMessage" class="form-success cover-generated-message">{{ coverGeneratedMessage }}</p>
        <p v-if="savedMessage" class="form-success">{{ savedMessage }}</p>

        <div class="form-actions composer-actions">
          <button class="secondary-button" type="button" :disabled="!hasDraft" @click="saveDraft">
            <Save :size="17" />
            <span>{{ copy.saveDraft }}</span>
          </button>
          <button class="primary-button" type="submit" :disabled="loading || uploading || formatting || generatingCover">
            <Send :size="17" />
            <span>{{ submitLabel }}</span>
          </button>
        </div>
      </div>

      <aside class="editor-side markdown-editor-side">
        <section class="side-panel">
          <div class="panel-title">
            <PenLine :size="18" />
            <span>{{ copy.publishSettings }}</span>
          </div>
          <label>
            <span>{{ copy.topic }}</span>
            <select v-model="form.categoryId">
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.label }}
              </option>
            </select>
          </label>
          <label>
            <span>{{ copy.contentLanguage }}</span>
            <select v-model="form.originalLanguage">
              <option value="zh_CN">{{ languageLabel('zh_CN') }}</option>
              <option value="en_US">{{ languageLabel('en_US') }}</option>
            </select>
          </label>
        </section>

        <section class="side-panel preset-panel">
          <div class="panel-title">
            <FileText :size="18" />
            <span>{{ copy.presets }}</span>
          </div>
          <button v-for="template in templates" :key="template.name" type="button" @click="applyTemplate(template.content)">
            <strong>{{ template.name }}</strong>
            <span>{{ template.description }}</span>
          </button>
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <Languages :size="18" />
            <span>{{ copy.languageRules }}</span>
          </div>
          <p>{{ copy.languageRulesText }}</p>
        </section>
      </aside>
    </form>
  </section>
</template>
