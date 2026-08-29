package com.campus.visit.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器（对标 architecture.md 8.2：TextChunker 500 字/100 重叠）
 *
 * 滑动窗口切分：
 *   chunk-size = 500   每块最多 500 字（BGE 模型单次输入上限 512 token，留安全余量）
 *   chunk-overlap = 100 相邻块重叠 100 字——防止关键句恰好被切成两半导致语义断裂
 *
 * 例：原文 1200 字 → 块1[0,500) 块2[400,900) 块3[800,1200) 共 3 块
 */
public final class TextChunker {

    private TextChunker() {
    }

    /**
     * @param text      原始全文
     * @param size      块大小（字）
     * @param overlap   相邻块重叠（字），必须小于 size
     */
    public static List<String> chunk(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        int step = Math.max(1, size - overlap);
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + size);
            chunks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }
}
