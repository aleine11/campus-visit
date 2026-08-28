package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.session.SessionQueryDTO;
import com.campus.visit.dto.session.SessionSaveDTO;
import com.campus.visit.entity.VisitSession;
import com.campus.visit.mapper.VisitSessionMapper;
import com.campus.visit.service.SessionService;
import com.campus.visit.vo.session.SessionDetailVO;
import com.campus.visit.vo.session.SessionListVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 参观场次 Service 实现
 *
 * 三大业务约束（对标开发文档"关键业务约束"，测试重点清单第 1 条）：
 *   1. 禁止创建过去时间场次  → DTO @FutureOrPresent 拦截（controller 层）+ save 二次防御
 *   2. 编辑缩容保护          → maxPeople 不得小于 usedPeople（40022）
 *   3. 删除保护              → usedPeople > 0 禁止删除（40022）
 *
 * 前台"可预约"的定义（对标 3.1 业务逻辑）：
 *   status=0（开放） AND visit_date >= 今天（过滤过期） AND 在查询日期范围内
 *
 * 乐观锁说明：实体带 @Version，本模块的编辑走 selectById→改→updateById，
 * MP 自动追加 AND version=? 条件；真正的"防超卖"战场在模块 4 扣减名额
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    /** 场次开放（状态字典 D3） */
    private static final int STATUS_OPEN = 0;
    /** 场次下架 */
    private static final int STATUS_OFFLINE = 1;

    /** 默认查询范围：今天 + 30 天 */
    private static final int DEFAULT_RANGE_DAYS = 30;

    @Resource
    private VisitSessionMapper sessionMapper;

    /* ==================== 前台 ==================== */

    /**
     * 可预约场次分页
     *
     * WHERE status=0 AND visit_date >= 起始 AND visit_date <= 截止
     * ORDER BY visit_date ASC, time_slot ASC（按日期+时段正序，越近的场次排越前）
     */
    @Override
    public Page<SessionListVO> listAvailable(LocalDate startDate, LocalDate endDate, Integer current, Integer size) {
        // 参数兜底：起始默认今天，截止默认今天+30天；起始晚于截止时纠正为默认范围
        LocalDate today = LocalDate.now();
        LocalDate start = startDate == null ? today : startDate;
        LocalDate end = endDate == null ? today.plusDays(DEFAULT_RANGE_DAYS) : endDate;
        if (start.isAfter(end)) {
            start = today;
            end = today.plusDays(DEFAULT_RANGE_DAYS);
        }
        int page = (current == null || current < 1) ? 1 : current;
        int rows = (size == null || size < 1) ? 10 : size;

        Page<VisitSession> p = new Page<>(page, rows);
        LambdaQueryWrapper<VisitSession> wrapper = new LambdaQueryWrapper<VisitSession>()
                .eq(VisitSession::getStatus, STATUS_OPEN)          // 只要开放的
                .ge(VisitSession::getVisitDate, start)             // >= 起始（ge = greater or equal）
                .le(VisitSession::getVisitDate, end)               // <= 截止（le = less or equal）
                .orderByAsc(VisitSession::getVisitDate)            // 日期正序
                .orderByAsc(VisitSession::getTimeSlot);            // 时段正序

        Page<VisitSession> result = sessionMapper.selectPage(p, wrapper);
        return toVoPage(result, false);
    }

    /**
     * 最新可预约场次（首页）
     * 与列表同条件，只是取前 N 条（LIMIT）
     */
    @Override
    public List<SessionListVO> latest(Integer count) {
        int limit = (count == null || count < 1) ? 3 : Math.min(count, 10);
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<VisitSession> wrapper = new LambdaQueryWrapper<VisitSession>()
                .eq(VisitSession::getStatus, STATUS_OPEN)
                .ge(VisitSession::getVisitDate, today)             // 过滤过期场次
                .orderByAsc(VisitSession::getVisitDate)
                .orderByAsc(VisitSession::getTimeSlot)
                .last("LIMIT " + limit);

        return sessionMapper.selectList(wrapper).stream()
                .map(s -> toListVO(s, false)).toList();
    }

    /**
     * 场次详情
     * 注意：详情不做"开放"过滤（管理员/详情页需要看到下架状态），
     * 只查存在性，status 字段交给前端判断
     */
    @Override
    public SessionDetailVO detail(Long id) {
        VisitSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);  // 40401
        }
        return SessionDetailVO.builder()
                .id(session.getId())
                .visitDate(session.getVisitDate())
                .timeSlot(session.getTimeSlot())
                .maxPeople(session.getMaxPeople())
                .usedPeople(session.getUsedPeople())
                .remaining(session.getMaxPeople() - session.getUsedPeople())
                .status(session.getStatus())
                .build();
    }

    /* ==================== 后台 ==================== */

    /**
     * 后台分页：管理员看全部场次（含下架、含过期）
     * 过滤条件全部可选（动态拼接）
     */
    @Override
    public Page<SessionListVO> pageForAdmin(SessionQueryDTO query) {
        Page<VisitSession> p = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<VisitSession> wrapper = new LambdaQueryWrapper<VisitSession>()
                .eq(query.getVisitDate() != null, VisitSession::getVisitDate, query.getVisitDate())
                .eq(query.getStatus() != null, VisitSession::getStatus, query.getStatus())
                .orderByDesc(VisitSession::getVisitDate)   // 后台倒序（最近创建的场次好找）
                .orderByAsc(VisitSession::getTimeSlot);

        Page<VisitSession> result = sessionMapper.selectPage(p, wrapper);
        return toVoPage(result, true);
    }

    /**
     * 新增场次
     * 二次防御：DTO 注解已拦过去日期，这里再拦一次——
     * 防止未来有人绕过 Controller（如直接调 Service）导致脏数据
     */
    @Override
    public Long save(SessionSaveDTO dto) {
        if (dto.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "禁止创建过去时间的参观场次");
        }

        VisitSession session = new VisitSession();
        session.setVisitDate(dto.getVisitDate());
        session.setTimeSlot(dto.getTimeSlot().trim());
        session.setMaxPeople(dto.getMaxPeople());
        session.setUsedPeople(0);        // 新场次已预约人数从 0 开始
        session.setStatus(dto.getStatus());
        session.setVersion(0);           // 乐观锁版本号初始值
        sessionMapper.insert(session);
        log.info("场次已创建: id={}, date={}, slot={}, max={}",
                session.getId(), session.getVisitDate(), session.getTimeSlot(), session.getMaxPeople());
        return session.getId();
    }

    /**
     * 编辑场次（含缩容保护）
     *
     * 场景：场次已预约 20 人，管理员想把最大人数改成 10 → 必须拦截（40022）
     * 否则会出现 usedPeople > maxPeople 的逻辑坏死数据
     *
     * updateById 走乐观锁：实体从 selectById 加载，自带 version，
     * MP 自动生成 UPDATE ... SET version=version+1 WHERE id=? AND version=?
     */
    @Override
    public void update(Long id, SessionSaveDTO dto) {
        VisitSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
        }
        // 缩容保护：已有预约时，新容量不得小于已预约人数
        if (session.getUsedPeople() > 0 && dto.getMaxPeople() < session.getUsedPeople()) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID.getCode(),
                    "该场次已有 " + session.getUsedPeople() + " 人预约，最大容纳人数不能低于该值");
        }

        session.setVisitDate(dto.getVisitDate());
        session.setTimeSlot(dto.getTimeSlot().trim());
        session.setMaxPeople(dto.getMaxPeople());
        session.setStatus(dto.getStatus());
        sessionMapper.updateById(session);
        log.info("场次已编辑: id={}", id);
    }

    /** 上架：只有下架状态可以上架（状态机） */
    @Override
    public void online(Long id) {
        VisitSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
        }
        if (session.getStatus() != STATUS_OFFLINE) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID.getCode(), "场次不是下架状态，无法上架");
        }
        session.setStatus(STATUS_OPEN);
        sessionMapper.updateById(session);
        log.info("场次已上架: id={}", id);
    }

    /** 下架：只有开放状态可以下架（状态机） */
    @Override
    public void offline(Long id) {
        VisitSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
        }
        if (session.getStatus() != STATUS_OPEN) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID.getCode(), "场次不是开放状态，无法下架");
        }
        session.setStatus(STATUS_OFFLINE);
        sessionMapper.updateById(session);
        log.info("场次已下架: id={}", id);
    }

    /**
     * 删除场次（逻辑删除）
     * 删除保护：有人预约时禁止删除——预约表还引用着这个场次，
     * 删了会导致预约记录查不到场次信息（孤儿数据，破坏引用完整性）
     */
    @Override
    public void remove(Long id) {
        VisitSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
        }
        if (session.getUsedPeople() > 0) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID.getCode(),
                    "该场次已有 " + session.getUsedPeople() + " 人预约，禁止删除；如需停止预约请先下架");
        }
        sessionMapper.deleteById(id);   // @TableLogic → UPDATE SET deleted=1
        log.info("场次已删除(逻辑): id={}", id);
    }

    /* ==================== 私有工具方法 ==================== */

    /** Entity → 列表 VO（withStatus=true 时后台接口附带状态字段） */
    private SessionListVO toListVO(VisitSession session, boolean withStatus) {
        SessionListVO vo = SessionListVO.builder()
                .id(session.getId())
                .visitDate(session.getVisitDate())
                .timeSlot(session.getTimeSlot())
                .maxPeople(session.getMaxPeople())
                .usedPeople(session.getUsedPeople())
                .remaining(session.getMaxPeople() - session.getUsedPeople())
                .build();
        return withStatus ? vo.toBuilder().status(session.getStatus()).build() : vo;
    }

    /** Page<Entity> → Page<VO> 泛型转换（保留分页元数据） */
    private Page<SessionListVO> toVoPage(Page<VisitSession> result, boolean withStatus) {
        Page<SessionListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(s -> toListVO(s, withStatus)).toList());
        return voPage;
    }
}
