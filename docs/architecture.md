# 项目架构与完整接口文档

> 基于 SpringBoot+RAG 的校园参观预约与智能咨询系统
>
> 哈尔滨剑桥学院 · 智能科学与工程学院 · 2026 届毕业设计
>
> 学生：钟啸林　指导教师：郭旭 副教授

---

## 一、文档说明

本文档定义系统**整体技术架构、后端分层结构、模块职责与依赖、统一返回格式、全局状态码、全局异常处理规则、全部接口清单（100% 全覆盖）**。

**配套文件**：
- 数据库设计：[database.md](./database.md)
- 前端原型：[frontend-prototype.md](./frontend-prototype.md)、[产品原型/](./产品原型/)
- 后端配置：`backend/src/main/resources/application.yml`

**强制约束**：开发阶段（Stage2）必须 100% 对标本文档接口路径、请求方法、参数名、参数类型、是否必填、返回结构，禁止私自新增接口、改路径、改参数。任何变更需走需求变更流程并同步更新本文档。

---

## 二、整体技术架构

### 2.1 技术架构分层图

```
┌──────────────────────────────────────────────────────────────────┐
│                         前端层（Vue3）                            │
│  访客前台（11 页）            │         管理员后台（9 页）           │
│  Element-Plus + Vue Router     │         ECharts + Element-Plus     │
│  Axios → 统一请求拦截器        │         Pinia 全局状态              │
└────────────────┬──────────────┴───────────────────────────────────┘
                 │ HTTP / JSON / JWT
                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                    后端表现层（Controller）                       │
│  AuthController / NoticeController / SessionController          │
│  ReservationController / VisitorController / AdminController    │
│  KnowledgeController / ChatController / StatsController         │
│  JWT 拦截器校验 → @RequiresRole 注解权限 → 调 Service             │
└────────────────┬─────────────────────────────────────────────────┘
                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                    后端业务层（Service）                          │
│  事务边界、业务校验、跨模块协作、统一返回 Result                  │
│  ReservationService（乐观锁防超卖）/ ChatService（RAG 编排）     │
└────────────────┬─────────────────────────────────────────────────┘
                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                    后端数据层（Mapper / RAG / 外部）              │
│  MyBatis-Plus Mapper  ───→ MySQL 8.0（8 张业务表）              │
│  MilvusService        ───→ Milvus（向量库 campus_knowledge）     │
│  BgeEmbeddingClient   ───→ 本地 BGE-small-zh 模型（768 维）       │
│  DashScopeClient      ───→ 阿里百炼 qwen-plus 大模型             │
│  PdfBox / POI         ───→ 文档解析（PDF/TXT/Word）              │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型定稿

| 层级 | 选型 | 版本 | 用途 |
|------|------|------|------|
| 后端框架 | Spring Boot | 3.2.x | 企业级 Java 框架 |
| 构建工具 | Maven | 3.9+ | 依赖管理 |
| ORM | MyBatis-Plus | 3.5.x | 增强版 MyBatis，支持乐观锁/逻辑删除/自动填充 |
| 认证 | JWT (jjwt) | 0.12.x | 无状态用户认证 |
| 工具库 | Hutool | 5.8.x | 通用工具（加密、HTTP、IO） |
| 文档解析 | PDFBox + POI | 3.0 / 5.2 | PDF 与 Word 文档内容提取 |
| 向量数据库 | Milvus SDK | 2.3.x | 向量存储与相似度检索 |
| 嵌入模型 | BGE-small-zh | v1.5 | 本地 768 维中文嵌入模型 |
| 大模型 API | 阿里百炼 DashScope SDK | - | 通义千问 qwen-plus 对话 |
| 前端框架 | Vue | 3.4.x | 渐进式前端框架 |
| 构建工具 | Vite | 5.x | 前端快速构建 |
| UI 库 | Element Plus | 2.7.x | 企业级组件库 |
| 状态管理 | Pinia | 2.x | Vue3 官方状态管理 |
| 路由 | Vue Router | 4.x | 前端路由 |
| HTTP | Axios | 1.x | 前端 HTTP 请求 |
| 图表 | ECharts | 5.x | 后台数据可视化 |

### 2.3 关键技术决策

**为什么不用 LangChain-Java？**
- LangChain-Java 依赖重、版本迭代快、API 不稳定，毕设项目调试成本高
- 手写 RAG 流程透明可控：BGE 编码 → Milvus 检索 → 拼提示词 → 百炼生成 → 写消息表
- 利于答辩讲解每一步原理，论文可逐行解释

**为什么用 BGE-small-zh 而不是云端嵌入？**
- 本地部署免费、不消耗 API 额度、数据不出校
- small 版 768 维，CPU 也能跑，毕设硬件要求低
- 百炼云端 `text-embedding-v2` 作为降级备份（见 `application.yml`）

**为什么预约名额用乐观锁？**
- 毕设项目并发量小，乐观锁（version 字段）实现简单、无锁等待
- 与悲观锁（SELECT FOR UPDATE）相比，乐观锁适合读多写少场景
- MyBatis-Plus `@Version` 注解原生支持

---

## 三、后端项目结构

```
backend/
├── src/main/java/com/campus/visit/
│   ├── CampusVisitApplication.java          # 启动类
│   ├── config/                              # 配置类
│   │   ├── CorsConfig.java                  # 跨域配置（已建）
│   │   ├── MybatisPlusConfig.java           # MyBatis-Plus 配置（已建，乐观锁/自动填充插件）
│   │   ├── WebMvcConfig.java                # 静态资源/拦截器注册（已建）
│   │   ├── JwtConfig.java                   # JWT 配置
│   │   └── MilvusConfig.java                # Milvus 客户端 Bean
│   ├── common/                              # 公共类（已建基础）
│   │   ├── Result.java                      # 统一返回格式（已建）
│   │   ├── ResultCode.java                  # 状态码枚举（已建）
│   │   ├── BusinessException.java           # 业务异常（已建）
│   │   └── GlobalExceptionHandler.java      # 全局异常处理（已建）
│   ├── interceptor/                         # 拦截器
│   │   └── JwtAuthInterceptor.java          # JWT 校验拦截器
│   ├── annotation/                          # 自定义注解
│   │   ├── RequiresLogin.java               # 标记接口需登录
│   │   └── RequiresRole.java                # 标记接口所需角色
│   ├── controller/                          # 控制层（9 个）
│   │   ├── AuthController.java              # 认证（登录/注册/改密/个人信息）
│   │   ├── NoticeController.java            # 公告（前台查询）
│   │   ├── SessionController.java           # 场次（前台查询）
│   │   ├── ReservationController.java       # 预约（访客侧）
│   │   ├── VisitorController.java           # 访客个人信息
│   │   ├── AdminNoticeController.java       # 管理员公告管理
│   │   ├── AdminSessionController.java      # 管理员场次管理
│   │   ├── AdminReservationController.java  # 管理员预约审核
│   │   ├── AdminVisitorController.java      # 管理员访客管理
│   │   ├── AdminAdminController.java        # 管理员账号管理
│   │   ├── KnowledgeController.java         # RAG 知识库管理
│   │   ├── ChatController.java              # AI 咨询
│   │   └── StatsController.java             # 统计与问答日志
│   ├── service/                             # 业务层接口
│   │   ├── AuthService.java
│   │   ├── NoticeService.java
│   │   ├── SessionService.java
│   │   ├── ReservationService.java
│   │   ├── VisitorService.java
│   │   ├── AdminUserService.java
│   │   ├── KnowledgeService.java
│   │   ├── ChatService.java
│   │   ├── StatsService.java
│   │   └── impl/                            # 业务实现
│   ├── mapper/                              # 数据访问层
│   │   ├── VisitorUserMapper.java
│   │   ├── AdminUserMapper.java
│   │   ├── CampusNoticeMapper.java
│   │   ├── VisitSessionMapper.java
│   │   ├── VisitReservationMapper.java
│   │   ├── KnowledgeDocMapper.java
│   │   ├── ChatSessionMapper.java
│   │   └── ChatMessageMapper.java
│   ├── entity/                              # 实体类（与表一一对应）
│   │   ├── VisitorUser.java
│   │   ├── AdminUser.java
│   │   ├── CampusNotice.java
│   │   ├── VisitSession.java
│   │   ├── VisitReservation.java
│   │   ├── KnowledgeDoc.java
│   │   ├── ChatSession.java
│   │   └── ChatMessage.java
│   ├── dto/                                 # 请求入参对象
│   │   ├── auth/LoginDTO.java
│   │   ├── auth/RegisterDTO.java
│   │   ├── auth/ChangePasswordDTO.java
│   │   ├── notice/NoticeSaveDTO.java
│   │   ├── notice/NoticeQueryDTO.java
│   │   ├── session/SessionSaveDTO.java
│   │   ├── session/SessionQueryDTO.java
│   │   ├── reservation/ReservationSubmitDTO.java
│   │   ├── reservation/ReservationAuditDTO.java
│   │   ├── reservation/ReservationQueryDTO.java
│   │   ├── visitor/VisitorProfileDTO.java
│   │   ├── visitor/VisitorQueryDTO.java
│   │   ├── admin/AdminSaveDTO.java
│   │   ├── knowledge/KnowledgeQueryDTO.java
│   │   └── chat/ChatAskDTO.java
│   ├── vo/                                   # 返回视图对象
│   │   ├── auth/LoginVO.java                # {token, role, userId, realName}
│   │   ├── notice/NoticeListVO.java
│   │   ├── notice/NoticeDetailVO.java
│   │   ├── session/SessionListVO.java
│   │   ├── session/SessionDetailVO.java
│   │   ├── reservation/ReservationListVO.java
│   │   ├── reservation/ReservationDetailVO.java
│   │   ├── visitor/VisitorListVO.java
│   │   ├── admin/AdminListVO.java
│   │   ├── knowledge/KnowledgeListVO.java
│   │   ├── chat/ChatAskVO.java              # {answer, referDocName, referChunk}
│   │   ├── chat/ChatSessionVO.java
│   │   ├── chat/ChatMessageVO.java
│   │   └── stats/DashboardVO.java
│   ├── rag/                                  # RAG 核心模块
│   │   ├── BgeEmbeddingClient.java           # 本地 BGE 嵌入加载与编码
│   │   ├── MilvusService.java                # Milvus 集合管理/插入/检索/删除
│   │   ├── DocumentParser.java              # PDF/Word/TXT 内容提取
│   │   ├── TextChunker.java                  # 文本分块（chunk_size=500, overlap=100）
│   │   ├── PromptBuilder.java                # RAG 提示词拼接
│   │   └── DashScopeClient.java              # 阿里百炼大模型调用
│   └── utils/                                # 工具类
│       ├── JwtUtil.java                      # JWT 生成/解析/校验
│       ├── BcryptUtil.java                   # 密码加密/校验
│       ├── UserContext.java                  # 当前登录用户上下文（ThreadLocal）
│       └── PageUtil.java                    # 分页工具
├── src/main/resources/
│   ├── application.yml                       # 主配置（已建）
│   ├── application-dev.yml                   # 开发环境覆盖
│   ├── application-prod.yml                  # 生产环境覆盖
│   ├── mapper/                               # MyBatis XML（如有自定义 SQL）
│   └── static/uploads/knowledge/             # 知识库文档上传目录
├── src/test/java/                            # 测试代码
└── pom.xml                                   # Maven 依赖（已建）
```

---

## 四、模块拆分与职责

| 模块 | 对应 Controller | 核心职责 | 依赖模块 |
|------|----------------|---------|---------|
| 用户认证 | AuthController | 注册、登录、改密、当前用户信息 | JwtUtil、BcryptUtil |
| 校园公告 | NoticeController / AdminNoticeController | 公告发布、下架、删除、查询 | - |
| 参观场次 | SessionController / AdminSessionController | 场次增改、上下架、查询 | - |
| 访客预约 | ReservationController | 提交预约、我的预约、取消 | SessionService（乐观锁扣名额） |
| 预约审核 | AdminReservationController | 管理员审核通过/驳回 | ReservationService、SessionService |
| 访客管理 | VisitorController / AdminVisitorController | 个人信息修改、冻结/解冻 | - |
| 管理员账号 | AdminAdminController | 新增管理员、重置密码 | AdminUserService |
| RAG 知识库 | KnowledgeController | 文档上传、解析、分块、向量化、删除 | rag 模块全部组件 |
| AI 智能咨询 | ChatController | 问答编排、会话管理、消息记录 | rag 模块 + ChatService |
| 问答日志统计 | StatsController | 看板、问答日志、高频问题 | ChatMessageMapper |

**跨模块协作点**：
- 预约提交 → 调 SessionService 校验名额 + 扣减（乐观锁）
- 预约审核通过 → 调 SessionService 扣减名额（乐观锁）；驳回/取消不扣减
- 文档删除 → 先调 MilvusService 删向量，再删 MySQL
- AI 问答 → 调 BgeEmbeddingClient 编码 + MilvusService 检索 + PromptBuilder 拼提示词 + DashScopeClient 生成

---

## 五、统一返回格式

### 5.1 Result 类结构

```java
public class Result<T> {
    private Integer code;     // 状态码：200 成功，非 200 失败
    private String message;   // 提示信息
    private T data;           // 业务数据

