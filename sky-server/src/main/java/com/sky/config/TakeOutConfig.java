package com.sky.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.take-out")
@Data
public class TakeOutConfig {
    private Shop shop;

    @Data
    public static class Shop {
        private String address; // 店铺地址
        private Amap amap;      // 高德配置
    }

    @Data
    public static class Amap {
        private String key;     // 高德 Web 服务 Key
    }
}