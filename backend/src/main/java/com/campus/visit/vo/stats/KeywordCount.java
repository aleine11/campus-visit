package com.campus.visit.vo.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关键词计数（对标 architecture.md 10.3 KeywordCount 结构）
 * 高频问题统计的最小单元：一个词 + 它出现的次数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordCount {

    /** 关键词（简化分词：单个汉字） */
    private String keyword;

    /** 出现次数 */
    private Integer count;
}
