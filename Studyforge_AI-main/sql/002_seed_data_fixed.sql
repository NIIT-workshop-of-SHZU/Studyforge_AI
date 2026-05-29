USE test_studyforge_ai_v2;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE admin_audit_logs;
TRUNCATE TABLE help_answer_likes;
TRUNCATE TABLE help_answers;
TRUNCATE TABLE help_requests;
TRUNCATE TABLE voice_records;
TRUNCATE TABLE ai_logs;
TRUNCATE TABLE reports;
TRUNCATE TABLE post_view_history;
TRUNCATE TABLE favorite_collection_items;
TRUNCATE TABLE favorite_collections;
TRUNCATE TABLE post_favorites;
TRUNCATE TABLE post_likes;
TRUNCATE TABLE comment_likes;
TRUNCATE TABLE comments;
TRUNCATE TABLE uploaded_files;
TRUNCATE TABLE post_i18n;
TRUNCATE TABLE posts;
TRUNCATE TABLE user_tokens;
TRUNCATE TABLE integration_settings;
TRUNCATE TABLE user_experience_logs;
TRUNCATE TABLE friend_messages;
TRUNCATE TABLE friendships;
TRUNCATE TABLE friend_requests;
TRUNCATE TABLE user_follows;
TRUNCATE TABLE users;
TRUNCATE TABLE category_i18n;
TRUNCATE TABLE categories;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO categories (category_code, sort_no, status)
VALUES
    ('TECHNOLOGY', 10, 'ACTIVE'),
    ('BUSINESS', 20, 'ACTIVE'),
    ('PRODUCTIVITY', 30, 'ACTIVE'),
    ('CAREER', 40, 'ACTIVE'),
    ('FINANCE', 50, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    sort_no = VALUES(sort_no),
    status = VALUES(status);

INSERT INTO category_i18n (category_id, language_code, name)
SELECT c.category_id, 'zh_CN', t.name
FROM categories c
JOIN (
    SELECT 'TECHNOLOGY' AS category_code, '鎶€鏈疄璺? AS name
    UNION ALL SELECT 'BUSINESS', '鍟嗕笟瑙傚療'
    UNION ALL SELECT 'PRODUCTIVITY', '鏁堢巼鏂规硶'
    UNION ALL SELECT 'CAREER', '鑱屼笟鎴愰暱'
    UNION ALL SELECT 'FINANCE', '璐㈠姟鍏ラ棬'
) t ON t.category_code = c.category_code
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO category_i18n (category_id, language_code, name)
SELECT c.category_id, 'en_US', t.name
FROM categories c
JOIN (
    SELECT 'TECHNOLOGY' AS category_code, 'Technology' AS name
    UNION ALL SELECT 'BUSINESS', 'Business'
    UNION ALL SELECT 'PRODUCTIVITY', 'Productivity'
    UNION ALL SELECT 'CAREER', 'Career'
    UNION ALL SELECT 'FINANCE', 'Finance'
) t ON t.category_code = c.category_code
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (
    username,
    display_name,
    email,
    password_hash,
    role,
    status,
    bio,
    avatar_url,
    banner_url,
    community_level,
    experience_points,
    last_login_reward_date,
    reputation_score
)
VALUES
    ('chen_jiayi', '闄堝槈浠?, 'jiayi.chen@studyforge.ai', 'sha256:aa5969061c710df50f3b9724264a64b8ab3cd41c9b3f62f73f19bf8cb444d9a0', 'USER', 'ACTIVE', '鍋忓墠绔拰瀛︿範鏂规硶锛屽枩娆㈡妸闀挎枃鏁寸悊鎴愬彲浠ュ涔犵殑鍗＄墖銆?, '/avatars/chen-jiayi.svg', '/banners/study-lab.svg', 14, 1375, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 860),
    ('li_minghao', '鏉庢槑鏄?, 'minghao.li@studyforge.ai', 'sha256:aa5969061c710df50f3b9724264a64b8ab3cd41c9b3f62f73f19bf8cb444d9a0', 'USER', 'ACTIVE', '鍏虫敞鍐呭杩愯惀銆佺煡璇嗗簱娌荤悊鍜屽洟闃熷崗浣滄祦绋嬨€?, '/avatars/li-minghao.svg', '/banners/content-ops.svg', 12, 1160, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 740),
    ('zhao_yiran', '璧典竴鐒?, 'yiran.zhao@studyforge.ai', 'sha256:aa5969061c710df50f3b9724264a64b8ab3cd41c9b3f62f73f19bf8cb444d9a0', 'USER', 'ACTIVE', '闀挎湡璁板綍澶嶇洏銆佽鍒掑拰涓诲姩鍥炲繂缁冧範銆?, '/avatars/zhao-yiran.svg', '/banners/review-system.svg', 11, 1045, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 690),
    ('wang_yu', '鐜嬪笨', 'yu.wang@studyforge.ai', 'sha256:aa5969061c710df50f3b9724264a64b8ab3cd41c9b3f62f73f19bf8cb444d9a0', 'USER', 'ACTIVE', '鍚庣寮€鍙戯紝鍠滄鎶婁簨鍔¤竟鐣屽拰鎺ュ彛濂戠害鍐欐竻妤氥€?, '/avatars/wang-yu.svg', '/banners/backend-notes.svg', 10, 960, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 610),
    ('emma_clark', 'Emma Clark', 'emma.clark@studyforge.ai', 'sha256:aa5969061c710df50f3b9724264a64b8ab3cd41c9b3f62f73f19bf8cb444d9a0', 'USER', 'ACTIVE', 'Writes about practical product UX, Markdown tools, and learning workflows.', '/avatars/emma-clark.svg', '/banners/editor-tools.svg', 13, 1260, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 780),
    ('noah_kim', 'Noah Kim', 'noah.kim@studyforge.ai', 'sha256:aa5969061c710df50f3b9724264a64b8ab3cd41c9b3f62f73f19bf8cb444d9a0', 'USER', 'ACTIVE', 'Keeps weekly review logs for interviews, systems thinking, and writing practice.', '/avatars/noah-kim.svg', '/banners/weekly-review.svg', 9, 820, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 530),
    ('ops_admin', 'StudyForge 杩愯惀', 'ops.admin@studyforge.ai', 'sha256:d120c09f9b058fd4177b5a79917dc5a67769b9b0d09ccaaf414a30e06d63898b', 'ADMIN', 'ACTIVE', '缁存姢鍐呭璐ㄩ噺銆丄I 閰嶇疆鍜岀ぞ鍖鸿繍琛岀姸鎬併€?, '/avatars/ops-admin.svg', '/banners/system-console.svg', 18, 1710, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 1200)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    status = VALUES(status),
    bio = VALUES(bio),
    avatar_url = VALUES(avatar_url),
    banner_url = VALUES(banner_url),
    community_level = VALUES(community_level),
    experience_points = VALUES(experience_points),
    last_login_reward_date = VALUES(last_login_reward_date),
    reputation_score = VALUES(reputation_score);

INSERT INTO user_follows (follower_id, following_id, status, created_time)
SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 18 DAY
FROM users f JOIN users t ON t.username = 'chen_jiayi'
WHERE f.username IN ('li_minghao', 'zhao_yiran', 'emma_clark')
UNION ALL SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 16 DAY FROM users f JOIN users t ON t.username = 'emma_clark' WHERE f.username IN ('chen_jiayi', 'noah_kim')
UNION ALL SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 14 DAY FROM users f JOIN users t ON t.username = 'zhao_yiran' WHERE f.username IN ('chen_jiayi', 'wang_yu')
UNION ALL SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 12 DAY FROM users f JOIN users t ON t.username = 'li_minghao' WHERE f.username IN ('chen_jiayi', 'ops_admin')
UNION ALL SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 10 DAY FROM users f JOIN users t ON t.username = 'wang_yu' WHERE f.username IN ('li_minghao', 'zhao_yiran')
UNION ALL SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 8 DAY FROM users f JOIN users t ON t.username = 'noah_kim' WHERE f.username IN ('emma_clark', 'chen_jiayi')
UNION ALL SELECT f.user_id, t.user_id, 'ACTIVE', NOW() - INTERVAL 7 DAY FROM users f JOIN users t ON t.username = 'ops_admin' WHERE f.username IN ('chen_jiayi', 'li_minghao');

INSERT INTO friend_requests (requester_id, addressee_id, message, status, processed_time, created_time)
SELECT requester.user_id, addressee.user_id, '鏈€杩戦兘鍦ㄦ暣鐞?Markdown 缂栬緫鍣ㄥ拰澶嶄範鍗＄墖锛屾兂鍔犲ソ鍙嬪悗缁х画浜ゆ祦銆?, 'ACCEPTED', NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 16 DAY
FROM users requester JOIN users addressee ON addressee.username = 'chen_jiayi'
WHERE requester.username = 'emma_clark'
UNION ALL
SELECT requester.user_id, addressee.user_id, '鎴戝湪鐪嬩綘鍐欑殑 Service 灞傛枃绔狅紝鍚庨潰鎯宠鏁欎簨鍔¤竟鐣屻€?, 'ACCEPTED', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 12 DAY
FROM users requester JOIN users addressee ON addressee.username = 'wang_yu'
WHERE requester.username = 'chen_jiayi'
UNION ALL
SELECT requester.user_id, addressee.user_id, '鎯充竴璧峰畬鍠勫涔犲鐩樻ā鏉匡紝鍙互鍔犱釜濂藉弸鍚楋紵', 'ACCEPTED', NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 9 DAY
FROM users requester JOIN users addressee ON addressee.username = 'zhao_yiran'
WHERE requester.username = 'chen_jiayi'
UNION ALL
SELECT requester.user_id, addressee.user_id, '鐪嬪埌浣犱篃鍦ㄦ暣鐞嗘妧鏈潰璇曞涔犺褰曪紝鎯充氦娴佷竴涓嬫瘡鍛ㄥ鐩樻柟寮忋€?, 'PENDING', NULL, NOW() - INTERVAL 4 HOUR
FROM users requester JOIN users addressee ON addressee.username = 'chen_jiayi'
WHERE requester.username = 'noah_kim'
UNION ALL
SELECT requester.user_id, addressee.user_id, '杩愯惀鍚庡彴杩欑瘒鍐欏緱寰堝疄鐢紝鎯充繚鎸佽仈绯汇€?, 'PENDING', NULL, NOW() - INTERVAL 2 HOUR
FROM users requester JOIN users addressee ON addressee.username = 'li_minghao'
WHERE requester.username = 'chen_jiayi';

INSERT INTO friendships (user_low_id, user_high_id, status, created_time)
SELECT LEAST(a.user_id, b.user_id), GREATEST(a.user_id, b.user_id), 'ACTIVE', NOW() - INTERVAL 15 DAY
FROM users a JOIN users b ON b.username = 'chen_jiayi'
WHERE a.username = 'emma_clark'
UNION ALL
SELECT LEAST(a.user_id, b.user_id), GREATEST(a.user_id, b.user_id), 'ACTIVE', NOW() - INTERVAL 11 DAY
FROM users a JOIN users b ON b.username = 'wang_yu'
WHERE a.username = 'chen_jiayi'
UNION ALL
SELECT LEAST(a.user_id, b.user_id), GREATEST(a.user_id, b.user_id), 'ACTIVE', NOW() - INTERVAL 8 DAY
FROM users a JOIN users b ON b.username = 'zhao_yiran'
WHERE a.username = 'chen_jiayi';

INSERT INTO friend_messages (sender_id, receiver_id, content, read_flag, created_time)
SELECT sender.user_id, receiver.user_id, '鎴戞妸 Markdown 缂栬緫鍣ㄧ殑鍥剧墖涓婁紶娴佺▼鍙堟暣鐞嗕簡涓€鐗堬紝浣犳湁绌哄彲浠ョ湅鐪嬨€?, 1, NOW() - INTERVAL 3 DAY
FROM users sender JOIN users receiver ON receiver.username = 'chen_jiayi'
WHERE sender.username = 'emma_clark'
UNION ALL
SELECT sender.user_id, receiver.user_id, '鐪嬪埌浜嗭紝淇濈暀 Markdown 婧愭枃杩欎釜鍐冲畾寰堢ǔ锛屾垜鍑嗗鎸夎繖涓€濊矾缁х画鍋氶瑙堛€?, 1, NOW() - INTERVAL 3 DAY + INTERVAL 30 MINUTE
FROM users sender JOIN users receiver ON receiver.username = 'emma_clark'
WHERE sender.username = 'chen_jiayi'
UNION ALL
SELECT sender.user_id, receiver.user_id, '浣犻偅绡囧涔犲崱鐗囨枃绔犻噷锛岄棶棰樿璁＄殑閮ㄥ垎寰堥€傚悎鏀捐繘鎴戠殑鍛ㄥ鐩樸€?, 0, NOW() - INTERVAL 1 DAY
FROM users sender JOIN users receiver ON receiver.username = 'zhao_yiran'
WHERE sender.username = 'chen_jiayi';

INSERT INTO user_experience_logs (user_id, action_type, experience_delta, source_id, created_date, created_time)
SELECT user_id, 'DAILY_LOGIN', 15, NULL, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), NOW() - INTERVAL 1 DAY
FROM users
WHERE username IN ('chen_jiayi', 'zhao_yiran', 'emma_clark', 'ops_admin')
UNION ALL
SELECT user_id, 'PUBLISH_POST', 45, NULL, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), NOW() - INTERVAL 3 DAY
FROM users
WHERE username IN ('chen_jiayi', 'emma_clark', 'zhao_yiran', 'li_minghao', 'wang_yu', 'noah_kim')
ON DUPLICATE KEY UPDATE experience_delta = VALUES(experience_delta);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'ai.base_url', 'https://api.siliconflow.cn/v1', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'ai.api_key', 'sk-sumhznadchbatbcaklzlttfzwocmqixrdaiicohoimpiuvpd', 1, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'ai.chat_model', 'deepseek-ai/DeepSeek-V4-Flash', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'image.base_url', 'https://api.hiyo.top/v1', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'image.api_key', 'sk-j1gAfU3lBu10wJrOoe0tdcleAd0KyWFa1FSSozbMqvHo31Hh', 1, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'image.model', 'gpt-image-2', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'image.size', '1536x1024', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'voice.base_url', 'https://api.siliconflow.cn/v1', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'voice.api_key', 'sk-sumhznadchbatbcaklzlttfzwocmqixrdaiicohoimpiuvpd', 1, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'voice.model', 'FunAudioLLM/CosyVoice2-0.5B', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO integration_settings (setting_key, setting_value, secret_flag, updated_by)
SELECT 'voice.name', 'FunAudioLLM/CosyVoice2-0.5B:alex', 0, u.user_id FROM users u WHERE u.username = 'ops_admin'
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), secret_flag = VALUES(secret_flag), updated_by = VALUES(updated_by);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'zh_CN', 'PUBLISHED', 1, 18, 10, 2, 426, 98.40
FROM users u JOIN categories c ON c.category_code = 'TECHNOLOGY'
WHERE u.username = 'chen_jiayi';
SET @post_vue_state = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_vue_state,
    'zh_CN',
    'Vue 鐭ヨ瘑娴侀〉闈㈢殑鐘舵€佽璁★細浠庤姹傚埌缂撳瓨',
    '鎶婂姞杞姐€侀敊璇€佺瓫閫夈€佽瑷€鍜屽垪琛ㄧ姸鎬佹媶娓呮锛岀煡璇嗘祦椤甸潰浼氭洿绋筹紝涔熸洿瀹规槗缁存姢銆?,
    '## 涓轰粈涔堝厛鏁寸悊鐘舵€?
