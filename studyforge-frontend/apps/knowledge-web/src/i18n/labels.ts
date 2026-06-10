export function languageLabel(code: string) {
  return ({ zh_CN: '中文', en_US: 'English' } as Record<string, string>)[code] ?? code;
}

export function alternateLanguageLabel(code: string) {
  if (code === 'zh_CN') {
    return 'English';
  }
  if (code === 'en_US') {
    return '中文';
  }
  return code;
}
