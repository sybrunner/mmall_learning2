package com.mmall.common;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

/**
 * Created by apple on 17/7/29.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL) //保证序列化Json的时候如果是NULL对象,key也会消失(例如没有data时 data消失)

//范型构造的通用服务端端的响应对象
public class ServerResponse<T>  implements Serializable{
    private  int status;
    private  String msg;
    private  T data;//范型数据对象 ,在返回的时候指定范型的内容

    //构造方法 (构造器,优雅简明 通用)
    //传入 string 赋值的是msg 不是范型 data
    //如果要吧 string 类型赋值给 data ,需对外 public 方法进行处理 (当传入参数为范型的时调用的也是范型) 做了间接
    private ServerResponse(int status){
        this.status = status;
    }
    private ServerResponse(int status,T data) {
        this.status = status;
        this.data =data;
    }

    private ServerResponse(int status,String msg,T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }
    private ServerResponse(int status,String msg) {
        this.status = status;
        this.msg = msg;

    }

    @JsonIgnore//使isSuccess不会显示在Json序列化结果当中
    public  boolean isSuccess() {
        return  this.status == ResponseCode.SUCCESS.getCode();//等式成立返回 ture 否则 false  (右边=0) 判断是否为0
    }

    public int getStatus() {
        return  status;
    }
    public T getData() {
        return data;
    }
    public String getMsg() {
        return  msg;
    }
    //调用本类的构造方法 做为公共接口
    public static <T> ServerResponse<T> createBySuccess() {
        return  new ServerResponse<T>(ResponseCode.SUCCESS.getCode());//函数内容 = 0  返回的内容 status == 0 其余为null
    }

    public static <T> ServerResponse<T> createBySuccessMessage(String msg) {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg);
        //过程叙述,调用public 共有方法,传入 msg 返回时调用私有构造方法ServerResponse,里面的两个参数分别是---
        //---枚举类ResponseCode SUCCESS 函数 调用的 getCode 方法 和 传入时的 msg
        // 返回 0 和 msg
    }

    public static <T> ServerResponse<T> createBySuccess(T data) {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),data);
    }

    public static <T> ServerResponse<T> createBySuccess(String msg, T data) {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg,data);
    }
    public static <T> ServerResponse<T> createByError() {
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),ResponseCode.ERROR.getDesc());
    }

    public static <T> ServerResponse<T> createByErrorMessage(String errorMessage) {
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),errorMessage);
    }
    public static <T> ServerResponse<T> createByErrorCodeMessage(int errorCode, String errorMessage){
        return new ServerResponse<T>(errorCode,errorMessage);
    }
}
