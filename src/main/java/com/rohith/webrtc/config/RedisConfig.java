package com.rohith.webrtc.config;

import com.rohith.webrtc.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.rohith.webrtc.controller.Reciever;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Slf4j
@EnableScheduling
public class RedisConfig {

    @Autowired
    private SimpMessagingTemplate template;

    @Value("${redis.redisURI}")
    private String redisURI;

    @Value("${websocket.topic}")
    private String websocketTopic;

    @Bean
    JedisConnectionFactory jedisConnectionFactory() throws URISyntaxException {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        URI uri = new URI(redisURI);

        String password = uri.getUserInfo().substring(8);
        String hostname = uri.getHost();
        int port        = uri.getPort();

        config.setHostName(hostname);
        config.setPort(port);
        config.setPassword(password);

        return new JedisConnectionFactory(config);
    }

    @Bean
    ChannelTopic topic(){
        return new ChannelTopic(websocketTopic);
    }

    @Bean(name = "customRedisTemplate")
    RedisTemplate<String, Game> redisTemplate() throws URISyntaxException {
        final RedisTemplate<String, Game> template = new RedisTemplate<String, Game>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<Game>(Game.class));
        template.setKeySerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(){
        return new MessageListenerAdapter(new Reciever(template));
    }

    @Bean
    public RedisMessageListenerContainer redisContainer() throws URISyntaxException {
        RedisMessageListenerContainer container  = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        container.addMessageListener(messageListenerAdapter(),topic());
        return container;
    }

}

