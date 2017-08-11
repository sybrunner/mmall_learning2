package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.vo.OrderVo;


import java.util.Map;

/**
 * Created by apple
 */
public interface IOrderService {
    ServerResponse pay(Long orderNo, Integer userId, String path); //支付接口
    ServerResponse aliCallback(Map<String, String> params);        //查询支付信息接口
    ServerResponse queryOrderPayStatus(Integer userId, Long orderNo); //支付宝调用的 回调接口(回调支付成功的信息)
    ServerResponse checkData(Map<String,String> params);   //检查参数



    ServerResponse createOrder(Integer userId, Integer shippingId);//创建订单
    ServerResponse<String> cancel(Integer userId, Long orderNo); //取消订单
    ServerResponse getOrderCartProduct(Integer userId);          //取得购物车已经选中的产品
    ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo); //获取订单详情
    ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize); //查看订单



    //backend
    ServerResponse<PageInfo> manageList(int pageNum, int pageSize); //后台订单查询
    ServerResponse<OrderVo> manageDetail(Long orderNo);  //后台查看详情
    ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize); //后台按订单搜索
    ServerResponse<String> manageSendGoods(Long orderNo); //后台 发货


}
