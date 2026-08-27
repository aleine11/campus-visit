package com.campus.visit.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局状态码常量
 *
 * 命名规则：对齐 architecture.md 第六章
 * - 2xx 成功（仅 SUCCESS）
 * - 400xx 客户端参数错误（通用）
 * - 401xx 认证错误（未登录/令牌）
 * - 403xx 权限错误（角色不匹配）
 * - 404xx 资源不存在
 * - 40010-40013 鉴权业务
 * - 40020-40023 场次/预约业务
 * - 40030-40031 知识库文档
 * - 40040 AI 服务
 * - 40050 Milvus 向量库
 * - 50000 服务器内部错误
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /* ============ 成功 ============ */
    SUCCESS(200, "操作成功"),

    /* ============ 通用客户端错误 ============ */
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40101, "未登录或登录已过期"),
    FORBIDDEN(40301, "无权限访问"),
    NOT_FOUND(40401, "资源不存在"),

    /* ============ 用户/鉴权 ============ */
    USERNAME_OR_PASSWORD_ERROR(40010, "用户名或密码错误"),
    USERNAME_EXISTS(40011, "用户名已存在"),
    ACCOUNT_FROZEN(40012, "账号已被冻结，请联系管理员"),
    OLD_PASSWORD_ERROR(40013, "旧密码错误"),

    /* ============ 场次 ============ */
    SESSION_NOT_FOUND(40401, "参观场次不存在"),
    SESSION_OFFLINE_OR_EXPIRED(40023, "场次已下架或已过期"),
    SESSION_PAST_DATE(40001, "不允许新增过去时间的场次"),

    /* ============ 预约 ============ */
    RESERVATION_NOT_FOUND(40401, "预约订单不存在"),
    RESERVATION_DUPLICATE(40020, "重复预约"),
    RESERVATION_NOT_ENOUGH(40021, "名额不足"),
    RESERVATION_STATUS_INVALID(40022, "订单状态不可流转"),
    RESERVATION_REQUIRE_REASON(40001, "驳回必须填写原因"),

    /* ============ 公告 ============ */
    NOTICE_NOT_FOUND(40401, "公告不存在"),

    /* ============ 知识库文档 ============ */
    DOC_TYPE_NOT_SUPPORT(40030, "文档类型不支持（仅 pdf/txt/docx）"),
    DOC_PARSE_FAILED(40031, "文档解析失败"),
    DOC_NOT_FOUND(40401, "知识库文档不存在"),

    /* ============ AI 服务 ============ */
    AI_SERVICE_ERROR(40040, "AI 服务调用失败，请稍后重试"),

    /* ============ Milvus 向量库 ============ */
    MILVUS_ERROR(40050, "Milvus 操作失败，请稍后重试"),

    /* ============ 服务器内部错误 ============ */
    SERVER_ERROR(50000, "服务器内部错误，请稍后重试");

    private final Integer code;
    /** 状态码对应的提示消息（与 architecture.md 的 Result.message 字段名保持一致） */
    private final String message;
}
