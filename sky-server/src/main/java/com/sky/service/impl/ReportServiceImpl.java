package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.spec.XECPublicKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        // 1. 日期区间
        List<LocalDate> dateList = begin.datesUntil(end.plusDays(1))
                .collect(Collectors.toList());

        // 2. 查询每日有效订单数（status = COMPLETED）
        List<Map<String, Object>> validRows = reportMapper.orderGroupByDay(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX),
                Orders.COMPLETED);

        // 查询每日总订单数
        List<Map<String, Object>> totalRows = reportMapper.orderTotalGroupByDay(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));

        // 3. 转成 Map<LocalDate, Integer>
        Map<LocalDate, Integer> validMap = validRows.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row.get("order_date")).toLocalDate(),
                        row -> ((Long) row.get("order_count")).intValue()));

        Map<LocalDate, Integer> totalMap = totalRows.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row.get("order_date")).toLocalDate(),
                        row -> ((Long) row.get("order_total")).intValue()));

        // 4. 日维度列表
        List<Integer> validOrderCountList = dateList.stream()
                .map(d -> validMap.getOrDefault(d, 0))
                .toList();
        List<Integer> orderCountList = dateList.stream()
                .map(d -> totalMap.getOrDefault(d, 0))
                .toList();

        // 5. 汇总指标
        Integer validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
        Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0
                : validOrderCount.doubleValue() / totalOrderCount;

        // 6. 组装 VO
        return OrderReportVO.builder()
                .dateList(dateList.stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.joining(",")))
                .orderCountList(orderCountList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .validOrderCountList(validOrderCountList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        
        List<GoodsSalesDTO> salesDTOS = reportMapper.getSalesTop10(LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));
        List<String> nameList = salesDTOS.stream()
                .map(GoodsSalesDTO::getName)
                .toList();
        List<Integer> countList = salesDTOS.stream()
                .map(GoodsSalesDTO::getNumber)
                .toList();
        return SalesTop10ReportVO.builder()
                .nameList(String.join(",", nameList))
                .numberList(countList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .build();
        
        
        

    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN),LocalDateTime.of(end, LocalTime.MAX));
        InputStream in =  this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try{
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            sheet1.getRow(1).getCell(1).setCellValue("时间：" +begin+"至"+end);

            XSSFRow row3 = sheet1.getRow(3);
            row3.getCell(2).setCellValue(businessDataVO.getTurnover());
            row3.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row3.getCell(6).setCellValue(businessDataVO.getNewUsers());

            XSSFRow row5 = sheet1.getRow(5);
            row5.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row5.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                //查询某一天的营业数据
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN),LocalDateTime.of(date, LocalTime.MAX));
                XSSFRow row =  sheet1.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            ServletOutputStream servletOutputStream = response.getOutputStream();
            excel.write(servletOutputStream);

            servletOutputStream.close();
            excel.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