鐭ヨ瘑娴侀〉闈㈢湅璧锋潵鍙槸涓€涓垪琛紝浣嗙湡姝ｈ繍琛屾椂浼氬悓鏃跺鐞嗗緢澶氱姸鎬侊細鐢ㄦ埛褰撳墠閫夋嫨鐨勪富棰樸€佹悳绱㈠叧閿瘝銆佺珯鐐硅瑷€銆佹帴鍙ｅ姞杞界姸鎬併€侀敊璇秷鎭€佸垎椤典綅缃€佺櫥褰曠姸鎬侊紝浠ュ強姣忓紶鍗＄墖鐨勭粺璁℃暟鎹€傚鏋滆繖浜涚姸鎬侀兘鏁ｈ惤鍦ㄧ粍浠堕噷锛岄〉闈㈠緢蹇細鍙樺緱闅句互鍒ゆ柇闂鏉ヨ嚜鍝噷銆?
鎴戠殑鍋氭硶鏄厛鎶婄姸鎬佸垎鎴愪笁绫伙細

- **璇锋眰鐘舵€?*锛歚loading`銆乣errorMessage`銆佹帴鍙ｈ繑鍥炵殑 `posts`銆?- **瑙嗗浘鐘舵€?*锛氬綋鍓嶅垎绫汇€佹悳绱㈣瘝銆佹帓搴忔柟寮忋€佸綋鍓嶈瑷€銆?- **鐢ㄦ埛鐘舵€?*锛氱櫥褰曚俊鎭€佹敹钘忓叧绯汇€侀槄璇诲巻鍙层€?
杩欐牱鍐欑殑濂藉鏄紝鎺ュ彛澶辫触鏃跺彧褰卞搷璇锋眰鐘舵€侊紝鐢ㄦ埛绛涢€夋椂鍙奖鍝嶈鍥剧姸鎬侊紝鐧诲綍鍙樺寲鏃朵篃涓嶄細鎶婃暣椤甸€昏緫鎵撴暎銆?
## 鍒楄〃鏁版嵁涓嶈鍦ㄧ粍浠堕噷浜屾閫犲亣

