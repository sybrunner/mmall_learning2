package com.mmall.common;


import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by apple on 17/7/30.
 */
//常量类 方便使用
public class Const {
    public static final String CURRENT_USER = "currentUser";
    public static final String EMAIL = "email";
    public static final String USERNAME= "username";

    //排序
    public interface ProductListOrderBy{
        Set<String> PRICE_ASC_DESC = Sets.newHashSet("price_desc","price_asc");//下划线分割
    }

    //购物车模块
    public interface Cart{
        int CHECKED = 1;//即购物车选中状态
        int UN_CHECKED = 0;//购物车中未选中状态

        String LIMIT_NUM_FAIL = "LIMIT_NUM_FAIL";
        String LIMIT_NUM_SUCCESS = "LIMIT_NUM_SUCCESS";
    }

    //用枚举显得过于繁重,用接口类 既能实现分组又不会太繁重 还是常量
    public interface Role {
        int ROLE_CUSTOMER = 0 ;//普通用户
        int ROLE_ADMIN = 1;// 管理员
    }

    //声明枚举类  ProductStatusEnu
    public enum ProductStatusEnum{
        ON_SALE(1,"在线");
        private String value;
        private int code;

        //构造器
        ProductStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    //支付订单模块
    public enum OrderStatusEnum{
        CANCELED(0,"已取消"),
        NO_PAY(10,"未支付"),
        PAID(20,"已付款"),
        SHIPPED(40,"已发货"),
        ORDER_SUCCESS(50,"订单完成"),
        ORDER_CLOSE(60,"订单关闭");

        //构造器
        OrderStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }

        //静态方法   把值传入 拿到枚举对象  再获取它的value (描述)
        public static OrderStatusEnum codeOf(int code){
            for(OrderStatusEnum orderStatusEnum : values()){
                if(orderStatusEnum.getCode() == code){
                    return orderStatusEnum;
                }
            }
            throw new RuntimeException("没有找到对应的枚举");
        }
    }

    public interface  AlipayCallback{
        String TRADE_STATUS_WAIT_BUYER_PAY = "WAIT_BUYER_PAY";  //等待付款
        String TRADE_STATUS_TRADE_SUCCESS = "TRADE_SUCCESS";  //支付成功

        //返回值
        String RESPONSE_SUCCESS = "success";
        String RESPONSE_FAILED = "failed";
    }

    //支付平台
    public enum PayPlatformEnum{
        ALIPAY(1,"支付宝");

        PayPlatformEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    //支付方式
    public enum PaymentTypeEnum{
        ONLINE_PAY(1,"在线支付");

        PaymentTypeEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }


        //返回描述 (调用此类的静态方法  把1传入 拿到枚举对象  再获取它的value)
        public static PaymentTypeEnum codeOf(int code){

            //values 为枚举类的数组对象
            for(PaymentTypeEnum paymentTypeEnum : values()){

                //gode相等 返回枚举
                if(paymentTypeEnum.getCode() == code){
                    return paymentTypeEnum;
                }
            }
            throw new RuntimeException("没有找到对应的枚举");
        }

    }

}
