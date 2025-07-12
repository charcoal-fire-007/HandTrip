package com.sky.utils;


import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.ObjectMetadata;
import com.sky.properties.AliyunOSSProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
@AllArgsConstructor
@Slf4j
//@Component
public class AliOssUtil {

    private String endpoint;
    private String bucketName;
    private String region;
//
//   @Autowired
//   AliyunOSSProperties properties;

   public String upload(byte[] data, String originalFileName)  throws Exception {
//       String endpoint = properties.getEndpoint();
//       String bucketName = properties.getBucketName();
//       String region = properties.getRegion();

       //环境中需要有AssKey配置
       EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

       String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
       String objectName = dir + UUID.randomUUID()+originalFileName.substring(originalFileName.lastIndexOf("."));
       log.info("新文件名称{}", objectName);

       ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
       clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

       OSS ossClient = OSSClientBuilder.create()
               .endpoint(endpoint)
               //环境中需要有AssKey配置
               .credentialsProvider(credentialsProvider)
               .clientConfiguration(clientBuilderConfiguration)
               .region(region)
               .build();
       try {
           ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(data));
       } finally {
           ossClient.shutdown();
       }
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;

   }
}
