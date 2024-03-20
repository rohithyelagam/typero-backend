package com.rohith.typero.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohith.typero.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Service
public class Reciever implements MessageListener {

    private final SimpMessagingTemplate template;

    public Reciever(SimpMessagingTemplate template){
        this.template = template;
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SendTo("/my-broker/{gid}")
    public void onMessage(Message message, byte[] pattern) {
        try{
            Game game = objectMapper.readValue(message.getBody(), Game.class);
            log.info("Message from Reddis : {}", game);
            template.convertAndSend("/my-broker/"+ game.getGid(), game);
        }catch (Exception e){
            log.error("error while parsing message ",e.getMessage());
            e.printStackTrace();
        }
    }

}
