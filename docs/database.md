# 数据库完整设计文档

> 基于 SpringBoot+RAG 的校园参观预约与智能咨询系统
>
> 哈尔滨剑桥学院 · 智能科学与工程学院 · 2026 届毕业设计
>
> 学生：钟啸林　指导教师：郭旭 副教授

---

## 一、文档说明

本文档定义系统**全部数据表结构、字段约束、表间关系、索引设计、状态字典、初始化数据**与 **Milvus 向量库集合设计**。

**适用范围**：MySQL 8.0 业务数据库 + Milvus 向量数据库。

**强制约束**：开发阶段（Stage2）必须 100% 对标本文档表名、字段名、字段类型、约束、索引，禁止私自新增表、改字段、改约束、改索引。任何变更需走需求变更流程并同步更新本文档。

**配套文件**：
- 建库脚本：[init-database.sql](./init-database.sql)（仅建空库）
- 表结构脚本：[schema.sql](./schema.sql)（本文档定稿后产出，用于一键建表）
- 初始化数据：[seed-data.sql](./seed-data.sql)（本文档定稿后产出，预置超管与示例数据）

---

## 二、数据库整体设计原则

### 2.1 设计原则

| 原则 | 说明 | 本项目体现 |
|------|------|-----------|
| 单一职责 | 一张表只表达一类业务实体 | 访客/管理员/场次/预约/公告/文档/会话/消息 各自独立 |
| 命名规范 | 表名小写下划线、字段名小写下划线、boolean 用 is_ 前缀 | 如 `visit_session`、`is_super` |
| 主键统一 | 所有表主键均为 `id BIGINT UNSIGNED AUTO_INCREMENT` | 与 MyBatis-Plus `id-type: auto` 一致 |
| 软删除可选 | 高频误删表启用逻辑删除 `deleted TINYINT` | 公告、场次、知识库文档启用；访客/管理员/预约/会话/消息不启用 |
| 审计字段 | 每张业务表必有 `create_time`、`update_time` | 由 MyBatis-Plus 自动填充 |
| 下划线驼峰映射 | 数据库下划线 ↔ Java 驼峰 | `application.yml` 已开启 `map-underscore-to-camel-case` |
| 字符集 | utf8mb4（支持 emoji 与生僻字） | 与 `init-database.sql` 一致 |
| 外键策略 | 物理外键不用，仅建索引，由应用层维护一致性 | 毕设项目便于迁移，避免外键级联导致删除受阻 |
| 状态字典统一 | 状态字段一律 `TINYINT UNSIGNED`，0 起步，含义集中维护 | 详见第七章状态字典 |

### 2.2 字符集与排序规则

```sql
-- 库级字符集（init-database.sql 已建）
CREATE DATABASE IF NOT EXISTS campus_visit
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
```

每张表均显式声明 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci`，避免环境差异。

### 2.3 命名约定

| 类型 | 规则 | 示例 |
|------|------|------|
| 表名 | 全小写 + 下划线，单数名词 | `visit_session`、`chat_message` |
| 字段名 | 全小写 + 下划线 | `visitor_id`、`create_time` |
| 主键 | 固定 `id` | - |
| 外键字段 | `{被引用表单数}_id` | `session_id`、`visitor_id` |
| 布尔字段 | `is_` 前缀 | `is_super` |
| 时间字段 | `xxx_time` 后缀 | `submit_time`、`audit_time` |
| 状态字段 | `status` 或 `xxx_status` | `status` |

---

## 三、数据表清单（总览）

系统共 **8 张 MySQL 业务表** + **1 个 Milvus 向量集合**。

| 序号 | 表名 | 中文名 | 用途 | 预估行数（毕设级） | 启用软删除 |
|------|------|--------|------|------------------|-----------|
| T1 | `visitor_user` | 访客用户表 | 访客注册账号、登录、个人信息 | 千级 | 否 |
| T2 | `admin_user` | 管理员账号表 | 管理员登录、超管标识 | 十级 | 否 |
| T3 | `campus_notice` | 校园公告表 | 公告内容、发布状态管理 | 百级 | 是 |
| T4 | `visit_session` | 参观场次表 | 参观日期、时段、容量、上下架 | 百级 | 是 |
| T5 | `visit_reservation` | 预约订单表 | 访客预约订单与审核流转 | 千级 | 否 |
| T6 | `knowledge_doc` | 知识库文档表 | RAG 上传文档元信息与解析状态 | 十级 | 否 |
| T7 | `chat_session` | AI 会话表 | 访客 AI 咨询会话隔离（一次会话含多条消息） | 百级 | 否 |
| T8 | `chat_message` | AI 问答消息表 | 单条 user/assistant 消息，同时支撑问答日志统计 | 万级 | 否 |
| V1 | `campus_knowledge` | Milvus 向量集合 | 768 维 BGE-small-zh 文本向量与原始片段 | 千级 chunk | - |

---

## 四、表间关系 ER 图（文字版）

```
                    ┌──────────────────┐
                    │   visitor_user   │
                    │  (访客用户表)     │
                    └────────┬─────────┘
                             │ 1
              ┌──────────────┼─────────────────────────┐
              │              │                          │
              │ N            │ N                        │ N
      ┌───────▼──────┐  ┌───▼──────────┐      ┌───────▼────────┐
      │ visit_         │  │ chat_session │      │  (chat_message │
      │ reservation   │  │  (AI 会话表)  │      │   属于会话)    │
      │ (预约订单表)   │  └───────┬──────┘      └────────────────┘
      └───────┬──────┘          │ 1
              │ N                │ 1
              │                  │
              │ 1                │ N
              │          ┌───────▼────────┐
      ┌───────▼──────┐  │  chat_message  │
      │ visit_session│  │ (AI 问答消息表) │
      │ (参观场次表)   │  └────────────────┘
      └──────────────┘

  visit_reservation.session_id  → visit_session.id  (N:1)
  visit_reservation.visitor_id  → visitor_user.id   (N:1)
  visit_reservation.audit_admin_id → admin_user.id  (N:1，可为空)
  chat_message.session_id       → chat_session.id    (N:1)
  chat_session.visitor_id       → visitor_user.id   (N:1)
  chat_message.refer_doc_id     → knowledge_doc.id  (N:1，可为空)
  knowledge_doc 上传者 admin_id → admin_user.id     (N:1)
  campus_notice.publish_admin_id→ admin_user.id     (N:1，可为空)
