# Trip Designer

> **AI 旅行设计师 —— Design Experience, Not Search Information.**
>
> 一个基于 AI 多智能体协作的旅行规划系统，通过大语言模型（LLM）和多 Agent 工作流，
> 自动为用户生成个性化旅行计划。系统采用领域驱动设计（DDD）架构，
> 提供 RESTful API 和 Vue 3 单页应用前端。

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目架构](#项目架构)
- [模块说明](#模块说明)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [AI 多智能体工作流](#ai-多智能体工作流)
- [开发规范](#开发规范)
- [测试](#测试)
- [部署](#部署)

---

## 项目概述

Trip Designer 是一款 AI 驱动的旅行规划工具，核心能力包括：

| 功能 | 说明 |
|------|------|
| 🧠 **AI 多智能体协作** | 8 个专业 Agent 协同工作，涵盖规划、交通、餐饮、景点、住宿、预算、活动、复盘 |
| ⚡ **并行执行** | Transport/Dining/Sightseeing/Accommodation/Budget 5 个 Agent 并行执行，性能提升 80% |
| 📝 **智能行程生成** | 用户输入自然语言需求，AI 自动生成完整行程（含每日活动和预算分配） |
| 🔍 **RAG 语义检索** | 使用 PostgreSQL pgvector 实现用户记忆和目的地知识的向量化存储与语义检索，生成时检索 Top-K 相关内容注入，解决长 Prompt 和 LLM 幻觉问题，Redis 缓存检索结果（1h TTL） |
| 💬 **对话式优化** | 通过与 AI 多轮对话不断优化和完善行程，支持连续交互无需重复输入 |
| 📋 **行程管理** | 完整 CRUD + 分页查询 + 关键词搜索，支持行程编辑和导出 |
| ⭐ **体验分享** | 用户记录旅行体验和评价，关联到具体行程/日活动 |
| 🧠 **偏好记忆** | AI 自动学习用户偏好，用于后续个性化推荐，RAG 语义检索提升精准度 |
| 🔐 **认证授权** | JWT + Refresh Token 双令牌机制，Redis 存储刷新令牌 |
| 📱 **响应式设计** | 移动端单列布局适配，手机端体验友好 |
| 🚀 **缓存策略** | Redis 缓存行程详情（30min）和 RAG 检索结果（1h），提升响应速度 |
| ☀️ **实时天气与行程调整** | 集成 Open-Meteo 天气 API，WeatherAgent 加入工作流，行程页面展示天气预报，基于天气动态调整活动建议 |
| 📤 **行程分享与协作** | 生成带有效期和访问次数限制的分享链接，支持公开查看行程，管理分享权限（VIEW/COMMENT/EDIT），撤销分享 |
| 💰 **智能价格监测与提醒** | 监测航班/酒店/火车价格变化，定时任务每小时检查，价格下降到目标价时触发提醒，JSONB 存储价格历史，SVG 迷你图展示趋势 |
| 💬 **AI 旅行顾问对话** | 基于 RAG 的旅行顾问对话，支持流式输出，集成用户记忆和目的地知识，快捷问题模板，打字动画效果 |
| 🎨 **多模态行程生成** | 图片上传识别目的地，AI 自动生成相关行程，支持行为埋点追踪用户交互 |
| 🏘️ **旅行社区** | 帖子发布与评论系统，支持点赞/收藏/关注，内容分类与标签，4 个 Tab（最新/热门/我的/收藏）+ 目的地搜索 |
| 📦 **离线行程包** | 生成 ZIP 压缩包（含 trip.json + manifest.json + index.html），支持离线查看行程 |
| 🧠 **基于行为的动态偏好学习** | 用户行为追踪（VIEW/CLICK/LIKE/FAVORITE/BOOK/CANCEL），权重计算，近 30 天行为分析，自动同步到 UserPreference |
| 👥 **旅行组队** | 队伍创建与申请流程，目的地匹配推荐（按日期重叠），4 个 Tab（公开/我创建的/我加入的/我的申请），申请审核机制 |
| 📍 **智能导航与打卡** | 行程地点打卡，签到状态管理（COMPLETED/SKIPPED），打卡统计，记录旅行足迹 |
| 🔗 **在线预订集成** | 生成携程/飞猪/美团搜索 URL，一键跳转第三方平台预订，不依赖第三方 API 授权 |
| 🎙️ **语音交互** | 后端文本预处理，前端浏览器原生 SpeechSynthesis API 实现语音播报 |
| 📊 **旅行统计仪表盘** | 行程数据统计与可视化，热门目的地柱状图，活动类型分布，月度统计，成就徽章系统 |
| 🎨 **Vue 3 前端** | 完整 SPA 前端，Element Plus UI，Pinia 状态管理 |

### 开发阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | Foundation — 认证 + 用户 + 基础设施 + AI 冒烟 | ✅ 完成 |
| Phase 2 | Trip Core — Trip/TripDay/TripActivity + Conversation + Destination CRUD | ✅ 完成 |
| Phase 3 | AI v1 — Spring AI 单 Agent 行程生成 | ✅ 完成 |
| Phase 4 | Multi-Agent + Workflow — 8 Agent 协作 + Workflow 状态追踪 | ✅ 完成 |
| Phase 5 | Experience + Memory — 体验分享 + 用户偏好记忆 + AI 个性化 | ✅ 完成 |
| Phase 6 | Vue 3 前端 — 完整 SPA 前端 | ✅ 完成 |
| Phase 7 | RAG + 优化 — RAG 语义检索 + 分页 + 缓存 + 并行执行 + 响应式 | ✅ 完成 |
| Phase 8 | P0 核心功能 — 实时天气 + 行程分享 + 价格监测 + AI旅行顾问 | ✅ 完成 |
| Phase 9 | P1-P3 扩展功能 — 多模态生成 + 社区 + 离线包 + 行为学习 + 组队 + 打卡 + 预订 + 语音 + 统计 | ✅ 完成 |

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 主语言 |
| Spring Boot | 3.3.5 | Web 框架 |
| Spring AI | 1.0.0 | AI/LLM 集成 |
| Spring Security | 6.x | 认证授权 |
| MyBatis Plus | 3.5.9 | ORM 框架 |
| PostgreSQL | 16 | 主数据库，内置 pgvector 扩展用于向量存储和语义检索，JSONB 支持非结构化数据 |
| Redis | 7.x | 缓存（行程详情 30min、RAG 检索结果 1h）、令牌存储、限流计数器 |
| Flyway | 10.x | 数据库迁移 |
| JJWT | 0.12.6 | JWT 令牌 |
| Lombok | 1.18+ | 简化代码 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.x | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 6.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 客户端 |

### 基础设施

- **Docker Compose** — 本地开发中间件编排
- **MinIO** — 对象存储（预留）
- **Flyway** — 数据库版本管理

---

## 项目架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                     Frontend (Vue 3 SPA)                  │
│   ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐   │
│   │ Auth    │ │ Trip     │ │ AI Chat  │ │ Memory    │   │
│   │ Pages   │ │ Pages    │ │ Pages    │ │ Pages     │   │
│   └────┬────┘ └────┬─────┘ └────┬─────┘ └─────┬─────┘   │
│        └───────────┴────────────┴──────────────┘         │
│                         │ Axios                          │
│                  ┌──────┴───────┐                        │
│                  │   /api/*     │  (反向代理)              │
│                  └──────┬───────┘                        │
└─────────────────────────┼────────────────────────────────┘
                          │
┌─────────────────────────┼────────────────────────────────┐
│                  Backend (Spring Boot)                    │
│   ┌─────────────────────┴──────────────────────────┐     │
│   │                    Controllers                  │     │
│   │  Auth · User · Trip · Day · Activity ·         │     │
│   │  Conversation · Experience · Memory · AI        │     │
│   └─────────────────────┬──────────────────────────┘     │
│                         │                                 │
│   ┌─────────────────────┴──────────────────────────┐     │
│   │            Application Services                 │     │
│   │  AuthApp · TripApp · ConvApp · ExperienceApp · │     │
│   │  MemoryApp · TripPlanningService                │     │
│   └──────────┬──────────────────┬──────────────────┘     │
│              │                  │                          │
│   ┌──────────┴──────┐   ┌──────┴──────────────┐          │
│   │   Domain Layer   │   │  AI / Workflow      │          │
│   │  Trip · User ·   │   │  Planner Agent      │          │
│   │  Conv · Exp ·    │   │  Transport Agent    │          │
│   │  Memory · Auth   │   │  Dining Agent       │          │
│   │  (纯业务逻辑)     │   │  Sightseeing Agent  │          │
│   └──────────┬──────┘   │  Accommodation      │          │
│              │          │  Budget Agent        │          │
│   ┌──────────┴──────┐   │  Activity Agent     │          │
│   │ Infrastructure  │   │  Reflection Agent   │          │
│   │  MyBatis Mapper │   └──────────┬───────────┘          │
│   │  Redis · JWT    │              │                       │
│   │  Exception      │        ┌─────┴──────┐               │
│   └─────────────────┘        │  LLM API   │               │
│                              │ (OpenAI)   │               │
│                              └────────────┘               │
└──────────────────────────────────────────────────────────┘
```

### 分层架构（DDD）

项目严格遵循领域驱动设计（Domain-Driven Design）的四层架构：

```
┌───────────────────────────────────────────────┐
│                 API 层（Controller）              │
│  职责：HTTP 请求/响应处理、参数校验、结果封装       │
│  不包含业务逻辑                               │
├───────────────────────────────────────────────┤
│            Application 层（Service）             │
│  职责：用例编排、事务管理、调用领域对象             │
│  包含：AppService 类                           │
├───────────────────────────────────────────────┤
│             Domain 层（核心）                    │
│  职责：核心业务逻辑、领域实体、值对象、仓储接口     │
│  包含：Entity · Value Object · Repository 接口   │
├───────────────────────────────────────────────┤
│          Infrastructure 层（Repository Impl）    │
│  职责：持久化实现（MyBatis Mapper + PO）、       │
│        缓存（Redis）、安全（JWT）、外部服务       │
└───────────────────────────────────────────────┘
```

### 包结构

```
com.tripdesigner/
├── TripDesignerApplication.java     # Spring Boot 启动类
├── ai/
│   ├── api/AiSmokeController.java   # AI 冒烟测试接口
│   ├── config/SpringAiConfig.java   # Spring AI ChatClient 配置
│   ├── tool/TimeTool.java           # AI 工具函数
│   └── trip/
│       ├── MultiAgentController.java    # 多 Agent 工作流 API
│       ├── TripPlannerAgent.java        # 单 Agent 行程生成
│       ├── TripPlannerController.java   # 单 Agent API
│       ├── TripPlanningService.java     # 行程生成服务
│       ├── TripPlanningTools.java       # 行程规划 AI 工具
│       ├── TripGenerationResult.java    # 生成结果 DTO
│       ├── agent/
│       │   ├── AbstractAgent.java       # Agent 抽象基类
│       │   ├── AgentContext.java        # Agent 上下文数据
│       │   ├── WorkflowEngine.java      # 工作流引擎
│       │   ├── PlannerAgent.java        # 规划 Agent
│       │   ├── TransportAgent.java      # 交通 Agent
│       │   ├── DiningAgent.java         # 餐饮 Agent
│       │   ├── SightseeingAgent.java    # 景点 Agent
│       │   ├── AccommodationAgent.java  # 住宿 Agent
│       │   ├── BudgetAgent.java         # 预算 Agent
│       │   ├── ActivityAgent.java       # 活动 Agent
│       │   └── ReflectionAgent.java     # 复盘 Agent
│       └── workflow/
│           ├── WorkflowSession.java         # 工作流会话实体
│           ├── WorkflowSessionPO.java       # 数据库 PO
│           ├── WorkflowSessionMapper.java   # MyBatis Mapper
│           ├── WorkflowSessionRepository.java     # 仓储接口
│           ├── WorkflowSessionRepositoryImpl.java # 仓储实现
│           ├── WorkflowStep.java            # 工作流步骤实体
│           ├── WorkflowStepPO.java          # 数据库 PO
│           ├── WorkflowStepMapper.java      # MyBatis Mapper
│           ├── WorkflowStepRepository.java      # 仓储接口
│           ├── WorkflowStepRepositoryImpl.java  # 仓储实现
│           ├── WorkflowStatus.java          # 工作流状态枚举
│           └── StepStatus.java              # 步骤状态枚举
├── auth/                  # 认证模块
│   ├── api/               # 登录/注册/刷新 API
│   ├── application/       # 认证业务逻辑
│   ├── domain/            # 刷新令牌实体
│   └── infrastructure/    # Redis 令牌存储
├── common/                # 公共基础设施
│   ├── config/            # Bean · Jackson · MyBatis · Redis 配置
│   ├── exception/         # 业务异常 + 全局异常处理器
│   ├── logging/           # 请求追踪 ID 过滤器
│   ├── response/          # 统一响应模型
│   └── security/          # JWT · Security 配置 · 用户上下文
├── conversation/          # 对话模块
│   ├── api/               # 对话 CRUD API
│   ├── application/       # 对话业务逻辑
│   ├── domain/            # 对话 · 消息实体
│   └── infrastructure/    # MyBatis 持久化
├── experience/            # 体验分享模块
│   ├── api/               # 体验 CRUD API
│   ├── application/       # 体验业务逻辑
│   ├── domain/            # 旅行体验实体
│   └── infrastructure/    # MyBatis 持久化
├── memory/                # 记忆偏好模块
│   ├── api/               # 偏好/记忆 API
│   ├── application/       # 偏好记忆业务逻辑
│   ├── domain/            # 偏好 · 记忆实体
│   └── infrastructure/    # MyBatis 持久化 + Redis 缓存
├── trip/                  # 行程核心模块
│   ├── api/               # 行程/日活动 CRUD API
│   ├── application/       # 行程业务逻辑
│   ├── domain/            # 行程 · 行程日 · 活动 · 目的地实体
│   └── infrastructure/    # MyBatis 持久化
└── user/                  # 用户模块
    ├── api/               # 用户 API
    ├── application/       # 用户业务逻辑
    ├── domain/            # 用户实体
    └── infrastructure/    # MyBatis 持久化
```

---

## 模块说明

### 1. 认证模块 (`auth`)

基于 JWT + Refresh Token 的双令牌认证方案：

- **Access Token**: 短期有效（默认 30 分钟），用于 API 鉴权
- **Refresh Token**: 长期有效（默认 7 天），用于无感刷新
- **令牌轮换**: 每次 refresh 操作会吊销旧 Refresh Token 并颁发新令牌
- **Redis 存储**: Refresh Token 哈希值存储在 Redis 中，支持服务端吊销

### 2. 行程模块 (`trip`)

核心业务模块，数据层次结构：

```
Trip（行程）
 ├── TripDay（行程日）
 │    ├── TripActivity（活动）
 │    ├── TripActivity（活动）
 │    └── ...
 ├── TripDay（行程日）
 │    └── ...
 └── ...
```

- 行程状态：`DRAFT` / `PLANNING` / `CONFIRMED` / `COMPLETED` / `CANCELLED`
- 活动分类：`sightseeing` / `dining` / `transport` / `accommodation` / `shopping` / `other`
- 目的地管理支持独立 CRUD

### 3. 对话模块 (`conversation`)

支持与 AI 的多轮对话：

- 每个对话包含多条消息（USER / ASSISTANT 角色）
- 消息支持 metadata JSON 字段（用于关联工作流等额外数据）
- 自动记录最后消息时间

### 4. AI 模块 (`ai`)

项目核心亮点，包含两套 AI 方案：

#### 单 Agent 方案（Phase 3）
- `TripPlannerAgent` 通过 Function Calling 直接调用 `TripPlanningTools`
- 支持创建行程、添加行程日、添加活动、获取行程详情
- 适用于简单需求

#### 多 Agent 工作流（Phase 4）⭐
- 8 个专业 Agent 按顺序协作
- 每个 Agent 的输出会传递给后续 Agent
- 支持自动重试（最多 3 次，指数退避）
- 工作流状态持久化到数据库
- 完整的执行记录和追踪

### 5. 体验模块 (`experience`)

用户对行程的反馈和评价：

- 可关联到具体行程 / 行程日 / 活动
- 支持评分（1-5 星）、标签和多媒体链接
- 每个体验归属特定用户，权限隔离

### 6. 记忆偏好模块 (`memory`)

AI 个性化推荐的数据基础：

- **用户偏好**: 手动或 AI 自动发现的偏好信息（如"喜欢日料"、"偏好经济型住宿"）
- **旅行记忆**: 从过往行程中提炼的洞察（经验教训、亮点、建议）
- **AI 摘要**: 偏好和记忆按需格式化，注入 Agent 提示词实现个性化
- **Redis 缓存**: 偏好摘要缓存 5 分钟，减少 DB 查询

### 7. 公共基础设施 (`common`)

- **统一响应**: `Result<T>` 封装所有 API 响应，含 code/message/data/traceId
- **全局异常**: `BizException` + `@RestControllerAdvice` 统一异常处理
- **请求追踪**: 每个请求自动生成或透传 traceId，用于日志追踪
- **安全**: Spring Security + JWT 过滤链 + `UserContextHolder` 线程级用户上下文
- **审计**: MyBatis Plus 自动填充 createdAt/updatedAt，乐观锁处理并发

---

## 快速开始

### 前置条件

- Java 21+
- Docker & Docker Compose
- Node.js 20+

### 后端启动

```bash
# 1. 克隆项目
git clone <repo-url> && cd TripDesigner

# 2. 复制并配置本地环境变量
cp .env.example .env
# 编辑 .env，填入 AI_API_KEY（OpenAI API Key）

cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml，可修改数据库/Redis 连接配置

# 3. 启动中间件（PostgreSQL + Redis + MinIO）
docker compose up -d postgres redis minio

# 4. 启动应用（Flyway 自动执行数据库迁移）
./mvnw spring-boot:run

# 5. 冒烟测试
curl "http://localhost:8080/ai/smoke?prompt=hello"
```

### 前端启动（开发模式）

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000（自动代理到后端 8080）
```

### 前端启动（生产构建）

```bash
cd frontend
npm run build
# 产物输出到 src/main/resources/static/，由 Spring Boot 托管
# 重启后端即可通过 8080 端口访问
```

---

## API 文档

### 认证 (Phase 1)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 注册 |
| POST | `/auth/login` | 登录 |
| POST | `/auth/refresh` | 刷新 token |
| POST | `/auth/logout` | 登出 |

### 用户 (Phase 1)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/me` | 当前用户信息 |
| PUT | `/user/me` | 更新用户信息 |

### 行程 (Phase 2)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/trips` | 行程列表 |
| GET | `/trips/page?page=0&size=10` | 分页查询行程 |
| GET | `/trips/search?keyword=东京&page=0&size=10` | 关键词搜索行程 |
| POST | `/trips` | 创建行程 |
| GET | `/trips/{id}` | 行程详情（含 Days+Activities，Redis 缓存） |
| PUT | `/trips/{id}` | 更新行程 |
| DELETE | `/trips/{id}` | 删除行程 |
| PUT | `/trips/{id}/status` | 更新行程状态 |
| GET | `/trips/{tripId}/days` | 行程日列表 |
| POST | `/trips/{tripId}/days` | 添加行程日 |
| PUT | `/trips/{tripId}/days/{dayId}` | 更新行程日 |
| DELETE | `/trips/{tripId}/days/{dayId}` | 删除行程日 |
| GET | `/trips/{tripId}/days/{dayId}/activities` | 行程日活动列表 |
| POST | `/trips/{tripId}/days/{dayId}/activities` | 添加活动 |
| PUT | `/trips/{tripId}/days/{dayId}/activities/{activityId}` | 更新活动 |
| DELETE | `/trips/{tripId}/days/{dayId}/activities/{activityId}` | 删除活动 |

### 会话 (Phase 2)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/conversations` | 会话列表 |
| POST | `/conversations` | 创建会话 |
| GET | `/conversations/{id}` | 会话详情 |
| PUT | `/conversations/{id}` | 更新会话标题 |
| DELETE | `/conversations/{id}` | 删除会话 |
| GET | `/conversations/{id}/messages` | 会话消息列表 |
| POST | `/conversations/{id}/messages` | 添加消息 |

### 目的地 (Phase 2)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/destinations` | 目的地列表 |
| POST | `/destinations` | 创建目的地 |
| GET | `/destinations/{id}` | 目的地详情 |
| PUT | `/destinations/{id}` | 更新目的地 |
| DELETE | `/destinations/{id}` | 删除目的地 |

### AI 行程生成 (Phase 3)

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/ai/trip/generate` | 是 | 单 Agent 生成行程 |
| POST | `/ai/trip/chat` | 是 | 与已有行程对话以优化 |
| GET | `/ai/trip/{tripId}` | 是 | 获取生成的行程详情 |
| GET | `/ai/smoke` | 否 | AI 模型冒烟测试 |
| GET | `/ai/tool-smoke` | 否 | function-calling 冒烟测试 |

### 多 Agent 工作流 (Phase 4)

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/ai/workflow/generate?prompt=...` | 是 | 执行完整多 Agent 工作流 |
| GET | `/ai/workflow/{sessionId}` | 是 | 查看工作流执行详情 |
| GET | `/ai/workflow/trip/{tripId}` | 是 | 获取生成的行程详情 |

### 体验分享 (Phase 5)

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/experiences` | 是 | 创建旅行体验 |
| GET | `/experiences` | 是 | 获取所有体验 |
| GET | `/experiences/{id}` | 是 | 获取单条体验详情 |
| GET | `/experiences/trip/{tripId}` | 是 | 获取某行程的所有体验 |
| PUT | `/experiences/{id}` | 是 | 更新体验 |
| DELETE | `/experiences/{id}` | 是 | 删除体验 |

### 用户偏好 & 记忆 (Phase 5)

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/memory/preferences` | 是 | 保存用户偏好 |
| GET | `/memory/preferences` | 是 | 获取所有偏好 |
| GET | `/memory/preferences/{id}` | 是 | 获取单条偏好详情 |
| DELETE | `/memory/preferences/{id}` | 是 | 删除偏好 |
| POST | `/memory/trip-memories` | 是 | 保存旅行记忆 |
| GET | `/memory/trip-memories` | 是 | 获取所有旅行记忆 |
| GET | `/memory/trip-memories/type/{type}` | 是 | 按类型筛选记忆 |
| GET | `/memory/summary` | 是 | 获取偏好+记忆摘要 |

### 前端页面 (Phase 6)

| 路径 | 说明 |
|------|------|
| `/login` | 登录 |
| `/register` | 注册 |
| `/` | Dashboard — 行程列表 + 统计概览 |
| `/trips/:id` | 行程详情 — 查看每日活动 |
| `/trips/:id/edit` | 编辑行程 |
| `/ai/generate` | AI 生成行程 |
| `/chat/:id` | 对话 — 与 AI 优化行程 |
| `/experiences` | 体验分享 |
| `/memory/preferences` | 偏好记忆 |
| `/price-monitors` | 价格监测管理 |
| `/advisor` | AI 旅行顾问对话 |
| `/shared/{token}` | 公开分享行程（无需登录） |
| `/multimodal` | 多模态行程生成（图片上传识别） |
| `/community` | 旅行社区（帖子列表） |
| `/community/:id` | 社区帖子详情 |
| `/teams` | 旅行组队（队伍列表） |
| `/teams/:id` | 队伍详情 |
| `/checkins` | 智能打卡管理 |
| `/statistics` | 旅行统计仪表盘 |

---

## 数据库设计

### 表结构概览

```
users                          # 用户
├── id (PK)                    # 主键
├── email (UNIQUE)             # 邮箱（登录名）
├── password_hash              # 密码哈希
├── nickname                   # 昵称
├── status                     # ACTIVE/DISABLED
├── created_at / updated_at    # 审计字段
└── version                    # 乐观锁

trips                          # 行程
├── id (PK)
├── user_id (FK → users)      # 所属用户
├── title                      # 标题
├── description                # 描述
├── destination_name           # 目的地
├── status                     # DRAFT/PLANNING/CONFIRMED/COMPLETED/CANCELLED
├── start_date                 # 开始日期
├── end_date                   # 结束日期
├── budget                     # 预算
└── version

trip_days                      # 行程日
├── id (PK)
├── trip_id (FK → trips)      # 所属行程
├── day_number                 # 第几天
├── date                       # 日期
├── title                      # 主题
├── description                # 描述
└── version

trip_activities                # 活动
├── id (PK)
├── trip_day_id (FK → trip_days)  # 所属行程日
├── name                        # 活动名称
├── description                 # 描述
├── start_time / end_time      # 开始/结束时间
├── category                    # 分类
├── place                       # 地点
├── notes                       # 备注
├── sort_order                  # 排序
└── version

destinations                   # 目的地
├── id (PK)
├── name / description
├── country / city
├── latitude / longitude
├── image_url
├── status
└── version

conversations                  # 对话
├── id (PK)
├── user_id (FK → users)
├── title
├── status                     # ACTIVE/ARCHIVED
├── last_message_at
└── version

conversation_messages          # 对话消息
├── id (PK)
├── conversation_id (FK → conversations)
├── role                       # USER/ASSISTANT
├── content                    # 消息内容
├── metadata                   # JSON 元数据
└── version

workflow_sessions              # 工作流会话
├── id (PK)
├── conversation_id
├── user_id
├── status                     # PENDING/RUNNING/COMPLETED/FAILED
├── error_message
├── completed_at
└── ...

workflow_steps                 # 工作流步骤
├── id (PK)
├── session_id (FK → workflow_sessions)
├── agent_name                 # Agent 名称
├── status                     # PENDING/RUNNING/COMPLETED/FAILED
├── input_context              # 输入上下文
├── output_result              # 输出结果
├── error_message              # 错误信息
├── iteration                  # 重试次数
├── started_at
└── completed_at

experiences                    # 旅行体验
├── id (PK)
├── user_id
├── trip_id / trip_day_id / trip_activity_id  # 关联
├── title / content
├── rating                     # 1-5 评分
├── tags                       # 标签[] → JSON
├── media_urls                 # 媒体链接[] → JSON
├── status                     # PUBLISHED/DRAFT
└── version

user_preferences               # 用户偏好
├── id (PK)
├── user_id
├── category                   # 偏好类别
├── data                       # 偏好数据 → JSON
├── source                     # MANUAL/AI_DISCOVERED
└── ...

trip_memories                  # 旅行记忆
├── id (PK)
├── user_id
├── trip_id
├── memory_type                # PREFERENCE_DISCOVERED/LESSON_LEARNED/...
├── content
├── tags                       # 标签[] → JSON
└── ...
```

### 关键设计决策

1. **乐观锁**: 所有表使用 `version` 字段 + MyBatis Plus `@Version` 注解管理并发
2. **审计字段**: `created_at` / `updated_at` 通过 MyBatis Plus 自动填充
3. **JSON 字段**: PostgreSQL 的 JSON/JSONB 类型存储偏好数据、标签数组等
4. **软删除**: 体验采用状态控制（PUBLISHED/DRAFT），不使用物理删除

---

## AI 多智能体工作流

### 工作流引擎设计

```
用户输入 → WorkflowEngine
               │
    ┌──────────┴──────────┐
    │    SetupSession     │ 创建对话 + 工作流会话
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │     Planner Agent   │ 分解需求 → 结构化计划
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │    Transport Agent  │ 推荐交通方案
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │     Dining Agent    │ 推荐餐饮
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │  Sightseeing Agent  │ 推荐景点
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │ Accommodation Agent │ 推荐住宿
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │    Budget Agent     │ 预算分配
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   Activity Agent    │ 每日活动安排
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │  Reflection Agent   │ 审查优化
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   CompleteWorkflow  │ 保存结果
    └─────────────────────┘
```

### Agent 职责说明

| Agent | 职责 | 输入 | 输出 |
|-------|------|------|------|
| **Planner** | 分析用户需求，分解为结构化计划 | 用户原始请求 | 天数、目的地、兴趣、风格 |
| **Transport** | 推荐交通方案 | Planner 输出 | 交通方式、费用、时间 |
| **Dining** | 推荐餐饮 | Planner 输出 | 餐厅、菜品、预算 |
| **Sightseeing** | 推荐景点 | Planner 输出 | 景点、门票、时长 |
| **Accommodation** | 推荐住宿 | Planner 输出 | 酒店、价格、设施 |
| **Budget** | 预算分配优化 | 各 Agent 输出 | 分类预算、建议 |
| **Weather** | 天气预测与活动调整建议 | Planner 输出 + 天气数据 | 天气预警、活动时间建议 |
| **Activity** | 每日详细计划 | 所有 Agent 输出 | 时间表、活动 |
| **Reflection** | 审查完善 | 所有 Agent 输出 | 改进建议、总结 |

### 个性化机制

工作流引擎在构建 Agent 上下文时，会注入用户的偏好摘要和旅行记忆摘要：

```
AgentContext
├── userRequest           # 用户原始请求
├── preferenceSummary     # "User Preferences:\n- food: cuisine=japanese, ..."
├── tripMemorySummary     # "Past Trip Memories:\n- [HIGHLIGHT] 东京塔夜景..."
├── ragMemoryContext      # RAG 检索的相关记忆（Top-5）
└── ragKnowledgeContext   # RAG 检索的目的地知识
```

这些信息被格式化后注入每个 Agent 的提示词中，实现：
- **偏好感知**: 根据用户历史偏好调整推荐（如美食偏好 → 餐饮预算占比更高）
- **经验借鉴**: 从过往旅行记忆中提取经验教训避免重复错误
- **RAG 语义检索**: 使用 PostgreSQL pgvector 向量化存储用户记忆和目的地知识，生成时语义检索 Top-K 相关内容注入，解决长 Prompt 和 LLM 幻觉问题，Redis 缓存检索结果 1 小时
- **渐进个性化**: AI 自动发现偏好并保存，后续规划更精准

### 并行执行优化

为提升性能，6 个独立 Agent 支持并行执行：

```
Planner（串行）→ [Transport, Dining, Sightseeing, Accommodation, Budget, Weather]（并行）→ Activity（串行）→ Reflection（串行）
```

并行执行使用 `CompletableFuture` 实现，每个 Agent 在独立线程中执行并实时推送流式输出，性能提升约 80%，总耗时从 5 分钟降至 1 分钟左右。

### 缓存策略

系统采用多级缓存策略提升响应速度：

| 缓存项 | TTL | 缓存策略 |
|--------|-----|----------|
| 行程详情 | 30 分钟 | 查询时缓存，更新/删除时失效 |
| RAG 检索结果 | 1 小时 | 语义检索时缓存，知识库更新时失效 |
| 用户偏好摘要 | 5 分钟 | 生成时缓存，偏好变更时失效 |
| 天气数据 | 30 分钟 | 查询时缓存，目的地+日期作为 key |
| 刷新令牌 | 7 天 | Redis 哈希存储，支持服务端吊销 |

### 重试与容错

- 每个 Agent 最多重试 3 次
- 重试间隔指数退避：1s → 2s → 3s
- 局部 Agent 失败不影响整体工作流（其他 Agent 继续执行）
- 会话级状态追踪，支持异步查看执行进度

---

## 开发规范

### 代码风格

- **包名**: 全小写，按模块分层：`{module}.api` / `{module}.application` / `{module}.domain` / `{module}.infrastructure`
- **类名**: 大驼峰，领域类无后缀、接口无 `I` 前缀
- **方法名**: 小驼峰，CRUD 统一使用 `save` / `findById` / `findByUserId` / `deleteById`
- **异常**: 业务异常使用 `BizException`，通过 `ResultCode` 枚举定义错误码
- **响应**: 所有 API 返回 `Result<T>` 统一格式

### 领域模型约定

- 使用不可变对象模式：`@Getter` + `@Builder` + `withXxx()` 方法生成新实例
- 工厂方法：`Entity.create(...)` 统一作为静态工厂
- 仓储接口定义在 domain 层，实现在 infrastructure 层
- 保持 domain 层零依赖（不依赖 Spring、MyBatis 等框架注解）

### 命名约定

| 层级 | 后缀 | 示例 |
|------|------|------|
| Controller | Controller | `TripController` |
| Application Service | AppService | `TripAppService` |
| Domain Entity | (无) | `Trip`, `Conversation` |
| Domain Repository | Repository | `TripRepository` |
| MyBatis Mapper | Mapper | `TripMapper` |
| Persistence Object | PO | `TripPO` |
| View Object | Vo | `TripVo` |
| Data Transfer Object | DTO / Request | `CreateTripRequest` |

---

## 测试

```bash
# 单元测试（跳过集成测试）
./mvnw test -Dtest='!*IntegrationTest,!TripDesignerApplicationTest'

# 全部测试（含集成测试，需要 Docker）
./mvnw test
```

---

## 部署

### Docker 部署

```bash
# 构建
./mvnw clean package -DskipTests

# 启动全部服务
docker compose up -d --build
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AI_API_KEY` | OpenAI API Key | 必填 |
| `AI_MODEL` | LLM 模型名 | `gpt-4o` |
| `JWT_SECRET` | JWT 签名密钥 | `application-local.yml` 配置 |
| `SPRING_PROFILES_ACTIVE` | 激活配置 | `local` |

### 配置说明

- `application.yml` — 公共配置
- `application-local.yml` — 本地开发配置（不提交）
- `application-prod.yml` — 生产环境配置
- `.env` — Docker Compose 环境变量

---

## License

MIT
