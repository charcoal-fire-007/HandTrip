package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (!CollectionUtils.isEmpty(flavors)) {
            flavors.forEach(f -> f.setDishId(dishId));
            dishFlavorMapper.insert(flavors);
        }

        // 事务提交后再清缓存，避免脏写
        evictCache(dish.getCategoryId());
    }

    // 事务外部，单独清缓存
    @CacheEvict(cacheNames = "Dish", key = "#categoryId")
    public void evictCache(Long categoryId) {
    }

    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {

        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.select(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    @CacheEvict(cacheNames = "Dish", allEntries = true)
    public void delete(List<String> ids) {

        List<Integer> status = dishMapper.statusById(ids);
        for (Integer statusId : status) {
            if (Objects.equals(statusId, StatusConstant.ENABLE)) {
                //状态
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<Long> dishids = setmealDishMapper.selectSetmealDishIds(ids);
        if (dishids != null && !dishids.isEmpty() ) {
            //套餐
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        dishFlavorMapper.delete(ids);
        dishMapper.delete(ids);

    }

    @Override
    public DishVO getById(Long id) {
        DishVO dish = dishMapper.selectById(id);
        List<DishFlavor> list = dishFlavorMapper.selectById(id);
        log.info("回显数据口味信息：{}", list);
        dish.setFlavors(list);
        return dish;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "Dish", allEntries = true)
    public void updateDish(DishDTO dishDTO) {
        Dish dish = Dish.builder().build();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        dishFlavorMapper.deleteById(dish.getId());

        List<DishFlavor> dishFlavors = dishDTO.getFlavors();

        if (!CollectionUtils.isEmpty(dishFlavors)) {
            dishFlavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insert(dishFlavors);
        }
    }

    @Override
    @CacheEvict(cacheNames = "Dish", allEntries = true)
    public void updateStatus(Integer status, Long id) {
        Dish dish = Dish.builder()
            .id(id)
            .status(status)
            .build();
        dishMapper.update(dish);
    }
    @Override
    @Cacheable(cacheNames = "Dish" ,key = "#dish.categoryId")
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectById(d.getId());
            log.info("微信查询页面查询到的口味数据{}", flavors);
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    @Override
    public List<Dish> getListById(Long id) {
        return dishMapper.selectList(id);
    }

}