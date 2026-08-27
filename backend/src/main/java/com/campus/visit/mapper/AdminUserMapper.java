package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员账号 Mapper
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
