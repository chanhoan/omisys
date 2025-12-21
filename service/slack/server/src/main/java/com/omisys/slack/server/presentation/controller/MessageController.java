package com.omisys.slack.server.presentation.controller;

import com.omisys.slack.server.application.service.MessageService;
import com.omisys.slack.server.presentation.request.MessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/message")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("")
    public void sendMessage(@RequestBody MessageRequest.Create messageRequest) {
        messageService.sendMessage(messageRequest);
    }
}
