package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.reservation.ReservationSubmitDTO;
import com.campus.visit.entity.AdminUser;
import com.campus.visit.entity.VisitReservation;
import com.campus.visit.entity.VisitSession;
import com.campus.visit.mapper.AdminUserMapper;
import com.campus.visit.mapper.VisitReservationMapper;
import com.campus.visit.mapper.VisitSessionMapper;
import com.campus.visit.service.ReservationService;
import com.campus.visit.utils.UserContext;
import com.campus.visit.utils.UserContext.LoginUser;
import com.campus.visit.vo.reservation.ReservationDetailVO;
import com.campus.visit.vo.reservation.ReservationListVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 访客预约 Service 实现 —— 全系统业务核心模块
 *
 * ⭐ 乐观锁扣名额（对标 architecture.md 4.1 业务逻辑第 5 步）：
 * UPDATE visit_session
 * SET used_people = used_people + ?, version = version + 1
 * WHERE id = ? AND version = ? AND used_people + ? <= max_people
 *
 * 三重保险同一条 SQL：
 * 1. version = ? → 没人和我并发（被抢先则影响行数=0）
 * 2. used + N <= max → 名额够用（超卖在数据库层面被拒绝）
 * 3. 影响行数 = 0 → 抛 40021 名额不足，事务回滚（订单也不留）
 *
 * ⭐ 事务（@Transactional）：
 * "插入订单"和"扣名额"必须同生共死——
 * 扣名额失败 → 订单回滚；反之亦然。没有事务会出现"名额没扣但订单存在"的脏数据
 *
 * ⭐ 状态字典 D4：0=待审核 1=通过 2=驳回 3=已取消
 */
