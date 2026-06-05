package com.omisys.delivery.server.domain.model;

import com.omisys.delivery.server.domain.model.vo.DeliveryState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "P_DELIVERY_TRACKING")
public class DeliveryTrackingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryState state;

    @Column(nullable = true, length = 255)
    private String memo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public static DeliveryTrackingHistory of(Delivery delivery, DeliveryState state, String memo) {
        return DeliveryTrackingHistory.builder()
                .delivery(delivery)
                .state(state)
                .memo(memo)
                .build();
    }
}
