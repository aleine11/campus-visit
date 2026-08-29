package com.campus.visit.vo.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员列表条目 VO（对标 architecture.md 7.1 AdminListVO）
 *
 * ⚠️ 安全设计：绝不返回 password 字段
 */
@Data
@Builder
public class AdminListVO {

    /** 管理员 ID */
    private Long id;

    /** 账号 */
    private String username;

    /** 姓名 */
    private String realName;

    /** 是否超管 */
    private Boolean isSuper;

    /** 创建时间 */
    private LocalDateTime createTime;
}