    public static <T> Result<T> success(T data);
    public static <T> Result<T> success();
    public static <T> Result<T> fail(ResultCode code);
    public static <T> Result<T> fail(Integer code, String message);
}
```

### 5.2 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "role": "visitor",
    "userId": 1001,
    "realName": "张三"
  }
}
```

### 5.3 失败响应示例

```json
{
  "code": 40010,
  "message": "用户名已被冻结，请联系管理员",
  "data": null
}
```

### 5.4 分页响应统一结构

分页查询返回 `data` 字段统一为：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

> 与 MyBatis-Plus `IPage` 字段名对齐，前端可直接使用。

---

## 六、全局状态码

| 状态码 | 含义 | 触发场景 |
|--------|------|---------|
| 200 | 成功 | 一切正常 |
| 40001 | 参数校验失败 | 字段格式/长度/必填校验不通过 |
| 40101 | 未登录或 token 过期 | 拦截器校验 token 失败 |
| 40301 | 无权限访问 | 角色不匹配（访客访问 admin 接口） |
| 40010 | 用户名或密码错误 | 登录失败 |
| 40011 | 用户名已存在 | 注册时 |
| 40012 | 账号已被冻结 | 访客登录被冻结 |
| 40013 | 旧密码错误 | 修改密码 |
| 40401 | 资源不存在 | 公告/场次/预约/文档 ID 不存在 |
| 40020 | 重复预约 | 同一场次重复预约 |
| 40021 | 名额不足 | 场次剩余名额不够 |
| 40022 | 订单状态不可流转 | 取消/审核时状态非法 |
| 40023 | 场次已下架/过期 | 提交预约时场次不可预约 |
| 40030 | 文档类型不支持 | 上传非 pdf/txt/docx |
| 40031 | 文档解析失败 | PDFBox/POI 解析异常 |
| 40040 | AI 服务调用失败 | 百炼超时/异常 |
| 40050 | Milvus 操作失败 | 向量库异常 |
| 50000 | 服务器内部错误 | 未捕获异常兜底 |