```

**关系说明**：
- 1 个访客可有多条预约订单（1:N），1 条预约只属于 1 个场次与 1 个访客（N:1）。
- 1 个访客可创建多个 AI 会话（1:N），1 个会话可有多条消息（1:N），1 条消息至多引用 1 个知识库文档（N:1，可为空，未命中时为 NULL）。
- 1 个管理员可审核多条预约（1:N），1 个管理员可发布多条公告（1:N），1 个管理员可上传多个知识库文档（1:N）。

---

## 五、表结构详细设计

> 字段说明列采用统一格式：
> **字段名** | 类型(长度) | 是否为空 | 默认值 | 主键/自增 | 索引 | 注释

---

### T1. `visitor_user` 访客用户表

**表注释**：访客注册账号信息，支持登录、个人信息维护、冻结/解冻。

**对应业务**：F10/F11 登录注册、F9 个人中心、A4 访客管理、Stage2 模块1/模块6。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 访客用户ID |
| `username` | VARCHAR(20) | 否 | - | - | UNIQUE | 用户名（4~20 位字母数字下划线，登录账号） |
| `password` | VARCHAR(100) | 否 | - | - | - | 密码（BCrypt 加密后 60 位，预留扩展至 100） |
| `real_name` | VARCHAR(10) | 否 | - | - | - | 真实姓名（2~10 字） |
| `phone` | VARCHAR(11) | 否 | - | - | - | 联系手机号（11 位中国手机号） |
| `status` | TINYINT UNSIGNED | 否 | 0 | - | INDEX | 账号状态：0=正常，1=冻结（见状态字典 D1） |
| `register_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | INDEX | 注册时间 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | - | 创建时间（MyBatis-Plus 自动填充） |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间（自动维护） |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `visitor_user`;
CREATE TABLE `visitor_user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '访客用户ID',
  `username`      VARCHAR(20)  NOT NULL COMMENT '用户名（4~20位字母数字下划线）',
  `password`      VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
  `real_name`     VARCHAR(10)  NOT NULL COMMENT '真实姓名（2~10字）',
  `phone`         VARCHAR(11)  NOT NULL COMMENT '联系手机号（11位）',
  `status`        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '账号状态 0=正常 1=冻结',
  `register_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访客用户表';
