# AI 多智能体工作流

## 工作流引擎设计

### 执行流程

```
用户输入 → WorkflowEngine
               │
    ┌──────────┴──────────┐
    │    SetupSession     │ 创建对话 + 工作流会话
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │     Planner Agent   │ 分解需求 → 结构化计划 → 创建行程
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │    [并行执行]        │ Transport + Dining + Sightseeing + Accommodation + Budget
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   Activity Agent    │ 整合所有信息 → 每日活动安排
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │  Reflection Agent   │ 审查优化 → 经验提取
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   CompleteWorkflow  │ 保存结果 → 标记完成
    └─────────────────────┘
```

## Agent 职责说明

| Agent | 职责 | 输入 | 输出 |
|-------|------|------|------|
| **Planner** | 分析用户需求，分解为结构化计划 | 用户原始请求 | 天数、目的地、兴趣、风格、预算 |
| **Transport** | 推荐交通方案 | Planner 输出 | 交通方式、费用、时间 |
| **Dining** | 推荐餐饮 | Planner 输出 | 餐厅、菜品、预算 |
| **Sightseeing** | 推荐景点 | Planner 输出 | 景点、门票、时长 |
| **Accommodation** | 推荐住宿 | Planner 输出 | 酒店、价格、设施 |
| **Budget** | 预算分配优化 | 各 Agent 输出 | 分类预算、建议 |
| **Activity** | 每日详细计划 | 所有 Agent 输出 | 时间表、活动 |
| **Reflection** | 审查完善 | 所有 Agent 输出 | 改进建议、总结、经验提取 |

## 个性化机制

### AgentContext 结构

工作流引擎在构建 Agent 上下文时，会注入用户的偏好摘要和旅行记忆摘要：

```
AgentContext
├── userId / userEmail          # 用户身份信息
├── userRequest                 # 用户原始请求
├── preferenceSummary           # "User Preferences:\n- food: cuisine=japanese, ..."
├── tripMemorySummary           # "Past Trip Memories:\n- [HIGHLIGHT] 东京塔夜景..."
├── conversationHistory         # 最近对话历史
├── ragMemoryContext            # RAG 检索的相关记忆（Top-K）
├── ragKnowledgeContext         # RAG 检索的目的地知识
├── userLanguage                # 检测到的用户语言（zh/en/ja/ko）
└── sharedData                  # 各 Agent 的中间输出
    ├── planner_output
    ├── transport_output
    ├── dining_output
    ├── sightseeing_output
    ├── accommodation_output
    ├── budget_output
    ├── activity_output
    └── reflection_output
```

### RAG 语义检索

使用 PostgreSQL pgvector 实现向量化存储与语义检索：

1. **向量化**: 用户偏好、旅行记忆、目的地知识被转换为向量存储
2. **语义检索**: 生成行程时，检索 Top-K 相关内容注入提示词
3. **Redis 缓存**: 检索结果缓存 1 小时，减少重复计算
4. **解决问题**: 解决长 Prompt 和 LLM 幻觉问题

## 并行执行优化

为提升性能，5 个独立 Agent 支持并行执行：

```
Planner（串行）→ [Transport, Dining, Sightseeing, Accommodation, Budget]（并行）→ Activity（串行）→ Reflection（串行）
```

### 并行执行细节

- 使用 `CompletableFuture` 实现并发
- 每个 Agent 在独立线程中执行
- 实时推送流式输出到前端
- 性能提升约 80%，总耗时从 5 分钟降至 1 分钟左右

## 缓存策略

| 缓存项 | TTL | 缓存策略 |
|--------|-----|----------|
| 行程详情 | 30 分钟 | 查询时缓存，更新/删除时失效 |
| RAG 检索结果 | 1 小时 | 语义检索时缓存，知识库更新时失效 |
| 用户偏好摘要 | 5 分钟 | 生成时缓存，偏好变更时失效 |
| 天气数据 | 30 分钟 | 查询时缓存，目的地+日期作为 key |
| 刷新令牌 | 7 天 | Redis 哈希存储，支持服务端吊销 |

## 重试与容错

- 每个 Agent 最多重试 3 次
- 重试间隔线性退避：1s → 2s → 3s
- 局部 Agent 失败不影响整体工作流（其他 Agent 继续执行）
- 会话级状态追踪，支持异步查看执行进度
- 工作流会话和步骤持久化到数据库

## 系统工具

### SystemTools

AI 可以调用以下工具获取系统信息：

| 工具 | 说明 | 返回 |
|------|------|------|
| `getCurrentDateTime()` | 获取当前完整日期时间 | JSON 格式（日期、时间、年、月、日、星期） |
| `getCurrentDate()` | 获取当前日期 | yyyy-MM-dd |
| `getCurrentYear()` | 获取当前年份 | int |

### 语言检测

`LanguageDetector` 自动检测用户请求语言：

- 支持：中文、英文、日文、韩文
- 通过字符识别和关键词匹配进行多维度检测
- 返回语言代码（zh/en/ja/ko）
- 所有 Agent 的输出语言与用户输入保持一致

## 执行状态

### WorkflowSession 状态

| 状态 | 说明 |
|------|------|
| PENDING | 待执行 |
| RUNNING | 执行中 |
| COMPLETED | 已完成 |
| FAILED | 执行失败 |
| CANCELLED | 用户取消 |

### WorkflowStep 状态

| 状态 | 说明 |
|------|------|
| PENDING | 待执行 |
| RUNNING | 执行中 |
| COMPLETED | 已完成 |
| FAILED | 执行失败 |