---

## 七、全局异常处理

### 7.1 异常分类与处理

| 异常类型 | 处理方式 | 返回码 |
|---------|---------|--------|
| `BusinessException` | 携带自定义 code + message，前端友好提示 | 业务码 |
| `MethodArgumentNotValidException` | 参数校验失败 | 40001 |
| `MaxUploadSizeExceededException` | 上传文件超 50MB | 40001 |
| `NoHandlerFoundException` | 接口不存在 | 40401 |
| `HttpRequestMethodNotSupportedException` | 请求方法不匹配 | 40001 |
| `Exception` 兜底 | 日志记录全栈，前端返回"系统繁忙" | 50000 |

### 7.2 全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(40001, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleAll(Exception e) {
        log.error("系统异常", e);
        return Result.fail(50000, "系统繁忙，请稍后重试");
    }
}
```

---

## 八、JWT 鉴权与权限拦截

### 8.1 JWT 载荷

```json
{
  "userId": 1001,
  "role": "visitor",         // visitor / admin
  "isSuper": false,           // 仅 admin 有意义
  "username": "zhangsan",
  "iat": 1693400000,
  "exp": 1693486400           // 24 小时后过期
}
```

### 8.2 拦截器流程

```
请求 → JwtAuthInterceptor.preHandle()
  ├─ 目标方法无 @RequiresLogin 注解 → 放行
  ├─ 有 @RequiresLogin
  │   ├─ Authorization 头为空 → 抛 40101
  │   ├─ JwtUtil.verify(token) 失败 → 抛 40101
  │   ├─ 解析得到 userId/role/isSuper → 写入 UserContext（ThreadLocal）
  │   └─ 方法标注 @RequiresRole("admin") 且当前 role != admin → 抛 40301
  └─ 放行
