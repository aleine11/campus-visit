package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.VisitorUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访客用户 Mapper
 * 继承 BaseMapper 即获得全部 CRUD 能力：
 *   selectById / selectOne / selectList / selectPage
 *   insert / updateById / deleteById / deleteBatchIds 等
 */
@Mapper
public interface VisitorUserMapper extends BaseMapper<VisitorUser> {
}
