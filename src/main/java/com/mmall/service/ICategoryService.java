package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.Category;

import java.util.List;

/**
 * Created by apple
 */
public interface ICategoryService {
    ServerResponse addCategory(String categoryName, Integer parentId);//添加分类节点

    ServerResponse updateCategoryName(Integer categoryId, String categoryName);//更改分类名

    ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId);//查找子节点信息

    ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId);//递归查询孩子节点

}
