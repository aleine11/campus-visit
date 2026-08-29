package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.dto.reservation.ReservationAuditDTO;
import com.campus.visit.service.ReservationService;
import com.campus.visit.vo.reservation.ReservationDetailVO;
import com.campus.visit.vo.reservation.ReservationListVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 预约审核后台接口（对标 architecture.md 模块 5，共 3 个端点，全部管理员）
 *
 *   GET  /api/admin/reservation/page          订单分页检索  🔒 @RequiresRole("admin")
 *   GET  /api/admin/reservation/{id}          订单详情      🔒（无归属校验，管理员可看全部）
 *   POST /api/admin/reservation/{id}/audit    审核订单      🔒（通过/驳回，驳回回滚名额）
 *
 * 路径 /admin/reservation 与访客 /reservation 区分：
 *   同一份订单数据，两个视角两套权限，Service 层共用（buildDetailVO 复用）
 */
@RestController
@RequestMapping("/admin/reservation")
@RequiresRole("admin")
public class AdminReservationController {

    @Resource
    private ReservationService reservationService;

    /**
     * 5.1 订单分页检索
     * realName 模糊 / status 过滤 / 提交时间范围，全部可选
     * 追加字段：phone / reason / auditAdminName / auditTime
     */
    @GetMapping("/page")
    public Result<Page<ReservationListVO>> page(
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(reservationService.pageForAdmin(realName, status, startDate, endDate, current, size));
    }

    /**
     * 5.2 订单详情（同访客版字段，但不做归属校验）
     */
    @GetMapping("/{id}")
    public Result<ReservationDetailVO> detail(@PathVariable Long id) {
        return Result.success(reservationService.detailForAdmin(id));
    }

    /**
     * 5.3 审核订单
     * pass=true 通过；pass=false 驳回（rejectReason 必填 5~200 字，驳回回滚名额）
     * 已审核订单再审核 → 40022
     */
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ReservationAuditDTO dto) {
        reservationService.audit(id, dto);
        return Result.success();
    }
}
