package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Shipping;

/**
 * Created by apple
 */
public interface IShippingService {

    ServerResponse add(Integer userId, Shipping shipping);//添加

    ServerResponse<String> del(Integer userId, Integer shippingId);//删除

    ServerResponse update(Integer userId, Shipping shipping);//更新

    ServerResponse<Shipping> select(Integer userId, Integer shippingId);//查询

    ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize);//分页 查询当前用户所有的收获地址

}
