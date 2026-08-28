package com.campus.visit.dto.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

import java.time.LocalDate;

/**
 * 场次新增/编辑请求参数（12.2 / 12.3 共用）
 *
 * 校验规则（对标 architecture.md 12.2）：
 * visitDate 不早于今天（过去日期拦截 → 40001）
 * timeSlot 非空，格式如 "09:00-11:00"
 * maxPeople 1~500
 * status 0=开放 1=下架
 *
 * 日期"不早于今天"用标准注解 @FutureOrPresent：
 * LocalDate 无时分秒，"present" 即指今天 → 注解效果 = 今天及以后才合法
 * 配合 Controller 的 @Valid 触发，非法值被 GlobalExceptionHandler 统一转 40001
 */
@Data
public class SessionSaveDTO {

    /** 参观日期（必须今天或未来） */
    @NotNull(message = "参观日期不能为空")
    @FutureOrPresent(message = "参观日期不能早于今天，禁止创建过去时间的场次")
    private LocalDate visitDate;

    /** 时段（如 "09:00-11:00"） */
    @NotBlank(message = "时段不能为空")
    @Size(max = 50, message = "时段长度不能超过50字符")
    @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$", message = "时段格式须为 HH:mm-HH:mm，如 09:00-11:00")
    private String timeSlot;

    /** 最大容纳人数 */
    @NotNull(message = "最大容纳人数不能为空")
    @Min(value = 1, message = "最大容纳人数最小为 1")
    @Max(value = 500, message = "最大容纳人数最大为 500")
    private Integer maxPeople;

    /** 场次状态：0=开放，1=下架 */
    @NotNull(message = "场次状态不能为空")
    @Min(value = 0, message = "status 只能为 0（开放）或 1（下架）")
    @Max(value = 1, message = "status 只能为 0（开放）或 1（下架）")
    private Integer status;
}