→ Controller 方法 → UserContext.getCurrent() 取当前用户
→ 请求结束 → afterCompletion 清理 ThreadLocal
```

### 8.3 公开接口（不走拦截器）

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/notice/list`、`/api/notice/latest`、`/api/notice/{id}`
- `GET /api/session/available`、`/api/session/latest`、`/api/session/{id}`
- `GET /api/public/home`（首页聚合）

---

## 九、接口前缀与路径规范

- **统一前缀**：所有接口均以 `/api` 开头（`application.yml` `server.servlet.context-path: /api`）
- **前台接口**：`/api/{模块}/{动作}`（如 `/api/notice/list`），无需登录或访客登录
- **后台接口**：`/api/admin/{模块}/{动作}`（如 `/api/admin/notice/page`），必须管理员登录
- **路径风格**：小写短横线分隔多词（如 `/reset-password`）
- **请求方法语义**：GET 查询、POST 新增/动作、PUT 修改、DELETE 删除

---

## 十、完整接口清单

> 共 **38 个接口**，按模块顺序列出。每个接口标注请求/响应/异常。
>
> 「认证」列：✅ 需登录；❌ 公开；🔒 管理员登录

### 模块 1：用户认证

#### 1.1 访客注册

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/auth/register` |
| 认证 | ❌ 公开 |
| 请求体 | `RegisterDTO` |

**请求参数（RegisterDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | 是 | 4~20 位字母数字下划线，唯一 | 用户名 |
| password | String | 是 | 6~20 位含字母与数字 | 密码明文 |
| realName | String | 是 | 2~10 字 | 真实姓名 |
| phone | String | 是 | 11 位中国手机号 | 联系手机 |

**响应**：`Result<Void>`，data 为 null

**异常**：
- 40011 用户名已存在
- 40001 参数校验失败

---

#### 1.2 统一登录

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/auth/login` |
| 认证 | ❌ 公开 |
| 请求体 | `LoginDTO` |

**请求参数（LoginDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | 是 | 非空 | 用户名（访客或管理员账号） |
| password | String | 是 | 非空 | 密码明文 |

**响应（LoginVO）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| token | String | JWT 令牌 |
| role | String | visitor / admin |
| userId | Long | 用户 ID |
| realName | String | 真实姓名 |
| isSuper | Boolean | 是否超管（仅 role=admin 时有意义） |

**业务逻辑**：
1. 先查 `visitor_user` 表，匹配 username + BCrypt 校验密码
   - 匹配且 `status=0` → 生成 visitor token 返回
   - 匹配且 `status=1` → 抛 40012 账号已冻结
2. 访客表未匹配 → 查 `admin_user` 表
   - 匹配 → 生成 admin token（含 isSuper）返回
3. 两表均未匹配 → 抛 40010 用户名或密码错误

**异常**：40010、40012

---

#### 1.3 修改密码

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/auth/change-password` |
| 认证 | ✅ 已登录（访客与管理员共用） |

**请求参数（ChangePasswordDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| oldPassword | String | 是 | 非空 | 原密码明文 |
| newPassword | String | 是 | 6~20 位含字母与数字，不与原密码相同 | 新密码明文 |

**响应**：`Result<Void>`

**业务逻辑**：根据 `UserContext.role` 决定查 `visitor_user` 还是 `admin_user`，BCrypt 校验原密码 → 加密新密码更新。

**异常**：40013 旧密码错误、40001 新密码格式不合法

---

#### 1.4 当前登录人信息

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/auth/profile` |
| 认证 | ✅ 已登录 |

**响应**：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| role | String | visitor / admin |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| phone | String | 访客有，管理员为 null |
| isSuper | Boolean | 管理员有，访客为 false |

---

### 模块 2：校园公告（前台）

#### 2.1 公告分页列表

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/notice/list` |
| 认证 | ❌ 公开 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（NoticeListVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 公告 ID |
| title | String | 标题 |
| publishTime | DateTime | 发布时间 |
| summary | String | 摘要（正文前 80 字） |

**业务逻辑**：`WHERE status=1 AND deleted=0 ORDER BY publish_time DESC`

---

#### 2.2 最新公告（首页用）

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/notice/latest` |
| 认证 | ❌ 公开 |

**请求参数**：`count`（Integer，默认 3，最大 10）

**响应**：`Result<List<NoticeListVO>>`，结构与 2.1 单条一致

---

