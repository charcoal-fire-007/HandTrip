package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SetmealServiceImpl  implements SetmealService {


    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.select(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = Setmeal.builder().build();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        try{
            setmealMapper.insert(setmeal);
        }catch (Exception e){}

        Long id = setmeal.getId();
        if (!setmealDTO.getSetmealDishes().isEmpty()) {
            setmealDTO.getSetmealDishes().forEach(setmealDish -> setmealDish.setSetmealId(id));
        }

        setmealDishMapper.insert(setmealDTO.getSetmealDishes());
    }
}
