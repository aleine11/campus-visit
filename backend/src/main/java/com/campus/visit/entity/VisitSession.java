package com.campus.visit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 参观场次实体类
 * 对应数据库表：visit_session
 * 状态字典 D3：0=开放，1=下架
 * 特性：逻辑删除 + 乐观锁（防超卖）
 *
 * 乐观锁原理：每次更新时带上 version 条件
 * UPDATE SET used_people=xxx, version=version+1 WHERE id=? AND version=?
 * 影响行数=0 表示被别人先更新了，回滚重试
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("visit_session")
public class VisitSession {

    /** 场次ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参观日期（仅日期，如 2026-09-01） */
    private LocalDate visitDate;

    /** 时段（如 "09:00-11:00"） */
    private String timeSlot;

    /** 最大容纳人数（1~500） */
    private Integer maxPeople;

    /** 已预约人数（审核通过 +1，取消 -1） */
    private Integer usedPeople;

    /** 乐观锁版本号（MyBatis-Plus 自动维护） */
    @Version
    private Integer version;

    /** 场次状态：0=开放，1=下架 */
    private Integer status;

    /** 逻辑删除：0=正常，1=已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
