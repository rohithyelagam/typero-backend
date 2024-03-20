package com.rohith.typero.controller;

import com.rohith.typero.model.ExitGame;
import com.rohith.typero.model.Player;
import com.rohith.typero.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/typero")
public class WebSocketMsgHandler {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private Publisher publisher;

    @Autowired
    private RedisMessageListenerContainer redisContainer;

    @Autowired
    private MessageListenerAdapter messageListenerAdapter;

    @Autowired
    private RedisTemplate<String, Game> redisTemplate;

    @GetMapping("/getGames")
    public ResponseEntity<List<Game>> getGames() throws Exception {
        List<Game> values = new ArrayList<>();
        try{
            Set<String> keysList = redisTemplate.keys("games:*");
            for(String x:keysList){
                if(!ObjectUtils.isEmpty(redisTemplate.opsForValue().get(x))){
                    Game temp = redisTemplate.opsForValue().get(x);
                    values.add(temp);
                }else{
                    values.add(new Game());
                }
            }
        }catch (Exception e){
            throw e;
        }
        return new ResponseEntity<>(values,HttpStatus.OK);
    }

    @PostMapping("/getGame")
    public ResponseEntity<Game> getGame(@RequestBody ExitGame req) throws Exception {
        try{
            String gid = req.getGid();
            Game game = redisTemplate.opsForValue().get("games:"+gid);
            return new ResponseEntity<>(game,HttpStatus.OK);
        }catch (Exception e){
            throw e;
        }
    }

    @PostMapping("/exit-game")
    public Game exitGame(@RequestBody Game req) throws Exception{
        try{
            String gid = req.getGid();
            String cid = req.getPlayers().get(0).getCid();
            Game game = redisTemplate.opsForValue().get("games:"+gid);
            if(!ObjectUtils.isEmpty(game)){
                Optional<Player> player = game.getPlayers().stream().filter(e -> e.getCid().equals(cid)).collect(Collectors.toList()).stream().findFirst();
                if(!ObjectUtils.isEmpty(player)){
                    for(Player temp:game.getPlayers()){
                        if(temp.getCid().equals(player.get().getCid())){
                            game.getPlayers().remove(temp);
                            game.setNo_of_players(game.getNo_of_players()-1L);
                            if(game.getNo_of_players() == 0L){
                                redisTemplate.opsForValue().getAndDelete("games:"+gid);
                            }else{
                                if(game.isGame_started()){
                                    Long playing_cnt = game.getPlayers().stream().filter(e -> e.getPlaying().equals(Boolean.TRUE)).count();
                                    if(playing_cnt == 0L){
                                        game.setStarted_time(null);
                                        game.setGame_started(Boolean.FALSE);
                                    }
                                }
                                redisTemplate.opsForValue().set("games:"+gid,game);
                            }
                            break;
                        }
                    }
                    simpMessagingTemplate.convertAndSend("/my-broker/"+gid,game);
                    return game;
                }else{
                    throw new Exception("Player does not exists!");
                }
            }else{
                throw new Exception("Game does not exists!");
            }
        }catch (Exception e){
            log.error("exception found during exitGame : ",e.getMessage());
            e.printStackTrace();
            throw  e;
        }
    }

    @SubscribeMapping("/subscribe/{gid}/{cid}/{chars}")
    public ResponseEntity<Game> subscribe(@DestinationVariable String gid, @DestinationVariable String cid,@DestinationVariable String chars) throws Exception {
        try{
            createMessageListener(gid);
            Game game = redisTemplate.opsForValue().get("games:"+gid);
            if(!ObjectUtils.isEmpty(game)){
                if(!game.isGame_started()){
                    Optional<Player> player = game.getPlayers().stream().filter(e -> e.getCid().equals(cid)).findFirst();
                    if(ObjectUtils.isEmpty(player)) {
                        Player new_player = new Player();
                        new_player.setCid(cid);
                        game.getPlayers().add(new_player);
                        game.setNo_of_players(game.getNo_of_players() + 1L);
                        redisTemplate.opsForValue().set("games:" + gid, game);
                    }
                }
                simpMessagingTemplate.convertAndSend("/my-broker/"+gid,game);
                return new ResponseEntity<>(game,HttpStatus.OK);
            }else{
                List<Player> players = new ArrayList<>();
                players.add(new Player(cid,Boolean.FALSE,0L,0L,0L,0L));
                Game new_game = new Game();
                new_game.setGid(gid);
                new_game.setPlayers(players);
                new_game.setNo_of_players(1L);
                new_game.setTotal_chars(Long.valueOf(chars));
                new_game.setGame_started(Boolean.FALSE);
                redisTemplate.opsForValue().set("games:"+gid,new_game);
                simpMessagingTemplate.convertAndSend("/my-broker/"+gid,new_game);
                return new ResponseEntity<>(new_game,HttpStatus.OK);
            }
        }catch (Exception e){
            throw e;
        }
    }

