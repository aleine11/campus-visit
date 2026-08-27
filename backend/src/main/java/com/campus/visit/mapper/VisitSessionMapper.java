package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.VisitSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参观场次 Mapper
 * 注意：entity 上有 @TableLogic + @Version
 * - @TableLogic：MP 自动过滤已删除记录
 * - @Version：updateById 时自动带上 version 条件，乐观锁防超卖
 */
@Mapper
public interface VisitSessionMapper extends BaseMapper<VisitSession> {
}