#### 2.3 公告详情

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/notice/{id}` |
| 认证 | ❌ 公开 |

**路径参数**：`id` Long

**响应（NoticeDetailVO）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 公告 ID |
| title | String | 标题 |
| content | String | 正文全文 |
| publishTime | DateTime | 发布时间 |
| prevId | Long | 上一条 ID，无则 null |
| nextId | Long | 下一条 ID，无则 null |

**异常**：40401 公告不存在或未发布

---

### 模块 3：参观场次（前台）

#### 3.1 可预约场次列表

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/session/available` |
| 认证 | ❌ 公开 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| startDate | Date | 否 | 今天 | 起始日期 |
| endDate | Date | 否 | 今天+30 天 | 截止日期 |
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（SessionListVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 场次 ID |
| visitDate | Date | 参观日期 |
| timeSlot | String | 时段 |
| maxPeople | Integer | 最大容纳人数 |
| usedPeople | Integer | 已预约人数 |
| remaining | Integer | 剩余名额（= max - used） |

**业务逻辑**：`WHERE status=0 AND deleted=0 AND visit_date >= today AND visit_date <= endDate ORDER BY visit_date, time_slot`；过滤已过期场次（visit_date < today）。

---

#### 3.2 最新可预约场次（首页用）

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/session/latest` |
| 认证 | ❌ 公开 |

**请求参数**：`count`（Integer，默认 3，最大 10）

**响应**：`Result<List<SessionListVO>>`

---

#### 3.3 场次详情

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/session/{id}` |
| 认证 | ❌ 公开 |

**路径参数**：`id` Long

**响应（SessionDetailVO）**：字段同 3.1 单条 + `status` 字段（0/1）

**异常**：40401 场次不存在

---

### 模块 4：访客预约

#### 4.1 提交预约

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/reservation` |
| 认证 | ✅ 访客登录 |

**请求参数（ReservationSubmitDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| sessionId | Long | 是 | 必须存在且开放且未过期 | 场次 ID |
| realName | String | 是 | 2~10 字 | 真实姓名 |
| phone | String | 是 | 11 位手机号 | 联系手机 |
| peopleCount | Integer | 是 | 1~剩余名额，最大 50 | 参观人数 |
| reason | String | 是 | 5~200 字 | 参观事由 |

**响应**：`Result<Long>`，返回预约订单 ID

**业务逻辑**：
1. 校验场次存在、`status=0`、`visit_date >= today`
2. 校验 `used_people + peopleCount <= max_people`
3. 重复预约校验：`SELECT COUNT(*) FROM visit_reservation WHERE session_id=? AND visitor_id=? AND status IN (0,1)` > 0 → 抛 40020
4. 插入订单（status=0 待审核）
5. **乐观锁扣减名额**：
   ```sql
   UPDATE visit_session SET used_people = used_people + ?, version = version + 1
   WHERE id = ? AND version = ? AND used_people + ? <= max_people
   ```
6. 影响行数=0 → 回滚事务，抛 40021 名额不足

**异常**：40401 场次不存在、40023 场次已下架/过期、40020 重复预约、40021 名额不足、40001 参数校验失败

> 注：本系统预约即扣名额，便于前端实时显示剩余；审核驳回时回滚名额（used_people - people_count）。

---

#### 4.2 我的预约列表

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/reservation/my` |
| 认证 | ✅ 访客登录 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| status | Integer | 否 | null（全部） | 0/1/2/3 |
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（ReservationListVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |
| sessionId | Long | 场次 ID |
| visitDate | Date | 参观日期 |
| timeSlot | String | 时段 |
| peopleCount | Integer | 参观人数 |
| status | Integer | 订单状态 |
| statusText | String | 状态中文（待审核/通过/驳回/已取消） |
| submitTime | DateTime | 提交时间 |

**业务逻辑**：`WHERE visitor_id = 当前访客ID [AND status=?] ORDER BY submit_time DESC`

---

#### 4.3 预约详情

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/reservation/{id}` |
| 认证 | ✅ 访客登录（且订单归属本人） |

**响应（ReservationDetailVO）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |
| sessionId | Long | 场次 ID |
| visitDate | Date | 参观日期 |
| timeSlot | String | 时段 |
| realName | String | 真实姓名 |
| phone | String | 手机号 |
| peopleCount | Integer | 参观人数 |
| reason | String | 参观事由 |
| status | Integer | 状态 |
| statusText | String | 状态中文 |
| submitTime | DateTime | 提交时间 |
| auditAdminName | String | 审核人姓名（未审核为 null） |
| auditTime | DateTime | 审核时间 |
| rejectReason | String | 驳回原因（status=2 时有） |
| cancelTime | DateTime | 取消时间 |

**异常**：40401 订单不存在、40301 非本人订单

---

#### 4.4 取消预约

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/reservation/{id}/cancel` |
| 认证 | ✅ 访客登录（且订单归属本人） |

**响应**：`Result<Void>`

**业务逻辑**：
1. 校验订单存在且 `visitor_id = 当前访客`
2. 校验 `status=0`（仅待审核可取消）→ 否则抛 40022
3. 更新 `status=3`，写 `cancel_time`
4. **回滚名额**（乐观锁）：`UPDATE visit_session SET used_people = used_people - peopleCount, version = version + 1 WHERE id = ? AND version = ?`

**异常**：40401、40301、40022 订单状态不可取消

---

### 模块 5：预约审核（管理员）

#### 5.1 预约订单分页查询

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/reservation/page` |
| 认证 | 🔒 管理员 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| realName | String | 否 | null | 访客姓名模糊 |
| status | Integer | 否 | null | 订单状态 |
| startDate | DateTime | 否 | null | 提交起始时间 |
| endDate | DateTime | 否 | null | 提交截止时间 |
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（ReservationListVO 列表分页）**：字段同 4.2 + `phone`、`reason`、`auditAdminName`、`auditTime`

---

#### 5.2 预约订单详情

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/reservation/{id}` |
| 认证 | 🔒 管理员 |

