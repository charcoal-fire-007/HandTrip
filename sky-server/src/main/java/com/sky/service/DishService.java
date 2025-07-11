package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService  {

    void save(DishDTO dish);

    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    void delete(List<String> ids);

    DishVO getById(Long id);

    void updateDish(DishDTO dishDTO);

    void updateStatus(Integer status, Long id);
}
