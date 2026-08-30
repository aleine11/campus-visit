package com.campus.visit.vo.reservation;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约列表条目 VO（4.2 我的预约 / 5.1 管理员分页 共用）
 *
 * 对标 4.2 响应字段 + statusText 状态中文
 * （管理员版（5.1）额外返回 phone/reason/auditAdminName/auditTime → 后台复用时追加）
 */
@Data
@ToString
@Builder(toBuilder = true)
public class ReservationListVO {

    /** 订单 ID */
    private Long id;

    /** 场次 ID */
    private Long sessionId;

    /** 参观日期 */
    private LocalDate visitDate;

    /** 时段 */
    private String timeSlot;

    /** 参观人数 */
    private Integer peopleCount;

    /** 订单状态：0=待审核 1=通过 2=驳回 3=已取消 */
    private Integer status;

    /** 状态中文（待审核/通过/驳回/已取消） */
    private String statusText;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /* ===== 管理员分页（5.1）追加字段，访客接口（4.2）不填 ===== */

    /** 访客姓名（订单提交时的快照，visit_reservation.real_name；模块 12 前端列表展示用） */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 参观事由 */
    private String reason;

    /** 审核人姓名（未审核为 null） */
    private String auditAdminName;

    /** 审核时间 */
    private LocalDateTime auditTime;
}
