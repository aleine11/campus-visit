package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.visit.entity.VisitReservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约订单 Mapper
 */
@Mapper
public interface VisitReservationMapper extends BaseMapper<VisitReservation> {
}