```

**索引说明**：
- `uk_username` 唯一索引：保证用户名全局唯一，登录时按用户名查询走唯一索引。
- `idx_status`：管理员后台按"正常/冻结"筛选访客列表。
- `idx_register_time`：管理员后台按注册时间排序/筛选访客。

---

### T2. `admin_user` 管理员账号表

**表注释**：管理员账号，含超管标识（系统初始化时预置 1 个超管 `admin/admin123`）。

**对应业务**：F10 登录（管理员侧）、A8 管理员账号管理、Stage2 模块7。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 管理员ID |
| `username` | VARCHAR(20) | 否 | - | - | UNIQUE | 管理员账号（4~20 位字母数字下划线） |
| `password` | VARCHAR(100) | 否 | - | - | - | 密码（BCrypt 加密） |
| `real_name` | VARCHAR(10) | 否 | - | - | - | 管理员姓名（用于审核记录展示） |
| `is_super` | TINYINT UNSIGNED | 否 | 0 | - | - | 是否超管：0=普通管理员，1=超管（仅超管可访问 A8 管理员账号管理） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | - | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间 |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username`    VARCHAR(20)  NOT NULL COMMENT '管理员账号',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
  `real_name`   VARCHAR(10)  NOT NULL COMMENT '管理员姓名',
  `is_super`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否超管 0=普通 1=超管',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员账号表';
```

**索引说明**：仅 `uk_username` 唯一索引（管理员数量少，无需其他索引）。

---

### T3. `campus_notice` 校园公告表

**表注释**：校园公告内容、发布状态管理，支持软删除。

**对应业务**：F2/F3 公告列表与详情、A5 公告管理、Stage2 模块2。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 公告ID |
| `title` | VARCHAR(100) | 否 | - | - | - | 标题（1~100 字） |
| `content` | TEXT | 否 | - | - | - | 正文（最长约 65535 字符，支持换行，毕设不需富文本） |
| `status` | TINYINT UNSIGNED | 否 | 0 | - | INDEX | 发布状态：0=未发布（草稿），1=已发布（前台可见），见状态字典 D2 |
| `publish_admin_id` | BIGINT UNSIGNED | 是 | NULL | - | INDEX | 发布人管理员ID（草稿未发布时为 NULL） |
| `publish_time` | DATETIME | 是 | NULL | - | INDEX | 发布时间（首次发布时写入，下架不重置） |
| `deleted` | TINYINT UNSIGNED | 否 | 0 | - | - | 逻辑删除：0=正常，1=已删除（MyBatis-Plus 全局逻辑删除字段） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | - | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间 |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `campus_notice`;
CREATE TABLE `campus_notice` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title`            VARCHAR(100) NOT NULL COMMENT '公告标题',
  `content`          TEXT         NOT NULL COMMENT '公告正文',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '发布状态 0=未发布 1=已发布',
  `publish_admin_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人管理员ID',
  `publish_time`     DATETIME DEFAULT NULL COMMENT '发布时间',
  `deleted`          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=已删除',
  `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`),
  KEY `idx_publish_admin_id` (`publish_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='校园公告表';
