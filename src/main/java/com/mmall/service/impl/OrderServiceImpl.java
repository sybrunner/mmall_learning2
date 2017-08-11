package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FTPUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;



/**
 * Created by apple
 */
@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

    /*初始化的静态块 */
    private static  AlipayTradeService tradeService;
    static {

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;


    //创建订单实现 (根据购物车创建订单)
    public ServerResponse createOrder(Integer userId, Integer shippingId){

        //从购物车中获取数据 (被勾选的)
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

        //创建子订单明细  (库存校验,产品状态校验,组装订单子表orderItem,加入到 orderItemList集合 ,并返回orderItemList集合)
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }

        //取得子订单明细(校验组装好的orderItemList的集合) 赋值给集合orderItemList
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();

        //计算这个订单的总价 (私有方法)
        BigDecimal payment = this.getOrderTotalPrice(orderItemList);


        //生成订单(私有方法)   order(包括订单号和数据填充)
        Order order = this.assembleOrder(userId,shippingId,payment);

        if(order == null){
            return ServerResponse.createByErrorMessage("生成订单错误");
        }
        if(CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //将生成的订单号赋值   到订单子对象(每件商品) 的订单号
        for(OrderItem orderItem : orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }

        //mybatis 批量插入  (每件商品对象  插入到订单子表)  (订单的一个子表代表一类预购买的商品)
        orderItemMapper.batchInsert(orderItemList);

        //生成成功,我们要减少我们产品的库存 (子表里每件产品的库存处理 )   (私有方法)
        this.reduceProductStock(orderItemList);

        //清空一下购物车     (私有)
        this.cleanCart(cartList);


        //返回给前端数据  (组装返回对象)
        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }


    //生成返回的OrderVo   (私有)  组装基本ordervo信息, 组装shippingVo 再给orderVo
    //(orderItemVoList的组装)  过程:OrderItemList拆出每个 orderItem 遍历组装为每个 OrderItemVo 再添一一加到  OrderItemVoList 最后把orderItemVoList 给 orderVo
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList){
        OrderVo orderVo = new OrderVo();  //初始化

        //组装信息 --------------------------------
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());

        // (支付方式)    过程:把1传入 拿到枚举对象  再获取它的value (描述)
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

        orderVo.setPostage(order.getPostage());//运费
        orderVo.setStatus(order.getStatus());//订单状态

        //(订单状态描述)    过程:把order.getStatus()传入枚举类 如果能匹配则拿到枚举对象  再获取它的value (描述)
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

        orderVo.setShippingId(order.getShippingId());  //发货地址ID

        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());//根据shippingID 查询发货地址
        if(shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));//组装shippingVo 再给orderVo
        }

        //继续组装orderVo
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

        //图片地址前缀
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));


        List<OrderItemVo> orderItemVoList = Lists.newArrayList();//初始化

        //遍历组装orderItemList 再加入 orderItemVoList
        for(OrderItem orderItem : orderItemList){

            //单个orderItem对象组装为单个orderItemVo对象
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }

        //(orderItemVoList 给orderVo对象
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    //orderItem 组装为 OrderItemVo
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){

        OrderItemVo orderItemVo = new OrderItemVo();//初始化

        //组装
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());

        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));//时间Data转为String
        return orderItemVo;
    }



    //ShippingVo 组装方法  (私有)
    private ShippingVo assembleShippingVo(Shipping shipping){

        ShippingVo shippingVo = new ShippingVo();  //初始化

        //组装 (返回给前段的收获地址数据)
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
        return shippingVo;
    }

    //清空购物车方法
    private void cleanCart(List<Cart> cartList){
        for(Cart cart : cartList){
            cartMapper.deleteByPrimaryKey(cart.getId()); //删除方法
        }
    }


    //减少库存的方法   (私有)
    private void reduceProductStock(List<OrderItem> orderItemList){

        //子表里每件产品的库存处理
        for(OrderItem orderItem : orderItemList){

            //拿到产品
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());

            //计算库存  更新 (减去购买数量)
            product.setStock(product.getStock()-orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }


    //生成订单私有方法
    private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment){
        Order order = new Order();   //设置订单对象

        //生成订单号方法
        long orderNo = this.generateOrderNo();

        order.setOrderNo(orderNo);   //组装订单号
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode()); //状态未付款
        order.setPostage(0);       //扩展用 目前为0
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode()); //在线支付
        order.setPayment(payment);   //订单金额

        order.setUserId(userId);
        order.setShippingId(shippingId); //发货地址ID
        //发货时间等等 (在发货时更新)
        //付款时间等等

        //插入数据 并判断(生效行数)   成功返回 order    失败返回null
        int rowCount = orderMapper.insert(order);
        if(rowCount > 0){
            return order;
        }
        return null;
    }


    //生成订单号 私有方法
    private long generateOrderNo(){

        //获取时间戳
        long currentTime =System.currentTimeMillis();
        //时间戳  + (0-100)随机数   (考虑了一定的并发而又简单粗暴)
        return currentTime+new Random().nextInt(100);
    }



    //计算总价的方法  (私有)  (入参:子订单集合)
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal payment = new BigDecimal("0"); //初始化

        //获取总价 (循环  连加)
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }



    //根据购物车对象创建子订单明细 (私有)
    private ServerResponse getCartOrderItem(Integer userId, List<Cart> cartList){

        List<OrderItem> orderItemList = Lists.newArrayList(); //初始化 返回集合

        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //校验购物车的数据,包括产品的状态和数量
        for(Cart cartItem : cartList){
            OrderItem orderItem = new OrderItem();

            //从购物车中获取产品
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());

            //判断产品状态 (1)
            if(Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"不是在线售卖状态");
            }

            //校验库存
            if(cartItem.getQuantity() > product.getStock()){
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"库存不足");
            }

            //组装orderItem
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity()));
            orderItemList.add(orderItem); //加入到orderItemList 集合
        }


        return ServerResponse.createBySuccess(orderItemList);
    }




    //取消订单
    public ServerResponse<String> cancel(Integer userId, Long orderNo){

        //拿到订单
        Order order  = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("该用户此订单不存在");
        }
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款,无法取消订单");
        }


        Order updateOrder = new Order();
        updateOrder.setId(order.getId()); //设置ID
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode()); //设置status 改成取消

        int row = orderMapper.updateByPrimaryKeySelective(updateOrder); //更新
        if(row > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }



    //已经选中的购物车商品
    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo(); //初始化返回 vo

        //从购物车中获取数据 (已选的商品)
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

        //创建私有订单明细
        ServerResponse serverResponse =  this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }

        //取得data (orderItemList) 到 orderItemList
        List<OrderItem> orderItemList =( List<OrderItem> ) serverResponse.getData();

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();//初始化 orderItemVo 集合

        //计算目前购物车中选中的总价
        BigDecimal payment = new BigDecimal("0");

        //遍历组装并添加到orderItemVoList
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());

            //组装
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        orderProductVo.setProductTotalPrice(payment);       //赋值总价 给 orderProductVo
        orderProductVo.setOrderItemVoList(orderItemVoList); //赋值OrderItemVoList集合 给orderProductVo
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix")); //图片地址前缀给orderProductVo

        return ServerResponse.createBySuccess(orderProductVo);
    }


    //订单详情
    public ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo){
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo); //找到Order (加userid防止横向越权)

        if(order != null){

            //获取订单的 orderItemList
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);

            //组装 orderVo
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }

        return  ServerResponse.createByErrorMessage("没有找到该订单");
    }


    //查看订单(通过userId)  实现方法
    public ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize){

        //分页
        PageHelper.startPage(pageNum,pageSize);

        //通过ID 获取订单集合
        List<Order> orderList = orderMapper.selectByUserId(userId);

        List<OrderVo> orderVoList = assembleOrderVoList(orderList,userId);//转化orderList 为 orderVoList 的方法

        PageInfo pageResult = new PageInfo(orderList);//分页
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }


    //转化orderList 为  orderVoList  (订单集合返回对象)
    private List<OrderVo> assembleOrderVoList(List<Order> orderList, Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList(); //初始化返回对象


        for(Order order : orderList){
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                // 管理员查询 不需要传userId (查的所有用户)
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }

            //生成返回的OrderVo 并添加
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }

        return orderVoList;
    }





















    //path 二维码存入路径 ,Long orderNo 订单号
    public ServerResponse pay(Long orderNo, Integer userId, String path){

        //承载的是 订单号 和二维码的url
        Map<String ,String> resultMap = Maps.newHashMap();

        //查询订单是否存在 (Order对象)
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo",String.valueOf(order.getOrderNo()));//放入订单号到对象中

        //组装参数 (参考demo)00000000000000000000000000000000000000000000000000

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();//订单号 TODO: 17/8/9 OK


        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("扫码支付,订单号:").append(outTradeNo).toString();//描述 TODO: 17/8/9


        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString(); //赋值成订单总价 TODO: 17/8/9


        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";



        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"   //body 拼装 TODO: 17/8/9
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();


        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");




        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，   (支付宝用的,需要填充)
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();//支付宝用的集合

        //获取订单下面的明细 (根据orderNo,userId拿到订单子集合)
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);

        //遍历item集合 填充GoodsDetail 然后添加到支付宝用的集合 ( goodsDetailList )中
        for(OrderItem orderItem : orderItemList){

            //构建goods    赋值 (ID,名字,    单价(单位为分,转成元需乘以100)    ,数量)
            GoodsDetail goods = GoodsDetail.newInstance(
                    orderItem.getProductId().toString(), //id
                    orderItem.getProductName(),     //名字
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),//分转元,单价
                    orderItem.getQuantity());   //数量
            goodsDetailList.add(goods); //添加到集合
        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)

                // todo 授权回调地址(内网穿透测试)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);



        //生成二维码
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                //保证path 下的 upload文件夹存在 (否则生成时会报错)
                File folder = new File(path);
                if(!folder.exists()){
                    folder.setWritable(true); //给写权限 推出
                    folder.mkdirs();          //创建
                }

                // 需要修改为运行机器上的路径
                //注意细节(/) path没有 / 所以得加上
                //二维码 qrPath
                String qrPath = String.format(path+"/qr-%s.png",response.getOutTradeNo());
                String qrFileName = String.format("qr-%s.png",response.getOutTradeNo()); //二维码文件名
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);//生成二维码到 qrPath

                //传到ftp服务器上
                File targetFile = new File(path,qrFileName); //目标文件的path(webapp)  目标文件的名字 (目标文件url地址)
                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));//上传方法
                } catch (IOException e) {
                    logger.error("上传二维码异常",e);
                }
                logger.info("qrPath:" + qrPath);

                //二维码url的返回 (ftp服务器目录的img目录下)
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFile.getName();
                resultMap.put("qrUrl",qrUrl);
                return ServerResponse.createBySuccess(resultMap);
            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }

    }
    // 0000000000000000000000000000000000000000000000000000000  上上上上上上上上

    // 简单打印应答    日志   (拿来主义)
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }

    //回调处理 (等待付款也会传入记录PayInfo信息,但不会更新订单)
    public ServerResponse aliCallback(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no")); //取得订单号
        String tradeNo = params.get("trade_no");         //交易凭证号
        String tradeStatus = params.get("trade_status");  //交易状态
        Order order = orderMapper.selectByOrderNo(orderNo);  //根据订单号 查询order
        if(order == null){
            return ServerResponse.createByErrorMessage("非本商城的订单,回调忽略");
        }

        //已付款枚举 从20 开始
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess("支付宝重复调用"); //也算回调成功
        }

        //判断交易状态 (如果成功)
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){

            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));//更新付款时间(官方文档字段gmt_payment 代表支付时间)
            order.setStatus(Const.OrderStatusEnum.PAID.getCode()); //置订单状态为已付款
            orderMapper.updateByPrimaryKeySelective(order); //更新订单状态
        }

        //组装payInfo对象
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode()); //支付平台
        payInfo.setPlatformNumber(tradeNo);    //交易号
        payInfo.setPlatformStatus(tradeStatus);   //交易状态

        payInfoMapper.insert(payInfo);   //插入数据

        return ServerResponse.createBySuccess();
    }


    public ServerResponse checkData(Map<String,String> params) {
        Long orderNo = Long.parseLong(params.get("out_trade_no")); //取得订单号
        BigDecimal amount = new BigDecimal(params.get("total_amount"));//取得实际金额
        String sellId = params.get("seller_id"); //取得卖家帐号

        Order order = orderMapper.selectByOrderNo(orderNo);  //根据订单号 查询order
        if(order == null){
            return ServerResponse.createByErrorMessage("订单号不匹配");
        }
        if(order.getPayment().compareTo(amount) != 0)  {
            return ServerResponse.createByErrorMessage("价格不匹配");
        }

        if(!sellId.equals("2088102172262704")) {
            return ServerResponse.createByErrorMessage("买家帐号不匹配");
        }

        return ServerResponse.createBySuccess();
    }


    public ServerResponse queryOrderPayStatus(Integer userId, Long orderNo){
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo); //查询订单是否存在
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }

        //查询成功  判断支付状态 (大于付款(20)时都认为是一个付款成功的订单)
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }














    //backend

    //后台查看订单 (无参)
    public ServerResponse<PageInfo> manageList(int pageNum, int pageSize){
        PageHelper.startPage(pageNum,pageSize);

        //查询 (无参数)
        List<Order> orderList = orderMapper.selectAllOrder();

        //组装 orderVoList
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);

        //分页
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);


        return ServerResponse.createBySuccess(pageResult);
    }


    //后台查看详情
    public ServerResponse<OrderVo> manageDetail(Long orderNo){

        //根据订单号获得订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){

            //通过订单获取  orderItemList
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);

            //组装集合 (组装成返回对象)
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


    //后台查询  (多加了个分页)
    public ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize){
        PageHelper.startPage(pageNum,pageSize);

        Order order = orderMapper.selectByOrderNo(orderNo);//根据订单号拿到 order
        if(order != null){

            //根据订单order 取得 orderItemList
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);

            //组装成要返回的orderVo 对象
            OrderVo orderVo = assembleOrderVo(order,orderItemList);


            //分页
            PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
            pageResult.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


    //后台发货
    public ServerResponse<String> manageSendGoods(Long orderNo){

        //查询订单
        Order order= orderMapper.selectByOrderNo(orderNo);
        if(order != null){

            //判断支付状态 (如果已付款)
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){

                //状态变为发货
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());

                //发货时间
                order.setSendTime(new Date());

                //更新数据库
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }


        return ServerResponse.createByErrorMessage("订单不存在");
    }







}
