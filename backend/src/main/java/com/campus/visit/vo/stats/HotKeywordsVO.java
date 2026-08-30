package com.campus.visit.vo.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 高频问题统计响应 VO（对标 architecture.md 10.3 响应结构）
 *
 * 同一次统计给两份结果：
 *   topKeywords —— Top 10，后台看板展示"最近大家在问什么"
 *   wordCloud   —— Top 100，前端词云图（ECharts wordCloud）数据源
 * 两者都按 count 降序。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotKeywordsVO {

    /** Top 10 高频关键词 */
    private List<KeywordCount> topKeywords;

    /** 词云数据（Top 100 关键词） */
    private List<KeywordCount> wordCloud;
}
