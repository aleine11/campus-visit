package com.campus.visit.dto.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交预约请求参数（对标 architecture.md 4.1 ReservationSubmitDTO）
 *
 * 校验规则：
 *   sessionId    必填（存在性/开放性/未过期在 Service 里校验）
 *   realName     2~10 字
 *   phone        11 位手机号（1 开头 + 10 位数字）
 *   peopleCount  1~50（不超过剩余名额在 Service 校验，因为要查库）
 *   reason       5~200 字
 *
 * 注意：peopleCount 的上限 50 是写死的注解校验，
 * 而"1~剩余名额"必须查数据库才知道，属于业务校验 → 放在 Service 层
 */
@Data
public class ReservationSubmitDTO {

    /** 场次 ID */
    @NotNull(message = "场次 ID 不能为空")
    @Min(value = 1, message = "场次 ID 不合法")
    private Long sessionId;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 10, message = "真实姓名长度须在2~10字之间")
    private String realName;

    /** 联系手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确（须为11位大陆手机号）")
    private String phone;

    /** 参观人数 */
    @NotNull(message = "参观人数不能为空")
    @Min(value = 1, message = "参观人数最少 1 人")
    @Max(value = 50, message = "单笔预约最多 50 人")
    private Integer peopleCount;

    /** 参观事由 */
    @NotBlank(message = "参观事由不能为空")
    @Size(min = 5, max = 200, message = "参观事由长度须在5~200字之间")
    private String reason;
}