鐭ヨ瘑娴佹渶閲嶈鐨勬槸鍙俊銆傚崱鐗囦笂鐨勬爣棰樸€佹憳瑕併€佺儹搴︺€佽瑷€銆佽瘎璁烘暟銆佹敹钘忔暟閮藉簲璇ユ潵鑷悗绔繑鍥烇紝鑰屼笉鏄墠绔嚜宸辫ˉ涓€涓湅璧锋潵濂界湅鐨勬暟瀛椼€傚墠绔彲浠ュ仛鏍煎紡鍖栵紝渚嬪鎶婄儹搴︿繚鐣欎竴浣嶅皬鏁般€佹妸璇█鐮佹樉绀轰负 `zh_CN` 鎴?`en_US`锛屼絾涓嶈缂栭€犱笟鍔′簨瀹炪€?
鎴戝€惧悜浜庤鎺ュ彛杩斿洖杩欐牱鐨勭粨鏋勶細

```ts
interface PostSummary {
  postId: number
  title: string
  summary: string
  languageCode: string
  categoryCode: string
  likeCount: number
  favoriteCount: number
  commentCount: number
  viewCount: number
  hotScore: number
}
```

鍓嶇鎷垮埌鏁版嵁鍚庡彧鍋氫袱浠朵簨锛氭寜褰撳墠鍒嗙被杩囨护銆佹寜鎼滅储璇嶈繃婊ゃ€傛帓搴忓拰鐑害璁＄畻鏀惧湪鍚庣锛岄伩鍏嶄笉鍚岄〉闈㈠嚭鐜颁笉涓€鑷寸殑鍐呭椤哄簭銆?
## 缂撳瓨瑕佸皬蹇冭繃鏈?
鐭ヨ瘑娴佸彲浠ュ仛杞婚噺缂撳瓨锛屼絾缂撳瓨瀵硅薄瑕佽窡绛涢€夋潯浠剁粦瀹氥€傛瘮濡?`categoryCode=TECHNOLOGY` 鍜?`categoryCode=CAREER` 涓嶅簲璇ュ叡鐢ㄤ竴涓垪琛ㄧ紦瀛橈紱绔欑偣璇█鍒囨崲鍚庯紝濡傛灉绯荤粺瑙勫垯鏄笉鎸夌珯鐐硅瑷€闅旂鐭ヨ瘑娴侊紝涔熻纭繚鍒楄〃浠嶇劧灞曠ず甯栧瓙鍘熷璇█锛岃€屼笉鏄嬁绔欑偣璇█鍘昏鐩栧崱鐗囧唴瀹广€?
涓€涓疄鐢ㄧ瓥鐣ユ槸锛?
1. 棣栨杩涘叆椤甸潰璇锋眰鐑棬鍐呭銆?2. 鍒囨崲涓婚鏃朵紭鍏堜娇鐢ㄥ凡鏈夊垪琛ㄥ仛鏈湴杩囨护銆?3. 鐢ㄦ埛鐐瑰嚮鍒锋柊鏃堕噸鏂拌姹傛帴鍙ｃ€?4. 鍙戝竷鏂版枃绔犲悗鍥炲埌鐭ヨ瘑娴侊紝涓诲姩鍒锋柊涓€娆°€?
## 缁勪欢杈圭晫

鎴戜細鎶婇〉闈㈡媶鎴愪笁涓眰娆★細

- `HomeView`锛氳礋璐ｈ姹傛暟鎹€佺瓫閫夋潯浠跺拰椤甸潰缁勫悎銆?- `TopicRail`锛氬彧璐熻矗灞曠ず鍒嗙被鍜屾姏鍑洪€夋嫨浜嬩欢銆?- `KnowledgeCard`锛氬彧璐熻矗灞曠ず涓€绡囨枃绔狅紝涓嶈嚜宸辫姹傝鎯呫€?
杩欐牱鏂板鐎戝竷娴併€佸皝闈㈠浘鎴栫粺璁℃暟鎹椂锛屽彧闇€瑕佹敼鍗＄墖鍜屾牱寮忥紝涓嶄細褰卞搷璇锋眰閫昏緫銆傚悗缁鍔犲垎椤碉紝涔熷彲浠ョ洿鎺ュ湪椤甸潰灞傚鐞嗐€?
## 澶嶇洏

鐭ヨ瘑娴侀〉闈㈢殑闅剧偣涓嶅湪瑙嗚锛岃€屽湪鐘舵€佽竟鐣屻€傚彧瑕佹暟鎹潵鑷悗绔€佺瓫閫夋潯浠堕泦涓鐞嗐€佸崱鐗囩粍浠朵繚鎸佺函灞曠ず锛岄〉闈㈠氨浼氭瘮鍫嗗彔涓存椂鍙橀噺绋冲畾寰楀銆?,
    'MARKDOWN',
    'Vue,鐘舵€佺鐞?鐭ヨ瘑娴?Axios',
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'en_US', 'PUBLISHED', 1, 15, 9, 2, 388, 94.20
FROM users u JOIN categories c ON c.category_code = 'TECHNOLOGY'
WHERE u.username = 'emma_clark';
SET @post_markdown_composer = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_markdown_composer,
    'en_US',
    'Designing a Markdown Composer That Users Can Trust',
    'A reliable composer needs plain Markdown, visual shortcuts, preview, image handling, and clear save behavior.',
    '## Start with the writing job

A Markdown composer is not just a large textarea. People use it when they want to turn messy notes into something another person can read. The editor should therefore support two habits at the same time: fast plain-text writing and structured formatting.

The first rule is to keep Markdown as the source of truth. Toolbar actions can insert headings, links, images, code blocks, task lists, or tables, but the user should always be able to see and edit the actual Markdown. This makes the content portable and avoids locking the article into a private editor format.

## The three modes that matter

I usually design the composer with three modes:

- **Write**: a focused Markdown textarea for drafting.
- **Split**: source on one side and rendered preview on the other.
- **Preview**: the final reading layout without editor noise.

Split mode is the default because it teaches the relationship between Markdown and the rendered article. Write mode is useful for longer drafts, and preview mode helps catch layout issues before publishing.

## Toolbar behavior

Toolbar buttons should be small and predictable. A button should not hide a complex workflow unless it clearly says what it does. For example:

```md
## Section title

> A quote that frames the point.

| Decision | Reason |
| --- | --- |
| Keep Markdown visible | Users can recover and edit content easily |
```

For links, prompting for text and URL is better than inserting an unfinished `[](https://)` fragment. For images, upload should return a stable URL and insert `![description](url)` at the cursor.

## Image and cover handling

Images need a storage path, not only a browser preview. A robust flow is:

1. User selects, pastes, or drops an image.
2. Frontend validates type and size.
3. Backend saves the file and writes metadata to the database.
4. API returns the file URL.
5. Composer inserts Markdown or stores the cover URL.

This keeps published articles readable after refresh, across devices, and after the editor state is gone.

## Save behavior

Local draft saving is still useful because it protects the user from accidental navigation. It should not pretend to be a cloud draft. A later database draft feature can add cross-device recovery, version history, and scheduled publishing.

## Final check

The preview should render exactly the same Markdown that will be sent to the server. If preview and published detail use different renderers, users will lose trust quickly. One renderer, sanitized output, and consistent article styles are enough for a dependable first version.',
    'MARKDOWN',
    'Markdown,Editor,Image Upload,UX',
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'zh_CN', 'PUBLISHED', 1, 13, 8, 2, 342, 91.80
FROM users u JOIN categories c ON c.category_code = 'PRODUCTIVITY'
WHERE u.username = 'zhao_yiran';
SET @post_review_cards = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_review_cards,
    'zh_CN',
    '鎶婁竴绡囬暱鏂囧彉鎴愬彲澶嶄範鍗＄墖鐨勫洓姝ユ硶',
    '璇诲畬鏂囩珷涔嬪悗绔嬪埢鎻愮偧闂銆佺瓟妗堝拰鍏抽敭璇嶏紝姣旈殧鍑犲ぉ閲嶆柊缈诲叏鏂囨洿鏈夋晥銆?,
    '## 璇诲畬涓嶇瓑浜庡浼?
寰堝浜烘敹钘忎簡寰堝鏂囩珷锛屼絾鐪熸澶嶄範鏃惰繕鏄粠澶村紑濮嬫壘閲嶇偣銆傞棶棰樹笉鍦ㄤ簬鏂囩珷涓嶅濂斤紝鑰屽湪浜庨槄璇荤粨鏉熸椂娌℃湁鎶婂唴瀹硅浆鎴愬彲浠ュ洖鐪嬬殑褰㈡€併€傚涔犲崱鐗囩殑鐩爣涓嶆槸澶嶅埗鍏ㄦ枃锛岃€屾槸鎶婃枃绔犲彉鎴愬嚑涓彲浠ヤ富鍔ㄥ洖蹇嗙殑闂銆?
## 绗竴姝ワ細鍐欏嚭鏂囩珷瑕佽В鍐崇殑闂

鍏堢敤涓€鍙ヨ瘽鍥炵瓟锛氳繖绡囨枃绔犲埌搴曞湪瑙ｅ喅浠€涔堥棶棰橈紵

