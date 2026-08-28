package com.campus.visit.dto.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * 场次后台分页查询参数（12.1）
 *
 * GET 参数自动绑定（?visitDate=2026-09-01&status=0&current=1&size=10）
 * Spring MVC 会把 "2026-09-01" 字符串自动转成 LocalDate（iso 格式默认支持）
 */
@Data
public class SessionQueryDTO {

    /** 按参观日期精确过滤（可空） */
    private LocalDate visitDate;

    /** 状态过滤（可空 = 全部）：0=开放，1=下架 */
    @Min(value = 0, message = "status 只能为 0 或 1")
    @Max(value = 1, message = "status 只能为 0 或 1")
    private Integer status;

    /** 页码（默认 1） */
    @Min(value = 1, message = "页码最小为 1")
    private Integer current = 1;

    /** 每页条数（默认 10，最大 50） */
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 50, message = "每页条数最大为 50")
    private Integer size = 10;
}
