package com.mmall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.OrderMapper;
import com.mmall.pojo.Order;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by apple on 17/7/29.
 */

@Controller
@RequestMapping("/order/")
public class OrderController {

    private static  final Logger logger = LoggerFactory.getLogger(OrderController.class);


    @Autowired
    private IOrderService iOrderService;


    //创建订单 (入参发货地址id)
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpSession session, Integer shippingId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.createOrder(user.getId(),shippingId);
    }


    //取消订单 (把订单状态置为已取消)
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancel(user.getId(),orderNo);
    }

    //获取购物车中已经选中的商品详情
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }


    //当前用户的订单详情
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderDetail(user.getId(),orderNo);
    }

    //查看订单 (加分页)
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpSession session, @RequestParam(value = "pageNum",defaultValue = "1") int pageNum, @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(),pageNum,pageSize);
    }























    //支付接口    入参订单号 (成功则返回 订单号 和 二维码url)
    // 订单号  (request 获取ServletR 上下文 拿到 upload文件夹 把自动生成的二维码 传入ftp服务器上 然后返回给前端二维码的图片地址)
    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //拿到upload文件夹路径 (本机上的 在webapp里面) (没有/)
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }


    //回调方法接口 (客户端请求参数都是放在requst里面)   (扫码的时候)
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();

        Map requestParams = request.getParameterMap();//获得客户机请求参数 (map)

        //Iterator迭代器  (集合)..iterator()   ;  iter.hasNext()  检查集合是否有下一个元素
        // 这时候使用keySet()方法获取所有的key值
        //把客户机请求的参数装填进 params  用逗号分割
        for(Iterator iter = requestParams.keySet().iterator();iter.hasNext();){

            //使用next()获得序列中的下一个元素
            String name = (String)iter.next();
            String[] values = (String[]) requestParams.get(name);  //value
            String valueStr = ""; //声明空字符串
            for(int i = 0 ; i <values.length;i++){

                //拼接字符串,不是最后一个元素用逗号分割
                valueStr = (i == values.length -1)?valueStr + values[i]:valueStr + values[i]+",";
            }
            params.put(name,valueStr);  //放入key value  (整体的参数)
        }
        //sign  签名,trade_status 交易状态
        logger.info("支付宝回调,sign:{},trade_status:{},参数:{}",params.get("sign"),params.get("trade_status"),params.toString());


        //非常重要,验证回调的正确性,是不是支付宝发的.并且呢还要避免重复通知.
        //sdk里面除去一个参数 sgin  根据文档要求还要除去 sign_type
        params.remove("sign_type");
        try {
            // 验签方法  (params,支付宝公钥,字符集,方式(RSA2))
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());

            if(!alipayRSACheckedV2){
                return ServerResponse.createByErrorMessage("非法请求,验证不通过");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }

        //校验数据的一致性 (防止假二维码渗透)  信息有问题则不能回调 也就更新不了支付状态(即使沙箱中支付成功)
        ServerResponse serverResponse = iOrderService.checkData(params);
        if(!serverResponse.isSuccess()) {
            return serverResponse;
        }

            //
        //回调处理 和返回
        serverResponse = iOrderService.aliCallback(params);
        if(serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }

    //查询支付状态接口 (前台轮询的方法)
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }


        //查询成功  判断支付状态 (大于付款(20)时都认为是一个付款成功的订单)  true  false
        return iOrderService.queryOrderPayStatus(user.getId(),orderNo);

    }



















}
