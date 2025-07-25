package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    List<Long> selectSetmealDishIds(List<String> ids);

//    @AutoFill(value = OperationType.INSERT)
    void insert(List<SetmealDish> setmealDishes);


    List<SetmealDish> getById(Long id);

    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void deleteBySetmealId(Long id);
}
