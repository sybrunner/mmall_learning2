package com.mmall.service.impl;

import com.alipay.api.domain.Data;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.ICategoryService;
import com.mmall.service.IProductService;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVo;
import com.mmall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by apple
 */
@Service("iProductService") //注解到service 头部
public class ProductServiceImpl implements IProductService {


    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ICategoryService iCategoryService;


    //保存还是更新产品  (后端可以写在一起,做个判定就行了)
    public ServerResponse saveOrUpdateProduct(Product product){


        if(product != null)
        {
            //判断子图是否为空
            if(StringUtils.isNotBlank(product.getSubImages())){

                //子图不为空 取第一个子图 赋给主图
                //添加分割 ','    以逗号为分割点
                String[] subImageArray = product.getSubImages().split(",");
                if(subImageArray.length > 0){
                    product.setMainImage(subImageArray[0]);//给主图
                }
            }

            //有ID 调用此方法则可更新
            if(product.getId() != null){

                //前端传入过来(全部传入)  更新整个产品
                int rowCount = productMapper.updateByPrimaryKey(product);
                if(rowCount > 0){
                    return ServerResponse.createBySuccess("更新产品成功");
                }
                return ServerResponse.createBySuccess("更新产品失败");
            }
            //等于空 则新增它
            else{
                int rowCount = productMapper.insert(product);
                if(rowCount > 0){
                    return ServerResponse.createBySuccess("新增产品成功");
                }
                return ServerResponse.createBySuccess("新增产品失败");
            }
        }



        return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
    }

    //更新产品销售状态
    public ServerResponse<String> setSaleStatus(Integer productId, Integer status){

        //参数不合法判断
        if(productId == null || status == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        product.setUpdateTime(DateTimeUtil.strToDate("2017-11-11 23:11:33"));//非空更新 传入一个yyyy-MM-dd HH:mm:ss
        int rowCount = productMapper.updateByPrimaryKeySelective(product);//属性不为空则更新 TODO 更新时间ok
        if(rowCount > 0){
            return ServerResponse.createBySuccess("修改产品销售状态成功");
        }
        return ServerResponse.createByErrorMessage("修改产品销售状态失败");
    }

    //获取产品详情的实现 (返回VO对象的数据)
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId){

        //判断参数是否错误
        if(productId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId); //查询产品对象 (return 对象)
        if(product == null){
            return ServerResponse.createByErrorMessage("产品已下架或者删除");
        }

        //初始化 vo
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    //productDetail 的组装方法
    private ProductDetailVo assembleProductDetailVo(Product product){

        //创建一个 productDetail的一个VO对象  并赋值
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        //key  和 defaulevalue  (文件服务器)  // TODO: 17/8/4 FTP 已改
        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","ftp.server.http.prefix"));

        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null){
            productDetailVo.setParentCategoryId(0);//默认根节点
        }else{
            productDetailVo.setParentCategoryId(category.getParentId());//赋予ID
        }

        //时间转化并赋值
        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }

    //分页方法实现
    public ServerResponse<PageInfo> getProductList(int pageNum, int pageSize){
        //startPage--start
        //填充自己的sql查询逻辑
        //pageHelper-收尾
        PageHelper.startPage(pageNum,pageSize);//传入当前页面  和页面大小
        List<Product> productList = productMapper.selectList();  //所有商品信息

        List<ProductListVo> productListVoList = Lists.newArrayList();//初始化List 接收VO
        //增强遍历
        for(Product productItem : productList){
            ProductListVo productListVo = assembleProductListVo(productItem);
            productListVoList.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(productList);//分页结果 (所有商品星系)
        pageResult.setList(productListVoList);//重置 (部分商品信息)
        return ServerResponse.createBySuccess(pageResult);  //返回部分信息
    }

    //ProductListVo的组装方法
    private ProductListVo assembleProductListVo(Product product){
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","ftp.server.http.prefix"));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        return productListVo;
    }


    //产品搜索实现
    public ServerResponse<PageInfo> searchProduct(String productName, Integer productId, int pageNum, int pageSize){

        PageHelper.startPage(pageNum,pageSize);//page初始化
        if(StringUtils.isNotBlank(productName)){

            //当对一个字符串进行多次操作 StringBuilder 的效率 远高于 string (需再次分配内存空间)
            //productName:   %productName% 并转化成string
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<Product> productList = productMapper.selectByNameAndProductId(productName,productId);

        //productList转化为productListVoList
        List<ProductListVo> productListVoList = Lists.newArrayList();//初始化
        for(Product productItem : productList){
            ProductListVo productListVo = assembleProductListVo(productItem);
            productListVoList.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(productList);// 初始化返回结果为 productList  (全部参数)
        pageResult.setList(productListVoList);          // 重置返回结果为   productListVoList (部分参数)
        return ServerResponse.createBySuccess(pageResult);
    }


    //前台访问产品的方法     区别在于未上架的商品不展示
    public ServerResponse<ProductDetailVo> getProductDetail(Integer productId){
        if(productId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null){
            return ServerResponse.createByErrorMessage("产品已下架或者删除");
        }
        //判断是否在售
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServerResponse.createByErrorMessage("产品已下架或者删除");
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }


    //客户端  搜索产品通过关键字分类(一个关键字) (分类集合)
    public ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy){

        //如果关键字和分类ID都不存在   参数错误
        if(StringUtils.isBlank(keyword) && categoryId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        //声明分类ID(集合)
        List<Integer> categoryIdList = new ArrayList<Integer>();

        //获取查询的分类对象
        if(categoryId != null){
            Category category = categoryMapper.selectByPrimaryKey(categoryId); //获取category

            //如果没有命中数据
            if(category == null && StringUtils.isBlank(keyword)){
                //没有该分类,并且还没有关键字,这个时候返回一个空的结果集,不报错
                PageHelper.startPage(pageNum,pageSize);   //分页的内容
                List<ProductListVo> productListVoList = Lists.newArrayList();//声明VO对象
                PageInfo pageInfo = new PageInfo(productListVoList);  //生效分页 (空的集合)
                return ServerResponse.createBySuccess(pageInfo);
            }
            //查子分类 返回当前分类ID和子分类ID
            // (传入一个分类ID 进入此分类和旗下所有子分类中去查找产品)  这里返回当前分类ID 和 子分类ID 的集合
            //需要判断 category 是否为NULL 如果是 return错误
            if(category == null) {
                return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"找不到分类ID");
            }
                categoryIdList = iCategoryService.selectCategoryAndChildrenById(category.getId()).getData();

        }

        //关键字不为空 拼接一下
        if(StringUtils.isNotBlank(keyword)){
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }

        PageHelper.startPage(pageNum,pageSize);//开始分页
        //排序处理
        if(StringUtils.isNotBlank(orderBy)){

            //contains(orderBy)  传过来的字符串 在此集合里面
            if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String[] orderByArray = orderBy.split("_"); //下划线分割 并给到数组
                PageHelper.orderBy(orderByArray[0]+" "+orderByArray[1]); // 传入 pageHelper.orderBy(价格 排序)
            }
        }

        //产品查找的方法 (传入 分类集合  关键字  )
        //?表达式  判断是否为空(传入不同值)
        List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword,categoryIdList.size()==0?null:categoryIdList);

        List<ProductListVo> productListVoList = Lists.newArrayList();//初始化VO
        for(Product product : productList){
            ProductListVo productListVo = assembleProductListVo(product);//组装方法
            productListVoList.add(productListVo);
        }

        //开始分页
        PageInfo pageInfo = new PageInfo(productList);    //放入
        pageInfo.setList(productListVoList);    //重置
        return ServerResponse.createBySuccess(pageInfo);
    }


























}
