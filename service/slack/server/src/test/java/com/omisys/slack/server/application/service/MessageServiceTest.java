package com.omisys.slack.server.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.slack.server.domain.model.Message;
import com.omisys.slack.server.domain.repository.MessageRepository;
import com.omisys.slack.server.exception.MessageErrorCode;
import com.omisys.slack.server.exception.MessageException;
import com.omisys.slack.server.presentation.request.MessageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;

    // MessageService 내부에 "new RestTemplate()"이 박혀있어서, 테스트에서 교체 주입해야 함
    @Mock private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MessageService newServiceWithToken(String token) {
        MessageService service = new MessageService(messageRepository, objectMapper);

        // @Value("${SLACK_TOKEN}") 필드는 테스트에서 직접 주입
        ReflectionTestUtils.setField(service, "slackToken", token);

        // private final RestTemplate restTemplate = new RestTemplate(); 를 mock으로 교체
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        return service;
    }

    @Test
    @DisplayName("sendMessage 성공: lookupByEmail → DB 저장 → chat.postMessage 호출(채널/텍스트/Authorization 포함)")
    void sendMessage_success_flow() throws Exception {
        // given
        MessageService service = newServiceWithToken("test-token");

        MessageRequest.Create req = new MessageRequest.Create();
        req.setReceiverEmail("user@omisys.com");
        req.setMessage("checkoutUrl 입니다.");

        // 1) Slack users.lookupByEmail 응답 스텁 (ok=true, user.id=U123)
        String lookupBody = """
                {"ok": true, "user": {"id": "U123"}}
                """;
        when(restTemplate.exchange(
                contains("https://slack.com/api/users.lookupByEmail"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(lookupBody));

        // 2) Slack chat.postMessage 호출은 성공했다고 가정
        when(restTemplate.postForEntity(
                eq("https://slack.com/api/chat.postMessage"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        // when
        service.sendMessage(req);

        // then
        // (A) 메시지 저장 검증: Message.create(request)가 잘 만들어져 save에 전달되는지
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message saved = messageCaptor.getValue();
        assertThat(saved.getReceiverEmail()).isEqualTo("user@omisys.com");
        assertThat(saved.getMessage()).isEqualTo("checkoutUrl 입니다.");
        assertThat(saved.getSendAt()).isNotNull();

        // (B) lookupByEmail 호출에서 Authorization Bearer 세팅 되었는지 검증
        ArgumentCaptor<HttpEntity> lookupEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                contains("users.lookupByEmail?email=user@omisys.com"),
                eq(HttpMethod.GET),
                lookupEntityCaptor.capture(),
                eq(String.class)
        );

        HttpHeaders lookupHeaders = lookupEntityCaptor.getValue().getHeaders();
        assertThat(lookupHeaders.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");

        // (C) chat.postMessage 호출에서 body(channel/text) + Authorization Bearer 검증
        ArgumentCaptor<HttpEntity> postEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://slack.com/api/chat.postMessage"),
                postEntityCaptor.capture(),
                eq(String.class)
        );

        HttpEntity<String> postEntity = postEntityCaptor.getValue();
        assertThat(postEntity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");
        assertThat(postEntity.getBody()).contains("\"channel\":\"U123\"");
        assertThat(postEntity.getBody()).contains("\"text\":\"checkoutUrl 입니다.\"");
    }

    @Test
    @DisplayName("getSlackUserId 실패: ok=false 또는 사용자 정보 없으면 USER_NOT_FOUND")
    void getSlackUserId_fail_user_not_found() throws Exception {
        // given
        MessageService service = newServiceWithToken("test-token");

        String lookupBody = """
                {"ok": false}
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(lookupBody));

        // when & then
        assertThatThrownBy(() -> service.getSlackUserId("no-user@omisys.com"))
                .isInstanceOf(MessageException.class)
                .hasMessage(MessageErrorCode.USER_NOT_FOUND.getStatus().name());
    }

    @Test
    @DisplayName("getSlackUserId 실패: Slack 응답 파싱/통신 예외면 INVALID_PARAMETER")
    void getSlackUserId_fail_invalid_parameter() {
        // given
        MessageService service = newServiceWithToken("test-token");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        // when & then
        assertThatThrownBy(() -> service.getSlackUserId("user@omisys.com"))
                .isInstanceOf(MessageException.class)
                .hasMessage(MessageErrorCode.INVALID_PARAMETER.getStatus().name());
    }
}
