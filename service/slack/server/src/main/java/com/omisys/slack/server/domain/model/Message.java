package com.omisys.slack.server.domain.model;

import com.omisys.slack.server.presentation.request.MessageRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_message")
@Builder(access = AccessLevel.PRIVATE)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @Column(name = "message")
    private String message;

    @Column(name = "send_at")
    private LocalDateTime sendAt;

    public static Message create(MessageRequest.Create request) {

        return Message.builder()
                .receiverEmail(request.getReceiverEmail())
                .message(request.getMessage())
                .sendAt(LocalDateTime.now())
                .build();

    }


}
