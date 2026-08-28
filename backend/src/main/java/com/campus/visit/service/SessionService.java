package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.dto.session.SessionQueryDTO;
import com.campus.visit.dto.session.SessionSaveDTO;
import com.campus.visit.vo.session.SessionDetailVO;
import com.campus.visit.vo.session.SessionListVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 参观场次 Service 接口
 *
 * 前台 3 个方法（对标 architecture.md 模块 3，公开访问）：
 *   listAvailable  可预约场次分页（自动过滤过期/下架/关闭场次）
 *   latest         最新可预约场次 N 条（首页用）
 *   detail         场次详情
 *
 * 后台 6 个方法（对标 architecture.md 模块 12，管理员专用）：
 *   pageForAdmin   分页（按日期/状态过滤）
 *   save           新增场次（禁止过去日期）
 *   update         编辑（缩容保护：maxPeople 不得小于 usedPeople）
 *   online         上架（下架 → 开放）
 *   offline        下架（开放 → 下架）
 *   remove         删除（有预约记录禁止删除）
 */
public interface SessionService {

    /* ============ 前台（公开） ============ */

    /**
     * 可预约场次分页
     * @param startDate 起始日期（null = 今天）
     * @param endDate   截止日期（null = 今天 + 30 天）
     */
    Page<SessionListVO> listAvailable(LocalDate startDate, LocalDate endDate, Integer current, Integer size);

    /** 最新可预约场次 N 条（count 上限 10） */
    List<SessionListVO> latest(Integer count);

    /** 场次详情（存在即返回，含下架状态，前端据 status 决定能否预约） */
    SessionDetailVO detail(Long id);

    /* ============ 后台（管理员） ============ */

    /** 后台分页：日期精确过滤 + 状态过滤 */
    Page<SessionListVO> pageForAdmin(SessionQueryDTO query);

    /** 新增场次（过去日期已被 DTO 注解拦截，这里做二次防御） */
    Long save(SessionSaveDTO dto);

    /**
     * 编辑场次
     * 缩容保护：已预约人数 > 0 时，maxPeople 不得改为小于 usedPeople（40022）
     */
    void update(Long id, SessionSaveDTO dto);

    /** 上架：下架 → 开放（状态机校验） */
    void online(Long id);

    /** 下架：开放 → 下架（状态机校验） */
    void offline(Long id);

    /**
     * 删除场次
     * 删除保护：usedPeople > 0 禁止删除（40022），防止预约表出现孤儿数据
     */
    void remove(Long id);
}
