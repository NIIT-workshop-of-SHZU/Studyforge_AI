# AI Token 计费系统实现说明

## 📋 概述

实现了基于真实 API 返回的 Token 使用情况的计费系统，完全符合硅基流动（SiliconFlow）等 OpenAI 兼容 API 的标准计费方式。

## ✨ 核心特性

1. **零侵入设计** - 不修改现有的 `SiliconFlowAiServiceImpl` 代码
2. **真实 Token 统计** - 直接从 API 响应的 `usage` 字段提取
3. **自动费用计算** - 根据模型价格配置自动计算每次调用的费用
4. **灵活扩展** - 支持多种模型和价格配置

## 🏗️ 架构设计

### 新增组件

#### 1. 数据库层
- **文件**: `sql/006_ai_token_billing.sql`
- **功能**:
  - 为 `ai_logs` 表添加 token 统计字段
  - 创建 `ai_model_pricing` 价格配置表
  
**新增字段**:
- `model_name` - 使用的模型名称
- `prompt_tokens` - 输入 token 数
- `completion_tokens` - 输出 token 数
- `total_tokens` - 总 token 数
- `cost_yuan` - 费用（元）

**价格配置表**:
```sql
CREATE TABLE ai_model_pricing (
    pricing_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    prompt_price_per_1k DECIMAL(10, 6) NOT NULL COMMENT '输入token价格（元/千tokens）',
    completion_price_per_1k DECIMAL(10, 6) NOT NULL COMMENT '输出token价格（元/千tokens）',
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币单位',
    effective_date DATE NOT NULL COMMENT '价格生效日期',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_model_pricing_model_date (model_name, effective_date),
    KEY idx_ai_model_pricing_active (is_active, effective_date DESC)
) ENGINE=InnoDB COMMENT='AI模型价格配置表';
```

#### 2. 实体层
- **文件**: `studyforge-ai/src/main/java/com/studyforge/ai/entity/AiLog.java`
- **修改**: 添加 token 相关字段的 getter/setter

#### 3. 服务层（两个独立组件）

##### AiTokenBillingService
- **文件**: `studyforge-ai/src/main/java/com/studyforge/ai/service/AiTokenBillingService.java`
- **职责**: 
  - 调用 AI API 并获取完整响应（包含 usage 信息）
  - 从响应中提取文本内容
  - 提供当前使用的模型名称

##### AiTokenUsageLogger
- **文件**: `studyforge-ai/src/main/java/com/studyforge/ai/service/AiTokenUsageLogger.java`
- **职责**:
  - 解析 API 响应中的 usage 字段
  - 根据价格配置计算费用
  - 更新 ai_logs 表的 token 统计字段

#### 4. Controller 层
- **文件**: `studyforge-webapi/src/main/java/com/studyforge/webapi/ai/AiController.java`
- **修改**:
  - 注入 `AiTokenBillingService` 和 `AiTokenUsageLogger`
  - 修改各接口方法，使用计费服务调用 API
  - 在调用后记录 token 使用情况

## 💰 计费公式

```
费用 = (prompt_tokens / 1000) × input_price + (completion_tokens / 1000) × output_price
```

**示例**（DeepSeek-V4-Flash）:
- 输入价格：¥0.002 / 1K tokens
- 输出价格：¥0.008 / 1K tokens
- 如果 prompt_tokens=100, completion_tokens=200：
  ```
  费用 = (100/1000) × 0.002 + (200/1000) × 0.008
       = 0.0002 + 0.0016
       = ¥0.0018
  ```

## 📊 支持的模型及价格

| 模型名称 | 输入价格（元/1K） | 输出价格（元/1K） |
|---------|-----------------|-----------------|
| deepseek-ai/DeepSeek-V4-Flash | ¥0.002 | ¥0.008 |
| deepseek-ai/DeepSeek-V3 | ¥0.002 | ¥0.008 |
| deepseek-ai/DeepSeek-R1 | ¥0.004 | ¥0.016 |
| google/gemma-2-9b-it | ¥0.001 | ¥0.001 |
| meta-llama/Llama-3.3-70B-Instruct | ¥0.004 | ¥0.016 |
| Qwen/Qwen2.5-72B-Instruct | ¥0.004 | ¥0.016 |
| THUDM/glm-4-9b-chat | ¥0.001 | ¥0.001 |

