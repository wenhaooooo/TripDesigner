# API 文档

## 认证模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/refresh` | 刷新 Token |
| POST | `/auth/logout` | 用户登出 |

## 用户模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/me` | 获取当前用户信息 |
| PUT | `/user/me` | 更新用户信息 |

## 行程模块

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

### 行程日

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/trips/{tripId}/days` | 行程日列表 |
| POST | `/trips/{tripId}/days` | 添加行程日 |
| PUT | `/trips/{tripId}/days/{dayId}` | 更新行程日 |
| DELETE | `/trips/{tripId}/days/{dayId}` | 删除行程日 |

### 活动

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/trips/{tripId}/days/{dayId}/activities` | 行程日活动列表 |
| POST | `/trips/{tripId}/days/{dayId}/activities` | 添加活动 |
| PUT | `/trips/{tripId}/days/{dayId}/activities/{activityId}` | 更新活动 |
| DELETE | `/trips/{tripId}/days/{dayId}/activities/{activityId}` | 删除活动 |

## 对话模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/conversations` | 会话列表 |
| POST | `/conversations` | 创建会话 |
| GET | `/conversations/{id}` | 会话详情 |
| PUT | `/conversations/{id}` | 更新会话标题 |
| DELETE | `/conversations/{id}` | 删除会话 |
| GET | `/conversations/{id}/messages` | 会话消息列表 |
| POST | `/conversations/{id}/messages` | 添加消息 |

## 目的地模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/destinations` | 目的地列表 |
| POST | `/destinations` | 创建目的地 |
| GET | `/destinations/{id}` | 目的地详情 |
| PUT | `/destinations/{id}` | 更新目的地 |
| DELETE | `/destinations/{id}` | 删除目的地 |

## AI 行程生成

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/ai/trip/generate` | 是 | 单 Agent 生成行程 |
| POST | `/ai/trip/generate/async` | 是 | 异步生成行程 |
| POST | `/ai/trip/chat` | 是 | 与已有行程对话以优化 |
| GET | `/ai/trip/{tripId}` | 是 | 获取生成的行程详情 |
| GET | `/ai/smoke` | 否 | AI 模型冒烟测试 |

## 多 Agent 工作流

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/ai/workflow/generate` | 是 | 执行完整多 Agent 工作流 |
| GET | `/ai/workflow/{sessionId}` | 是 | 查看工作流执行详情 |
| POST | `/ai/workflow/{sessionId}/cancel` | 是 | 取消工作流 |

## 体验分享

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/experiences` | 是 | 创建旅行体验 |
| GET | `/experiences` | 是 | 获取所有体验 |
| GET | `/experiences/{id}` | 是 | 获取单条体验详情 |
| GET | `/experiences/trip/{tripId}` | 是 | 获取某行程的所有体验 |
| PUT | `/experiences/{id}` | 是 | 更新体验 |
| DELETE | `/experiences/{id}` | 是 | 删除体验 |

## 用户偏好 & 记忆

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/memory/preferences` | 是 | 保存用户偏好 |
| GET | `/memory/preferences` | 是 | 获取所有偏好 |
| GET | `/memory/preferences/{id}` | 是 | 获取单条偏好详情 |
| DELETE | `/memory/preferences/{id}` | 是 | 删除偏好 |
| POST | `/memory/trip-memories` | 是 | 保存旅行记忆 |
| GET | `/memory/trip-memories` | 是 | 获取所有旅行记忆 |
| GET | `/memory/summary` | 是 | 获取偏好+记忆摘要 |

## 旅行社区

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/community/posts` | 是 | 帖子列表 |
| POST | `/community/posts` | 是 | 创建帖子 |
| GET | `/community/posts/{id}` | 是 | 帖子详情 |
| PUT | `/community/posts/{id}` | 是 | 更新帖子 |
| DELETE | `/community/posts/{id}` | 是 | 删除帖子 |
| POST | `/community/posts/{id}/comments` | 是 | 添加评论 |
| POST | `/community/posts/{id}/like` | 是 | 点赞/取消点赞 |
| POST | `/community/posts/{id}/favorite` | 是 | 收藏/取消收藏 |

## 旅行组队

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/teams` | 是 | 队伍列表 |
| POST | `/teams` | 是 | 创建队伍 |
| GET | `/teams/{id}` | 是 | 队伍详情 |
| POST | `/teams/{id}/apply` | 是 | 申请加入队伍 |
| PUT | `/teams/{id}/applications/{applicationId}` | 是 | 审核申请 |
| GET | `/teams/{id}/members` | 是 | 队伍成员列表 |

## 价格监测

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/price-monitors` | 是 | 创建价格监测 |
| GET | `/price-monitors` | 是 | 监测列表 |
| GET | `/price-monitors/{id}` | 是 | 监测详情 |
| DELETE | `/price-monitors/{id}` | 是 | 删除监测 |

## 多模态

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/multimodal/upload` | 是 | 上传图片识别 |

## 响应格式

所有 API 返回统一格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "abc-123"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，0 表示成功 |
| message | string | 提示信息 |
| data | any | 响应数据 |
| traceId | string | 请求追踪 ID |

## 错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 无效 Token |
| 1002 | 用户未登录 |
| 2001 | 行程不存在 |
| 3001 | AI 生成失败 |
| 4001 | 参数校验失败 |
