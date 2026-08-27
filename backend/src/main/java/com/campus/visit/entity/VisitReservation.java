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
 * 预约订单实体类
 * 对应数据库表：visit_reservation
 * 状态字典 D4：0=待审核，1=通过，2=驳回，3=已取消
 *
 * 状态流转：
 *   访客提交 → 0(待审核)
 *   管理员审核通过 → 1(通过)
 *   管理员驳回 → 2(驳回)，必填 reject_reason
 *   访客主动取消 → 3(已取消)，仅 status=0 可取消
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("visit_reservation")
public class VisitReservation {

    /** 预约订单ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联场次ID */
    private Long sessionId;

    /** 关联访客用户ID */
    private Long visitorId;

    /** 真实姓名（冗余存入，避免访客改名影响历史订单） */
    private String realName;

    /** 联系手机号（冗余存入） */
    private String phone;

    /** 参观人数（1~剩余名额，最大 50） */
    private Integer peopleCount;

    /** 参观事由（5~200字） */
    private String reason;

    /** 订单状态：0=待审核，1=通过，2=驳回，3=已取消 */
    private Integer status;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核人管理员ID（待审核/已取消时为 NULL） */
    private Long auditAdminId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 驳回原因（status=2 时必填） */
    private String rejectReason;

    /** 取消时间（访客主动取消时写入） */
    private LocalDateTime cancelTime;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