## 🚀 部署步骤

### 1. 执行数据库脚本

```powershell
# 执行 token 计费配置脚本（会提示输入密码）
mysql -u root -p studyforge_ai < sql\006_ai_token_billing.sql

# 验证表结构
mysql -u root -p studyforge_ai -e "DESCRIBE ai_logs;"
mysql -u root -p studyforge_ai -e "SELECT * FROM ai_model_pricing;"
```

### 2. 重新编译项目

```bash
cd studyforge-server
mvn clean package -DskipTests
```

### 3. 重启应用

```powershell
# 停止现有服务
.\scripts\stop_api_maven.sh

# 启动服务
.\scripts\start_api_maven.sh
```

### 4. 测试功能

```powershell
# 直接执行验证查询，不依赖额外脚本
mysql -u root -p studyforge_ai -e "
-- 查看 token 统计字段是否生效
SELECT COUNT(*) AS logs_with_tokens FROM ai_logs WHERE prompt_tokens IS NOT NULL;

-- 查看模型价格配置
SELECT model_name, prompt_price_per_1k, completion_price_per_1k, effective_date
FROM ai_model_pricing WHERE is_active = 1;

-- 查看当月费用统计
SELECT COALESCE(SUM(cost_yuan), 0) AS month_total_cost
FROM ai_logs
WHERE YEAR(created_time) = YEAR(CURDATE()) AND MONTH(created_time) = MONTH(CURDATE());
"
```

## 🔍 工作流程

```
用户请求 → AiController
    ↓
AiTokenBillingService.callApiWithUsage(prompt)
    ↓
调用硅基流动 API
    ↓
获取完整响应（包含 usage）
    ↓
提取文本内容 → 返回给用户
    ↓
AiTokenUsageLogger.logTokenUsage(logId, response, modelName)
    ↓
解析 usage 字段 → 计算费用 → 更新 ai_logs 表
```

## 📝 API 响应示例

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "deepseek-ai/DeepSeek-V4-Flash",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "这是 AI 生成的内容..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 100,
    "completion_tokens": 200,
    "total_tokens": 300
  }
}
```

## ⚠️ 注意事项

1. **向后兼容**: 旧的 ai_logs 记录 token 字段为 NULL，不影响查询
2. **错误处理**: Token 记录失败不会影响主流程，只记录错误日志
3. **性能影响**: 额外的数据库 UPDATE 操作，性能开销很小
4. **价格更新**: 可通过修改 `ai_model_pricing` 表来调整价格

## 🔧 维护指南

### 添加新模型价格

```sql
INSERT INTO ai_model_pricing (model_name, prompt_price_per_1k, completion_price_per_1k, effective_date, is_active, remark)
VALUES ('new-model-name', 0.003, 0.012, CURDATE(), 1, '新模型');
```

### 查看费用统计

```sql
-- 总费用统计
SELECT 
    COUNT(*) AS total_calls,
    SUM(total_tokens) AS total_tokens,
    ROUND(SUM(cost_yuan), 4) AS total_cost
FROM ai_logs 
WHERE prompt_tokens IS NOT NULL;

-- 按模型分组
SELECT 
    model_name,
    COUNT(*) AS call_count,
    ROUND(SUM(cost_yuan), 4) AS total_cost
FROM ai_logs 
GROUP BY model_name;
```

### 清理旧数据

```sql
-- 删除 30 天前的记录
DELETE FROM ai_logs WHERE created_time < NOW() - INTERVAL 30 DAY;
```

## 🎯 优势总结

1. ✅ **完全不修改现有 AI Service 代码** - 符合您的要求
2. ✅ **基于真实 API 数据** - 准确可靠
3. ✅ **行业标准** - 符合 OpenAI/SiliconFlow 标准
4. ✅ **易于维护** - 独立组件，职责清晰
5. ✅ **灵活配置** - 支持多模型、多价格
6. ✅ **向后兼容** - 不影响现有功能

## 📞 技术支持

如有问题，请检查：
1. 数据库表结构是否正确
2. 价格配置是否存在
3. API 密钥是否有效
4. 日志中是否有错误信息
