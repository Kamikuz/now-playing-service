package com.widdit.nowplaying.service.lilith;

import lombok.Data;

@Data
public class LilithMessage {

    private String type;
    private String id;
    private String title;
    private String artist;
    private Boolean playing;
}
