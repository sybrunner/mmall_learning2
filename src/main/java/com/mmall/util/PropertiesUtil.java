package com.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by apple
 */

//配置类 (工具类)
public class PropertiesUtil {

    private static Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);//记录器(日志)

    private static Properties props;//配置的常量

    //静态块,comtact启动的时候读取配置  静态块执行顺序优于普通代码块   普通代码块优于构造代码块
    //静态代码块 在本类被加载的时候执行  且只执行一次   , 一般用于初始化静态变量
    static {
        String fileName = "mmall.properties";
        props = new Properties();

        //InputStreamReader 字符流    PropertiesUtil.class.getClassLoader() 获得该类对象的装载器 ,路径..
        try {
            props.load(new InputStreamReader(PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName),"UTF-8"));
        } catch (IOException e) { //捕获异常
            logger.error("配置文件读取异常",e); //抛出
        }
    }

    //获取配置文件的key值
    public static String getProperty(String key){

        //trim() 可以忽略空格
        String value = props.getProperty(key.trim());
        if(StringUtils.isBlank(value)){
            return null;
        }
        return value.trim();
    }

    //key 为空则传入defaultValue
    public static String getProperty(String key,String defaultValue){

        //trim() 可以忽略空格
        String value = props.getProperty(key.trim());
        if(StringUtils.isBlank(value)){
            value = defaultValue;
        }
        return value.trim();
    }



}
