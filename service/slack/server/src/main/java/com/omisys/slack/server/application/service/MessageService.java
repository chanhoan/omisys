package com.omisys.slack.server.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.slack.server.domain.model.Message;
import com.omisys.slack.server.domain.repository.MessageRepository;
import com.omisys.slack.server.exception.MessageErrorCode;
import com.omisys.slack.server.exception.MessageException;
import com.omisys.slack.server.presentation.request.MessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${SLACK_TOKEN}")
    private String slackToken;

    private static final String SLACK_URL = "https://slack.com/api/chat.postMessage";

    public void sendMessage(MessageRequest.Create messageRequest) {

        String slackUserId = getSlackUserId(messageRequest.getReceiverEmail());

        messageRepository.save(Message.create(messageRequest));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(slackToken);

        String requestBody =
                String.format(
                        "{\"channel\":\"%s\", \"text\":\"%s\"}", slackUserId, messageRequest.getMessage());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        restTemplate.postForEntity(SLACK_URL, entity, String.class);
    }

    public String getSlackUserId(String email) {

        String url = "https://slack.com/api/users.lookupByEmail";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(slackToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            String apiUrlWithParams = url + "?email=" + email;
            log.info("apiUrlWithParams = {}", apiUrlWithParams);

            ResponseEntity<String> response =
                    restTemplate.exchange(apiUrlWithParams, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("ok").asBoolean()) {
                    return root.path("user").path("id").asText();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MessageException(MessageErrorCode.INVALID_PARAMETER);
        }
        throw new MessageException(MessageErrorCode.USER_NOT_FOUND);
    }
}
