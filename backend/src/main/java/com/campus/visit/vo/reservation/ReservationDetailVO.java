package com.campus.visit.vo.reservation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约详情 VO（4.3）
 *
 * 在列表 VO 基础上追加：联系人信息（realName/phone）、参观事由 reason、
 * 审核信息（auditAdminName/auditTime/rejectReason）、取消时间 cancelTime
 */
@Data
@Builder
public class ReservationDetailVO {

    /** 订单 ID */
    private Long id;

    /** 场次 ID */
    private Long sessionId;

    /** 参观日期 */
    private LocalDate visitDate;

    /** 时段 */
    private String timeSlot;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 参观人数 */
    private Integer peopleCount;

    /** 参观事由 */
    private String reason;

    /** 订单状态 */
    private Integer status;

    /** 状态中文 */
    private String statusText;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核人姓名（未审核为 null） */
    private String auditAdminName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 驳回原因（status=2 时有） */
    private String rejectReason;

    /** 取消时间（访客取消时写入） */
    private LocalDateTime cancelTime;
}
