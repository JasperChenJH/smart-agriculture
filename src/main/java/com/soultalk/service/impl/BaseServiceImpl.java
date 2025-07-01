package com.soultalk.service.impl;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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
        assert originalFileName != null;
        String ext = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = uuid + ext;

        // 创建 OSSClient 实例，使用显式配置的凭证
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(OSS_ENDPOINT)
                .credentialsProvider(new DefaultCredentialProvider(OSS_ACCESSKEY_ID, OSS_ACCESSKEY_SECRET))
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
}