**响应**：同 4.3 ReservationDetailVO

---

#### 5.3 审核预约

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/reservation/{id}/audit` |
| 认证 | 🔒 管理员 |

**请求参数（ReservationAuditDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| pass | Boolean | 是 | true/false | true 通过，false 驳回 |
| rejectReason | String | pass=false 时必填 | 5~200 字 | 驳回原因 |

**响应**：`Result<Void>`

**业务逻辑**：
- 通过：
  1. 校验订单 `status=0` → 否则 40022
  2. 更新 `status=1`、`audit_admin_id`、`audit_time`
  3. 名额已在提交时扣减，无需再扣
- 驳回：
  1. 校验订单 `status=0` → 否则 40022
  2. 必填 `rejectReason`
  3. 更新 `status=2`、`audit_admin_id`、`audit_time`、`reject_reason`
  4. **回滚名额**：`UPDATE visit_session SET used_people = used_people - peopleCount, version = version + 1 WHERE id = ? AND version = ?`

**异常**：40401 订单不存在、40022 状态不可审核、40001 驳回原因缺失

---

### 模块 6：访客用户管理

#### 6.1 访客分页查询（管理员）

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/visitor/page` |
| 认证 | 🔒 管理员 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| keyword | String | 否 | null | 用户名/姓名/手机号模糊 |
| status | Integer | 否 | null | 0/1 |
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（VisitorListVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 访客 ID |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| phone | String | 手机号 |
| status | Integer | 0/1 |
| statusText | String | 正常/冻结 |
| registerTime | DateTime | 注册时间 |

---

#### 6.2 冻结访客

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/visitor/{id}/freeze` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：`UPDATE visitor_user SET status=1 WHERE id=?`

---

#### 6.3 解冻访客

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/visitor/{id}/unfreeze` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

---

#### 6.4 访客修改个人信息

| 项 | 内容 |
|----|------|
| 接口 | PUT `/api/visitor/profile` |
| 认证 | ✅ 访客登录 |

**请求参数（VisitorProfileDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| realName | String | 是 | 2~10 字 | 真实姓名 |
| phone | String | 是 | 11 位手机号 | 手机号 |

**响应**：`Result<Void>`

---

### 模块 7：管理员账号管理

#### 7.1 管理员列表

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/admin/page` |
| 认证 | 🔒 超级管理员（isSuper=true） |

**响应（AdminListVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 管理员 ID |
| username | String | 账号 |
| realName | String | 姓名 |
| isSuper | Boolean | 是否超管 |
| createTime | DateTime | 创建时间 |

**异常**：40301 非超管访问

---

#### 7.2 新增管理员

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/admin` |
| 认证 | 🔒 超级管理员 |

**请求参数（AdminSaveDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | 是 | 4~20 位字母数字下划线，唯一 | 账号 |
| password | String | 是 | 6~20 位含字母数字 | 初始密码明文 |
| realName | String | 是 | 2~10 字 | 姓名 |

**响应**：`Result<Long>`，返回新管理员 ID

**异常**：40011 用户名已存在、40001 参数校验失败、40301 非超管

---

#### 7.3 重置管理员密码

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/admin/{id}/reset-password` |
| 认证 | 🔒 超级管理员 |

**请求参数（Query/Body）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| newPassword | String | 是 | 6~20 位含字母数字 | 新密码明文 |

**响应**：`Result<Void>`

**异常**：40401 管理员不存在、40301 非超管

---

### 模块 8：RAG 知识库文档管理

#### 8.1 文档分页查询

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/knowledge/page` |
| 认证 | 🔒 管理员 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| fileName | String | 否 | null | 文件名模糊 |
| fileType | String | 否 | null | pdf/txt/docx |
| status | Integer | 否 | null | 0/1/2 |
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（KnowledgeListVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档 ID |
| fileName | String | 文件名 |
| fileType | String | 类型 |
| fileSize | Long | 文件大小（字节） |
| chunkCount | Integer | 向量块数 |
| status | Integer | 解析状态 |
| statusText | String | 解析中/已完成/失败 |
| uploadAdminName | String | 上传人姓名 |
| createTime | DateTime | 上传时间 |
| errorMsg | String | 失败原因（status=2 时有） |

---

#### 8.2 上传文档

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/knowledge/upload` |
| 认证 | 🔒 管理员 |
| Content-Type | multipart/form-data |

**请求参数**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| file | MultipartFile | 是 | pdf/txt/docx，≤50MB | 文件 |

**响应**：`Result<Long>`，返回文档 ID

**业务逻辑**（异步处理，前端通过列表 status 轮询）：
1. 保存文件到 `./uploads/knowledge/{yyyy}/{uuid}.{ext}`
2. 插入 `knowledge_doc`（status=0 解析中）
3. 异步：DocumentParser 提取文本 → TextChunker 分块（500 字/100 重叠）→ BGE 编码 → Milvus 插入 → 更新 `knowledge_doc` status=1、chunk_count=N
4. 失败：更新 status=2、error_msg

**异常**：40030 文档类型不支持、40001 文件超限

---

#### 8.3 删除文档

| 项 | 内容 |
|----|------|
| 接口 | DELETE `/api/admin/knowledge/{id}` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：
1. 按 `doc_id` 删除 Milvus 全部向量
2. 删除 `knowledge_doc` 行（软删）+ 删除物理文件
3. Milvus 删除失败 → 不删 MySQL，抛 40050

**异常**：40401 文档不存在、40050 Milvus 删除失败

---

#### 8.4 重新解析文档

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/knowledge/{id}/reparse` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：
1. 按 `doc_id` 删 Milvus 旧向量
2. 重置 `knowledge_doc` status=0、chunk_count=0、error_msg=null
3. 重新走 8.2 的解析流程

