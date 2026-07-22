# 开发指南

## 代码风格

### 包结构

按模块分层，遵循 DDD 架构：

```
{module}.api          # Controller + DTO/VO
{module}.application  # AppService
{module}.domain       # Entity + Repository 接口
{module}.infrastructure  # Repository Impl + Mapper + PO
```

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
| Data Transfer Object | Request/DTO | `CreateTripRequest` |

### 方法命名

小驼峰，CRUD 统一使用：

| 操作 | 方法名 |
|------|--------|
| 创建 | `save()`, `create()` |
| 按 ID 查询 | `findById()` |
| 按用户查询 | `findByUserId()` |
| 分页查询 | `findPage()` |
| 更新 | `update()` |
| 删除 | `deleteById()` |

## 领域模型约定

### 不可变对象模式

使用 `@Getter` + `@Builder` + `withXxx()` 方法生成新实例：

```java
@Getter
@Builder
public class Trip {
    private final Long id;
    private final String title;
    
    public Trip withTitle(String title) {
        return Trip.builder()
                .id(this.id)
                .title(title)
                .build();
    }
}
```

### 工厂方法

使用 `Entity.create(...)` 作为统一的静态工厂：

```java
public static Trip create(String title, String description) {
    return Trip.builder()
            .title(title)
            .description(description)
            .status(TripStatus.DRAFT)
            .build();
}
```

### 仓储接口

仓储接口定义在 domain 层，实现在 infrastructure 层：

```java
// domain 层 - 接口
public interface TripRepository {
    Trip findById(Long id);
}

// infrastructure 层 - 实现
@Repository
public class TripRepositoryImpl implements TripRepository {
    // MyBatis Mapper 实现
}
```

### Domain 层零依赖

保持 domain 层不依赖 Spring、MyBatis 等框架注解：

- ✅ 纯 Java 类，仅使用 Lombok
- ❌ 不使用 `@Component`、`@Autowired`、`@Mapper` 等

## 异常处理

### 业务异常

使用 `BizException` + `ResultCode` 枚举：

```java
throw new BizException(ResultCode.TRIP_NOT_FOUND, "Trip not found: " + tripId);
```

### 全局异常处理

`GlobalExceptionHandler` 统一处理所有异常：

- `BizException`: 返回业务错误码和消息
- `Exception`: 返回系统错误码和通用消息

### 错误码规范

| 错误码范围 | 说明 |
|-----------|------|
| 1000-1099 | 认证相关 |
| 2000-2099 | 行程相关 |
| 3000-3099 | AI 相关 |
| 4000-4099 | 参数校验 |
| 5000-5099 | 系统错误 |

## 响应格式

所有 API 返回统一格式 `Result<T>`：

```java
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;
    
    public static <T> Result<T> success(T data) { ... }
    public static Result<Void> success() { ... }
    public static <T> Result<T> error(ResultCode code) { ... }
}
```

## 审计字段

所有实体通过 MyBatis Plus 自动填充：

```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createdAt;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

配置类：`MyBatisPlusMetaObjectHandler`

## 乐观锁

所有表使用 `version` 字段：

```java
@Version
@TableField(fill = FieldFill.INSERT)
private Integer version;
```

MyBatis Plus 配置：

```java
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: status
      version-field: version
```

## 测试

### 单元测试

```bash
# 单元测试（跳过集成测试）
./mvnw test -Dtest='!*IntegrationTest,!TripDesignerApplicationTest'
```

### 全部测试

```bash
# 全部测试（含集成测试，需要 Docker）
./mvnw test
```

### 冒烟测试

```bash
# AI 模型冒烟测试
curl "http://localhost:8080/ai/smoke?prompt=hello"
```

## 部署

### Docker 部署

```bash
# 构建
./mvnw clean package -DskipTests

# 启动全部服务
docker compose up -d --build
```

### 本地开发

```bash
# 1. 启动中间件
docker compose up -d postgres redis

# 2. 启动后端
./mvnw spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_DB` | 数据库名 | tripdesigner |
| `POSTGRES_USER` | 数据库用户 | tripdesigner |
| `POSTGRES_PASSWORD` | 数据库密码 | tripdesigner |
| `AI_API_KEY` | AI API Key | 必填 |
| `AI_BASE_URL` | AI API 基础 URL | https://token.sensenova.cn |
| `AI_MODEL` | LLM 模型名 | sensenova-6.7-flash-lite |
| `JWT_SECRET` | JWT 签名密钥 | 必填（至少 256 位） |
| `SPRING_PROFILES_ACTIVE` | 激活配置 | local |

### 配置文件

| 文件 | 说明 | 是否提交 |
|------|------|----------|
| `application.yml` | 公共配置 | ✅ 是 |
| `application-local.yml` | 本地开发配置 | ❌ 否 |
| `application-prod.yml` | 生产环境配置 | ✅ 是 |
| `.env` | Docker Compose 环境变量 | ❌ 否 |
| `.env.example` | 环境变量示例 | ✅ 是 |

## 前端开发

### 构建命令

```bash
# 开发模式
npm run dev

# 生产构建（输出到 src/main/resources/static/）
npm run build

# 类型检查
npm run lint
```

### 目录结构

```
frontend/src/
├── api/          # API 接口定义
├── components/   # 公共组件
├── layouts/      # 页面布局
├── pages/        # 页面组件
├── router/       # 路由配置
├── stores/       # Pinia 状态管理
├── styles/       # 全局样式
├── types/        # TypeScript 类型定义
├── utils/        # 工具函数
├── App.vue       # 根组件
└── main.ts       # 入口文件
```

### 代码规范

- 使用 Composition API
- 组件命名使用 PascalCase
- 路由命名使用 kebab-case
- API 接口统一在 `api/` 目录定义
- 状态管理使用 Pinia
- 类型定义统一在 `types/api.ts`

## CI/CD

### GitHub Actions

配置文件：`.github/workflows/ci.yml`

自动执行：
- 代码格式检查
- 单元测试
- 构建验证

### 触发条件

- 推送代码到 main 分支
- 创建 Pull Request

## 代码审查

### 审查要点

1. **DDD 分层**: 是否符合领域驱动设计
2. **依赖方向**: Domain 层不应依赖 Infrastructure 层
3. **异常处理**: 是否正确使用 BizException
4. **事务管理**: 是否需要 @Transactional
5. **缓存策略**: 是否正确处理缓存失效
6. **并发安全**: 是否需要乐观锁
7. **参数校验**: 是否有足够的输入验证

### 提交规范

```
<type>: <description>

<optional body>

<optional footer>
```

类型：
- feat: 新功能
- fix: 修复 bug
- refactor: 重构
- docs: 文档
- test: 测试
- chore: 构建/工具

## 性能优化建议

1. **批量操作**: 使用 MyBatis Plus 的批量插入/更新
2. **分页查询**: 使用 `Page` 对象，避免全表扫描
3. **缓存优先**: 查询前检查 Redis 缓存
4. **异步处理**: 耗时操作使用 `@Async` 或消息队列
5. **连接池**: 合理配置数据库连接池大小
6. **索引优化**: 为高频查询字段添加索引

## 安全建议

1. **输入验证**: 所有用户输入必须校验
2. **SQL 注入**: 使用 MyBatis Plus 参数化查询
3. **XSS 攻击**: 前端使用 DOMPurify 净化 HTML
4. **JWT 安全**: 设置合理的 token 过期时间
5. **密码安全**: 使用 BCrypt 哈希存储密码
6. **文件上传**: 限制文件类型和大小
7. **权限控制**: 每个 API 检查用户权限
