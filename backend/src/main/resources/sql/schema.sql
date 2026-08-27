-- ============================================================
-- 校园参观预约与智能咨询系统 — 建表脚本
-- 对标 docs/database.md v1.0
-- 字符集：utf8mb4 | 引擎：InnoDB
-- 所有表按依赖顺序创建（先主后从）
-- ============================================================

-- 切换到目标数据库
USE campus_visit;

-- ============================================================
-- T1. 访客用户表 (visitor_user)
-- ============================================================
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

-- ============================================================
-- T2. 管理员账号表 (admin_user)
-- ============================================================
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

-- ============================================================
-- T3. 校园公告表 (campus_notice) — 启用逻辑删除
-- ============================================================
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

-- ============================================================
-- T4. 参观场次表 (visit_session) — 启用逻辑删除 + 乐观锁
-- ============================================================
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

-- ============================================================
-- T5. 预约订单表 (visit_reservation)
-- ============================================================
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

-- ============================================================
-- T6. 知识库文档表 (knowledge_doc)
-- ============================================================
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

-- ============================================================
-- T7. AI 会话表 (chat_session)
-- ============================================================
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

-- ============================================================
-- T8. AI 问答消息表 (chat_message)
-- ============================================================
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
