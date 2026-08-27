package com.campus.visit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 校园公告实体类
 * 对应数据库表：campus_notice
 * 状态字典 D2：0=未发布（草稿），1=已发布（前台可见）
 * 启用逻辑删除（deleted 字段，0=正常，1=已删除）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("campus_notice")
public class CampusNotice {

    /** 公告ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题（1~100字） */
    private String title;

    /** 公告正文（TEXT 类型，支持换行） */
    private String content;

    /** 发布状态：0=未发布，1=已发布 */
    private Integer status;

    /** 发布人管理员ID（草稿状态为 NULL） */
    private Long publishAdminId;

    /** 发布时间（首次发布写入，下架不重置） */
    private LocalDateTime publishTime;

    /** 逻辑删除：0=正常，1=已删除（全局自动过滤） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
