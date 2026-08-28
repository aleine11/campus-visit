package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.dto.reservation.ReservationSubmitDTO;
import com.campus.visit.vo.reservation.ReservationDetailVO;
import com.campus.visit.vo.reservation.ReservationListVO;

/**
 * 访客预约 Service 接口（对标 architecture.md 模块 4）
 *
 * submit    提交预约（4.1）——本系统业务核心：
 *           乐观锁扣减场次名额 + 重复预约校验 + 事务保证"插订单"与"扣名额"同生共死
 * myList    我的预约分页（4.2），只能看自己的
 * detail    预约详情（4.3），只能看自己的（越权 40301）
 * cancel    取消预约（4.4），仅待审核可取消，取消后回滚名额
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
}
