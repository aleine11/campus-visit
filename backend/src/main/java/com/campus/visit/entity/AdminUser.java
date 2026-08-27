package com.campus.visit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员账号实体类
 * 对应数据库表：admin_user
 * is_super：0=普通管理员，1=超级管理员（拥有全部权限）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_user")
public class AdminUser {

    /** 管理员ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员账号（唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 管理员姓名 */
    private String realName;

    /** 是否超管：0=普通管理员，1=超级管理员 */
    private Integer isSuper;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
