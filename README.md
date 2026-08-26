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
├── frontend/     # 前端 Vue3 项目
├── docs/         # 设计文档与进度追踪
│   ├── architecture.md          # 项目架构与接口文档
│   ├── database.md              # 数据库设计文档
│   ├── frontend-prototype.md    # 前端原型结构文档
│   ├── 产品原型/                # 产品原型 HTML 文件夹
│   └── progress.json             # 项目进度追踪
├── .gitignore
└── README.md
```

## 启动方式

### 后端
```bash
cd backend
mvn spring-boot:run
# 访问 http://localhost:8088/api
```

### 前端
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### Milvus（Docker 启动）
```bash
docker run -d --name milvus-standalone \
  -p 19530:19530 -p 9091:9091 \
  -v ./milvus/data:/var/lib/milvus \
  -v ./milvus/conf:/milvus/config \
  milvusdb/milvus:latest standalone
```

## 开发阶段

本项目按以下阶段推进：

- ✅ Stage1 前半段：需求沟通 + 技术选型
- 🔄 Stage0：项目初始化（当前阶段）
- ⏳ Stage1 后半段：产出四份设计文档
- ⏳ Stage2：分模块代码开发
- ⏳ Stage3：分模块功能测试
- ⏳ Stage4：部署上线
