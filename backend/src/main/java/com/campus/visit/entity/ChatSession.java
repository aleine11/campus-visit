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
 * AI 会话实体类
 * 对应数据库表：chat_session
 *
 * 设计说明：
 * - 一次会话 = 访客和 AI 的一段完整对话
 * - 一次会话下有多条 chat_message（user 提问 + assistant 回答）
 * - title 默认由首条问题前 20 字自动生成
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_session")
public class ChatSession {

    /** 会话ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属访客用户ID */
    private Long visitorId;

    /** 会话标题（首条问题前 20 字，便于历史列表展示） */
    private String title;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