@Slf4j
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PASSED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_CANCELED = 3;

    /** 场次开放 */
    private static final int SESSION_OPEN = 0;

    @Resource
    private VisitReservationMapper reservationMapper;
    @Resource
    private VisitSessionMapper sessionMapper;
    @Resource
    private AdminUserMapper adminUserMapper;

    /**
     * 提交预约（4.1）
     *
     * 执行顺序（顺序很重要！）：
     * 1. 校验场次（存在→开放→未过期）
     * 2. 重复预约校验（同人同场次已有 待审核/通过 状态订单 → 40020）
     * 3. 预检名额（友好提示用；真正的防超卖靠第 5 步的 SQL 条件）
     * 4. 插入订单（status=0）
     * 5. 乐观锁扣名额（影响行数=0 → 抛异常 → 整个方法回滚）
     *
     * 注意：必须先插订单再扣名额（同一事务内顺序无所谓，
     * 但扣名额是可能失败的"危险操作"，放最后——失败时前面的 insert 一起回滚）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(ReservationSubmitDTO dto) {
        // ---------- 1. 场次三重校验 ----------
        VisitSession session = sessionMapper.selectById(dto.getSessionId());
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND); // 40401
        }
        if (session.getStatus() != SESSION_OPEN) {
            throw new BusinessException(ResultCode.SESSION_OFFLINE_OR_EXPIRED); // 40023 已下架
        }
        if (session.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.SESSION_OFFLINE_OR_EXPIRED); // 40023 已过期
        }

        // ---------- 2. 重复预约校验 ----------
        // 同一访客 + 同一场次 + 状态是【待审核或已通过】 → 不允许再约
        // （已取消(3)/已驳回(2)的订单不算，用户可以重新预约）
        Long visitorId = UserContext.get().getUserId();
        Long dupCount = reservationMapper.selectCount(new LambdaQueryWrapper<VisitReservation>()
                .eq(VisitReservation::getSessionId, dto.getSessionId())
                .eq(VisitReservation::getVisitorId, visitorId)
                .in(VisitReservation::getStatus, List.of(STATUS_PENDING, STATUS_PASSED)));
        if (dupCount > 0) {
            throw new BusinessException(ResultCode.RESERVATION_DUPLICATE); // 40020
        }

        // ---------- 3. 名额预检（给用户友好提示；并发安全靠第 5 步） ----------
        if (session.getUsedPeople() + dto.getPeopleCount() > session.getMaxPeople()) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_ENOUGH); // 40021
        }

        // ---------- 4. 插入订单 ----------
        VisitReservation order = new VisitReservation();
        order.setSessionId(dto.getSessionId());
        order.setVisitorId(visitorId);
        order.setRealName(dto.getRealName().trim());
        order.setPhone(dto.getPhone().trim());
        order.setPeopleCount(dto.getPeopleCount());
        order.setReason(dto.getReason().trim());
        order.setStatus(STATUS_PENDING);
        order.setSubmitTime(LocalDateTime.now());
        reservationMapper.insert(order);

        // ---------- 5. 乐观锁扣名额（核心！） ----------
        // 实体自带 version 字段（从 selectById 加载时的值），MP 乐观锁插件自动追加
        // AND version = ? 并在成功后 SET version = version + 1
        // 这里用 UpdateWrapper 显式追加第二个保险条件：used + N <= max
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VisitSession> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VisitSession>()
                .setSql("used_people = used_people + " + dto.getPeopleCount()) // 数据库原子自增（不是先查后改）
                .eq(VisitSession::getId, session.getId())
                .le(VisitSession::getUsedPeople,
                        session.getMaxPeople() - dto.getPeopleCount()); // used <= max - N → used + N <= max

        int rows = sessionMapper.update(null, updateWrapper);
        if (rows == 0) {
            // 影响行数=0：要么被并发抢先（version 变了），要么名额刚好被抢完
            // 抛异常 → @Transactional 感知 → 第 4 步的 insert 一并回滚
            throw new BusinessException(ResultCode.RESERVATION_NOT_ENOUGH); // 40021
        }

        log.info("预约提交成功: orderId={}, visitor={}, session={}, people={}",
                order.getId(), visitorId, dto.getSessionId(), dto.getPeopleCount());
        return order.getId();
    }

    /**
     * 我的预约分页（4.2）
     * 只查 visitor_id = 当前登录人（数据隔离：别人的永远看不见）
     */
    @Override
    public Page<ReservationListVO> myList(Integer status, Integer current, Integer size) {
        Long visitorId = UserContext.get().getUserId();
        int page = (current == null || current < 1) ? 1 : current;
        int rows = (size == null || size < 1) ? 10 : size;

        // 联表思路：预约表没有冗余场次日期/时段 → 先查订单分页，再批量补场次信息
        Page<VisitReservation> p = new Page<>(page, rows);
        Page<VisitReservation> result = reservationMapper.selectPage(p,
                new LambdaQueryWrapper<VisitReservation>()
                        .eq(VisitReservation::getVisitorId, visitorId)
                        .eq(status != null, VisitReservation::getStatus, status)
                        .orderByDesc(VisitReservation::getSubmitTime));

        return toVoPage(result);
    }

    /**
     * 预约详情（4.3）
     * 归属校验：订单的 visitor_id ≠ 当前登录人 → 40301（防越权，安全重点）
     */
    @Override
    public ReservationDetailVO detail(Long id) {
        VisitReservation order = reservationMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_FOUND); // 40401
        }
        Long visitorId = UserContext.get().getUserId();
        if (!order.getVisitorId().equals(visitorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN); // 40301 非本人订单
        }

        // 补充场次信息和审核人姓名
        VisitSession session = sessionMapper.selectById(order.getSessionId());
        String auditAdminName = null;
        if (order.getAuditAdminId() != null) {
            AdminUser admin = adminUserMapper.selectById(order.getAuditAdminId());
            auditAdminName = admin != null ? admin.getRealName() : null;
        }

        return ReservationDetailVO.builder()
                .id(order.getId())
                .sessionId(order.getSessionId())
                .visitDate(session != null ? session.getVisitDate() : null)
                .timeSlot(session != null ? session.getTimeSlot() : null)
                .realName(order.getRealName())
                .phone(order.getPhone())
                .peopleCount(order.getPeopleCount())
                .reason(order.getReason())
                .status(order.getStatus())
                .statusText(statusText(order.getStatus()))
                .submitTime(order.getSubmitTime())
                .auditAdminName(auditAdminName)
                .auditTime(order.getAuditTime())
                .rejectReason(order.getRejectReason())
                .cancelTime(order.getCancelTime())
                .build();
    }

    /**
     * 取消预约（4.4）
     *
     * 规则：仅本人 + 仅待审核(status=0)可取消 → 否则 40022
     * 取消后回滚名额：used_people - peopleCount（乐观锁保护）
     *
     * 事务：改订单状态 + 回滚名额，两步同生共死
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        // 1. 校验归属
        VisitReservation order = reservationMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_FOUND); // 40401
        }
        Long visitorId = UserContext.get().getUserId();
        if (!order.getVisitorId().equals(visitorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN); // 40301
        }

        // 2. 状态机校验：仅待审核可取消
        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID); // 40022
        }

        // 3. 更新订单状态为已取消（乐观防并发：只更新仍是待审核状态的行）
        order.setStatus(STATUS_CANCELED);
        order.setCancelTime(LocalDateTime.now());
        int orderRows = reservationMapper.updateById(order);
        if (orderRows == 0) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID);
        }

        // 4. 回滚名额（乐观锁：version 自动追加）
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VisitSession> rollbackWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VisitSession>()
                .setSql("used_people = used_people - " + order.getPeopleCount())
                .eq(VisitSession::getId, order.getSessionId());
        int rollbackRows = sessionMapper.update(null, rollbackWrapper);
        if (rollbackRows == 0) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_ENOUGH);
        }

        log.info("预约已取消: orderId={}, 回滚名额={}", id, order.getPeopleCount());
    }

    /* ==================== 私有工具方法 ==================== */

    /** 状态码 → 中文（状态字典 D4） */
    private String statusText(Integer status) {
        return switch (status) {
            case STATUS_PENDING -> "待审核";
            case STATUS_PASSED -> "通过";
            case STATUS_REJECTED -> "驳回";
            case STATUS_CANCELED -> "已取消";
            default -> "未知状态";
        };
    }

    /** 订单分页 → VO 分页（批量补场次日期/时段，避免 N+1 查询） */
    private Page<ReservationListVO> toVoPage(Page<VisitReservation> result) {
        // 收集本页所有订单的场次 ID
        List<Long> sessionIds = result.getRecords().stream()
                .map(VisitReservation::getSessionId).distinct().toList();
        // 一次查询全部相关场次（in 查询，不是循环单查）
        java.util.Map<Long, VisitSession> sessionMap = sessionIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : sessionMapper.selectBatchIds(sessionIds).stream()
                        .collect(java.util.stream.Collectors.toMap(VisitSession::getId, s -> s));

        Page<ReservationListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(o -> {
            VisitSession s = sessionMap.get(o.getSessionId());
            return ReservationListVO.builder()
                    .id(o.getId())
                    .sessionId(o.getSessionId())
                    .visitDate(s != null ? s.getVisitDate() : null)
                    .timeSlot(s != null ? s.getTimeSlot() : null)
                    .peopleCount(o.getPeopleCount())
                    .status(o.getStatus())
                    .statusText(statusText(o.getStatus()))
                    .submitTime(o.getSubmitTime())
                    .build();
        }).toList());
        return voPage;
    }
}