```

**索引说明**：
- `idx_status_publish` 联合索引：前台列表查询 `WHERE status=1 ORDER BY publish_time DESC` 走该联合索引（最左前缀原则：先等值 `status`，再排序 `publish_time`）。
- `idx_publish_admin_id`：管理员后台按发布人筛选公告。

---

### T4. `visit_session` 参观场次表

**表注释**：参观场次（日期+时段+容量），访客预约的容量来源，支持软删除。

**对应业务**：F4 场次列表、A2 场次管理、Stage2 模块3。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 场次ID |
| `visit_date` | DATE | 否 | - | - | INDEX | 参观日期（仅日期，如 2026-09-01） |
| `time_slot` | VARCHAR(20) | 否 | - | - | - | 时段（如 "09:00-11:00"，方便任意时段） |
| `max_people` | INT UNSIGNED | 否 | - | - | - | 最大容纳人数（1~500） |
| `used_people` | INT UNSIGNED | 否 | 0 | - | - | 已预约人数（预约审核通过 +1，取消 -1，并发用乐观锁版本号维护） |
| `version` | INT UNSIGNED | 否 | 0 | - | - | 乐观锁版本号（MyBatis-Plus `@Version`）防并发超卖 |
| `status` | TINYINT UNSIGNED | 否 | 0 | - | INDEX | 场次状态：0=开放，1=下架，见状态字典 D3 |
| `deleted` | TINYINT UNSIGNED | 否 | 0 | - | - | 逻辑删除 0=正常 1=已删除 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | - | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间 |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `visit_session`;
CREATE TABLE `visit_session` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场次ID',
  `visit_date`  DATE NOT NULL COMMENT '参观日期',
  `time_slot`   VARCHAR(20) NOT NULL COMMENT '时段 如 09:00-11:00',
  `max_people`  INT UNSIGNED NOT NULL COMMENT '最大容纳人数 1~500',
  `used_people` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已预约人数',
  `version`     INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '场次状态 0=开放 1=下架',
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_date_status` (`visit_date`, `status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参观场次表';
```

**索引说明**：
- `idx_visit_date_status` 联合索引：访客前台查询"某日期范围内、开放中"的场次，`WHERE visit_date BETWEEN ? AND ? AND status=0 ORDER BY visit_date` 走该索引。
- `idx_status`：管理员后台按"开放/下架"筛选场次。

**乐观锁防超卖**：
- 访客提交预约时，后端先校验 `used_people + people_count <= max_people`，通过后执行：
  ```sql
  UPDATE visit_session
  SET used_people = used_people + #{people_count}, version = version + 1
  WHERE id = #{sessionId} AND version = #{原版本号} AND used_people + #{people_count} <= max_people
  ```
- 影响行数=0 即版本冲突或名额不足，回滚并返回"名额已被抢完"。

---

### T5. `visit_reservation` 预约订单表

**表注释**：访客预约订单，记录预约信息与审核流转，4 种状态 + 驳回原因。

**对应业务**：F5 预约提交、F6 我的预约、F7 预约详情与取消、A3 预约审核、Stage2 模块4/模块5。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 预约订单ID |
| `session_id` | BIGINT UNSIGNED | 否 | - | - | INDEX | 关联 visit_session.id |
| `visitor_id` | BIGINT UNSIGNED | 否 | - | - | INDEX | 关联 visitor_user.id |
| `real_name` | VARCHAR(10) | 否 | - | - | INDEX | 真实姓名（提交时冗余存入，避免访客改名影响历史订单） |
| `phone` | VARCHAR(11) | 否 | - | - | - | 联系手机号（冗余存入） |
| `people_count` | INT UNSIGNED | 否 | - | - | - | 参观人数（1~剩余名额，最大 50） |
| `reason` | VARCHAR(200) | 否 | - | - | - | 参观事由（5~200 字） |
| `status` | TINYINT UNSIGNED | 否 | 0 | - | INDEX | 订单状态：0=待审核，1=通过，2=驳回，3=已取消，见状态字典 D4 |
| `submit_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | INDEX | 提交时间 |
| `audit_admin_id` | BIGINT UNSIGNED | 是 | NULL | - | - | 审核人管理员ID（待审核/已取消时为 NULL） |
| `audit_time` | DATETIME | 是 | NULL | - | - | 审核时间 |
| `reject_reason` | VARCHAR(200) | 是 | NULL | - | - | 驳回原因（status=2 时必填，5~200 字） |
| `cancel_time` | DATETIME | 是 | NULL | - | - | 取消时间（访客主动取消时写入） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | - | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间 |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `visit_reservation`;
CREATE TABLE `visit_reservation` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约订单ID',
  `session_id`      BIGINT UNSIGNED NOT NULL COMMENT '场次ID',
  `visitor_id`      BIGINT UNSIGNED NOT NULL COMMENT '访客用户ID',
  `real_name`       VARCHAR(10)  NOT NULL COMMENT '真实姓名',
  `phone`           VARCHAR(11)  NOT NULL COMMENT '联系手机号',
  `people_count`    INT UNSIGNED NOT NULL COMMENT '参观人数',
  `reason`          VARCHAR(200) NOT NULL COMMENT '参观事由',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单状态 0=待审核 1=通过 2=驳回 3=已取消',
  `submit_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `audit_admin_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人管理员ID',
  `audit_time`      DATETIME DEFAULT NULL COMMENT '审核时间',
  `reject_reason`   VARCHAR(200) DEFAULT NULL COMMENT '驳回原因（status=2 时必填）',
  `cancel_time`     DATETIME DEFAULT NULL COMMENT '取消时间',
  `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_visitor_id_status` (`visitor_id`, `status`),
  KEY `idx_status_submit` (`status`, `submit_time`),
  KEY `idx_real_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约订单表';
```

**索引说明**：
- `idx_session_id`：按场次查询订单（如统计场次已预约人数）。
- `idx_visitor_id_status` 联合：F6 我的预约 `WHERE visitor_id=? AND status=? ORDER BY submit_time DESC` 走该索引。
- `idx_status_submit` 联合：A3 预约审核 `WHERE status=0 ORDER BY submit_time` 走该索引（待审核队列）。
- `idx_real_name`：A3 按访客姓名模糊搜索（前缀模糊 `LIKE '张%'` 走索引；左模糊 `%张` 不走索引，毕设项目接受全表扫）。

**业务规则**：
- 访客重复预约拦截：提交前查 `SELECT COUNT(*) FROM visit_reservation WHERE session_id=? AND visitor_id=? AND status IN (0,1)` > 0 则拒绝。
- 审核通过：`status` 0→1，写 `audit_admin_id` + `audit_time`，同步 `visit_session.used_people + people_count`（乐观锁）。
- 驳回：`status` 0→2，必填 `reject_reason`，不增加 `used_people`。
- 访客取消（仅 status=0 可取消）：`status` 0→3，写 `cancel_time`，不增加 `used_people`（因为审核通过前不占用名额）。
- 审核通过后访客不可取消（业务规则）。

---

### T6. `knowledge_doc` 知识库文档表

**表注释**：RAG 上传文档元信息与解析状态，删除时同步删除 Milvus 对应向量。

**对应业务**：A6 RAG 知识库管理、Stage2 模块8。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 文档ID |
| `file_name` | VARCHAR(200) | 否 | - | - | - | 原始文件名（如 "入校须知.pdf"） |
| `file_type` | VARCHAR(10) | 否 | - | - | INDEX | 文件类型：pdf/txt/docx |
| `file_path` | VARCHAR(500) | 否 | - | - | - | 存储路径（相对路径，如 `./uploads/knowledge/2026/xxx.pdf`） |
| `file_size` | BIGINT UNSIGNED | 否 | 0 | - | - | 文件大小（字节） |
| `chunk_count` | INT UNSIGNED | 否 | 0 | - | - | 文本分块数（= Milvus 该文档向量条数，解析完成后写入） |
| `status` | TINYINT UNSIGNED | 否 | 0 | - | INDEX | 解析状态：0=解析中，1=已完成，2=解析失败，见状态字典 D5 |
| `upload_admin_id` | BIGINT UNSIGNED | 否 | - | - | - | 上传人管理员ID |
| `error_msg` | VARCHAR(500) | 是 | NULL | - | - | 失败原因（status=2 时填） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | - | 创建时间（=上传时间） |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间 |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `knowledge_doc`;
CREATE TABLE `knowledge_doc` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `file_name`        VARCHAR(200) NOT NULL COMMENT '原始文件名',
  `file_type`        VARCHAR(10)  NOT NULL COMMENT '文件类型 pdf/txt/docx',
  `file_path`        VARCHAR(500) NOT NULL COMMENT '存储路径',
  `file_size`        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `chunk_count`      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文本分块数',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '解析状态 0=解析中 1=已完成 2=失败',
  `upload_admin_id`  BIGINT UNSIGNED NOT NULL COMMENT '上传人管理员ID',
  `error_msg`        VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_status` (`status`),
  KEY `idx_upload_admin_id` (`upload_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档表';
```

**索引说明**：
- `idx_file_type`：按类型筛选文档（pdf/txt/docx）。
- `idx_status`：管理员后台按"解析中/已完成/失败"筛选。
- `idx_upload_admin_id`：按上传人筛选文档。

**与 Milvus 同步规则**：
- 文档解析完成：在 `knowledge_doc` 写入 `chunk_count`、`status=1`，同时在 Milvus `campus_knowledge` 集合插入 `chunk_count` 条向量（每条向量附 `doc_id` = 本表 `id`）。
- 删除文档：先按 `doc_id` 删除 Milvus 全部向量，再删 `knowledge_doc` 行（或软删）。
- 重新解析：先删 Milvus 旧向量，再重新解析插入，更新 `chunk_count`。

---

### T7. `chat_session` AI 会话表

**表注释**：访客 AI 咨询会话隔离，1 个会话含多条消息，访客可清空自己的全部会话。

**对应业务**：F8 AI 历史会话清空、F12 AI 悬浮窗、Stage2 模块9。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 会话ID |
| `visitor_id` | BIGINT UNSIGNED | 否 | - | - | INDEX | 关联 visitor_user.id |
| `title` | VARCHAR(100) | 是 | NULL | - | - | 会话标题（取首条问题前 20 字，便于历史会话列表展示） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | INDEX | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | - | - | 更新时间（最后一条消息时间由消息表 join 取） |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `visitor_id`  BIGINT UNSIGNED NOT NULL COMMENT '访客用户ID',
  `title`       VARCHAR(100) DEFAULT NULL COMMENT '会话标题（首条问题前20字）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_visitor_id_create` (`visitor_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 会话表';
```

**索引说明**：
- `idx_visitor_id_create` 联合：F8 历史会话按访客倒序列出 `WHERE visitor_id=? ORDER BY create_time DESC` 走该索引。

---

### T8. `chat_message` AI 问答消息表

**表注释**：单条 user/assistant 消息，同时支撑 F8 历史会话与 A7 问答日志统计。

**对应业务**：F8 历史会话列表、F12 消息气泡、A7 问答日志统计、Stage2 模块9/模块10。

| 字段名 | 类型(长度) | 为空 | 默认值 | 主键/自增 | 索引 | 注释 |
|--------|-----------|------|--------|----------|--------|------|
| `id` | BIGINT UNSIGNED | 否 | - | PK/AUTO_INCREMENT | - | 消息ID |
| `session_id` | BIGINT UNSIGNED | 否 | - | - | INDEX | 关联 chat_session.id |
| `visitor_id` | BIGINT UNSIGNED | 否 | - | - | INDEX | 冗余 visitor_id，便于 A7 统计直接查（不必 join 会话表） |
| `role` | VARCHAR(10) | 否 | - | - | - | 消息角色：`user`=访客提问，`assistant`=AI 回答 |
| `content` | TEXT | 否 | - | - | - | 消息内容（user=问题，assistant=回答全文） |
| `refer_doc_id` | BIGINT UNSIGNED | 是 | NULL | - | INDEX | 引用文档ID（命中知识库时关联 knowledge_doc.id，未命中为 NULL） |
| `refer_chunk` | VARCHAR(500) | 是 | NULL | - | - | 引用片段原文（截取 Milvus top-1 片段，便于前端展示"参考来源"） |
| `tokens` | INT UNSIGNED | 是 | NULL | - | - | 本次回答消耗 token 数（来自百炼返回，统计用） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | - | INDEX | 创建时间（即问答时间） |

**建表 SQL**：
```sql
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`    BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `visitor_id`    BIGINT UNSIGNED NOT NULL COMMENT '访客用户ID（冗余）',
  `role`          VARCHAR(10)  NOT NULL COMMENT '消息角色 user/assistant',
  `content`       TEXT         NOT NULL COMMENT '消息内容',
  `refer_doc_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '引用文档ID',
  `refer_chunk`   VARCHAR(500) DEFAULT NULL COMMENT '引用片段原文',
  `tokens`        INT UNSIGNED DEFAULT NULL COMMENT '消耗token数',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_visitor_id_create` (`visitor_id`, `create_time`),
  KEY `idx_role_create` (`role`, `create_time`),
  KEY `idx_refer_doc_id` (`refer_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 问答消息表';
```

**索引说明**：
- `idx_session_id`：F12 按会话加载消息列表 `WHERE session_id=? ORDER BY create_time`。
- `idx_visitor_id_create` 联合：F8 历史会话按访客倒序分页 `WHERE visitor_id=? ORDER BY create_time DESC`。
- `idx_role_create` 联合：A7 全站问答日志按时间倒序、按 role=user 聚合（高频问题统计用 user 消息）。
- `idx_refer_doc_id`：A6 文档删除前查"该文档被引用过多少次"（统计展示）。

**业务规则**：
- 1 次问答 = 2 条消息：先插 `role=user`（访客问题），AI 回答完成后再插 `role=assistant`（带 `refer_doc_id`、`refer_chunk`、`tokens`）。
- 若 AI 失败（百炼超时/异常）：assistant 消息仍写入，`content="抱歉，服务暂时不可用，请稍后重试"`，`refer_doc_id=NULL`，便于日志排查。
- A7 词云统计：`SELECT content FROM chat_message WHERE role='user' AND create_time BETWEEN ? AND ?`，应用层分词聚合（毕设可用 HanLP 或简单按字符切分）。

---

## 六、Milvus 向量数据库集合设计

### 6.1 集合基本信息

| 项 | 值 | 来源 |
|----|----|----|
| 集合名 | `campus_knowledge` | `application.yml` `campus.milvus.collection-name` |
| 向量维度 | 768 | `application.yml` `campus.bge.vector-dim`（BGE-small-zh 输出维度） |
| 索引类型 | IVF_FLAT | 毕设数据量小（千级），无需 HNSW，IVF_FLAT 简单可调 |
| 度量方式 | IP（内积） | BGE 模型推荐 IP 或 L2，本项目用 IP |
| nlist | 128 | IVF 聚类中心数，小数据集 128 足够 |
| nprobe | 8 | 检索时探查聚类数，8 平衡精度与速度 |

### 6.2 集合字段 Schema

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | INT64（主键，自增） | Milvus 主键 |
| `embedding` | FLOAT_VECTOR(768) | BGE-small-zh 嵌入向量 |
| `doc_id` | INT64 | 关联 MySQL `knowledge_doc.id`，删除文档时按此过滤删除向量 |
| `chunk_id` | INT64 | 该文档内分块序号（从 0 起），调试与展示用 |
| `content` | VARCHAR(500) | 分块原文（用于检索后拼提示词与前端"参考来源"展示） |
| `file_name` | VARCHAR(200) | 原始文件名（冗余，便于直接展示） |

> 注：Milvus 2.x 支持 Scalar 字段，`doc_id`、`chunk_id`、`content`、`file_name` 均建为标量字段并参与过滤。

### 6.3 检索流程（与 MySQL 协同）

```
访客提问 → BGE-small-zh 把问题转 768 维向量
        → Milvus 搜索 top-3 相似片段（IP 排序）
        → 若 top-1 相似度 < 阈值 0.5（可调）→ 返回固定话术（见 application.yml rag.no-answer-tip）
        → 命中则取 3 段 content 拼 RAG 提示词
        → 调阿里百炼 qwen-plus 生成回答
        → 回答 + 参考来源（file_name）写 chat_message 表
```

### 6.4 与 MySQL 的一致性保证

| 触发动作 | MySQL 操作 | Milvus 操作 |
|---------|-----------|------------|
| 上传文档解析完成 | `knowledge_doc` `status=1`、`chunk_count=N` | 插入 N 条向量，每条 `doc_id=文档ID` |
| 删除文档 | `knowledge_doc` 删除/软删 | `delete expr: doc_id == 文档ID` |
| 重新解析 | `knowledge_doc` `chunk_count` 更新 | 先按 `doc_id` 删旧向量，再插新向量 |

**事务边界**：由于跨 MySQL + Milvus 无法强一致，采用"先 Milvus 后 MySQL"顺序：
1. Milvus 写入成功 → 写 MySQL `status=1`。
2. Milvus 写入失败 → 写 MySQL `status=2`、`error_msg`，前端提示失败可重新解析。
3. 删除：先删 Milvus 成功 → 再删 MySQL。Milvus 删除失败则不删 MySQL，前端提示"删除失败请重试"。

---

## 七、状态字段字典（汇总）

### D1. `visitor_user.status` 访客账号状态

| 值 | 含义 | 业务表现 |
|----|------|---------|
| 0 | 正常 | 可登录、可预约、可咨询 |
| 1 | 冻结 | 登录拒绝，提示"账号已被冻结，请联系管理员" |

### D2. `campus_notice.status` 公告状态

| 值 | 含义 | 业务表现 |
|----|------|---------|
| 0 | 未发布（草稿） | 仅管理员后台可见，前台不展示 |
| 1 | 已发布 | 前台列表可见，可下架（status 改回 0 或单独下架字段，本项目复用 0） |

> 简化方案：下架即将 `status` 改回 0；`publish_time` 保留首次发布时间。

### D3. `visit_session.status` 场次状态

| 值 | 含义 | 业务表现 |
|----|------|---------|
| 0 | 开放 | 前台可见、可预约 |
| 1 | 下架 | 前台不可见、不可预约，已有订单仍有效 |

### D4. `visit_reservation.status` 预约订单状态

| 值 | 含义 | 业务表现 | 可流转到 |
|----|------|---------|---------|
| 0 | 待审核 | 访客提交后初始状态 | 1 通过 / 2 驳回 / 3 取消 |
| 1 | 通过 | 管理员审核通过，占用名额 | 终态（不可取消） |
| 2 | 驳回 | 管理员驳回，必填原因，不占名额 | 终态 |
| 3 | 已取消 | 访客主动取消，不占名额 | 终态 |

### D5. `knowledge_doc.status` 文档解析状态

| 值 | 含义 | 业务表现 |
|----|------|---------|
| 0 | 解析中 | 上传后正在解析分块与向量化 |
| 1 | 已完成 | 可被 AI 检索引用 |
| 2 | 解析失败 | `error_msg` 填失败原因，可"重新解析" |

### D6. `chat_message.role` 消息角色

| 值 | 含义 |
|----|------|
| `user` | 访客提问 |
| `assistant` | AI 回答 |

### D7. 通用逻辑删除 `deleted`

| 值 | 含义 |
|----|------|
| 0 | 正常 |
| 1 | 已删除（MyBatis-Plus 全局过滤，查询自动过滤） |

---

## 八、索引设计汇总

| 表 | 索引名 | 字段 | 类型 | 用途 |
|----|--------|------|------|------|
| visitor_user | uk_username | username | UNIQUE | 用户名唯一、登录查询 |
| visitor_user | idx_status | status | INDEX | 后台按状态筛选 |
| visitor_user | idx_register_time | register_time | INDEX | 后台按注册时间排序 |
| admin_user | uk_username | username | UNIQUE | 账号唯一 |
| campus_notice | idx_status_publish | (status, publish_time) | INDEX | 前台公告列表 |
| campus_notice | idx_publish_admin_id | publish_admin_id | INDEX | 按发布人筛选 |
| visit_session | idx_visit_date_status | (visit_date, status) | INDEX | 前台场次查询 |
| visit_session | idx_status | status | INDEX | 后台按状态筛选 |
| visit_reservation | idx_session_id | session_id | INDEX | 按场次查订单 |
| visit_reservation | idx_visitor_id_status | (visitor_id, status) | INDEX | 我的预约 |
| visit_reservation | idx_status_submit | (status, submit_time) | INDEX | 待审核队列 |
| visit_reservation | idx_real_name | real_name | INDEX | 姓名模糊搜索 |
| knowledge_doc | idx_file_type | file_type | INDEX | 按类型筛选 |
| knowledge_doc | idx_status | status | INDEX | 按状态筛选 |
| knowledge_doc | idx_upload_admin_id | upload_admin_id | INDEX | 按上传人筛选 |
| chat_session | idx_visitor_id_create | (visitor_id, create_time) | INDEX | 历史会话 |
| chat_message | idx_session_id | session_id | INDEX | 会话消息列表 |
| chat_message | idx_visitor_id_create | (visitor_id, create_time) | INDEX | 历史会话分页 |
| chat_message | idx_role_create | (role, create_time) | INDEX | 问答日志统计 |
| chat_message | idx_refer_doc_id | refer_doc_id | INDEX | 文档引用统计 |

---

## 九、初始化数据（预置）

### 9.1 超级管理员（必置）

```sql
-- 密码明文：admin123，BCrypt 加密后如下（10 轮 salt）
INSERT INTO `admin_user` (`username`, `password`, `real_name`, `is_super`)
VALUES (
  'admin',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- admin123 的 BCrypt 散列
  '系统超管',
  1
);
```

> 实际开发时 BCrypt 散列值由后端启动时用 `BCryptPasswordEncoder.encode("admin123")` 生成，本文档示例值仅占位，最终以代码生成为准。

### 9.2 示例公告（可选，便于毕设演示）

```sql
INSERT INTO `campus_notice` (`title`, `content`, `status`, `publish_admin_id`, `publish_time`)
VALUES
('2026 年秋季校园开放日公告', '哈尔滨剑桥学院智能科学与工程学院定于 2026 年 9 月 20 日举办校园开放日...', 1, 1, '2026-08-26 10:00:00'),
('参观入校须知', '入校请携带身份证，在校门口登记...', 1, 1, '2026-08-26 10:05:00');
```

### 9.3 示例场次（可选，便于毕设演示）

```sql
INSERT INTO `visit_session` (`visit_date`, `time_slot`, `max_people`, `used_people`, `status`)
VALUES
('2026-09-20', '09:00-11:00', 50, 0, 0),
('2026-09-20', '14:00-16:00', 50, 0, 0),
('2026-09-21', '09:00-11:00', 30, 0, 0);
```

---

## 十、MyBatis-Plus 配置对齐

`application.yml` 已声明：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 下划线 ↔ 驼峰
  global-config:
    db-config:
      id-type: auto                      # 主键自增，与所有表 id BIGINT AUTO_INCREMENT 一致
      logic-delete-field: deleted        # 全局逻辑删除字段名 deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**Java 实体对齐约定**：
- 所有实体类继承 `Model<T>` 或标注 `@TableName("表名")`。
- 主键字段标注 `@TableId(type = IdType.AUTO)`。
- 逻辑删除字段标注 `@TableLogic`（仅 `campus_notice`、`visit_session` 启用）。
- 乐观锁字段 `version` 标注 `@Version`（仅 `visit_session`）。
- 自动填充：`create_time` 标 `@TableField(fill = FieldFill.INSERT)`，`update_time` 标 `@TableField(fill = FieldFill.INSERT_UPDATE)`。

---

## 十一、容量与性能预估（毕设级）

| 表 | 预估最大行数 | 单行平均大小 | 预估总大小 | 性能关注点 |
|----|------------|-------------|-----------|-----------|
| visitor_user | 1,000 | ~200B | 200KB | 登录查询走唯一索引，无瓶颈 |
| admin_user | 10 | ~150B | 1.5KB | 无 |
| campus_notice | 100 | ~2KB（含正文） | 200KB | 前台列表走联合索引 |
| visit_session | 100 | ~150B | 15KB | 乐观锁防超卖 |
| visit_reservation | 5,000 | ~300B | 1.5MB | 我的预约/审核队列走联合索引 |
| knowledge_doc | 20 | ~300B | 6KB | 无 |
| chat_session | 500 | ~100B | 50KB | 历史会话走联合索引 |
| chat_message | 10,000 | ~1KB（含正文） | 10MB | 日志统计按时间倒序 |

**结论**：毕设级数据规模下，所有表单表性能无瓶颈，无需分库分表，无需引入缓存（如 Redis）。如未来扩展到百万级预约，再考虑缓存与分表。

---

## 十二、文档变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-08-26 | 初始版本，定义 8 张 MySQL 表 + 1 个 Milvus 集合 | 开发团队 |

---

**本文档定稿后禁止私自修改，任何变更需走需求变更流程并更新版本号。**
