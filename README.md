# 校园参观预约与智能咨询系统

> 基于 SpringBoot + RAG 的校园参观预约与智能咨询系统设计与实现
>
> 哈尔滨剑桥学院 · 智能科学与工程学院 · 2026 届毕业设计

## 项目简介

面向高校对外开放参观场景的一体化 Web 系统，集成：

- 访客线上预约参观（注册登录、查看场次、提交预约、订单状态管理）
- 管理员后台审核管理（场次、预约、访客、公告、知识库）
- RAG 私有知识库 AI 智能咨询（基于本校文档，杜绝大模型幻觉）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | SpringBoot 3.2 + MyBatis-Plus + JWT |
| 前端 | Vue3 + Element-Plus + Axios + Vite |
| 业务数据库 | MySQL 8.0 |
| 向量数据库 | Milvus（Docker，768 维，集合 campus_knowledge） |
| 嵌入模型 | BGE-small-zh（本地） |
| 大模型 | 阿里百炼 DashScope（qwen-plus） |
| 文档解析 | PDFBox + POI |
| RAG 编排 | 手写（不依赖 LangChain 框架） |

## 目录结构

```
campus-visit/
├── backend/      # 后端 SpringBoot 项目
│   └── src/main/resources/
│       ├── application.yml          # 主配置（端口8088，context-path=/api）
│       ├── application-dev.yml      # 本地环境配置（不入库，见 .example 模板）
│       └── sql/                     # 建表 + 种子数据 SQL
├── frontend/     # 前端 Vue3 项目
├── docs/         # 设计文档与进度追踪
│   ├── architecture.md          # 项目架构与 49 个接口文档
│   ├── database.md              # 数据库设计文档（8 表 + Milvus 集合）
│   ├── frontend-prototype.md    # 前端原型结构文档
│   ├── 产品原型/                # 26 个产品原型 HTML
│   ├── 知识点.md                # 逐模块知识点手册（复习/答辩用）
│   ├── init-database.sql        # 建库脚本
│   └── progress.json            # 项目进度追踪
├── .gitignore
└── README.md
```

## 启动方式

### 1. 初始化数据库（MySQL 8.0）
```bash
mysql -uroot -p你的密码 < docs/init-database.sql                                  # 建库
mysql -uroot -p你的密码 campus_visit < backend/src/main/resources/sql/schema.sql   # 建表
mysql -uroot -p你的密码 campus_visit < backend/src/main/resources/sql/seed-data.sql # 种子数据
```

### 2. 配置本地环境
复制 `backend/src/main/resources/application-dev.yml.example` 为 `application-dev.yml`（该文件已被 .gitignore 忽略，不会上传），填入你的 MySQL 密码。

### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
# 访问 http://localhost:8088/api
```

### 4. 启动前端（可选，前端模块开发中）
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### 5. Milvus（AI 咨询模块需要，Docker 启动）
```bash
docker run -d --name milvus-standalone \
  -p 19530:19530 -p 9091:9091 \
  -v ./milvus/data:/var/lib/milvus \
  milvusdb/milvus:v2.3.4 standalone
```

## 预置账号

| 角色 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 超级管理员 | admin | admin123 | seed-data.sql 预置 |
| 访客 | 注册页面自行创建 | - | POST /api/auth/register |

## 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| Stage1 前半段 | 需求沟通 + 技术选型 | ✅ 完成 |
| Stage0 | 项目初始化（前后端骨架 + Git 仓库） | ✅ 完成 |
| Stage1 后半段 | 四份设计文档 + 26 页产品原型 | ✅ 完成 |
| Stage2 模块 0 | 后端基础组件（SQL/Entity/Mapper/JWT/拦截器） | ✅ 完成 |
| Stage2 模块 1 | 用户认证（注册/双表登录/改密/个人信息） | ✅ 完成（12 项实测通过） |
| Stage2 模块 2-12 | 公告/场次/预约/审核/RAG/AI 咨询/前端页面 | ⏳ 进行中 |
| Stage3 | 分模块功能测试 | ⏳ 待开始 |
| Stage4 | 部署上线 | ⏳ 待开始 |

> 实时进度见 [docs/progress.json](docs/progress.json)

## 安全说明

- 真实密码/密钥配置文件（application-dev.yml）不入 Git，仓库内只保留 `.example` 模板
- 密码使用 BCrypt 加密存储，数据库不存明文
- 接口鉴权使用 JWT（HS256），权限注解 @RequiresLogin / @RequiresRole 控制访问
