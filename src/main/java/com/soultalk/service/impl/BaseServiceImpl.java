package com.soultalk.service.impl;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.soultalk.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.soultalk.config.Configs.*;

@Service
@Slf4j
public class BaseServiceImpl implements BaseService {

    @Override
    public String saveFileToOSS(String fileName, MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new RuntimeException("文件名错误");
        }

        String ext = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = uuid + ext;

        // 创建 OSSClient 实例，使用显式配置的凭证
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(OSS_ENDPOINT)
                .credentialsProvider(new DefaultCredentialProvider(ALI_ACCESSKEY_ID, ALI_ACCESSKEY_SECRET))
                .clientConfiguration(clientBuilderConfiguration)
                .region(OSS_REGION)
                .build();

        try {
            // 创建 PutObjectRequest 对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(OSS_BUCKET, objectName, file.getInputStream());
            ossClient.putObject(putObjectRequest);
            // 生成可访问的 URL
            return "https://" + OSS_BUCKET + "." + OSS_ENDPOINT.replace("https://", "") + "/" + objectName;

        } catch (Exception oe) {
            System.out.println("Error Message:" + oe.getMessage());
            System.out.println("Request ID:" + (oe instanceof com.aliyun.oss.OSSException ? ((com.aliyun.oss.OSSException) oe).getRequestId() : "N/A"));
            System.out.println("Host ID:" + (oe instanceof com.aliyun.oss.OSSException ? ((com.aliyun.oss.OSSException) oe).getHostId() : "N/A"));
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            log.error(oe.getMessage());
            return null;
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void removeFileFromOSS(String url) {
        //https://ai-soul.oss-cn-chengdu.aliyuncs.com/e3cca49509f74b16bf84e3583f3e4c3f.png
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String[] urlSplit = url.split("/");
        String endpoint = urlSplit[0] + "//" + urlSplit[2];
        // 填写Bucket名称，例如examplebucket。
        String bucketName = endpoint.split("\\.")[0];
        // 填写文件完整路径。文件完整路径中不能包含Bucket名称。
        String objectName = url.replace(endpoint + "/", "");
        // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
        String region = endpoint.split("\\.")[1];

        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(new DefaultCredentialProvider(ALI_ACCESSKEY_ID, ALI_ACCESSKEY_SECRET))
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 删除文件或目录。如果要删除目录，目录必须为空。
            ossClient.deleteObject(bucketName, objectName);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }


}
