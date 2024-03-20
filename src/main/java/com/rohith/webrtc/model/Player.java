package com.rohith.webrtc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private String cid;
    private Boolean playing = false;
    private Long correct_chars = 0L;
    private Long wrong_chars = 0L;
    private Long time_taken = 0L;
    private Long wpm = 0L;
}
