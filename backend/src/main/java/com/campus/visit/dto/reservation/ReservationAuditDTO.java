package com.campus.visit.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预约审核请求参数（对标 architecture.md 5.3 ReservationAuditDTO）
 *
 *   pass          true=通过 / false=驳回（必填）
 *   rejectReason  驳回原因（5~200 字）
 *
 * ⭐ 条件必填：rejectReason 只在 pass=false 时必填
 *   Bean Validation 注解只能表达"永远必填"，表达不了"条件必填"
 *   → @Size 只管格式（填了就须 5~200 字），"驳回时必须填"的逻辑在 Service 里判断
 *
 * 注意：@Size 对 null 值直接跳过校验（null 被视为"合法"），
 * 所以这里不能只靠注解拦截"驳回但没写原因"的场景
 */
@Data
public class ReservationAuditDTO {

    /** 审核结论：true=通过，false=驳回 */
    @NotNull(message = "审核结论 pass 不能为空（true=通过，false=驳回）")
    private Boolean pass;

    /** 驳回原因（pass=false 时必填，5~200 字；Service 层校验必填性） */
    @Size(min = 5, max = 200, message = "驳回原因长度须在5~200字之间")
    private String rejectReason;
}
