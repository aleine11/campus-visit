package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.dto.notice.NoticeQueryDTO;
import com.campus.visit.dto.notice.NoticeSaveDTO;
import com.campus.visit.vo.notice.NoticeDetailVO;
import com.campus.visit.vo.notice.NoticeListVO;

import java.util.List;

/**
 * 校园公告 Service 接口
 *
 * 前台 3 个方法（对标 architecture.md 模块 2，全部只查已发布公告）：
 * listPublished 公告分页列表（访客端）
 * latest 最新 N 条公告（首页用）
 * detail 公告详情（带上一篇/下一篇 ID）
 *
 * 后台 6 个方法（对标 architecture.md 模块 11，管理员专用）：
 * pageForAdmin 分页（支持标题模糊 + 状态过滤）
 * save 新增（存草稿或直接发布）
 * update 编辑
 * publish 发布（草稿 → 已发布）
 * offline 下架（已发布 → 草稿）
 * remove 删除（逻辑删除）
 */
public interface NoticeService {

    /* ============ 前台（公开） ============ */

    /** 公告分页列表：仅已发布，按发布时间倒序 */
    List<NoticeListVO> listPublished(Integer current, Integer size);

    /** 最新 N 条公告（首页展示），count 上限 10 */
    List<NoticeListVO> latest(Integer count);

    /** 公告详情：仅已发布可见，附带上一篇/下一篇 ID */
    NoticeDetailVO detail(Long id);

    /* ============ 后台（管理员） ============ */

    /** 后台分页：标题模糊 + 状态过滤，返回含 status（Page 对象自带总数） */
    Page<NoticeListVO> pageForAdmin(NoticeQueryDTO query);

    /** 新增公告：status=0 存草稿 / status=1 直接发布（记录发布人和发布时间） */
    Long save(NoticeSaveDTO dto);

    /** 编辑公告：更新标题和正文，不影响发布状态 */
    void update(Long id, NoticeSaveDTO dto);

    /** 发布：草稿 → 已发布，写入发布人和发布时间 */
    void publish(Long id);

    /** 下架：已发布 → 草稿（publishTime 按文档要求不重置） */
    void offline(Long id);

    /** 删除：逻辑删除（deleted=1），前台立即不可见 */
    void remove(Long id);
}
