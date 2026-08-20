package dev.csyhorizon.chatbe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import dev.csyhorizon.chatbe.domain.chat.dto.ChatMessageDto;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, ChatMessageDto> chatMessageRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<ChatMessageDto> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(ChatMessageDto.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, ChatMessageDto> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, ChatMessageDto> context = builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context = 
                RedisSerializationContext.<String, String>newSerializationContext(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
