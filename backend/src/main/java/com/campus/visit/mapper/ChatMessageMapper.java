package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 问答消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
