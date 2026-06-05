package com.omisys.delivery.server.domain.model.vo;

import lombok.Getter;

@Getter
public enum Courier {

    CJ_LOGISTICS("CJ대한통운"),
    LOTTE_LOGISTICS("롯데택배"),
    HANJIN_LOGISTICS("한진택배"),
    LOGEN_LOGISTICS("로젠택배"),
    POST_OFFICE("우체국택배"),
    KGB_LOGISTICS("KGB택배"),
    ;

    private final String displayName;

    Courier(String displayName) {
        this.displayName = displayName;
    }
}