濡傛灉涓€绡囨枃绔犺鐨勬槸 Vue 鐘舵€佺鐞嗭紝涓嶈鍙啓鈥淰ue 绗旇鈥濄€傛洿濂界殑闂鏄細

> 鍦ㄤ竴涓煡璇嗘祦椤甸潰閲岋紝鍝簺鐘舵€佸簲璇ユ斁鍦ㄩ〉闈㈠眰锛屽摢浜涚姸鎬佸簲璇ョ暀缁欑粍浠惰嚜宸卞鐞嗭紵

闂瓒婂叿浣擄紝涔嬪悗澶嶄範瓒婂鏄撱€?
## 绗簩姝ワ細鎻愮偧涓変釜鍏抽敭鍒ゆ柇

鎴戜細浠庢枃绔犱腑鎵句笁涓垽鏂紝鑰屼笉鏄笁涓钀芥爣棰樸€傚垽鏂€氬父闀胯繖鏍凤細

- 鍒楄〃鎺掑簭搴旇鐢卞悗绔粰鍑猴紝鍓嶇鍙仛灞曠ず鍜岃交閲忕瓫閫夈€?- Markdown 缂栬緫鍣ㄥ簲璇ヤ互婧愮爜涓哄噯锛岄瑙堝彧鏄覆鏌撶粨鏋溿€?- 鍥剧墖涓婁紶蹇呴』鏈夋湇鍔＄瀛樺偍鍜屾暟鎹簱璁板綍锛屼笉鑳藉彧渚濊禆鏈湴棰勮銆?
杩欎簺鍒ゆ柇鍙互鐩存帴杞垚鍗＄墖绛旀銆?
## 绗笁姝ワ細缁欐瘡寮犲崱鐗囧姞鍏抽敭璇?
鍏抽敭璇嶄笉鏄爣绛惧锛岃€屾槸甯姪鑷繁涓嬫鎵惧埌璁板繂璺緞銆傛瘮濡備竴寮犲崱鐗囧彲浠ュ啓锛?
```text
闂锛氫负浠€涔堢煡璇嗘祦鍒楄〃涓嶅簲璇ユ寜绔欑偣璇█闅愯棌甯栧瓙锛?绛旀锛氬洜涓虹敤鎴峰彂甯冪殑鍐呭浠ュ師濮嬭瑷€涓哄噯锛岀珯鐐硅瑷€鍙奖鍝嶇晫闈㈡枃妗堬紝涓嶅簲璇ユ敼鍙樼ぞ鍖哄唴瀹瑰彲瑙佹€с€?鍏抽敭璇嶏細original_language銆乴anguageCode銆佺煡璇嗘祦銆佸師鏂囧睍绀?```

## 绗洓姝ワ細瀹夋帓涓嬩竴娆″洖鐪?
澶嶄範鍗＄墖鏈€濂藉湪 24 灏忔椂鍐呯湅涓€娆°€傜涓€娆″洖鐪嬪彧闇€瑕佸垽鏂嚜宸辫兘涓嶈兘鍥炵瓟闂锛涚瓟涓嶄笂鏉ュ啀鎵撳紑鍘熸枃銆傝繖鏍峰涔犱細浠庘€滈噸鏂伴槄璇烩€濆彉鎴愨€滄鏌ヨ蹇嗏€濄€?
## 涓€涓畝鍗曟ā鏉?
```md
### 鍗＄墖鏍囬

- 闂锛?- 绠€鐭瓟妗堬細
- 鍏抽敭璇嶏細
- 闇€瑕佸洖鍒板師鏂囩殑浣嶇疆锛?```

## 灏忕粨

闀挎枃鐨勪环鍊间笉鍦ㄦ敹钘忓す閲岋紝鑰屽湪浣犺兘涓嶈兘鐢ㄨ嚜宸辩殑璇濆杩般€傛妸鏂囩珷鍙樻垚鍗＄墖锛屽氨鏄妸琚姩闃呰鏀规垚涓诲姩璁板繂銆?,
    'MARKDOWN',
    '澶嶄範鍗＄墖,瀛︿範鏂规硶,涓诲姩鍥炲繂',
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'zh_CN', 'PUBLISHED', 0, 11, 5, 2, 286, 87.10
FROM users u JOIN categories c ON c.category_code = 'BUSINESS'
WHERE u.username = 'li_minghao';
SET @post_content_ops = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_content_ops,
    'zh_CN',
    '瀛︿範绀惧尯涓轰粈涔堥渶瑕佸彲杩芥函鐨勫唴瀹硅繍钀?,
    '鍐呭骞冲彴涓嶅彧鏄彂甯栧垪琛紝瀹℃牳銆佽缃€佹棩蹇楀拰鏁版嵁鏉ユ簮閮借鑳借鍥㈤槦杩借釜銆?,
    '## 鍐呭杩愯惀鐨勬牳蹇冧笉鏄垹甯?
瀛︿範绀惧尯鐨勮繍钀ョ洰鏍囦笉鏄埗閫犵儹闂癸紝鑰屾槸璁╂湁浠峰€肩殑鍐呭琚湅鍒般€佽淇濆瓨銆佽缁х画璁ㄨ銆備负浜嗗仛鍒拌繖涓€鐐癸紝骞冲彴闇€瑕佺煡閬撳唴瀹逛粠鍝噷鏉ャ€佽皝鍙戝竷銆佽皝淇敼杩囥€佽鍝簺鐢ㄦ埛鏀惰棌銆佸摢浜涜璁哄甫鏉ヤ簡鏂扮殑瑙ｉ噴銆?
濡傛灉杩愯惀鍚庡彴鍙兘鐪嬪埌涓€涓潤鎬佸垪琛紝鍥㈤槦寰堥毦鍒ゆ柇涓€绡囨枃绔犱负浠€涔堝彉鐑紝涔熸棤娉曞揩閫熷鐞嗕綆璐ㄩ噺鍐呭銆?
## 鏈€灏戦渶瑕佽拷韪殑鍑犵被淇℃伅

鎴戣涓烘棭鏈熺郴缁熻嚦灏戣淇濈暀杩欎簺璁板綍锛?
- 鏂囩珷浣滆€呫€佸垎绫汇€佸師濮嬭瑷€銆佸彂甯冩椂闂淬€?- 鐐硅禐銆佹敹钘忋€佽瘎璁恒€侀槄璇荤瓑浜掑姩璁℃暟銆?- 涓炬姤鍘熷洜銆佸鐞嗙姸鎬併€佸鐞嗕汉鍜屽鐞嗘椂闂淬€?- AI 鎽樿銆佸涔犲崱鐗囥€佽闊虫湕璇荤瓑澶栭儴鏈嶅姟璋冪敤鏃ュ織銆?- 绠＄悊绔缃彉鏇磋褰曪紝灏ゅ叾鏄?API Key銆佹ā鍨嬪拰 Base URL銆?
杩欎簺淇℃伅涓嶆槸涓轰簡鍫嗗姛鑳斤紝鑰屾槸涓轰簡璁╁洟闃熻兘鍥炵瓟鈥滀负浠€涔堜細杩欐牱鈥濄€?
## 绠＄悊绔簲璇ュ厠鍒?
鍚庡彴涓嶉渶瑕佷竴寮€濮嬪氨鍋氭垚澶嶆潅 BI 绯荤粺銆傛洿瀹炵敤鐨勭涓€鐗堟槸锛?
1. 鑳界櫥褰曞苟璇嗗埆绠＄悊鍛樸€?2. 鑳芥煡鐪嬫湇鍔″仴搴风姸鎬併€?3. 鑳芥煡鐪嬪唴瀹瑰垪琛ㄥ拰璇︽儏銆?4. 鑳界淮鎶?AI 涓庤闊抽厤缃€?5. 鑳借褰曞叧閿搷浣溿€?
鍙杩欎簺閾捐矾鐪熷疄鎺ュ叆鏁版嵁搴擄紝鍚庣画澧炲姞瀹℃牳鍜岀敤鎴风鐞嗗氨涓嶄細鎺ㄥ€掗噸鏉ャ€?
## 鏁版嵁瑕佺湡瀹?
杩愯惀鐪嬫澘鏈€蹇岃涓轰簡濂界湅鍐欐鏁板瓧銆傚亣鏁版嵁浼氳鍥㈤槦璇垽绯荤粺鐘舵€侊紝涔熶細鎺╃洊鎺ュ彛闂銆傞椤靛彲浠ユ湁浜у搧浠嬬粛锛屼絾鎺у埗鍙伴噷鐨勬寚鏍囧簲璇ユ潵鑷湡瀹炴帴鍙ｏ紝鍝€曚竴寮€濮嬪彧鏈夋湇鍔＄姸鎬佸拰鍐呭鍒楄〃銆?
## 灏忕粨

