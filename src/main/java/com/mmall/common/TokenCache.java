package com.mmall.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by apple on 17/7/29.
 */
public class TokenCache {


    //声明日志
    private static Logger logger = LoggerFactory.getLogger(TokenCache.class);

    //设置变量
    public static final String TOKEN_PREFIX = "token_";


    //声明静态内存块 guava 里面的  (key和value   都是string类型)
    private static LoadingCache<String,String> localCache = CacheBuilder.newBuilder()

            //设置缓存的初始化容量
            .initialCapacity(1000)

            //设置最大缓存容量 ,超过缓存最大容量使用LRU算法移除缓存项
            .maximumSize(10000)

            //缓存存活时间 :12小时
            .expireAfterAccess(12, TimeUnit.HOURS)

            .build(new CacheLoader<String, String>() {
                //new CacheLoader<String, String>: 匿名实现
                //默认的数据加载实现,当调用get取值的时候,如果key没有对应的值,就调用这个方法进行加载.

                @Override
                public String load(String s) throws Exception {
                    return "null";// 不用null  是防止当key不存在时 guava内部会抛出异常
                }
            });

    //set方法
    public static void setKey(String key,String value){
        localCache.put(key,value);
    }

    //get 方法
    public static String getKey(String key){

        String value = null;

        try {
            value = localCache.get(key);
            if("null".equals(value)){    //通过键获取缓存中的值，若不存在直接返回null
                return null;
            }
            return value;
        } catch (Exception e){           //存在异常则抛出   try   catch 的固定用法
            logger.error("localCache get error",e);   //日志  打印异常堆栈  (异常类e)
        }
        return null;
    }
}
