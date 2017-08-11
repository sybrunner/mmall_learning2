package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by apple
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService {

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);//打印日志


    //需要把上传之后的文件名返回回去 (参数:文件名 路径)
    public String upload(MultipartFile file,String path){
        String fileName = file.getOriginalFilename();  //拿到文件名
        //扩展名
        //abc.jpg
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".")+1);//获取扩展名(从最后开始获取) +1是去掉'.'

        //上传文件的名字 (放入不带重复的UUID能避免重名覆盖的问题)
        String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;
        logger.info("开始上传文件,上传文件的文件名:{},上传的路径:{},新文件名:{}",fileName,path,uploadFileName);

        //声明目录的File
        File fileDir = new File(path);
        if(!fileDir.exists()){        //没有则创建
            fileDir.setWritable(true);//赋予可写权限
            fileDir.mkdirs();//创建文件夹  (多级别)
        }
        File targetFile = new File(path,uploadFileName); // 创建文件


        try {
            file.transferTo(targetFile);
            //文件已经上传成功了

            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //已经上传到ftp服务器上

            targetFile.delete();  //上传完成删除upload下面的文件
        } catch (IOException e) {  //捕获异常
            logger.error("上传文件异常",e);
            return null;
        }
        //A:abc.jpg
        //B:abc.jpg
        return targetFile.getName();
    }

}