鍙拷婧笉鏄ぇ鍏徃鎵嶉渶瑕佺殑娴佺▼锛岃€屾槸瀛︿範浜у搧淇濇寔淇′换鐨勫熀纭€銆傜敤鎴风浉淇″钩鍙帮紝鏄洜涓哄唴瀹广€佷簰鍔ㄥ拰绠＄悊鍔ㄤ綔閮借兘琚В閲婏紝鑰屼笉鏄洜涓洪〉闈笂鏈夋紓浜暟瀛椼€?,
    'MARKDOWN',
    '鍐呭杩愯惀,瀹¤鏃ュ織,绠＄悊绔?,
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'zh_CN', 'PUBLISHED', 0, 9, 6, 1, 244, 83.60
FROM users u JOIN categories c ON c.category_code = 'FINANCE'
WHERE u.username = 'wang_yu';
SET @post_cashflow = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_cashflow,
    'zh_CN',
    '缁欏垰寮€濮嬪伐浣滅殑浜虹殑鐜伴噾娴佺瑪璁?,
    '鍏堟妸鏀跺叆銆佸浐瀹氭敮鍑恒€佸脊鎬ф敮鍑哄拰搴旀€ラ噾鍒嗘竻锛屽啀璋堟姇璧勪細鏇寸ǔ銆?,
    '## 鍏堢湅鐜伴噾娴侊紝涓嶆€ョ潃璋堟敹鐩婄巼

鍒氬紑濮嬪伐浣滄椂锛屽緢澶氱悊璐㈠缓璁細鐩存帴璋堝熀閲戙€佽偂绁ㄥ拰鏀剁泭鐜囥€備絾鐪熸褰卞搷鐢熸椿绋冲畾鎬х殑锛屽線寰€鏄瘡涓湀鐨勯挶浠庡摢閲屾潵銆佹祦鍚戝摢閲屻€佷粈涔堟椂鍊欎細绱у紶銆傜幇閲戞祦娓呮涔嬪悗锛岀悊璐㈡墠鏈夊熀纭€銆?
## 鍥涗釜璐︽埛灏卞鐢?
鎴戝缓璁厛鎶婇挶鍒嗘垚鍥涚被锛?
| 绫诲埆 | 鐢ㄩ€?| 寤鸿 |
| --- | --- | --- |
| 鏃ュ父璐︽埛 | 鍚冮キ銆佷氦閫氥€佺敓娲荤敤鍝?| 姣忔湀鍥哄畾棰濆害 |
| 鍥哄畾鏀嚭 | 鎴跨銆佷繚闄┿€佽闃呫€佽繕娆?| 鍙戝伐璧勫悗鍏堢暀鍑?|
| 搴旀€ラ噾 | 鐢熺梾銆佹崲宸ヤ綔銆佺獊鍙戞敮鍑?| 閫愭鏀掑埌 3 鍒?6 涓湀鐢熸椿璐?|
| 闀挎湡璐︽埛 | 瀛︿範銆佹姇璧勩€侀暱鏈熺洰鏍?| 涓嶇敤鐭湡鐢熸椿閽辨壙鎷呴闄?|

鍒嗙被鐨勭洰鐨勪笉鏄檺鍒剁敓娲伙紝鑰屾槸閬垮厤鎵€鏈夋敮鍑烘贩鍦ㄤ竴璧凤紝鏈€鍚庝笉鐭ラ亾閽辫姳鍒颁簡鍝噷銆?
## 姣忔湀鍙褰曚笁浠朵簨

璁拌处涓嶉渶瑕佸鏉傘€傛瘡涓湀鍥哄畾鐪嬩笁浠朵簨锛?
1. 鍥哄畾鏀嚭鍗犳敹鍏ュ灏戙€?2. 寮规€ф敮鍑烘湁娌℃湁鏄庢樉瓒呭嚭棰勬湡銆?3. 搴旀€ラ噾鏄惁姣斾笂涓湀澧炲姞銆?
鍙杩欎笁椤圭ǔ瀹氾紝鐜伴噾娴佸氨涓嶄細澶樊銆?
## 鎶曡祫鍓嶇殑妫€鏌?
鍦ㄨ€冭檻鎶曡祫涔嬪墠锛屽厛闂嚜宸憋細

- 鏄惁杩樻湁楂樺埄鐜囪礋鍊猴紵
- 搴旀€ラ噾鏄惁澶熶笁涓湀锛?- 鎶曡祫鐨勯挶鏄惁涓夊勾鍐呬笉鐢紵
- 鏄惁鐞嗚В鏈€鍧忔儏鍐典笅浼氫簭澶氬皯锛?
濡傛灉杩欎簺闂绛斾笉涓婃潵锛屽厛瀛︿範鍜屾暣鐞嗙幇閲戞祦锛屾瘮鎬ョ潃涔颁骇鍝佹洿閲嶈銆?
## 灏忕粨

鍒氬伐浣滅殑浜烘渶闇€瑕佺殑涓嶆槸澶嶆潅妯″瀷锛岃€屾槸绋冲畾鎰熴€傛妸鐜伴噾娴佺湅娓呮锛岀煡閬撹嚜宸辨瘡涓湀鑳芥壙鎷呬粈涔堬紝鍐嶅幓瀛︿範鎶曡祫锛屼細灏戝緢澶氱劍铏戙€?,
    'MARKDOWN',
    '鐜伴噾娴?搴旀€ラ噾,涓汉璐㈠姟',
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'en_US', 'PUBLISHED', 0, 10, 6, 1, 218, 81.30
FROM users u JOIN categories c ON c.category_code = 'CAREER'
WHERE u.username = 'noah_kim';
SET @post_interview_log = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_interview_log,
    'en_US',
    'How I Prepare a Technical Interview Study Log',
    'A useful interview log records decisions, mistakes, source links, and the next question to practice.',
    '## Why a study log matters

Technical interview preparation can become a pile of links very quickly. I used to save algorithm notes, backend articles, and system design diagrams without recording what I had actually learned from them. A study log fixed that problem because every session ended with a clear artifact.

## The structure I use

Each entry has five sections:

```md
## Topic

## What I understood

## Mistakes I made

## Source links

## Next practice question
```

The structure is simple on purpose. If the template is too long, I will avoid writing it after a hard practice session.

## Record mistakes directly

The most valuable part is the mistake section. A mistake might be:

- I explained caching before clarifying the read/write ratio.
- I used a data structure because it was familiar, not because it fit the operation.
- I forgot to mention failure modes in an API design.

These notes are more useful than copying the correct answer because they show what I need to watch next time.

## Connect notes to review cards

After writing the log, I turn the hardest point into one review card. For example:

> Question: When should a feed service use cursor pagination instead of page numbers?

The answer does not need to be long. It only needs to trigger the reasoning path: stable ordering, new items arriving, duplicate avoidance, and database index usage.

## Weekly review

Every Friday I scan the logs and mark repeated patterns. If the same mistake appears three times, it becomes next week focus. This makes preparation feel less random.

## Final thought

A study log is not a diary. It is a feedback system. The goal is to make the next practice session sharper than the last one.',
    'MARKDOWN',
    'Career,Interview,Study Log',
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'en_US', 'PUBLISHED', 0, 8, 5, 1, 196, 78.90
FROM users u JOIN categories c ON c.category_code = 'PRODUCTIVITY'
WHERE u.username = 'emma_clark';
SET @post_weekly_review = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_weekly_review,
    'en_US',
    'A Weekly Review System for Learning Projects',
    'A weekly review helps turn scattered reading, code experiments, and questions into a clear next plan.',
    '## The problem with scattered learning

Learning projects often spread across browser tabs, code branches, notebooks, and chat messages. By the end of the week, it can be hard to tell what actually changed. A weekly review gives the work a visible shape.

## Three questions

I use three questions every Sunday:

1. What did I understand better this week?
2. What still feels confusing?
3. What will I do first next week?

The first question prevents progress from disappearing. The second question keeps unresolved topics visible. The third question lowers the cost of restarting.

## Evidence, not impressions

For each answer I link to evidence: an article I finished, a pull request, a Markdown note, a discussion thread, or a review card. This keeps the review honest. If I cannot point to evidence, I rewrite the statement as an intention instead of pretending it is progress.

## A short review template

```md
## This week

- Finished:
- Built:
- Asked:

## Still unclear

- 

## Next Monday

- First task:
- Reading:
- Practice:
```

## Keep it small

The weekly review should take less than twenty minutes. If it becomes a reporting ritual, it will not last. The value comes from noticing patterns and choosing the next move.

## Result

After a month, the reviews become a map of learning decisions. They show which topics repeat, which notes became useful, and which questions deserve deeper work.',
    'MARKDOWN',
    'Weekly Review,Productivity,Learning System',
    'ORIGINAL'
);

INSERT INTO posts (author_id, category_id, original_language, status, featured, like_count, favorite_count, comment_count, view_count, hot_score)
SELECT u.user_id, c.category_id, 'zh_CN', 'PUBLISHED', 0, 12, 7, 2, 304, 86.40
FROM users u JOIN categories c ON c.category_code = 'TECHNOLOGY'
WHERE u.username = 'li_minghao';
SET @post_service_layer = LAST_INSERT_ID();

