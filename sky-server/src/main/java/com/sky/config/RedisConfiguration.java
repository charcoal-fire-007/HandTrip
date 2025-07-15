package com.sky.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory f) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(f);

//        ObjectMapper objectMapper = new JacksonObjectMapper();
//        Jackson2JsonRedisSerializer<Object> serializer =
//                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);


        t.setKeySerializer(new StringRedisSerializer());
//        t.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class)); // JSON 序列化 value
        return t;
    }
}



