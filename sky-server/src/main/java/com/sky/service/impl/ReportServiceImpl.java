package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1. 生成日期列表
        List<LocalDate> dateList = begin.datesUntil(end.plusDays(1))
                .collect(Collectors.toList());

        // 2. 一次性查询区间内所有营业额（按天聚合）
        List<Map<String, Object>> rows = reportMapper.sumAmountGroupByDay(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX),
                Orders.COMPLETED
        );
        // 3. 转成 Map<日期, 金额>
        Map<LocalDate, Double> turnoverMap = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row.get("order_date")).toLocalDate(),
                        row -> Optional.ofNullable((BigDecimal) row.get("total_amount"))
                                .map(BigDecimal::doubleValue)
                                .orElse(0.0)
                ));

        // 4. 补齐没有订单的日期
        List<Double> turnoverList = dateList.stream()
                .map(d -> turnoverMap.getOrDefault(d, 0.0))
                .collect(Collectors.toList());

        // 5. 返回 VO
        return TurnoverReportVO.builder()
                .dateList(dateList.stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.joining(",")))
                .turnoverList(turnoverList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1. 日期列表
        List<LocalDate> dateList = begin.datesUntil(end.plusDays(1))
                .collect(Collectors.toList());

        // 2. 每天新增用户
        List<Map<String, Object>> newUserRows = reportMapper.newUserGroupByDay(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX)
        );
        Map<LocalDate, Integer> newUserMap = newUserRows.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row.get("order_date")).toLocalDate(),
                        row -> ((Long) row.get("new_user")).intValue()
                ));

        // 3. 截止到每一天的累计用户（先查出历史上所有注册日期）
        List<Map<String, Object>> allUserRows = reportMapper.totalUserGroupByDay(
                LocalDateTime.of(end, LocalTime.MAX)
        );
        // 转成 Map<日期, 当日注册人数>
        Map<LocalDate, Integer> registerOnDay = allUserRows.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row.get("order_date")).toLocalDate(),
                        row -> ((Long) row.get("total_user")).intValue()
                ));

        // 4. 计算累计用户
        int cumulative = 0;
        Map<LocalDate, Integer> totalUserMap = new LinkedHashMap<>();
        for (LocalDate d : dateList) {
            cumulative += registerOnDay.getOrDefault(d, 0);
            totalUserMap.put(d, cumulative);
        }

        // 5. 组装结果
        List<Integer> totalUserList = dateList.stream()
                .map(totalUserMap::get)
                .collect(Collectors.toList());
        List<Integer> newUserList = dateList.stream()
                .map(d -> newUserMap.getOrDefault(d, 0))
                .collect(Collectors.toList());

        return UserReportVO.builder()
                .dateList(dateList.stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.joining(",")))
                .totalUserList(totalUserList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .newUserList(newUserList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .build();
    }
}
