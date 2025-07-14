package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/admin/setmeal")
@Tag(name = "套餐管理页面",description = "套餐管理页面接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;


    @GetMapping("/page")
    @Operation(summary = "套餐分页查询接口")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping
    @Operation(summary = "新增套餐接口")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("前端传过来的套餐数据：{}",setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询套餐",description = "根据套餐id查询套餐和菜品的关联关系")
    public Result<SetmealVO> list(@PathVariable Long id) {
        return Result.success(setmealService.getById(id));
    }
}
