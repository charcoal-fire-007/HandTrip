package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.impl.DishServiceImpl;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Tag(name = "菜品接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @Operation(summary = "新增菜品",description = "新增菜品接口")
    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("前端传入过来的菜品参数信息{}", dishDTO.toString());
        dishService.save(dishDTO);
        return Result.success();
    }

    @Operation(summary = "分页查询菜品",description = "分页查询菜品接口")
    @GetMapping("page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("前端传入的查询参数{}", dishPageQueryDTO.toString());
        PageResult pageResult =  dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @Operation(summary = "删除菜品",description = "根据id删除菜品信息")
    @DeleteMapping
    public Result delete(@RequestParam List<String> ids) {
        dishService.delete(ids);
        return Result.success();
    }

    @Operation(summary = "回显菜品信息",description = "根据id编辑回显菜品信息")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("接收到的回显查询id{}",id);
        DishVO dish  = dishService.getById(id);
        return Result.success(dish);
    }

    @Operation(summary = "编辑菜品信息",description = "根据id编辑菜品信息")
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("编辑菜品时前端传入的菜品信息{}",dishDTO.toString());
        dishService.updateDish(dishDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result postMethodName(@PathVariable Integer status,Long id) {
        log.info("前端传入的状态值{},菜品id{}",status,id);
        dishService.updateStatus(status,id);
        return Result.success();
    }
    
    
}
