package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.vo.ProductDetailVo;

/**
 * Created by apple
 */
public interface IProductService {

    ServerResponse saveOrUpdateProduct(Product product);//保存或更新产品

    ServerResponse<String> setSaleStatus(Integer productId, Integer status);//更新产品上下架

    ServerResponse<ProductDetailVo> manageProductDetail(Integer productId);//获取产品详情

    ServerResponse<PageInfo> getProductList(int pageNum, int pageSize);//产品信息的分页

    ServerResponse<PageInfo> searchProduct(String productName, Integer productId, int pageNum, int pageSize);//后台搜索产品

    ServerResponse<ProductDetailVo> getProductDetail(Integer productId);//前台访问产品的方法

    //搜索产品(通过产品关键字和分类ID) 涉及价格排序   未上架商品不展示
    ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy);



}
