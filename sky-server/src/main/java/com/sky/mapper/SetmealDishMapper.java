package com.sky.mapper;


import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    @Select("SELECT dish_id from setmeal_dish WHERE dish_id = #{id}")
    Long selectMealDishById(Long id);
}
