package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);


    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);


    void deleteWithFlavor(List<Long> ids);

    void updateWithFlavor(DishDTO dishDTO);

    DishVO selectDishById(Long id);

    void updateStatusById(int status, Long id);


    List<Dish> selectDishByCategoryId(Long categoryId);


    List<DishVO> listWithFlavor(Dish dish);

}