    public void createMessageListener(String gid){
        ThreadPoolTaskExecutor newExecutor = new ThreadPoolTaskExecutor();
        newExecutor.setCorePoolSize(10);
        newExecutor.setMaxPoolSize(20);
        newExecutor.initialize();
        redisContainer.setSubscriptionExecutor(newExecutor);
        if(!redisContainer.isRunning()){
            redisContainer.start();
        }
        redisContainer.addMessageListener(messageListenerAdapter,new ChannelTopic("payments/"+gid));
    }

    @MessageMapping("/send-msg/{gid}")
    public void greeting(@RequestBody Player player, @DestinationVariable String gid) throws Exception{
        log.info("Message from Player : {}",player);
        try{
             Game game = redisTemplate.opsForValue().get("games:"+gid);
            if(!ObjectUtils.isEmpty(game)){
                if(!game.isGame_started()){
                    throw new Exception("Game is not yet Started!");
                }
                Optional<Player> player1 = game.getPlayers().stream().filter(e -> e.getCid().equals(player.getCid())).findFirst();
                if(!ObjectUtils.isEmpty(player1)){
                    for(Player temp:game.getPlayers()){
                        if(temp.getCid().equals(player.getCid()) && game.isGame_started()){
                            game.getPlayers().remove(temp);
                            game.getPlayers().add(player);
                            break;
                        }
                    }
                    Long totalPlayers = game.getNo_of_players();
                    Long playingCount = game.getPlayers().stream().filter(e -> e.getPlaying().equals(Boolean.TRUE)).count();
                    Long watchingCount = game.getPlayers().stream().filter(e -> e.getWpm()  == -1).count();
                    Long completedCount = game.getPlayers().stream().filter(e -> e.getWpm() != -1).count();
                    if(playingCount == 0L){
                        if(completedCount > 0 ){
                            if(completedCount == totalPlayers - watchingCount){
                                // game completed.
                                game.setGame_completed(Boolean.TRUE);
                            }
                        }else{
                            // only players are watching
                            game.setGame_started(Boolean.FALSE);
                            game.setStarted_time(null);
                            game.setGame_completed(Boolean.FALSE);
                        }
                    }
                    redisTemplate.opsForValue().set("games:"+gid,game);
                    publisher.publish(game,gid);
                }else{
                    throw new Exception("Player "+player.getCid()+" does not exists!");
                }
            }else{
                throw new Exception("Game "+gid+" does not exists!");
            }
        }catch (Exception e){
            log.error("exception during sending message! : ",e.getMessage());
            throw e;
        }
    }

    @PostMapping("/start-game")
    public Game startGame(@RequestBody Game gm) throws Exception{
        try{
            Game game = redisTemplate.opsForValue().get("games:"+gm.getGid());
            if(!ObjectUtils.isEmpty(game)){
                game.setGame_started(gm.isGame_started());
                game.setGame_completed(gm.isGame_completed());
                game.setStarted_time(gm.getStarted_time());
                for(Player p:game.getPlayers()){
                    p.setPlaying(Boolean.TRUE);
                    p.setWpm(0L);
                    p.setTime_taken(0L);
                    p.setCorrect_chars(0L);
                }
                redisTemplate.opsForValue().set("games:"+game.getGid(),game);
                publisher.publish(game,gm.getGid());
                return game;
            }else{
                throw new Exception("Game "+game.getGid()+" does not exists!");
            }
        }catch (Exception e){
            log.error("exception during starting game! : ",e.getMessage());
            throw e;
        }
    }

}
