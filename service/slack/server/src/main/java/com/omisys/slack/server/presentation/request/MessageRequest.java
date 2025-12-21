package com.omisys.slack.server.presentation.request;

import lombok.Getter;
import lombok.Setter;

public class MessageRequest {

    @Getter
    @Setter
    public static class Create {

        private String receiverEmail;
        private String message;

    }

}
