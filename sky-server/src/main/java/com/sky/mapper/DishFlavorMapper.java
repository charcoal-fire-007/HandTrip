package com.sky.mapper;

import com.sky.entity.DishFlavor;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    void insert(List<DishFlavor> dishFlavors);

    void delete(List<String> ids);

    @Select("select * from dish_flavor where dish_id = #{id}")
    List<DishFlavor> selectById(Long id);

    @Delete("delete from dish_flavor where dish_id = #{id}")
    void deleteById(Long id);
}
