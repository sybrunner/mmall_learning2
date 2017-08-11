package com.mmall.util;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by apple
 */
public class FTPUtil {

    private static  final Logger logger = LoggerFactory.getLogger(FTPUtil.class);//打印日志的对象

    //连接参数
    private static String ftpIp = PropertiesUtil.getProperty("ftp.server.ip");
    private static String ftpUser = PropertiesUtil.getProperty("ftp.user");
    private static String ftpPass = PropertiesUtil.getProperty("ftp.pass");

    //本类的构造器
    public FTPUtil(String ip, int port, String user, String pwd){
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }


    //主方法 上传文件
    public static boolean uploadFile(List<File> fileList) throws IOException {  //抛出异常
        FTPUtil ftpUtil = new FTPUtil(ftpIp,21,ftpUser,ftpPass); //21端口
        logger.info("开始连接ftp服务器");
        boolean result = ftpUtil.uploadFile("img",fileList);// 传到FTP文件夹下的img文件夹 注意赋予文件权限和拥有者
        logger.info("开始连接ftp服务器,结束上传,上传结果:{}");
        return result;
    }


    //上传的具体逻辑  remotePath(远程路径)
    private boolean uploadFile(String remotePath,List<File> fileList) throws IOException {
        boolean uploaded = true;   //判断是否传完了
        FileInputStream fis = null; // 声明InputStream
        //连接FTP服务器
        if(connectServer(this.ip,this.port,this.user,this.pwd)){
            try {

                ftpClient.changeWorkingDirectory(remotePath); //更改工作目录 , remotePath = null (不切换)
                ftpClient.setBufferSize(1024);  //设置缓冲区
                ftpClient.setControlEncoding("UTF-8");  //设置编码格式
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE); //文件类型 : (防止一些乱码的问题)(2)二进制
                ftpClient.enterLocalPassiveMode();   //打开本地的被动模式
                for(File fileItem : fileList){
                    fis = new FileInputStream(fileItem);
                    ftpClient.storeFile(fileItem.getName(),fis);  //存储文件的方法 (文件名,input流)
                }

            } catch (IOException e) {
                logger.error("上传文件异常",e);
                uploaded = false;
                e.printStackTrace();
            } finally {
                fis.close();   //关闭流
                ftpClient.disconnect();  //关闭连接
            }
        }
        return uploaded;
    }

    //连接FTP服务器的封装方法
    private boolean connectServer(String ip,int port,String user,String pwd){

        boolean isSuccess = false;  //成功则返回true
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(ip);//连接
            isSuccess = ftpClient.login(user,pwd);//登陆
        } catch (IOException e) {
            logger.error("连接FTP服务器异常",e);
        }
        return isSuccess;
    }











    private String ip;
    private int port;
    private String user;
    private String pwd;
    private FTPClient ftpClient;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }
}
