package com.omisys.slack.server.presentation.response;

import com.omisys.slack.server.presentation.request.MessageRequest;
import lombok.Getter;
import lombok.Setter;

public class MessageResponse {

    @Getter
    @Setter
    public static class Create {

        private String receiverEmail;
        private String message;

        public Create(MessageRequest.Create messageRequest) {
            this.receiverEmail = messageRequest.getReceiverEmail();
            this.message = messageRequest.getMessage();
        }

    }

}
