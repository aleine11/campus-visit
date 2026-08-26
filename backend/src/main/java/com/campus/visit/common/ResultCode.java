package com.campus.visit.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局状态码常量
 *
 * 命名规则：
 *   - 2xx 成功
 *   - 4xx 客户端错误（参数错、未登录、无权限等）
 *   - 5xx 服务端错误
 *   - 1xxx 业务错误（按模块分段）
 *
 * 业务码分段：
 *   - 1xxx 通用业务
 *   - 2xxx 用户/鉴权模块
 *   - 3xxx 场次模块
 *   - 4xxx 预约模块
 *   - 5xxx 公告模块
 *   - 6xxx 知识库/RAG 模块
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /* ============== 通用 ============== */
    SUCCESS(200, "操作成功"),
    PARAM_INVALID(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    /* ============== 用户/鉴权模块 2xxx ============== */
    USERNAME_OR_PASSWORD_ERROR(2001, "账号或密码错误"),
    ACCOUNT_FROZEN(2002, "账号已被冻结，请联系管理员"),
    USERNAME_EXISTS(2003, "用户名已存在"),
    OLD_PASSWORD_ERROR(2004, "原密码错误"),
    TOKEN_INVALID(2005, "令牌无效"),

    /* ============== 场次模块 3xxx ============== */
    SESSION_NOT_FOUND(3001, "参观场次不存在"),
    SESSION_FULL(3002, "场次名额已满"),
    SESSION_OFFLINE(3003, "场次已下架"),
    SESSION_EXPIRED(3004, "场次已过期"),
    SESSION_PAST_DATE(3005, "不允许新增过去时间的场次"),

    /* ============== 预约模块 4xxx ============== */
    RESERVATION_NOT_FOUND(4001, "预约订单不存在"),
    RESERVATION_DUPLICATE(4002, "同一访客不可重复预约同一场次"),
    RESERVATION_CANNOT_CANCEL(4003, "仅待审核订单可取消"),
    RESERVATION_AUDITED(4004, "已审核订单不可重复审核"),
    RESERVATION_REQUIRE_REASON(4005, "驳回必须填写原因"),

    /* ============== 公告模块 5xxx ============== */
    NOTICE_NOT_FOUND(5001, "公告不存在"),

    /* ============== 知识库/RAG 模块 6xxx ============== */
    DOC_PARSE_FAILED(6001, "文档解析失败"),
    DOC_NOT_FOUND(6002, "知识库文档不存在"),
    VECTOR_DB_ERROR(6003, "向量数据库异常"),
    LLM_CALL_FAILED(6004, "大模型调用失败"),
    EMBEDDING_FAILED(6005, "文本向量化失败"),
    RAG_NO_ANSWER(6006, "知识库未查询到相关内容，请咨询人工老师。禁止编造任何信息。");

    private final Integer code;
    private final String msg;
}
