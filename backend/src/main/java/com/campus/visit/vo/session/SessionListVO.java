package com.campus.visit.vo.session;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 场次列表条目 VO（前台可预约列表 / 最新场次 / 后台分页 三处共用）
 *
 * 对标 architecture.md 3.1 响应字段：
 *   id         场次 ID
 *   visitDate  参观日期
 *   timeSlot   时段
 *   maxPeople  最大容纳人数
 *   usedPeople 已预约人数
 *   remaining  剩余名额（= maxPeople - usedPeople，前端直接展示不用自己算）
 *
 * 后台分页（12.1）额外需要 status 字段 → 复用本类 status 字段
 */
@Data
@ToString
@Builder(toBuilder = true)
public class SessionListVO {

    /** 场次 ID */
    private Long id;

    /** 参观日期 */
    private LocalDate visitDate;

    /** 时段（如 "09:00-11:00"） */
    private String timeSlot;

    /** 最大容纳人数 */
    private Integer maxPeople;

    /** 已预约人数 */
    private Integer usedPeople;

    /** 剩余名额（max - used） */
    private Integer remaining;

    /** 场次状态（仅后台接口填：0=开放 1=下架） */
    private Integer status;
}
