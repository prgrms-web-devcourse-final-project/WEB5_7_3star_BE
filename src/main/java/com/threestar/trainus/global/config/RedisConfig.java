package com.threestar.trainus.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {
    //@Value로 처리 -> yml 에 넣어두고
    private String host;

    private int port;

    @Bean
    public RediscConnectionFactory rediscConnectionFactory(){
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate(){
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }

}
