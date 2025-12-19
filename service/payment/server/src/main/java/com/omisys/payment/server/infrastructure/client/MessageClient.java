package com.omisys.payment.server.infrastructure.client;

import com.omisys.slack.slack_dto.dto.MessageInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "slack")
public interface MessageClient {

    @PostMapping("/internal/message")
    void sendMessage(@RequestBody MessageInternalDto.Create messageRequest);

}
