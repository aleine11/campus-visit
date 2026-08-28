package com.campus.visit.vo.session;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 场次详情 VO（3.3）
 *
 * 字段同列表 VO + status（前台详情页需要知道场次是否还开放）
 */
@Data
@Builder
public class SessionDetailVO {

    /** 场次 ID */
    private Long id;

    /** 参观日期 */
    private LocalDate visitDate;

    /** 时段 */
    private String timeSlot;

    /** 最大容纳人数 */
    private Integer maxPeople;

    /** 已预约人数 */
    private Integer usedPeople;

    /** 剩余名额（max - used） */
    private Integer remaining;

    /** 状态：0=开放，1=下架 */
    private Integer status;
}