---

### 模块 9：AI 智能咨询

#### 9.1 提问

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/chat/ask` |
| 认证 | ✅ 访客登录 |

**请求参数（ChatAskDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| sessionId | Long | 否 | null 时自动新建会话 | 会话 ID |
| question | String | 是 | 1~500 字 | 访客问题 |

**响应（ChatAskVO）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | Long | 会话 ID（新建时返回新 ID） |
| answer | String | AI 回答全文 |
| referDocName | String | 引用文档名（未命中为 null） |
| referChunk | String | 引用片段原文（未命中为 null） |

**业务逻辑**：
1. 无 sessionId → 新建 `chat_session`，title=question 前 20 字
2. 插入 `chat_message` role=user（content=question）
3. BGE 编码 question → 768 维向量
4. Milvus 检索 top-3 相似片段（IP 排序）
5. 若 top-1 相似度 < 0.5 → answer=固定话术（`application.yml rag.no-answer-tip`），referDocName=null
6. 命中 → 拼 RAG 提示词 → 调百炼 qwen-plus → answer=模型输出
7. 插入 `chat_message` role=assistant（content=answer、refer_doc_id、refer_chunk、tokens）

**异常**：40040 AI 服务调用失败（answer 仍写入提示消息，便于日志）、40050 Milvus 异常

---

#### 9.2 我的会话列表

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/chat/session/my` |
| 认证 | ✅ 访客登录 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（ChatSessionVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 会话 ID |
| title | String | 会话标题 |
| createTime | DateTime | 创建时间 |
| lastMessageTime | DateTime | 最后一条消息时间（join 查询） |

---

#### 9.3 会话消息列表

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/chat/session/{sessionId}/messages` |
| 认证 | ✅ 访客登录（且会话归属本人） |

**请求参数（Query）**：`current`、`size`

**响应（ChatMessageVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 消息 ID |
| role | String | user / assistant |
| content | String | 消息内容 |
| referDocName | String | 引用文档名（仅 assistant 有） |
| createTime | DateTime | 时间 |

**异常**：40401 会话不存在、40301 非本人会话

---

#### 9.4 新建会话

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/chat/session/new` |
| 认证 | ✅ 访客登录 |

**响应**：`Result<Long>`，返回新会话 ID

**业务逻辑**：插入 `chat_session`（title=null，待首条提问时更新）

---

#### 9.5 清空我的全部会话

| 项 | 内容 |
|----|------|
| 接口 | DELETE `/api/chat/session/my` |
| 认证 | ✅ 访客登录 |

**响应**：`Result<Void>`

**业务逻辑**：
1. 查当前访客全部 `chat_session` id 列表
2. 物理删除对应 `chat_message`
3. 物理删除 `chat_session`

---

### 模块 10：统计与问答日志

#### 10.1 管理员后台首页看板

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/stats/dashboard` |
| 认证 | 🔒 管理员 |

**响应（DashboardVO）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| todayReservationCount | Integer | 今日新增预约数 |
| pendingAuditCount | Integer | 待审核订单数 |
| visitorTotal | Integer | 访客总数 |
| chatTotalCount | Integer | AI 问答总次数（assistant 消息数） |
| weeklyTrend | List<DayCount> | 近 7 天每日预约数（含日期 + 计数） |
| recentPending | List<ReservationListVO> | 最近 5 条待审核订单（结构同 5.1） |

**DayCount 结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| date | Date | 日期 |
| count | Integer | 当日预约数 |

---

#### 10.2 问答日志分页

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/stats/chat-log/page` |
| 认证 | 🔒 管理员 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| visitorId | Long | 否 | null | 按访客筛选 |
| keyword | String | 否 | null | 问题关键词模糊 |
| startDate | DateTime | 否 | null | 起始时间 |
| endDate | DateTime | 否 | null | 截止时间 |
| current | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

**响应（ChatLogVO 列表分页）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 消息 ID |
| visitorName | String | 访客姓名（join visitor_user） |
| question | String | 问题（同会话上一条 user 消息） |
| answer | String | AI 回答（assistant 消息） |
| referDocName | String | 引用文档名 |
| createTime | DateTime | 时间 |

**业务逻辑**：以 `chat_message role=assistant` 为主表，join `chat_session`、`visitor_user` 取访客姓名，按 assistant 消息 id 倒序。

---

#### 10.3 高频问题统计

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/stats/hot-keywords` |
| 认证 | 🔒 管理员 |

**请求参数（Query）**：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| days | Integer | 否 | 30 | 统计近 N 天 |

**响应**：

| 字段 | 类型 | 说明 |
|------|------|------|
| topKeywords | List<KeywordCount> | Top 10 关键词（含词 + 计数） |
| wordCloud | List<KeywordCount> | 词云数据（Top 100 关键词） |

**KeywordCount 结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| keyword | String | 关键词 |
| count | Integer | 出现次数 |

**业务逻辑**：取近 N 天 `chat_message role=user` 全部 content，应用层分词（按字符切分 + 停用词过滤）+ 频次聚合，排序取 Top 10 与 Top 100。

---

### 公共聚合接口

#### P1 访客首页聚合

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/public/home` |
| 认证 | ❌ 公开 |

**响应**：

