package com.mmall.dao;

import com.mmall.pojo.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);//非空更新

    int updateByPrimaryKey(Order record);

    //根据订单号和用户ID 查询订单是否存在
    Order selectByUserIdAndOrderNo(@Param("userId")Integer userId, @Param("orderNo")Long orderNo);

    Order selectByOrderNo(Long orderNo); //根据订单号 查询订单是否存在

    List<Order> selectByUserId(Integer userId);//根据 userID 查询订单

    List<Order> selectAllOrder();//后台的查询
}