package com.campus.visit.dto.notice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 公告后台分页查询参数
 *
 * 对标 architecture.md 11.1 公告分页（管理员）：
 *   keyword  标题模糊搜索（可空）
 *   status   0=草稿 1=已发布（可空 = 查全部）
 *   current  页码，默认 1
 *   size     每页条数，默认 10，最大 50（防止一次查太多拖慢数据库）
 *
 * 注意：这是 GET 请求的 Query 参数（?keyword=x&current=1），
 * Spring 自动按字段名绑定，不需要 @RequestBody
 */
@Data
public class NoticeQueryDTO {

    /** 标题模糊搜索关键字（可空） */
    private String keyword;

    /** 发布状态过滤（可空 = 全部） */
    @Min(value = 0, message = "status 只能为 0 或 1")
    @Max(value = 1, message = "status 只能为 0 或 1")
    private Integer status;

    /** 页码（默认 1） */
    @Min(value = 1, message = "页码最小为 1")
    private Integer current = 1;

    /** 每页条数（默认 10，最大 50） */
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 50, message = "每页条数最大为 50")
    private Integer size = 10;
}
