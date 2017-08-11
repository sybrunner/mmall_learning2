package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.vo.CartVo;

/**
 * Created by apple
 */
public interface ICartService {
    ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count);//添加购物车

    ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count);//更新购物车

    ServerResponse<CartVo> deleteProduct(Integer userId, String productIds);//删除购物车

    ServerResponse<CartVo> list(Integer userId); //查询

    ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer productId, Integer checked);//全选 全反选

    ServerResponse<Integer> getCartProductCount(Integer userId);////查询当前用户的购物车里面的产品数量,如果一个产品有10个,那么数量就是10.
}
