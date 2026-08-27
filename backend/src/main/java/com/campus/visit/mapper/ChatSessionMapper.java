package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