| 字段 | 类型 | 说明 |
|------|------|------|
| latestNotices | List<NoticeListVO> | 最新 3 条公告 |
| latestSessions | List<SessionListVO> | 最新 3 条可预约场次 |
| campusIntro | String | 校园简介静态文本（从配置或常量读取，毕设可直接硬编码） |

---

## 十一、接口总览表

| 模块 | 接口数 | 路径前缀 |
|------|--------|---------|
| 用户认证 | 4 | /api/auth |
| 公告前台 | 3 | /api/notice |
| 场次前台 | 3 | /api/session |
| 访客预约 | 4 | /api/reservation |
| 预约审核 | 3 | /api/admin/reservation |
| 访客管理 | 4 | /api/admin/visitor + /api/visitor |
| 管理员账号 | 3 | /api/admin/admin |
| 知识库管理 | 4 | /api/admin/knowledge |
| AI 咨询 | 5 | /api/chat |
| 统计日志 | 3 | /api/admin/stats |
| 公共聚合 | 1 | /api/public |
| 公告后台 | 6 | /api/admin/notice |
| 场次后台 | 6 | /api/admin/session |
| **合计** | **49** | - |

> 完整后台管理接口（公告/场次/账号管理）下表补充：

### 补充：模块 11 - 公告后台管理

#### 11.1 公告分页（管理员）

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/notice/page` |
| 认证 | 🔒 管理员 |

**请求参数**：`keyword`（标题模糊）、`status`、`current`、`size`

**响应**：NoticeListVO 列表分页 + status 字段

---

#### 11.2 新增公告

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/notice` |
| 认证 | 🔒 管理员 |

**请求参数（NoticeSaveDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| title | String | 是 | 1~100 字 | 标题 |
| content | String | 是 | 1~10000 字 | 正文 |
| status | Integer | 是 | 0/1 | 0 保存草稿，1 保存并发布 |

**响应**：`Result<Long>`

---

#### 11.3 编辑公告

| 项 | 内容 |
|----|------|
| 接口 | PUT `/api/admin/notice/{id}` |
| 认证 | 🔒 管理员 |

**请求参数**：同 11.2

**响应**：`Result<Void>`

---

#### 11.4 发布公告

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/notice/{id}/publish` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：`UPDATE campus_notice SET status=1, publish_admin_id=?, publish_time=NOW() WHERE id=? AND status=0`

---

#### 11.5 下架公告

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/notice/{id}/offline` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：`UPDATE campus_notice SET status=0 WHERE id=? AND status=1`

---

#### 11.6 删除公告

| 项 | 内容 |
|----|------|
| 接口 | DELETE `/api/admin/notice/{id}` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：逻辑删除 `UPDATE campus_notice SET deleted=1 WHERE id=?`

---

### 补充：模块 12 - 场次后台管理

#### 12.1 场次分页（管理员）

| 项 | 内容 |
|----|------|
| 接口 | GET `/api/admin/session/page` |
| 认证 | 🔒 管理员 |

**请求参数**：`visitDate`、`status`、`current`、`size`

**响应**：SessionListVO 列表分页 + status 字段

---

#### 12.2 新增场次

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/session` |
| 认证 | 🔒 管理员 |

**请求参数（SessionSaveDTO）**：

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| visitDate | Date | 是 | 不早于今天 | 参观日期 |
| timeSlot | String | 是 | 非空，如 "09:00-11:00" | 时段 |
| maxPeople | Integer | 是 | 1~500 | 最大人数 |
| status | Integer | 是 | 0/1 | 开放/下架 |

**响应**：`Result<Long>`

---

#### 12.3 编辑场次

| 项 | 内容 |
|----|------|
| 接口 | PUT `/api/admin/session/{id}` |
| 认证 | 🔒 管理员 |

**请求参数**：同 12.2

**响应**：`Result<Void>`

**业务逻辑**：`used_people > 0` 时禁止改 `maxPeople` 小于 `used_people`，抛 40022。

---

#### 12.4 上架场次

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/session/{id}/online` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

---

#### 12.5 下架场次

| 项 | 内容 |
|----|------|
| 接口 | POST `/api/admin/session/{id}/offline` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

---

#### 12.6 删除场次

| 项 | 内容 |
|----|------|
| 接口 | DELETE `/api/admin/session/{id}` |
| 认证 | 🔒 管理员 |

**响应**：`Result<Void>`

**业务逻辑**：`used_people > 0` 抛 40022 不可删除；否则逻辑删除。

---

## 十二、CORS 跨域配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("http://localhost:5173", "http://localhost:8080")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

开发环境允许 `localhost:5173`（Vite 默认端口）访问。

---

## 十三、配置文件关键项

| 配置 | 值 | 用途 |
|------|----|----|
| server.port | 8088 | 后端端口 |
| server.servlet.context-path | /api | 接口前缀 |
| campus.jwt.secret | 见 yml | JWT 密钥 |
| campus.jwt.expiration | 86400000 | 24 小时 |
| campus.milvus.collection-name | campus_knowledge | Milvus 集合名 |
| campus.milvus.top-k | 3 | 检索 top-3 |
| campus.bge.vector-dim | 768 | BGE 向量维度 |
| campus.rag.chunk-size | 500 | 分块大小 |
| campus.rag.chunk-overlap | 100 | 分块重叠 |
| spring.servlet.multipart.max-file-size | 50MB | 上传大小 |

---

## 十四、文档变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-08-26 | 初始版本，定义架构分层 + 49 个接口 | 开发团队 |

---

**本文档定稿后禁止私自修改，任何变更需走需求变更流程并更新版本号。**
