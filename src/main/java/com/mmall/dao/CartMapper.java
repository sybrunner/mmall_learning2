package com.mmall.dao;

import com.mmall.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);//非空更新

    int updateByPrimaryKey(Cart record);//更新购物车 (全更)

    //根据 两个ID 查询
    Cart selectCartByUserIdProductId(@Param("userId") Integer userId, @Param("productId")Integer productId);

    List<Cart> selectCartByUserId(Integer userId); //通过id 查找购物车对象

    int selectCartProductCheckedStatusByUserId(Integer userId); //判断全选(查表里面有没有没有勾选的)

    //删除  (指定某个用户)  (购物车里要删除的产品ID集合)
    int deleteByUserIdProductIds(@Param("userId") Integer userId,@Param("productIdList")List<String> productIdList);

    //全选 全反选   单选  单反选
    int checkedOrUncheckedProduct(@Param("userId") Integer userId,@Param("productId")Integer productId,@Param("checked") Integer checked);

    int selectCartProductCount(@Param("userId") Integer userId);

    List<Cart> selectCheckedCartByUserId(Integer userId);//从购物车中获取已经被勾选的商品
}