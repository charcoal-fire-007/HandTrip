package com.sky.controller.admin;


import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/category")
@Tag(name = "分类管理接口",description = "菜品分类相关接口")
public class CategoryController {

    @Autowired
    CategoryService categoryService;
    /**
     * 分类分页后查询
     * @Return 分页查询结果
     */
    @GetMapping("/page")
    @Operation(summary = "分类分页查询",description = "分类分页查询接口")
    public Result<PageResult> queryCategory(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分类信息{}",categoryPageQueryDTO);
        PageResult pageResult = categoryService.page(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 新增分类信息
     */
    @PostMapping
    @Operation(summary = "新增分类",description = "新增分类接口")
    public Result addCategory(@RequestBody CategoryDTO categoryDTO) {

        categoryService.accCategory(categoryDTO);
        return Result.success();
    }

    /**
     * 状态调整
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "分类状态调整", description = "分类状态调整接口")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        categoryService.updateStatus(status,id);
        return Result.success();
    }

    /**
     * 更新分类数据
     */
    @PutMapping
    @Operation(summary = "编辑分类数据",description = "编辑分类数据接口")
    public Result updateCategory(@RequestBody CategoryDTO categoryDTO) {
        categoryService.update(categoryDTO);
        return Result.success();
    }
    /**
     * 删除分类信息
     * @Return
     */
    @DeleteMapping
    @Operation(summary = "删除分类信息",description = "删除分类信息接口")
    public Result deleteCategory(@RequestParam("id") Long  id) {
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根据类型查询分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}
