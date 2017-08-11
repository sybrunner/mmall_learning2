package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by apple
 */
public interface IFileService {

    String upload(MultipartFile file, String path);//上传文件
}
