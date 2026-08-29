package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.dto.reservation.ReservationAuditDTO;
import com.campus.visit.dto.reservation.ReservationSubmitDTO;
import com.campus.visit.vo.reservation.ReservationDetailVO;
import com.campus.visit.vo.reservation.ReservationListVO;

/**
 * 访客预约 Service 接口（对标 architecture.md 模块 4）
 * 
 * 提交预约（4.1）——本系统业务核心：
 * 乐观锁扣减场次名额 + 重复预约校验 + 事务保证"插订单"与"扣名额"同生共死
 * myList 我的预约分页（4.2），只能看自己的
 * detail 预约详情（4.3），只能看自己的（越权 40301）
 * cancel 取消预约（4.4），仅待审核可取消，取消后回滚名额
 */
public interface ReservationService {

    /** 提交预约，返回订单 ID */
    Long submit(ReservationSubmitDTO dto);

    /** 我的预约分页（按当前登录访客过滤，status 可选） */
    Page<ReservationListVO> myList(Integer status, Integer current, Integer size);

    /** 预约详情（校验归属：非本人订单抛 40301） */
    ReservationDetailVO detail(Long id);

    /** 取消预约（仅本人 + 仅待审核；取消后乐观锁回滚名额） */
    void cancel(Long id);

    /* ============ 管理员端（对标 architecture.md 模块 5） ============ */

    /**
     * 5.1 预约订单分页（管理员看全部订单）
     * 
     * @param realName  访客姓名模糊（可空）
     * @param status    状态过滤（可空）
     * @param startDate 提交起始时间（可空）
     * @param endDate   提交截止时间（可空）
     */
    Page<ReservationListVO> pageForAdmin(String realName, Integer status,
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            Integer current, Integer size);

    /** 5.2 订单详情（管理员视角：不做归属校验，任何订单都能看） */
    ReservationDetailVO detailForAdmin(Long id);

    /**
     * 5.3 审核预约（通过 / 驳回）
     * 仅待审核可审（40022）；驳回必填原因（40001）；驳回回滚名额（乐观锁 + 事务）
     */
    void audit(Long id, ReservationAuditDTO dto);
}
