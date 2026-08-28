package com.campus.visit.dto.notice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增/编辑请求参数
 *
 * 对标 architecture.md 11.2 新增公告 / 11.3 编辑公告（两者参数相同，共用一个 DTO）：
 *   title    1~100 字
 *   content  1~10000 字
 *   status   0=保存草稿，1=保存并发布
 */
@Data
public class NoticeSaveDTO {

    /** 公告标题 */
    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度须在1~100字之间")
    private String title;

    /** 公告正文 */
    @NotBlank(message = "正文不能为空")
    @Size(min = 1, max = 10000, message = "正文长度须在1~10000字之间")
    private String content;

    /** 保存方式：0=存草稿，1=直接发布 */
    @NotNull(message = "保存方式不能为空")
    @Min(value = 0, message = "status 只能为 0（草稿）或 1（发布）")
    @Max(value = 1, message = "status 只能为 0（草稿）或 1（发布）")
    private Integer status;
}
