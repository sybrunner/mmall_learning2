package com.mmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by apple
 */
@Service("iCartService")
public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;



    // 查询    获得一个用户的所有购物车对象集合  并返回)
    public ServerResponse<CartVo> list (Integer userId){
        CartVo cartVo = this.getCartVoLimit(userId);    //购物车集合对象
        return ServerResponse.createBySuccess(cartVo);
    }


    //添加购物车 方法实现
    public ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count){

        //判断参数合法性
       if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        //1.商品ID能在商品列表里面找到   并且是已上架的商品
        Product product = productMapper.selectByPrimaryKey(productId); //查询产品对象 (return 对象)
        if(product == null){
            return ServerResponse.createByErrorMessage("产品不存在");
        }
        //判断是否在售
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServerResponse.createByErrorMessage("产品已下架");
        }



        //查询购物车里是否有对应的产品
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
        if(cart == null){

            //如果这个产品不在这个购物车里,需要新增一个这个产品的记录 (赋值)
            //一个Cart对象 的初始化
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            cartItem.setChecked(Const.Cart.CHECKED); //默认选中状态
            cartItem.setProductId(productId);
            cartItem.setUserId(userId);

            //插入购物车
            cartMapper.insert(cartItem);
        }else{
            //这个产品已经在购物车里了.
            //如果产品已存在,数量相加
            count = cart.getQuantity() + count;
            cart.setQuantity(count);  //赋值
            cartMapper.updateByPrimaryKeySelective(cart);//更新
        }
        return this.list(userId);// 获得一个用户的所有购物车对象集合  并返回
    }

    //更新购物车实现(productId, count)
    public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);//拿到购物车的一个 产品对象
        if(cart != null){
            cart.setQuantity(count);//更新数量(全更新)  (为空不更)
        }
        cartMapper.updateByPrimaryKey(cart);
        return this.list(userId);
    }

    //删除购物车 (若传入参数productIds : 1,3   则通过逗号分割可进行批量移除)
    public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds){

        //guava Splitter 方法 (否则需要转成数组再遍历数组添加到集合)
        List<String> productList = Splitter.on(",").splitToList(productIds);//用逗号分割 ,再转成集合

        //为空则参数错误
        if(CollectionUtils.isEmpty(productList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        //删除(指定某个用户)  (购物车里要删除的产品ID集合)
        cartMapper.deleteByUserIdProductIds(userId,productList);
        return this.list(userId);//从DB中获取最新的CartVo对象返回
    }





    //全选/全反选 单选/单反选  实现
    public ServerResponse<CartVo> selectOrUnSelect (Integer userId, Integer productId, Integer checked){
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }


    //查询购物车产品总数
    public ServerResponse<Integer> getCartProductCount(Integer userId){
        if(userId == null){
            return ServerResponse.createBySuccess(0);
        }

        //
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }



    //关键方法 !
    //得到一个用户的所有购物车内容  初始化并赋值单个(购物车和商品的对象)
    // (处理丢失精度,
    // 判断库存进行赋值
    // 超过限制就更新能选到商品的最大容量
    //全选判断

    private CartVo getCartVoLimit(Integer userId){
        CartVo cartVo = new CartVo();   //声明
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);//查询用户的购物车对象并放到cartList集合
        List<CartProductVo> cartProductVoList = Lists.newArrayList();// 结合了产品和购物车的抽象对象集合 (初始化)

        BigDecimal cartTotalPrice = new BigDecimal("0");//初始化购物车总价  BigDecimal:商业计算类型

        //判断这个集合不是空的 (说明能找到购物车对象)
        if(CollectionUtils.isNotEmpty(cartList)){

            //增强循环 cartList元素用cartItem 承载
            for(Cart cartItem : cartList){

                //创建cartProductVo,初始化并赋值  (把每个购物车对象放入 cartProductVo)
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cartItem.getId());
                cartProductVo.setUserId(userId);
                cartProductVo.setProductId(cartItem.getProductId());

                //查询购物车产品对象 (通过主键)
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if(product != null){

                    //把产品信息组装进去
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());

                    //判断库存 (最大购买数量 初始化为0)
                    int buyLimitCount = 0;

                    if(product.getStock() >= cartItem.getQuantity()){
                        //库存充足的时候
                        buyLimitCount = cartItem.getQuantity(); //赋值购买数量
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);//没超过限制
                    }else{
                        buyLimitCount = product.getStock();  //赋值为当前库存数量
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);//超过限制

                        //取得更新字段 (超过限制就更新能选到商品的最大容量)
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cartItem.getId());
                        cartForQuantity.setQuantity(buyLimitCount);

                        //购物车中更新有效库存(id为必须字段,属性非空则更新)
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }

                    //购买数量 (库存充足则为选择数量,反之则为最大库存数量)
                    cartProductVo.setQuantity(buyLimitCount);

                    //计算总价 (BigDecima避免精度丢失) //调用工具类的方法
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVo.getQuantity()));
                    cartProductVo.setProductChecked(cartItem.getChecked());//勾选状态赋值

                    //如果已经勾选,增加到整个的购物车总价中  (业务场景)  (当前目前勾选购物车的总价=之前勾选购物车的总价+本产品的总价)
                    if(cartItem.getChecked() == Const.Cart.CHECKED){
                        cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                    }


                    cartProductVoList.add(cartProductVo);//添加到产品购物车的对象集合

                }//产品不为空条件结束


            }//增强循环结束
        }// 产品

        //赋值给返回对象  (找不到对象就赋值初始化的 空值)
        cartVo.setCartTotalPrice(cartTotalPrice);   //总价
        cartVo.setCartProductVoList(cartProductVoList);  //赋值cartProductVo集合给cartVo集合
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));  //判断全选
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix")); //图片host

        return cartVo;//返回
    }

    //判断是否全选的方法
    private boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0; //未勾选数量为0代表全选  (等式成立返回true)

    }


























}
