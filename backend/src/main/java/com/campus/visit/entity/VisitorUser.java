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
 * 访客用户实体类
 * 对应数据库表：visitor_user
 * 状态字典 D1：0=正常，1=冻结
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("visitor_user")
public class VisitorUser {

    /** 访客用户ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（4~20位字母数字下划线，唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 真实姓名（2~10字） */
    private String realName;

    /** 联系手机号（11位中国手机号） */
    private String phone;

    /** 账号状态：0=正常，1=冻结 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
