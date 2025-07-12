package com.sky.config;


import com.sky.properties.AliyunOSSProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliyunOSSProperties aliyunOSSProperties) {
        return new AliOssUtil(
                aliyunOSSProperties.getEndpoint()
                ,aliyunOSSProperties.getBucketName()
                ,aliyunOSSProperties.getRegion());
    }
}
