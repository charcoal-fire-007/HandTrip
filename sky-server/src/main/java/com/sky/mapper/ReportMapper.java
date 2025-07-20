package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    Double subByMap(Map<Object, Object> objectObjectHashMap);

    List<Map<String, Object>> sumAmountGroupByDay(
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end,
            @Param("status") Integer status
    );

    // 每天新增用户
    List<Map<String, Object>> newUserGroupByDay(
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end
    );

    // 每天累计用户（历史上）
    List<Map<String, Object>> totalUserGroupByDay(
            @Param("end") LocalDateTime end
    );
}
