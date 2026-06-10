import type { LanguageCode } from '@/stores/preferences';
import type { TopicCategory } from '@/types/api';

export function topicCategories(languageCode: LanguageCode, includeAll = true): TopicCategory[] {
  const localized =
    languageCode === 'en_US'
      ? [
          ...(includeAll ? [{ code: 'ALL', name: 'All', description: 'Fresh picks', accent: '#0f766e' }] : []),
          { code: 'TECHNOLOGY', name: 'Technology', description: 'Frontend, backend, tools', accent: '#2563eb' },
          { code: 'BUSINESS', name: 'Business', description: 'Cases and decisions', accent: '#7c3aed' },
          { code: 'PRODUCTIVITY', name: 'Productivity', description: 'Notes and reviews', accent: '#b45309' },
          { code: 'CAREER', name: 'Career', description: 'Interviews and growth', accent: '#15803d' },
          { code: 'FINANCE', name: 'Finance', description: 'Budget and money basics', accent: '#0891b2' }
        ]
      : [
          ...(includeAll ? [{ code: 'ALL', name: '全部', description: '今天推荐', accent: '#0f766e' }] : []),
          { code: 'TECHNOLOGY', name: '技术实践', description: '前端、后端、工具', accent: '#2563eb' },
          { code: 'BUSINESS', name: '商业观察', description: '案例、决策、市场', accent: '#7c3aed' },
          { code: 'PRODUCTIVITY', name: '效率方法', description: '笔记、复盘、计划', accent: '#b45309' },
          { code: 'CAREER', name: '职业成长', description: '求职、面试、成长', accent: '#15803d' },
          { code: 'FINANCE', name: '财务入门', description: '预算、风险、常识', accent: '#0891b2' }
        ];

  return localized;
}

export function categoryMap(languageCode: LanguageCode): Record<string, TopicCategory> {
  return Object.fromEntries(topicCategories(languageCode, false).map((category) => [category.code, category]));
}

export function categoryForCode(languageCode: LanguageCode, categoryCode: string): TopicCategory {
  return (
    categoryMap(languageCode)[categoryCode] ?? {
      code: categoryCode,
      name: categoryCode,
      description: '',
      accent: '#0f766e'
    }
  );
}
