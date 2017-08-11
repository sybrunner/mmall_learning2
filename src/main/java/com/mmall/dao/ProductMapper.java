package com.mmall.dao;

import com.mmall.pojo.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Product record); //新增商品 (传入的是Prodict的字段)

    int insertSelective(Product record);

    Product selectByPrimaryKey(Integer id);//通过ID 查询产品

    int updateByPrimaryKeySelective(Product record);// 通过ID  更新产品(不为空更新)

    int updateByPrimaryKey(Product record);//更新产品 (全部更新)

    List<Product> selectList();  //所有商品信息

    //通过名字或者ID 查询
    List<Product> selectByNameAndProductId(@Param("productName")String productName, @Param("productId") Integer productId);

    //通过名字和 分类  查询
    List<Product> selectByNameAndCategoryIds(@Param("productName")String productName,@Param("categoryIdList")List<Integer> categoryIdList);


}