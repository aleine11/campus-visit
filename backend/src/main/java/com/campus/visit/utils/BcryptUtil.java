package com.campus.visit.utils;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码加密 / 校验工具类
 *
 * BCrypt 原理（为什么不用 MD5？）：
 *   ┌───────────────────────────────────────────────────────────┐
 *   │  MD5/SHA：速度快、单向、每次输出相同 → 彩虹表撞库攻击可用  │
 *   │  BCrypt：带随机盐 + 可调节 cost（默认 10 次哈希叠加）      │
 *   │     同一个明文每次加密结果不同（随机盐不一样）                │
 *   │     cost=10 时，单次加密约 100ms，撞库不现实                 │
 *   │     输出格式：$2a$10$<22位盐><31位哈希> 共 60 字符          │
 *   └───────────────────────────────────────────────────────────┘
 *
 * 加密流程：bcrypt("admin123") → "$2a$10$xxxxx..."
 * 校验流程：checkpw("admin123", "$2a$10$xxxxx...") → true/false
 */
@Component
public class BcryptUtil {

    /**
     * 加密 —— 每次生成不同的散列（因为随机盐）
     *
     * @param rawPassword 明文密码
     * @return BCrypt 散列字符串
     */
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 校验 —— 拿明文和已加密的散列比对
     *
     * @param rawPassword     用户输入的明文（登录时前端传过来的）
     * @param hashedPassword  数据库里存的 BCrypt 散列
     * @return true=密码匹配，false=不匹配
     */
    public boolean matches(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
