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

## 全球旅行知识库

### kb_countries — 国家表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(255) | 名称 |
| iso_code2 | VARCHAR(2) | ISO 2位代码 |
| iso_code3 | VARCHAR(3) | ISO 3位代码 |
| continent | VARCHAR(100) | 大洲 |
| capital | VARCHAR(100) | 首都 |
| currency_code | VARCHAR(3) | 货币代码 |
| languages | JSONB | 语言列表 |
| latitude | DECIMAL(9,6) | 纬度 |
| longitude | DECIMAL(9,6) | 经度 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_cities — 城市表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| country_id | BIGINT | 国家 ID |
| name | VARCHAR(255) | 名称 |
| name_local | VARCHAR(255) | 本地名称 |
| timezone | VARCHAR(50) | 时区 |
| population | INT | 人口 |
| latitude | DECIMAL(9,6) | 纬度 |
| longitude | DECIMAL(9,6) | 经度 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_pois — 兴趣点表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| city_id | BIGINT | 城市 ID |
| name | VARCHAR(255) | 名称 |
| name_local | VARCHAR(255) | 本地名称 |
| category | VARCHAR(50) | 分类 |
| subcategory | VARCHAR(50) | 子分类 |
| description | TEXT | 描述 |
| latitude | DECIMAL(9,6) | 纬度 |
| longitude | DECIMAL(9,6) | 经度 |
| address | VARCHAR(500) | 地址 |
| opening_hours | JSONB | 营业时间 |
| price_info | JSONB | 价格信息 |
| contact_info | JSONB | 联系方式 |
| rating | DECIMAL(3,2) | 评分 |
| review_count | INT | 评论数 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_travel_guides — 旅行指南表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| city_id | BIGINT | 城市 ID |
| country_id | BIGINT | 国家 ID |
| title | VARCHAR(255) | 标题 |
| language | VARCHAR(10) | 语言 |
| content | TEXT | 内容 |
| summary | TEXT | 摘要 |
| sections | JSONB | 章节列表 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_restaurants — 餐厅表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| city_id | BIGINT | 城市 ID |
| name | VARCHAR(255) | 名称 |
| name_local | VARCHAR(255) | 本地名称 |
| cuisine_type | VARCHAR(100) | 菜系 |
| price_range | VARCHAR(10) | 价格范围 |
| address | VARCHAR(500) | 地址 |
| latitude | DECIMAL(9,6) | 纬度 |
| longitude | DECIMAL(9,6) | 经度 |
| opening_hours | JSONB | 营业时间 |
| contact_info | JSONB | 联系方式 |
| rating | DECIMAL(3,2) | 评分 |
| review_count | INT | 评论数 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_hotels — 酒店表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| city_id | BIGINT | 城市 ID |
| name | VARCHAR(255) | 名称 |
| name_local | VARCHAR(255) | 本地名称 |
| category | VARCHAR(50) | 分类 |
| star_rating | INT | 星级 |
| address | VARCHAR(500) | 地址 |
| latitude | DECIMAL(9,6) | 纬度 |
| longitude | DECIMAL(9,6) | 经度 |
| amenities | JSONB | 设施列表 |
| room_info | JSONB | 房间信息 |
| contact_info | JSONB | 联系方式 |
| rating | DECIMAL(3,2) | 评分 |
| review_count | INT | 评论数 |
| price_from | DECIMAL(10,2) | 最低价格 |
| currency_code | VARCHAR(3) | 货币代码 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_routes — 路线表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| from_city_id | BIGINT | 出发城市 ID |
| to_city_id | BIGINT | 到达城市 ID |
| route_type | VARCHAR(50) | 路线类型 |
| distance_km | DECIMAL(10,2) | 距离(公里) |
| estimated_duration | JSONB | 预计时长 |
| transportation_methods | JSONB | 交通方式 |
| description | TEXT | 描述 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| content_hash | VARCHAR(64) | 内容哈希 |
| last_synced_at | TIMESTAMPTZ | 最后同步时间 |
| version | INT | 乐观锁 |

### kb_knowledge_chunks — 知识块表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| entity_type | VARCHAR(50) | 实体类型 |
| entity_id | BIGINT | 实体 ID |
| chunk_type | VARCHAR(50) | 块类型 |
| chunk_index | INT | 块索引 |
| title | VARCHAR(255) | 标题 |
| content | TEXT | 内容 |
| content_hash | VARCHAR(64) | 内容哈希 |
| embedding | vector(1536) | 向量嵌入 |
| metadata | JSONB | 元数据 |
| language | VARCHAR(10) | 语言 |
| token_count | INT | 令牌数 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
| version | INT | 乐观锁 |

