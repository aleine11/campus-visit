package com.campus.visit.vo.notice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告详情 VO
 *
 * 对标 architecture.md 2.3 公告详情响应：
 *   id           公告 ID
 *   title        标题
 *   content      正文全文
 *   publishTime  发布时间
 *   prevId       上一条公告 ID（详情页"上一篇"跳转用，无则 null）
 *   nextId       下一条公告 ID（详情页"下一篇"跳转用，无则 null）
 *
 * prev/next 只在已发布范围内计算，草稿和已删除不参与
 */
@Data
@Builder
public class NoticeDetailVO {

    /** 公告 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 正文全文 */
    private String content;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 上一条公告 ID（无则 null） */
    private Long prevId;

    /** 下一条公告 ID（无则 null） */
    private Long nextId;
}
