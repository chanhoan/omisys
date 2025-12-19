package com.omisys.slack.slack_dto.dto;

import lombok.Getter;
import lombok.Setter;

public class MessageInternalDto {

    @Getter
    @Setter
    public static class Create {

        private String receiverEmail;
        private String message;

    }

}
