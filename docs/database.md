# 数据库设计

## 设计原则

1. **乐观锁**: 所有表使用 `version` 字段 + MyBatis Plus `@Version` 注解管理并发
2. **审计字段**: `created_at` / `updated_at` 通过 MyBatis Plus 自动填充
3. **JSON 字段**: PostgreSQL 的 JSON/JSONB 类型存储偏好数据、标签数组等
4. **软删除**: 采用状态控制，不使用物理删除

## 表结构

### users — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| email | VARCHAR(255) | 邮箱（登录名，唯一） |
| password_hash | VARCHAR(255) | 密码哈希 |
| nickname | VARCHAR(100) | 昵称 |
| status | VARCHAR(20) | ACTIVE/DISABLED |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |
| version | INT | 乐观锁 |

### trips — 行程表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 所属用户 ID |
| title | VARCHAR(255) | 标题 |
| description | TEXT | 描述 |
| destination_name | VARCHAR(100) | 目的地 |
| status | VARCHAR(20) | DRAFT/PLANNING/CONFIRMED/COMPLETED/CANCELLED |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| budget | DECIMAL(12,2) | 预算 |
| version | INT | 乐观锁 |

### trip_days — 行程日表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| trip_id | BIGINT | 所属行程 ID |
| day_number | INT | 第几天 |
| date | DATE | 日期 |
| title | VARCHAR(255) | 主题 |
| description | TEXT | 描述 |
| version | INT | 乐观锁 |

### trip_activities — 活动表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| trip_day_id | BIGINT | 所属行程日 ID |
| name | VARCHAR(255) | 活动名称 |
| description | TEXT | 描述 |
| start_time | TIME | 开始时间 |
| end_time | TIME | 结束时间 |
| category | VARCHAR(50) | sightseeing/dining/transport/accommodation/shopping/other |
| place | VARCHAR(255) | 地点 |
| notes | TEXT | 备注 |
| sort_order | INT | 排序 |
| version | INT | 乐观锁 |

### destinations — 目的地表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(255) | 名称 |
| description | TEXT | 描述 |
| country | VARCHAR(100) | 国家 |
| city | VARCHAR(100) | 城市 |
| latitude | DECIMAL(9,6) | 纬度 |
| longitude | DECIMAL(9,6) | 经度 |
| image_url | VARCHAR(500) | 图片 URL |
| status | VARCHAR(20) | ACTIVE/INACTIVE |
| version | INT | 乐观锁 |

### conversations — 对话表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| title | VARCHAR(255) | 标题 |
| status | VARCHAR(20) | ACTIVE/ARCHIVED |
| last_message_at | TIMESTAMPTZ | 最后消息时间 |
| version | INT | 乐观锁 |

### conversation_messages — 对话消息表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| conversation_id | BIGINT | 对话 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(20) | USER/ASSISTANT |
| content | TEXT | 消息内容 |
| metadata | JSONB | JSON 元数据 |
| version | INT | 乐观锁 |

### workflow_sessions — 工作流会话表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| conversation_id | BIGINT | 对话 ID |
| user_id | BIGINT | 用户 ID |
| status | VARCHAR(20) | PENDING/RUNNING/COMPLETED/FAILED/CANCELLED |
| error_message | TEXT | 错误信息 |
| completed_at | TIMESTAMPTZ | 完成时间 |
| version | INT | 乐观锁 |

### workflow_steps — 工作流步骤表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | BIGINT | 会话 ID |
| agent_name | VARCHAR(50) | Agent 名称 |
| status | VARCHAR(20) | PENDING/RUNNING/COMPLETED/FAILED |
| input_context | TEXT | 输入上下文 |
| output_result | TEXT | 输出结果 |
| error_message | TEXT | 错误信息 |
| iteration | INT | 重试次数 |
| started_at | TIMESTAMPTZ | 开始时间 |
| completed_at | TIMESTAMPTZ | 完成时间 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### experiences — 旅行体验表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| trip_id | BIGINT | 行程 ID（可选） |
| trip_day_id | BIGINT | 行程日 ID（可选） |
| trip_activity_id | BIGINT | 活动 ID（可选） |
| title | VARCHAR(255) | 标题 |
| content | TEXT | 内容 |
| rating | INT | 1-5 评分 |
| tags | JSONB | 标签数组 |
| media_urls | JSONB | 媒体链接数组 |
| status | VARCHAR(20) | PUBLISHED/DRAFT |
| version | INT | 乐观锁 |

### user_preferences — 用户偏好表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| category | VARCHAR(50) | 偏好类别 |
| data | JSONB | 偏好数据 |
| source | VARCHAR(20) | MANUAL/AI_DISCOVERED |
| version | INT | 乐观锁 |