## SSE 流式输出

工作流支持 Server-Sent Events 实时推送：

| 事件类型 | 说明 | 数据 |
|----------|------|------|
| `start` | 工作流开始 | sessionId |
| `agent_start` | Agent 开始执行 | agentName |
| `agent_content` | Agent 输出内容 | agentName, content |
| `agent_end` | Agent 执行结束 | agentName |
| `agent_failed` | Agent 执行失败 | agentName, error |
| `agent_skipped` | Agent 被跳过 | agentName, reason |
| `summary` | 最终摘要 | content |
| `complete` | 工作流完成 | sessionId |
| `error` | 工作流错误 | error |

## 记忆回路

ReflectionAgent 输出中的 MEMORY 标签会自动落库：

| 标签 | 存储位置 | 说明 |
|------|----------|------|
| PREFERENCE_DISCOVERED | UserPreference | AI 发现的新偏好 |
| HIGHLIGHT | TripMemory | 旅行亮点 |
| LESSON_LEARNED | TripMemory | 经验教训 |
| LOWLIGHT | TripMemory | 不理想的经历 |
| ADVICE | TripMemory | 建议 |

格式：`MEMORY: <TYPE> - <content>`

## 全球旅行知识库集成

### 知识库架构

```
┌─────────────────────────────────────────────────────────────┐
│                     知识采集层                                │
│  OpenStreetMap  Wikivoyage  Wikipedia  OpenTripMap           │
├─────────────────────────────────────────────────────────────┤
│                     知识处理管道                              │
│  Crawler → Parser → Cleaner → EntityResolver → Chunker       │
│                      ↓                                       │
│               EmbeddingService                               │
│                      ↓                                       │
│              VectorStorage (pgvector)                        │
├─────────────────────────────────────────────────────────────┤
│                     RAG 检索层                               │
│  VectorSearch  HybridSearch  MMR  MetadataFilter             │
├─────────────────────────────────────────────────────────────┤
│                     Agent 集成层                             │
│  Planner  POI  Food  Hotel  Budget  Transport               │
└─────────────────────────────────────────────────────────────┘
```

### Agent 知识库接口

[KnowledgeAgentService](file:///Users/wenhao/code/Java/TripDesigner/src/main/java/com/tripdesigner/knowledge/agent/KnowledgeAgentService.java) 为每个 Agent 提供专用接口：

| Agent | 接口方法 | 说明 |
|-------|----------|------|
| **Planner** | `buildDestinationContext()` | 获取目的地上下文（用于生成行程大纲） |
| **POI/Sightseeing** | `recommendPois()` | 按城市和分类推荐景点 |
| **Dining/Food** | `recommendRestaurants()` | 按城市和菜系推荐餐厅 |
| **Accommodation/Hotel** | `recommendHotels()` | 按城市和等级推荐酒店 |
| **Transport** | `getRoutes()` | 获取城市间路线和交通方式 |
| **Weather** | `getTravelGuide()` | 获取目的地旅行指南 |
| **Shopping** | `searchForAgent()` | 通用知识搜索 |

### RAG 检索策略

知识库支持三种检索策略：

| 策略 | 说明 | 使用场景 |
|------|------|----------|
| **Vector Search** | 纯语义相似度检索 | 通用查询、模糊匹配 |
| **Hybrid Search** | 向量 + 关键词全文检索融合 | 需要精确匹配的查询 |
| **MMR** | 最大边际相关性检索 | 需要结果多样性的场景 |

### 知识管道流程

```
Crawler  →  Parser  →  Cleaner  →  EntityResolver
    ↓          ↓          ↓              ↓
获取原始数据  解析结构化  清理文本内容  解析实体引用
    ↓
Chunker  →  Embedding  →  VectorStorage
    ↓          ↓              ↓
语义切分(600-800token)  生成向量  存储到pgvector
    ↓
KnowledgeGraph  ←  Relations
    ↓
建立实体关系（CONTAINS, NEAR, BEST_ROUTE）
```

### 知识库配置

```yaml
knowledge:
  embedding:
    provider: spring-ai           # spring-ai, dashscope, ollama
    dimensions: 1536              # 向量维度
  rag:
    top-k: 5                      # 返回数量
    similarity-threshold: 0.5      # 相似度阈值
    mmr-lambda: 0.5               # MMR 多样性参数
    hybrid-alpha: 0.7             # 混合搜索权重
    cache-ttl-minutes: 60         # 缓存时长
  crawler:
    enabled: true                 # 是否启用采集
    rate-limit-ms: 1000           # 请求间隔
    retry-count: 3                # 重试次数
  scheduler:
    enabled: true                 # 是否启用定时同步
    cron: "0 0 2 * * ?"           # 每日凌晨2点同步
    incremental: true             # 增量更新
```

### 数据源支持

| 数据源 | 类型 | 增量支持 | API Key | 说明 |
|--------|------|----------|---------|------|
| OpenStreetMap | 开放 | 是 | 否 | 全球地图数据、POI |
| Wikivoyage | 开放 | 是 | 否 | 旅行指南内容 |
| Wikipedia | 开放 | 是 | 否 | 目的地百科知识 |
| OpenTripMap | API | 否 | 是 | POI 详情、评分 |
| Google Places | API | 待实现 | 是 | 商业 POI 数据 |
| TripAdvisor | API | 待实现 | 是 | 评论、评分 |
| Lonely Planet | API | 待实现 | 是 | 专业旅行指南 |
