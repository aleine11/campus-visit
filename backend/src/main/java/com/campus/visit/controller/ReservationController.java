package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresLogin;
import com.campus.visit.common.Result;
import com.campus.visit.dto.reservation.ReservationSubmitDTO;
import com.campus.visit.service.ReservationService;
import com.campus.visit.vo.reservation.ReservationDetailVO;
import com.campus.visit.vo.reservation.ReservationListVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访客预约接口（对标 architecture.md 模块 4，共 4 个端点，全部需要访客登录）
 *
 *   POST /api/reservation              提交预约        🔒 @RequiresLogin
 *   GET  /api/reservation/my           我的预约分页    🔒 @RequiresLogin
 *   GET  /api/reservation/{id}         预约详情        🔒 @RequiresLogin（归属校验在 Service）
 *   POST /api/reservation/{id}/cancel  取消预约        🔒 @RequiresLogin（归属+状态校验在 Service）
 *
 * 注意：当前登录人从 UserContext 取（拦截器已写入），前端不用传 visitorId
 * —— 绝不信任前端传的身份，这是防越权的基本原则
 */
@RestController
@RequestMapping("/reservation")
@RequiresLogin
public class ReservationController {

    @Resource
    private ReservationService reservationService;

    /**
     * 4.1 提交预约（返回订单 ID）
     * 业务规则：重复预约 40020 / 名额不足 40021 / 场次下架或过期 40023
     */
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody ReservationSubmitDTO dto) {
        return Result.success(reservationService.submit(dto));
    }

    /**
     * 4.2 我的预约分页（status 可选过滤：0/1/2/3）
     */
    @GetMapping("/my")
    public Result<Page<ReservationListVO>> my(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(reservationService.myList(status, current, size));
    }

    /**
     * 4.3 预约详情（非本人订单 → 40301）
     */
    @GetMapping("/{id}")
    public Result<ReservationDetailVO> detail(@PathVariable Long id) {
        return Result.success(reservationService.detail(id));
    }

    /**
     * 4.4 取消预约（仅待审核可取消；取消后回滚场次名额）
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return Result.success();
    }
}
