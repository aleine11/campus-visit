package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.CampusNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校园公告 Mapper
 * 注意：entity 上有 @TableLogic，MP 自动过滤 deleted=1 的记录
 */
@Mapper
public interface CampusNoticeMapper extends BaseMapper<CampusNotice> {
}
