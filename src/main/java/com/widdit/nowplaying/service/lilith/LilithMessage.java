package com.widdit.nowplaying.service.lilith;

import lombok.Data;

@Data
public class LilithMessage {

    private String type;
    private String id;
    private String title;
    private String artist;
    private Boolean playing;
    private String lrc;
    private String trans;
    private Double duration;
    private Double elapsed;
}
