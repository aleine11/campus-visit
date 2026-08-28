package com.campus.visit.vo.notice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告列表条目 VO（前台列表 / 最新公告 / 后台分页 三处共用）
 *
 * 对标 architecture.md 2.1 公告分页列表响应：
 * id 公告 ID
 * title 标题
 * publishTime 发布时间
 * summary 摘要（正文前 80 字，列表页不用加载全文，省流量提速度）
 *
 * 后台分页（11.1）额外需要 status 字段 → 复用本类的 status 字段
 * （前台永远是已发布数据，status 恒为 1，前台构建时不填这个字段即可）
 */
@Data
@Builder(toBuilder = true)
public class NoticeListVO {

    /** 公告 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 摘要（正文前 80 字） */
    private String summary;

    /** 发布状态（仅后台分页接口填：0=草稿 1=已发布） */
    private Integer status;
}
