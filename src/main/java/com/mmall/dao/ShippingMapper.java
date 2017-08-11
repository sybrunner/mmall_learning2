package com.mmall.dao;

import com.mmall.pojo.Shipping;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShippingMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Shipping record); //插入

    int insertSelective(Shipping record);

    Shipping selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Shipping record);

    int updateByPrimaryKey(Shipping record);

    //绑定删除(防止横向越权)   当前用户只能删除属于他自己的收获地址表
    int deleteByShippingIdUserId(@Param("userId")Integer userId, @Param("shippingId") Integer shippingId);

    int updateByShipping(Shipping record);//根据属性更新

    Shipping selectByShippingIdUserId(@Param("userId")Integer userId,@Param("shippingId") Integer shippingId);

    List<Shipping> selectByUserId(@Param("userId")Integer userId);//分页查询
}