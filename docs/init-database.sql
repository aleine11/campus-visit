# 数据库初始化脚本 - 仅建库
# Stage0 阶段只创建空库，表结构在 Stage1 数据库设计文档定稿后通过单独 SQL 创建
CREATE DATABASE IF NOT EXISTS campus_visit
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE campus_visit;
