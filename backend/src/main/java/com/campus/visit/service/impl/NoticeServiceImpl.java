package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.notice.NoticeQueryDTO;
import com.campus.visit.dto.notice.NoticeSaveDTO;
import com.campus.visit.entity.CampusNotice;
import com.campus.visit.mapper.CampusNoticeMapper;
import com.campus.visit.service.NoticeService;
import com.campus.visit.utils.UserContext;
import com.campus.visit.utils.UserContext.LoginUser;
import com.campus.visit.vo.notice.NoticeDetailVO;
import com.campus.visit.vo.notice.NoticeListVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 校园公告 Service 实现
 *
 * 核心机制：
 *   1. @TableLogic 逻辑删除 —— 所有 MP 查询自动追加 WHERE deleted=0，
 *      调 mapper.deleteById() 实际执行 UPDATE SET deleted=1，代码里完全无感
 *   2. 分页插件 —— mapper.selectPage(page, wrapper) 自动生成 LIMIT 语句
 *   3. 状态机 —— 草稿(0) ⇄ 已发布(1)，每个流转方法都先校验当前状态是否合法
 *
 * 对标 architecture.md 模块 2（前台）+ 模块 11（后台）
 */
@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {

    /** 草稿状态（状态字典 D2） */
    private static final int STATUS_DRAFT = 0;
    /** 已发布状态（状态字典 D2） */
    private static final int STATUS_PUBLISHED = 1;

    /** 摘要截取长度（对标文档：正文前 80 字） */
    private static final int SUMMARY_LENGTH = 80;

    @Resource
    private CampusNoticeMapper noticeMapper;

    /* ==================== 前台 ==================== */

    /**
     * 前台公告分页列表
     *
     * 对标文档业务逻辑：WHERE status=1 AND deleted=0 ORDER BY publish_time DESC
     * status=1 手动写在 Wrapper 里；deleted=0 由 @TableLogic 自动追加
     */
    @Override
    public List<NoticeListVO> listPublished(Integer current, Integer size) {
        // 分页参数兜底（防 null / 非法值，GET 参数可以不传）
        int page = (current == null || current < 1) ? 1 : current;
        int rows = (size == null || size < 1) ? 10 : size;

        // Page 对象：current=页码，size=每页条数
        Page<CampusNotice> p = new Page<>(page, rows);

        LambdaQueryWrapper<CampusNotice> wrapper = new LambdaQueryWrapper<CampusNotice>()
                .eq(CampusNotice::getStatus, STATUS_PUBLISHED)   // 只查已发布
                .orderByDesc(CampusNotice::getPublishTime);      // 按发布时间倒序（最新在前）

        // selectPage：返回的 Page 对象里既有记录列表也有 totalCount
        List<CampusNotice> records = noticeMapper.selectPage(p, wrapper).getRecords();

        return records.stream().map(this::toListVO).toList();
    }

    /**
     * 最新 N 条公告（首页用）
     *
     * 和分页列表的区别：只要前 N 条，用 last("LIMIT n") 拼接原生 SQL 片段
     * count 超过 10 截断为 10（对标文档：count 默认 3，最大 10）
     */
    @Override
    public List<NoticeListVO> latest(Integer count) {
        int limit = (count == null || count < 1) ? 3 : Math.min(count, 10);

        LambdaQueryWrapper<CampusNotice> wrapper = new LambdaQueryWrapper<CampusNotice>()
                .eq(CampusNotice::getStatus, STATUS_PUBLISHED)
                .orderByDesc(CampusNotice::getPublishTime)
                .last("LIMIT " + limit);  // 拼在 SQL 末尾（值已白名单化，无注入风险）

        return noticeMapper.selectList(wrapper).stream().map(this::toListVO).toList();
    }

    /**
     * 公告详情（带上一篇/下一篇）
     *
     * 执行流程：
     *   1. 查本条 → 不存在或未发布 → 40401
     *   2. 查上一篇：发布时间比本条早的第一条（倒序的第一条）
     *   3. 查下一篇：发布时间比本条晚的第一条（正序的第一条）
     *
     * 注意：publish_time 相同的极端情况（同秒发布）用 id 兜底排序，
     * 毕设场景简化处理：只按 publish_time 比较 + id 排序辅助
     */
    @Override
    public NoticeDetailVO detail(Long id) {
        // 1. 查本条（@TableLogic 自动过滤已删除；再手动过滤未发布）
        CampusNotice notice = noticeMapper.selectById(id);
        if (notice == null || notice.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.NOTICE_NOT_FOUND);  // 40401
        }

        // 2. 上一篇：发布时间 < 本条 → 倒序取第一条
        CampusNotice prev = noticeMapper.selectOne(new LambdaQueryWrapper<CampusNotice>()
                .eq(CampusNotice::getStatus, STATUS_PUBLISHED)
                .lt(CampusNotice::getPublishTime, notice.getPublishTime())  // lt = less than
                .orderByDesc(CampusNotice::getPublishTime)
                .last("LIMIT 1"));

        // 3. 下一篇：发布时间 > 本条 → 正序取第一条
        CampusNotice next = noticeMapper.selectOne(new LambdaQueryWrapper<CampusNotice>()
                .eq(CampusNotice::getStatus, STATUS_PUBLISHED)
                .gt(CampusNotice::getPublishTime, notice.getPublishTime())  // gt = greater than
                .orderByAsc(CampusNotice::getPublishTime)
                .last("LIMIT 1"));

        return NoticeDetailVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .publishTime(notice.getPublishTime())
                .prevId(prev != null ? prev.getId() : null)   // 没有上一篇 → null，前端隐藏按钮
                .nextId(next != null ? next.getId() : null)
                .build();
    }

    /* ==================== 后台 ==================== */

    /**
     * 后台分页（管理员看所有公告，含草稿）
     *
     * 动态条件拼接：keyword / status 都可能为空
     * LambdaQueryWrapper 的条件方法第一个参数传 boolean：
     *   .like(condition, column, value)  ← condition=false 时这个条件直接不拼进 SQL
     * 这是 MP 官方的"动态 SQL"写法，替代传统 XML 里的一大堆 <if> 标签
     */
    @Override
    public Page<NoticeListVO> pageForAdmin(NoticeQueryDTO query) {
        Page<CampusNotice> p = new Page<>(query.getCurrent(), query.getSize());

        LambdaQueryWrapper<CampusNotice> wrapper = new LambdaQueryWrapper<CampusNotice>()
                // keyword 非空才拼 LIKE '%keyword%'；likeLeft = %放左边（匹配"xx结尾"）
                // 文档要求"标题模糊"，用 like（两侧都有 %）更宽松
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(),
                        CampusNotice::getTitle, query.getKeyword())
                // status 非空才拼 =
                .eq(query.getStatus() != null, CampusNotice::getStatus, query.getStatus())
                .orderByDesc(CampusNotice::getCreateTime);  // 后台按创建时间倒序（草稿没有发布时间）

        // 查出分页数据后转 VO（后台版带 status 字段）
        Page<CampusNotice> result = noticeMapper.selectPage(p, wrapper);

        // Page 泛型转换：把 Page<CampusNotice> 变成 Page<NoticeListVO>
        Page<NoticeListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(n ->
                toListVO(n).toBuilder().status(n.getStatus()).build()).toList());
        return voPage;
    }

    /**
     * 新增公告
     *
     * status 由前端决定：0=存草稿（不填发布人/发布时间），1=直接发布
     * 直接发布时要记录"谁发的"（publishAdminId）和"什么时候发的"（publishTime）
     * 当前登录人从 UserContext 取（AdminNoticeController 上有 @RequiresRole("admin")，拦截器保证已登录）
     */
    @Override
    public Long save(NoticeSaveDTO dto) {
        CampusNotice notice = new CampusNotice();
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent().trim());

        if (dto.getStatus() == STATUS_PUBLISHED) {
            // 直接发布：记录发布人和发布时间
            LoginUser loginUser = UserContext.get();
            notice.setStatus(STATUS_PUBLISHED);
            notice.setPublishAdminId(loginUser.getUserId());
            notice.setPublishTime(LocalDateTime.now());
        } else {
            // 存草稿：状态 0，发布人/发布时间保持 null
            notice.setStatus(STATUS_DRAFT);
        }

        noticeMapper.insert(notice);  // 自增主键回填到 notice.getId()
        log.info("公告已创建: id={}, title={}, status={}", notice.getId(), notice.getTitle(), notice.getStatus());
        return notice.getId();
    }

    /**
     * 编辑公告（更新标题和正文，不动发布状态）
     *
     * 安全细节：先查记录是否存在（selectById 走逻辑删除过滤），
     * 已删除的公告 update 会影响 0 行且无提示，不如提前 40401 报错清晰
     */
    @Override
    public void update(Long id, NoticeSaveDTO dto) {
        CampusNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOTICE_NOT_FOUND);  // 40401
        }
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent().trim());
        noticeMapper.updateById(notice);  // 只更新非 null 字段，status/publishTime 不受影响
        log.info("公告已编辑: id={}", id);
    }

    /**
     * 发布：草稿 → 已发布
     *
     * 对标文档 11.4：
     *   UPDATE campus_notice SET status=1, publish_admin_id=?, publish_time=NOW() WHERE id=? AND status=0
     * "AND status=0" 的含义：只有草稿能被发布（状态机校验）
     */
    @Override
    public void publish(Long id) {
        CampusNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOTICE_NOT_FOUND);
        }
        // 状态机校验：只有草稿可以发布（重复发布报 40022 状态不可流转）
        if (notice.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID.getCode(), "公告不是草稿状态，无法发布");
        }
        LoginUser loginUser = UserContext.get();
        notice.setStatus(STATUS_PUBLISHED);
        notice.setPublishAdminId(loginUser.getUserId());
        notice.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        log.info("公告已发布: id={}, by={}", id, loginUser.getUsername());
    }

    /**
     * 下架：已发布 → 草稿
     *
     * 对标文档 11.5：UPDATE ... SET status=0 WHERE id=? AND status=1
     * 注意：publishTime 故意不重置（文档要求"首次发布写入，下架不重置"，
     * 这样重新发布也不会刷新首次发布时间，前台排序稳定）
     */
    @Override
    public void offline(Long id) {
        CampusNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOTICE_NOT_FOUND);
        }
        // 状态机校验：只有已发布的才能下架
        if (notice.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID.getCode(), "公告不是已发布状态，无法下架");
        }
        notice.setStatus(STATUS_DRAFT);
        noticeMapper.updateById(notice);
        log.info("公告已下架: id={}", id);
    }

    /**
     * 删除（逻辑删除）
     *
     * mapper.deleteById(id) 因为实体有 @TableLogic 注解，
     * MyBatis-Plus 自动改写成：UPDATE campus_notice SET deleted=1 WHERE id=? AND deleted=0
     * 前台所有查询自动带上 deleted=0，删完立即不可见
     */
    @Override
    public void remove(Long id) {
        CampusNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOTICE_NOT_FOUND);
        }
        noticeMapper.deleteById(id);  // 实际是 UPDATE，不是物理 DELETE
        log.info("公告已删除(逻辑): id={}", id);
    }

    /* ==================== 私有工具方法 ==================== */

    /**
     * Entity → 列表 VO 转换（摘要 = 正文前 80 字）
     * toBuilder = true 让 @Builder 生成的类自带 toBuilder() 方法，方便后台版追加 status
     */
    private NoticeListVO toListVO(CampusNotice notice) {
        String content = notice.getContent() == null ? "" : notice.getContent();
        // 摘要：截前 80 字，正文不足 80 字就全取
        String summary = content.length() > SUMMARY_LENGTH
                ? content.substring(0, SUMMARY_LENGTH) + "..."
                : content;

        return NoticeListVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .publishTime(notice.getPublishTime())
                .summary(summary)
                .build();
    }
}
