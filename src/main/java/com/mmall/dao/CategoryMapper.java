package com.mmall.dao;

import com.mmall.pojo.Category;

import java.util.List;

public interface CategoryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Category record);//插入一个Category对象到Category 表

    int insertSelective(Category record);

    Category selectByPrimaryKey(Integer id);//根据ID返回对象

    int updateByPrimaryKeySelective(Category record);//更新分类(非空)

    int updateByPrimaryKey(Category record);

    List<Category> selectCategoryChildrenByParentId(Integer parentId);//把传入参数做为parentID 获取有此parentID对节点信息
}