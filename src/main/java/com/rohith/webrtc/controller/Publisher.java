package com.rohith.webrtc.controller;

import com.rohith.webrtc.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Publisher {

    @Autowired
    @Qualifier("customRedisTemplate")
    private RedisTemplate<String, Game> template;
    @Autowired
    private ChannelTopic topic;

    @Autowired
    private ApplicationContext context;

    @PostMapping("/publish/{gid}")
    public String publish(@RequestBody Game game, @DestinationVariable String gid){
        template.convertAndSend(topic.getTopic()+"/"+gid, game);
        return "event published!!";
    }

}