### trip_memories — 旅行记忆表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| trip_id | BIGINT | 行程 ID |
| memory_type | VARCHAR(50) | PREFERENCE_DISCOVERED/HIGHLIGHT/LESSON_LEARNED/LOWLIGHT/ADVICE |
| content | TEXT | 内容 |
| tags | JSONB | 标签数组 |
| version | INT | 乐观锁 |

### community_posts — 社区帖子表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| title | VARCHAR(255) | 标题 |
| content | TEXT | 内容 |
| category | VARCHAR(50) | 分类 |
| tags | JSONB | 标签数组 |
| status | VARCHAR(20) | PUBLISHED/DRAFT |
| like_count | INT | 点赞数 |
| comment_count | INT | 评论数 |
| version | INT | 乐观锁 |

### community_comments — 社区评论表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| post_id | BIGINT | 帖子 ID |
| user_id | BIGINT | 用户 ID |
| content | TEXT | 评论内容 |
| parent_id | BIGINT | 父评论 ID（支持回复） |
| version | INT | 乐观锁 |

### travel_teams — 旅行队伍表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 创建者 ID |
| name | VARCHAR(100) | 队伍名称 |
| description | TEXT | 描述 |
| destination | VARCHAR(100) | 目的地 |
| start_date | DATE | 出发日期 |
| end_date | DATE | 返回日期 |
| max_members | INT | 最大人数 |
| status | VARCHAR(20) | OPEN/CLOSED/COMPLETED |
| version | INT | 乐观锁 |

### team_members — 队伍成员表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| team_id | BIGINT | 队伍 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(20) | LEADER/MEMBER |
| joined_at | TIMESTAMPTZ | 加入时间 |

### team_applications — 队伍申请表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| team_id | BIGINT | 队伍 ID |
| user_id | BIGINT | 申请人 ID |
| message | TEXT | 申请理由 |
| status | VARCHAR(20) | PENDING/APPROVED/REJECTED |
| version | INT | 乐观锁 |

### price_monitors — 价格监测表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| monitor_type | VARCHAR(20) | FLIGHT/TRAIN/HOTEL |
| departure | VARCHAR(100) | 出发地 |
| destination | VARCHAR(100) | 目的地 |
| departure_time | TIME | 出发时间 |
| arrival_time | TIME | 到达时间 |
| target_price | DECIMAL(10,2) | 目标价格 |
| current_price | DECIMAL(10,2) | 当前价格 |
| ticket_class | VARCHAR(20) | 座位等级 |
| price_history | JSONB | 价格历史记录 |
| status | VARCHAR(20) | ACTIVE/INACTIVE/TRIGGERED |
| version | INT | 乐观锁 |

### train_ticket_info — 火车票信息表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| train_number | VARCHAR(50) | 车次 |
| departure | VARCHAR(100) | 出发地 |
| destination | VARCHAR(100) | 目的地 |
| departure_time | TIME | 出发时间 |
| arrival_time | TIME | 到达时间 |
| ticket_class | VARCHAR(20) | 座位等级 |
| price | DECIMAL(10,2) | 价格 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### multimodal_uploads — 多模态上传表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| file_name | VARCHAR(255) | 文件名 |
| file_path | VARCHAR(500) | 文件路径 |
| file_type | VARCHAR(50) | 文件类型 |
| file_size | BIGINT | 文件大小 |
| recognition_result | JSONB | AI 识别结果 |
| status | VARCHAR(20) | UPLOADED/RECOGNIZING/COMPLETED/FAILED |
| version | INT | 乐观锁 |

## ER 图

```
users ─── 1:N ─── trips ─── 1:N ─── trip_days ─── 1:N ─── trip_activities
users ─── 1:N ─── conversations ─── 1:N ─── conversation_messages
users ─── 1:N ─── experiences
users ─── 1:N ─── user_preferences
users ─── 1:N ─── trip_memories
users ─── 1:N ─── community_posts ─── 1:N ─── community_comments
users ─── 1:N ─── travel_teams ─── 1:N ─── team_members
users ─── 1:N ─── team_applications
users ─── 1:N ─── price_monitors
users ─── 1:N ─── multimodal_uploads
workflow_sessions ─── 1:N ─── workflow_steps
```

## 索引策略

| 表 | 索引 | 说明 |
|------|------|------|
| users | email (UNIQUE) | 登录查询 |
| trips | user_id, status | 列表查询 |
| trips | destination_name | 搜索 |
| conversations | user_id | 列表查询 |
| conversation_messages | conversation_id | 消息列表 |
| workflow_sessions | user_id, status | 进度查询 |
| workflow_steps | session_id, agent_name | 步骤查询 |
| community_posts | user_id, status, category | 帖子列表 |
| community_comments | post_id | 评论列表 |
| travel_teams | user_id, status | 队伍列表 |
| price_monitors | user_id, status | 监测列表 |
