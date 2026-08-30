package com.campus.visit.vo.stats;

import com.campus.visit.vo.reservation.ReservationListVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理员后台首页看板 VO（对标 architecture.md 10.1）
 *
 * 六块数据一览：
 *   4 个数字卡片（今日预约 / 待审核 / 访客总数 / 问答总数）
 *   + 1 个 7 天趋势图数据
 *   + 1 个待审核快捷列表（管理员点进去就能审单）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    /** 今日新增预约数（submit_time 在今天 0 点及之后，含全部状态） */
    private Integer todayReservationCount;

    /** 待审核订单数（status=0） */
    private Integer pendingAuditCount;

    /** 访客总数（visitor_user 全表计数） */
    private Integer visitorTotal;

    /** AI 问答总次数（chat_message 中 role=assistant 的消息数：一次 AI 回答 = 一次有效问答） */
    private Integer chatTotalCount;

    /** 近 7 天每日预约数趋势（含今天，没有预约的日期补 0，保证图表 7 个点连续） */
    private List<DayCount> weeklyTrend;

    /** 最近 5 条待审核订单（复用模块 5 的管理员列表 VO 结构） */
    private List<ReservationListVO> recentPending;

    /**
     * 单日计数（趋势图的一个点）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayCount {

        /** 日期（近 7 天内某一天，格式 yyyy-MM-dd） */
        private LocalDate date;

        /** 当日新增预约数 */
        private Integer count;
    }
}