### kb_knowledge_sources — 知识源表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| source_type | VARCHAR(50) | 源类型 |
| source_url | VARCHAR(500) | 源 URL |
| source_id | VARCHAR(255) | 源 ID |
| entity_type | VARCHAR(50) | 实体类型 |
| entity_id | BIGINT | 实体 ID |
| raw_content | TEXT | 原始内容 |
| content_hash | VARCHAR(64) | 内容哈希 |
| fetched_at | TIMESTAMPTZ | 获取时间 |
| etag | VARCHAR(255) | ETag |
| last_modified | VARCHAR(100) | 最后修改时间 |
| status | VARCHAR(20) | 状态 |
| error_message | TEXT | 错误信息 |
| retry_count | INT | 重试次数 |
| version | INT | 乐观锁 |

### kb_knowledge_relations — 知识关系表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| from_entity_type | VARCHAR(50) | 源实体类型 |
| from_entity_id | BIGINT | 源实体 ID |
| to_entity_type | VARCHAR(50) | 目标实体类型 |
| to_entity_id | BIGINT | 目标实体 ID |
| relation_type | VARCHAR(50) | 关系类型 |
| weight | DECIMAL(3,2) | 权重 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| version | INT | 乐观锁 |

### kb_knowledge_tags — 知识标签表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| entity_type | VARCHAR(50) | 实体类型 |
| entity_id | BIGINT | 实体 ID |
| tag | VARCHAR(100) | 标签 |
| tag_type | VARCHAR(50) | 标签类型 |
| language | VARCHAR(10) | 语言 |
| version | INT | 乐观锁 |

### kb_images — 图片表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| entity_type | VARCHAR(50) | 实体类型 |
| entity_id | BIGINT | 实体 ID |
| url | VARCHAR(500) | URL |
| url_thumb | VARCHAR(500) | 缩略图 URL |
| caption | VARCHAR(255) | 标题 |
| alt_text | VARCHAR(255) | 替代文本 |
| width | INT | 宽度 |
| height | INT | 高度 |
| metadata | JSONB | 元数据 |
| source | VARCHAR(50) | 数据源 |
| source_id | VARCHAR(255) | 源 ID |
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

## 知识库 ER 图

kb_countries ─── 1:N ─── kb_cities ─── 1:N ─── kb_pois
kb_countries ─── 1:N ─── kb_travel_guides
kb_cities ─── 1:N ─── kb_travel_guides
kb_cities ─── 1:N ─── kb_restaurants
kb_cities ─── 1:N ─── kb_hotels
kb_cities ─── 1:N ─── kb_routes (from_city_id, to_city_id)
kb_pois ─── 1:N ─── kb_knowledge_chunks
kb_travel_guides ─── 1:N ─── kb_knowledge_chunks
kb_restaurants ─── 1:N ─── kb_knowledge_chunks
kb_hotels ─── 1:N ─── kb_knowledge_chunks
kb_routes ─── 1:N ─── kb_knowledge_chunks
kb_knowledge_sources ─── 1:N ─── kb_knowledge_chunks
kb_knowledge_tags ─── N:N ─── (all entities via entity_type/entity_id)
kb_knowledge_relations ─── 1:N ─── (all entities via from/to entity_type/id)
kb_images ─── N:N ─── (all entities via entity_type/entity_id)
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

## 知识库索引策略

| 表 | 索引 | 说明 |
|------|------|------|
| kb_countries | iso_code2 (UNIQUE) | 快速查询 |
| kb_countries | name | 名称搜索 |
| kb_cities | country_id, name | 城市列表 |
| kb_cities | name | 名称搜索 |
| kb_pois | city_id, category | POI 列表 |
| kb_pois | city_id | 城市 POI |
| kb_pois | rating | 评分排序 |
| kb_restaurants | city_id, cuisine_type | 餐厅搜索 |
| kb_restaurants | rating | 评分排序 |
| kb_hotels | city_id, category | 酒店搜索 |
| kb_hotels | star_rating | 星级排序 |
| kb_knowledge_chunks | entity_type, entity_id | 实体块查询 |
| kb_knowledge_chunks | embedding (HNSW) | 向量检索 |
| kb_knowledge_chunks | content (GIN) | 全文检索 |
| kb_knowledge_sources | source_type, source_id | 源追踪 |
| kb_knowledge_relations | from_entity_type, from_entity_id | 关系查询 |
| kb_knowledge_relations | to_entity_type, to_entity_id | 反向关系 |