INSERT INTO post_i18n (post_id, language_code, title, summary, content, content_format, ai_tags, source_type)
VALUES (
    @post_service_layer,
    'zh_CN',
    'Spring MVC + MyBatis 椤圭洰閲岋紝Service 灞傚簲璇ユ壙鎷呬粈涔?,
    'Controller 淇濇寔杞婚噺锛孧apper 涓撴敞 SQL锛孲ervice 灞傝礋璐ｄ笟鍔¤鍒欍€佷簨鍔″拰璺ㄦā鍧楀崗璋冦€?,
    '## Service 灞備笉鏄浆鍙戝櫒

鍦?Spring MVC + MyBatis 椤圭洰閲岋紝鏈€甯歌鐨勯棶棰樻槸 Service 鍙仛涓€琛岃皟鐢細Controller 璋?Service锛孲ervice 鍘熸牱璋?Mapper銆傝繖鏍疯櫧鐒惰兘璺戯紝浣嗕笟鍔¤鍒欎細鎱㈡參鏁ｈ惤鍒?Controller銆丮apper XML 鍜屽墠绔噷銆?
Service 灞傜湡姝ｅ簲璇ユ壙鎷呬笁浠朵簨锛氫笟鍔″垽鏂€佷簨鍔¤竟鐣屻€佽法妯″潡鍗忚皟銆?
## Controller 鍙鐞?Web 璇箟

Controller 閫傚悎鍋氾細

- 鎺ユ敹璺緞鍙傛暟銆佹煡璇㈠弬鏁板拰璇锋眰浣撱€?- 璇诲彇鐧诲綍鐢ㄦ埛銆?- 璋冪敤 Service銆?- 杩斿洖缁熶竴 JSON銆?
瀹冧笉閫傚悎鍐欏鏉備笟鍔¤鍒欍€備緥濡傗€滃彂甯栨椂瑕佹牎楠屽垎绫绘槸鍚﹀瓨鍦ㄣ€佸皝闈㈠湴鍧€鏄惁鍚堟硶銆佹鏂囨牸寮忔槸浠€涔堚€濓紝杩欎簺搴旇鍦ㄥ唴瀹?Service 涓畬鎴愩€?
## Mapper 鍙鐞嗘暟鎹闂?
MyBatis Mapper 搴旇璁?SQL 娓呮櫚鍙帶锛屼絾涓嶈鎶婁笟鍔″垽鏂杩?XML銆俋ML 鍙互璐熻矗鏌ヨ鏉′欢銆佹帓搴忋€佸垎椤靛拰瀛楁鏄犲皠锛涙槸鍚﹀厑璁告搷浣溿€佹搷浣滃悗瑕佸啓鍝簺鍏宠仈璁板綍锛屽簲璇ョ敱 Service 鍐冲畾銆?
## 浜嬪姟杈圭晫鏀惧湪 Service

鍙戝竷鏂囩珷鏃惰嚦灏戜細鍐欎袱寮犺〃锛?
1. `posts`锛氭枃绔犺仛鍚堜俊鎭€?2. `post_i18n`锛氭爣棰樸€佹憳瑕併€佹鏂囧拰璇█銆?
杩欎袱涓啓鍏ュ繀椤诲湪涓€涓簨鍔￠噷銆傚惁鍒欎富琛ㄥ啓鎴愬姛銆佹鏂囧啓澶辫触锛屽垪琛ㄥ氨浼氬嚭鐜版墦涓嶅紑鐨勬枃绔犮€俙@Transactional` 鏀惧湪 Service 鏂规硶涓婏紝姣旀斁鍦?Controller 鎴?Mapper 涓婃洿绗﹀悎涓氬姟杈圭晫銆?
## 璺ㄦā鍧楀崗璋?
褰撶敤鎴锋敹钘忔枃绔犳椂锛屼簰鍔ㄦā鍧楄鍐欐敹钘忚〃锛屽唴瀹规ā鍧楄鏇存柊鏀惰棌鏁般€傝繖閲屼笉鑳借 Controller 鍚屾椂璋冧袱涓?Mapper銆傛洿濂界殑鍋氭硶鏄簰鍔?Service 娉ㄥ叆鍐呭妯″潡鍏紑鐨?Mapper 鎴?Service锛屽湪涓€涓笟鍔℃柟娉曚腑瀹屾垚銆?
## 灏忕粨

Service 灞傚啓寰楀ソ锛岄」鐩細鏇村儚涓€涓骇鍝侊紱Service 灞傚彧鏄浆鍙戝櫒锛岄」鐩緢蹇細鍙樻垚鎺ュ彛鍜?SQL 鐨勫爢鍙犮€傛妸涓氬姟瑙勫垯闆嗕腑鍦?Service锛屾槸妯″潡鍖栧崟浣撹兘闀挎湡缁存姢鐨勫叧閿€?,
    'MARKDOWN',
    'Spring MVC,Service,MyBatis,浜嬪姟',
    'ORIGINAL'
);

INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_vue_state, u.user_id, 'zh_CN', '鐘舵€佸垎灞傝繖涓€娈靛緢瀹炵敤锛屽挨鍏舵槸鎶婅姹傜姸鎬佸拰瑙嗗浘鐘舵€佸垎寮€涔嬪悗锛岄〉闈㈤棶棰樼‘瀹炴洿濂藉畾浣嶃€?
FROM users u WHERE u.username = 'li_minghao';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_vue_state, u.user_id, 'zh_CN', '鎴戜箣鍓嶆妸鎼滅储璇嶅拰鎺ュ彛缁撴灉娣峰湪涓€涓?store 閲岋紝鍚庨潰鍒嗛〉寰堥毦澶勭悊锛岃繖绡囩粰浜嗘垜涓€涓皟鏁存柟鍚戙€?
FROM users u WHERE u.username = 'zhao_yiran';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_markdown_composer, u.user_id, 'en_US', 'Keeping Markdown as the source of truth is the part that makes the editor feel safe for long-form notes.'
FROM users u WHERE u.username = 'noah_kim';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_markdown_composer, u.user_id, 'en_US', 'The image upload flow is clear. I would also add a small image reuse library later.'
FROM users u WHERE u.username = 'emma_clark';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_review_cards, u.user_id, 'zh_CN', '鎶婂崱鐗囬棶棰樺啓鍏蜂綋杩欎竴鐐瑰緢鍏抽敭锛岄棶棰樺お娉涚殑鏃跺€欏涔犲氨鍙樻垚閲嶆柊闃呰銆?
FROM users u WHERE u.username = 'chen_jiayi';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_review_cards, u.user_id, 'zh_CN', '杩欎釜妯℃澘鍙互鐩存帴鏀惧埌鎴戠殑瀛︿範椤甸噷锛屽悗闈㈢敓鎴愬崱鐗囨椂涔熻兘鍙傝€冦€?
FROM users u WHERE u.username = 'wang_yu';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_content_ops, u.user_id, 'zh_CN', '杩愯惀鍚庡彴涓嶅啓鍋囨寚鏍囪繖涓€鐐硅禐鍚岋紝瀹佸彲灏戝睍绀猴紝涔熶笉瑕佸睍绀轰笉鑳借В閲婄殑鏁版嵁銆?
FROM users u WHERE u.username = 'chen_jiayi';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_content_ops, u.user_id, 'zh_CN', '鍚庣画濡傛灉鍔犲鏍搁槦鍒楋紝鍙互鎶婁妇鎶ュ拰 AI 椋庨櫓寤鸿鏀惧湪鍚屼竴涓鐞嗛〉銆?
FROM users u WHERE u.username = 'ops_admin';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_cashflow, u.user_id, 'zh_CN', '鍥涗釜璐︽埛鐨勬媶娉曞緢瀹规槗鎵ц锛屾瘮涓€寮€濮嬪氨鍒嗙被鍑犲崄涓秷璐归」杞诲緢澶氥€?
FROM users u WHERE u.username = 'zhao_yiran';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_interview_log, u.user_id, 'en_US', 'The mistake section is exactly what I usually skip. I will try making it mandatory in my next practice round.'
FROM users u WHERE u.username = 'emma_clark';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_weekly_review, u.user_id, 'en_US', 'Evidence-based review is a useful framing. It keeps the weekly note from becoming a vague status update.'
FROM users u WHERE u.username = 'noah_kim';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_service_layer, u.user_id, 'zh_CN', '浜嬪姟杈圭晫杩欐瑙ｉ噴寰楀緢娓呮锛屽彂甯冩枃绔犲啓涓ゅ紶琛ㄥ氨鏄緢濂界殑渚嬪瓙銆?
FROM users u WHERE u.username = 'chen_jiayi';
INSERT INTO comments (post_id, user_id, language_code, content)
SELECT @post_service_layer, u.user_id, 'zh_CN', 'Service 涓嶆槸杞彂鍣紝杩欏彞璇濆彲浠ヨ创鍦ㄤ唬鐮佽瘎瀹℃竻鍗曢噷銆?
FROM users u WHERE u.username = 'wang_yu';

UPDATE comments c
JOIN (
    SELECT
        comment_id,
        ROW_NUMBER() OVER (PARTITION BY post_id ORDER BY created_time ASC, comment_id ASC) AS floor_no
    FROM comments
) ranked ON ranked.comment_id = c.comment_id
SET c.floor_no = ranked.floor_no
WHERE c.floor_no = 0;

INSERT INTO post_likes (post_id, user_id)
SELECT p.post_id, u.user_id
FROM (
    SELECT @post_vue_state AS post_id UNION ALL SELECT @post_markdown_composer UNION ALL SELECT @post_review_cards UNION ALL
    SELECT @post_content_ops UNION ALL SELECT @post_cashflow UNION ALL SELECT @post_interview_log UNION ALL
    SELECT @post_weekly_review UNION ALL SELECT @post_service_layer
) p
JOIN users u ON u.username IN ('chen_jiayi', 'li_minghao', 'zhao_yiran', 'wang_yu', 'emma_clark', 'noah_kim')
WHERE NOT (p.post_id = @post_vue_state AND u.username = 'chen_jiayi')
  AND NOT (p.post_id = @post_markdown_composer AND u.username = 'emma_clark')
  AND NOT (p.post_id = @post_review_cards AND u.username = 'zhao_yiran')
  AND NOT (p.post_id = @post_content_ops AND u.username = 'li_minghao')
  AND NOT (p.post_id = @post_cashflow AND u.username = 'wang_yu')
  AND NOT (p.post_id = @post_interview_log AND u.username = 'noah_kim')
  AND NOT (p.post_id = @post_weekly_review AND u.username = 'emma_clark')
  AND NOT (p.post_id = @post_service_layer AND u.username = 'li_minghao')
LIMIT 42;

INSERT INTO post_favorites (post_id, user_id)
SELECT @post_vue_state, u.user_id FROM users u WHERE u.username IN ('li_minghao', 'zhao_yiran', 'emma_clark')
UNION ALL SELECT @post_markdown_composer, u.user_id FROM users u WHERE u.username IN ('chen_jiayi', 'noah_kim', 'ops_admin')
UNION ALL SELECT @post_review_cards, u.user_id FROM users u WHERE u.username IN ('chen_jiayi', 'wang_yu')
UNION ALL SELECT @post_content_ops, u.user_id FROM users u WHERE u.username IN ('ops_admin', 'chen_jiayi')
UNION ALL SELECT @post_cashflow, u.user_id FROM users u WHERE u.username IN ('zhao_yiran', 'li_minghao')
UNION ALL SELECT @post_interview_log, u.user_id FROM users u WHERE u.username IN ('emma_clark', 'chen_jiayi')
UNION ALL SELECT @post_weekly_review, u.user_id FROM users u WHERE u.username IN ('noah_kim', 'zhao_yiran')
UNION ALL SELECT @post_service_layer, u.user_id FROM users u WHERE u.username IN ('chen_jiayi', 'wang_yu', 'ops_admin');

INSERT INTO favorite_collections (user_id, name, description, visibility, sort_no)
SELECT user_id, '榛樿鏀惰棌', '涓存椂淇濆瓨锛岀◢鍚庡啀鏁寸悊銆?, 'PRIVATE', 0
FROM users
UNION ALL
SELECT user_id, '鍓嶅悗绔」鐩?, 'Vue銆丼pring MVC銆丮yBatis 鍜屽伐绋嬪疄璺垫枃绔犮€?, 'PUBLIC', 10
FROM users
WHERE username IN ('chen_jiayi', 'wang_yu', 'emma_clark', 'ops_admin')
UNION ALL
SELECT user_id, '瀛︿範澶嶇洏', '閫傚悎杞垚澶嶄範鍗＄墖鍜屽懆璁″垝鐨勫唴瀹广€?, 'PUBLIC', 20
FROM users
WHERE username IN ('chen_jiayi', 'zhao_yiran', 'noah_kim')
UNION ALL
SELECT user_id, '绀惧尯杩愯惀', '鍐呭娌荤悊銆佸鏍稿拰鐭ヨ瘑绀惧尯杩愯惀璧勬枡銆?, 'PUBLIC', 30
FROM users
WHERE username IN ('li_minghao', 'ops_admin')
UNION ALL
SELECT user_id, '鑱屼笟鎴愰暱', '闈㈣瘯銆佸伐浣滆褰曞拰闀挎湡鑳藉姏寤鸿銆?, 'PUBLIC', 40
FROM users
WHERE username IN ('emma_clark', 'noah_kim');

INSERT INTO favorite_collection_items (collection_id, post_id, user_id, created_time)
SELECT fc.collection_id, @post_markdown_composer, fc.user_id, NOW() - INTERVAL 9 DAY
FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id
WHERE u.username = 'chen_jiayi' AND fc.name = '鍓嶅悗绔」鐩?
UNION ALL SELECT fc.collection_id, @post_service_layer, fc.user_id, NOW() - INTERVAL 8 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'chen_jiayi' AND fc.name = '鍓嶅悗绔」鐩?
UNION ALL SELECT fc.collection_id, @post_review_cards, fc.user_id, NOW() - INTERVAL 7 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'chen_jiayi' AND fc.name = '瀛︿範澶嶇洏'
UNION ALL SELECT fc.collection_id, @post_interview_log, fc.user_id, NOW() - INTERVAL 6 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'chen_jiayi' AND fc.name = '瀛︿範澶嶇洏'
UNION ALL SELECT fc.collection_id, @post_content_ops, fc.user_id, NOW() - INTERVAL 5 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'li_minghao' AND fc.name = '绀惧尯杩愯惀'
UNION ALL SELECT fc.collection_id, @post_vue_state, fc.user_id, NOW() - INTERVAL 4 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'li_minghao' AND fc.name = '榛樿鏀惰棌'
UNION ALL SELECT fc.collection_id, @post_cashflow, fc.user_id, NOW() - INTERVAL 3 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'zhao_yiran' AND fc.name = '瀛︿範澶嶇洏'
UNION ALL SELECT fc.collection_id, @post_weekly_review, fc.user_id, NOW() - INTERVAL 2 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'zhao_yiran' AND fc.name = '瀛︿範澶嶇洏'
UNION ALL SELECT fc.collection_id, @post_review_cards, fc.user_id, NOW() - INTERVAL 2 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'wang_yu' AND fc.name = '榛樿鏀惰棌'
UNION ALL SELECT fc.collection_id, @post_service_layer, fc.user_id, NOW() - INTERVAL 1 DAY FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'wang_yu' AND fc.name = '鍓嶅悗绔」鐩?
UNION ALL SELECT fc.collection_id, @post_vue_state, fc.user_id, NOW() - INTERVAL 12 HOUR FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'emma_clark' AND fc.name = '鍓嶅悗绔」鐩?
UNION ALL SELECT fc.collection_id, @post_interview_log, fc.user_id, NOW() - INTERVAL 10 HOUR FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'emma_clark' AND fc.name = '鑱屼笟鎴愰暱'
UNION ALL SELECT fc.collection_id, @post_markdown_composer, fc.user_id, NOW() - INTERVAL 8 HOUR FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'noah_kim' AND fc.name = '榛樿鏀惰棌'
UNION ALL SELECT fc.collection_id, @post_weekly_review, fc.user_id, NOW() - INTERVAL 6 HOUR FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'noah_kim' AND fc.name = '瀛︿範澶嶇洏'
UNION ALL SELECT fc.collection_id, @post_service_layer, fc.user_id, NOW() - INTERVAL 5 HOUR FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'ops_admin' AND fc.name = '鍓嶅悗绔」鐩?
UNION ALL SELECT fc.collection_id, @post_content_ops, fc.user_id, NOW() - INTERVAL 4 HOUR FROM favorite_collections fc JOIN users u ON u.user_id = fc.user_id WHERE u.username = 'ops_admin' AND fc.name = '绀惧尯杩愯惀';

INSERT INTO post_view_history (post_id, user_id, view_time)
SELECT @post_vue_state, u.user_id, NOW() - INTERVAL 7 HOUR FROM users u WHERE u.username = 'li_minghao'
UNION ALL SELECT @post_markdown_composer, u.user_id, NOW() - INTERVAL 6 HOUR FROM users u WHERE u.username = 'chen_jiayi'
UNION ALL SELECT @post_review_cards, u.user_id, NOW() - INTERVAL 5 HOUR FROM users u WHERE u.username = 'wang_yu'
UNION ALL SELECT @post_content_ops, u.user_id, NOW() - INTERVAL 4 HOUR FROM users u WHERE u.username = 'ops_admin'
UNION ALL SELECT @post_cashflow, u.user_id, NOW() - INTERVAL 3 HOUR FROM users u WHERE u.username = 'zhao_yiran'
UNION ALL SELECT @post_interview_log, u.user_id, NOW() - INTERVAL 2 HOUR FROM users u WHERE u.username = 'emma_clark'
UNION ALL SELECT @post_weekly_review, u.user_id, NOW() - INTERVAL 1 HOUR FROM users u WHERE u.username = 'noah_kim'
UNION ALL SELECT @post_service_layer, u.user_id, NOW() - INTERVAL 30 MINUTE FROM users u WHERE u.username = 'chen_jiayi';

UPDATE posts p
LEFT JOIN (SELECT post_id, COUNT(*) AS cnt FROM comments WHERE status = 'VISIBLE' GROUP BY post_id) c ON c.post_id = p.post_id
LEFT JOIN (SELECT post_id, COUNT(*) AS cnt FROM post_likes GROUP BY post_id) l ON l.post_id = p.post_id
LEFT JOIN (SELECT post_id, COUNT(*) AS cnt FROM post_favorites GROUP BY post_id) f ON f.post_id = p.post_id
SET p.comment_count = COALESCE(c.cnt, 0),
    p.like_count = COALESCE(l.cnt, 0),
    p.favorite_count = COALESCE(f.cnt, 0);

INSERT INTO reports (
    post_id,
    reporter_id,
    reason,
    status,
    ai_risk_level,
    ai_suggestion,
    processed_by,
    processed_time,
    created_time
)
SELECT @post_content_ops,
       reporter.user_id,
       '鏂囩珷閲屾彁鍒扮殑杩愯惀鎸囨爣姣旇緝缁濆锛屾媴蹇冩柊鐢ㄦ埛浼氭妸瀹冨綋鎴愬浐瀹氭壙璇猴紝甯屾湜绠＄悊鍛樼‘璁よ〃杈炬槸鍚﹂渶瑕佽皟鏁淬€?,
       'PENDING',
       'MEDIUM',
       '寤鸿鏍稿姝ｆ枃涓殑鎸囨爣鎻忚堪銆傝嫢灞炰簬缁忛獙鍒嗕韩锛屽彲浠ヤ繚鐣欙紱鑻ュ儚骞冲彴鎵胯锛屽缓璁姹備綔鑰呮敼鍐欍€?,
       NULL,
       NULL,
       NOW() - INTERVAL 5 HOUR
FROM users reporter
WHERE reporter.username = 'wang_yu'
UNION ALL
SELECT @post_cashflow,
       reporter.user_id,
       '杩欑瘒鐜伴噾娴佹枃绔犳湁涓嶅皯涓汉鐞嗚储寤鸿锛屽笇鏈涜ˉ鍏呴闄╂彁绀猴紝閬垮厤璇昏€呯洿鎺ョ収鎼€?,
       'PENDING',
       'MEDIUM',
       '寤鸿瀹℃牳鏄惁鍖呭惈鏄庣‘鏀剁泭鎵胯銆傝嫢娌℃湁杩濊锛屽彲浠ヤ繚鐣欏苟鎻愮ず浣滆€呰ˉ鍏呭厤璐ｅ０鏄庛€?,
       NULL,
       NULL,
       NOW() - INTERVAL 3 HOUR
FROM users reporter
WHERE reporter.username = 'emma_clark'
UNION ALL
SELECT @post_markdown_composer,
       reporter.user_id,
       '鎴戜互涓虹ず渚嬮噷鐨勭紪杈戝櫒閾炬帴浼氳烦鍒板閮ㄥ箍鍛婏紝澶嶆牳鍚庡彂鐜板彧鏄骇鍝佸伐鍏疯鏄庛€?,
       'REJECTED',
       'LOW',
       '涓炬姤鐞嗙敱椋庨櫓杈冧綆锛屾鏂囨湭鍙戠幇骞垮憡瀵兼祦銆傚缓璁┏鍥炲苟淇濈暀鏂囩珷銆?,
       admin.user_id,
       NOW() - INTERVAL 1 DAY,
       NOW() - INTERVAL 2 DAY
FROM users reporter
JOIN users admin ON admin.username = 'ops_admin'
WHERE reporter.username = 'li_minghao';

INSERT INTO help_requests (user_id, title, description, category_id, status, reward_points)
SELECT u.user_id,
       'Markdown 缂栬緫鍣ㄧ矘璐村浘鐗囧悗锛屾€庢牱閬垮厤涓婁紶閲嶅鏂囦欢锛?,
       '鎴戝湪瀹炵幇绮樿创鍥剧墖鑷姩涓婁紶鏃跺彂鐜帮紝鍚屼竴寮犲浘鐗囪繛缁矘璐翠細鐢熸垚澶氫釜鏂囦欢璁板綍銆傜幇鍦ㄦ兂鐭ラ亾鍓嶇搴旇鍋氬搱甯屽幓閲嶏紝杩樻槸鍚庣鏍规嵁鏂囦欢鍐呭鍘婚噸鏇村悎閫傦紵甯屾湜鑳藉吋椤惧疄鐜版垚鏈拰鏁版嵁涓€鑷存€с€?,
       c.category_id,
       'OPEN',
       30
FROM users u JOIN categories c ON c.category_code = 'TECHNOLOGY'
WHERE u.username = 'chen_jiayi';
SET @help_image_dedupe = LAST_INSERT_ID();

INSERT INTO help_requests (user_id, title, description, category_id, status, reward_points)
SELECT u.user_id,
       '澶嶄範鍗＄墖搴旇鎸夋枃绔犱繚瀛橈紝杩樻槸鎸夌敤鎴蜂繚瀛橈紵',
       '濡傛灉鍚屼竴绡囨枃绔犺寰堝鐢ㄦ埛鐢熸垚澶嶄範鍗＄墖锛屽崱鐗囧唴瀹瑰彲鑳藉拰鐢ㄦ埛鐨勫涔犵洰鏍囨湁鍏炽€傛兂璁ㄨ涓€涓嬫暟鎹簱璁捐锛氬涔犲崱鐗囨槸鎸傚湪鏂囩珷涓嬮潰鍏变韩锛岃繕鏄寕鍦ㄧ敤鎴蜂笅闈綔涓轰釜浜哄涔犺褰曪紵',
       c.category_id,
       'OPEN',
       20
FROM users u JOIN categories c ON c.category_code = 'PRODUCTIVITY'
WHERE u.username = 'zhao_yiran';
SET @help_review_card_scope = LAST_INSERT_ID();

INSERT INTO help_requests (user_id, title, description, category_id, status, reward_points)
SELECT u.user_id,
       'How should the feed handle mixed-language search?',
       'The feed now shows posts in their original language. I am trying to decide whether keyword search should match only the visible original text or also use AI-generated tags across languages. I would like a practical first version.',
       c.category_id,
       'OPEN',
       25
FROM users u JOIN categories c ON c.category_code = 'TECHNOLOGY'
WHERE u.username = 'emma_clark';
SET @help_mixed_search = LAST_INSERT_ID();

INSERT INTO help_answers (help_id, user_id, content, is_accepted)
SELECT @help_image_dedupe, u.user_id,
       '绗竴鐗堝缓璁厛鍦ㄥ悗绔仛鍐呭鍝堝笇銆傚墠绔矘璐村拰鎷栨斁閮藉彲鑳界粫杩囧悓涓€娈甸€昏緫锛屽悗绔牴鎹枃浠?bytes 璁＄畻 hash 鍚庡啀鍐冲畾澶嶇敤鎴栨柊寤鸿褰曪紝浼氭洿涓€鑷淬€傚墠绔彲浠ュ彧鍋氫笂浼犱腑鐨勪复鏃跺幓閲嶏紝閬垮厤鐢ㄦ埛杩炵画瑙﹀彂涓ゆ銆?,
       0
FROM users u WHERE u.username = 'li_minghao';

INSERT INTO help_answers (help_id, user_id, content, is_accepted)
SELECT @help_review_card_scope, u.user_id,
       '寤鸿鎸夌敤鎴蜂繚瀛樸€傚涔犲崱鐗囨槸涓汉鐞嗚В鍜岃蹇嗚矾寰勶紝搴旇鎸傚湪 user_id 涓嬶紱鏂囩珷绾у埆鍙互缂撳瓨 AI 鎽樿锛屼絾鍗＄墖鏈€濂藉厑璁哥敤鎴风紪杈戝拰淇濈暀鑷繁鐨勭増鏈€?,
       1
FROM users u WHERE u.username = 'chen_jiayi';

INSERT INTO help_answers (help_id, user_id, content, is_accepted)
SELECT @help_mixed_search, u.user_id,
       'For the first version, search the original visible text and tags only. Cross-language semantic search can come later after tags and embeddings are trustworthy. This keeps the feed behavior easy to explain.',
       0
FROM users u WHERE u.username = 'noah_kim';

UPDATE help_answers a
JOIN (
    SELECT
        answer_id,
        ROW_NUMBER() OVER (PARTITION BY help_id ORDER BY created_time ASC, answer_id ASC) AS floor_no
    FROM help_answers
) ranked ON ranked.answer_id = a.answer_id
SET a.floor_no = ranked.floor_no
WHERE a.floor_no = 0;

INSERT INTO admin_audit_logs (admin_id, target_type, target_id, action_type, remark)
SELECT u.user_id, 'integration_settings', 0, 'UPDATE_INTEGRATION_SETTINGS', 'AI and voice provider settings initialized for the local StudyForge environment.'
FROM users u WHERE u.username = 'ops_admin';

