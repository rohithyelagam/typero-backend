package com.rohith.webrtc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Game {
    private String gid;
    private Long no_of_players;
    private boolean game_started = Boolean.FALSE;
    private boolean game_completed = Boolean.FALSE;
    private String started_time;
    private Long total_chars;
    List<Player> players;
}
