package com.campus.visit.vo.visitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客列表条目 VO（对标 architecture.md 6.1 VisitorListVO）
 *
 * ⚠️ 安全设计：绝不返回 password 字段（即使是加密散列也不出网关）
 */
@Data
@Builder
public class VisitorListVO {

    /** 访客 ID */
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 状态：0=正常 1=冻结（状态字典 D1） */
    private Integer status;

    /** 状态中文：正常 / 冻结 */
    private String statusText;

    /** 注册时间 */
    private LocalDateTime registerTime;
}
