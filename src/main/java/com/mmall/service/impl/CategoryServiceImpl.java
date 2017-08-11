package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import com.mmall.util.DateTimeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Created by apple
 */
@Service("iCategoryService")//声明服务  注入到controller供其调用
public class CategoryServiceImpl implements ICategoryService {

    //添加日志
    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryMapper categoryMapper;//注入CategoryMapper


    //增加节点service 的实现
    public ServerResponse addCategory(String categoryName, Integer parentId){

        //对分类名和ID  判空
        if(parentId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("添加品类参数错误");
        }

        //Category 就是ojo层的数据库对象  创建对象并赋值
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);//这个分类是可用的

        //判断插入是否生效(生效行数)
        int rowCount = categoryMapper.insert(category);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("添加品类成功");
        }
        return ServerResponse.createByErrorMessage("添加品类失败");
    }

    //更新分类名 (当前ID,新名字)
    public ServerResponse updateCategoryName(Integer categoryId, String categoryName){

        //对ID和名字  判空
        if(categoryId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("更新品类参数错误");
        }
        //拿对象 并赋值
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        category.setUpdateTime(DateTimeUtil.strToDate("2017-11-11 23:11:33"));//随便放入的时间
        //// TODO: 17/8/3     更新时间updatetime需要更新ok


        //判断生效行
        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("更新品类名字成功");
        }
        return ServerResponse.createByErrorMessage("更新品类名字失败");
    }

    //传入parentID查询子节点的category信息,并且不递归,保持平级
    //范型是一个集合 <List<Category>>
    public ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId){

        //查询的孩子节点集合
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);

        //判空
        if(CollectionUtils.isEmpty(categoryList)){
            logger.info("未找到当前分类的子分类");    //打印日志
            return ServerResponse.createByErrorMessage("未找到当前分类的子分类");
        }
        return ServerResponse.createBySuccess(categoryList);
    }


    /**
     * 递归查询本节点的id及孩子节点的id
     * @param categoryId
     * @return
     */
    public ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId){

        //初始化Set<Category> categorySet  //guava里面的 Sets.newHashSet()
        Set<Category> categorySet = Sets.newHashSet();

        //调用递归算法 返回过来的是 一个Category集合对象(categorySet)
        findChildCategory(categorySet,categoryId);

        //初始化 集合类型(int)  guava里面的初始化方法
        List<Integer> categoryIdList = Lists.newArrayList();

        if(categoryId != null){
            //遍历categorySet 增强循环遍历 Category类型的categoryItem变量 容纳每次遍历内容
            for(Category categoryItem : categorySet){

                categoryIdList.add(categoryItem.getId());//取出ID
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }


    //递归算法,算出子节点,返回当前parentID的所有节点和其子节点

    //Category 不同普通对象 比如string 可直接用equal方法 和 哈希code
    //自定义类实例 要重写! Category 对象的哈希code和equal方法  (pojo层里面的Category)
    //Set<Category> 是集合对象 (此方法的类型) set结构不能插入相同元素 可以排重
    private Set<Category> findChildCategory(Set<Category> categorySet , Integer categoryId){

        //查询ID的对象并赋予category
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null){
            categorySet.add(category);//添加ID查询的对象 categorySet
        }

        //查找子节点,递归算法一定要有一个退出的条件
        //通过ID获取子节点 并把它放入集合
        //mybatis 返回集合 没有查到 也不会返回NULL 对象
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);

        //增强循环 {用集合的类型定义一个容纳每次遍历内容的对象}  categoryList是被遍历的集合,没有内容或遍历完成 则循环结束
        //跳出for循环递归结束
        for(Category categoryItem : categoryList){

            findChildCategory(categorySet,categoryItem.getId());
        }
        return categorySet;
    }






}
