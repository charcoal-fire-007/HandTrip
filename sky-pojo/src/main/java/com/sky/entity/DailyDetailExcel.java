package com.sky.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyDetailExcel {

    @ExcelProperty("日期")
    private String date;

    @ExcelProperty("营业额")
    @NumberFormat("#,##0.00")
    private BigDecimal turnover;

    @ExcelProperty("有效订单数")
    private Integer validOrderCount;

    @ExcelProperty("订单完成率")
    @NumberFormat("0.00%")
    private Double orderCompletionRate;

    @ExcelProperty("客单价")
    @NumberFormat("#,##0.00")
    private BigDecimal unitPrice;

    @ExcelProperty("新增用户数")
    private Integer newUsers;
}