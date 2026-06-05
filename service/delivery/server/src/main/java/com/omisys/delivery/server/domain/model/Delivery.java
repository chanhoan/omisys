package com.omisys.delivery.server.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.delivery.server.domain.model.vo.Courier;
import com.omisys.delivery.server.domain.model.vo.DeliveryState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "P_DELIVERY")
@SQLRestriction("is_deleted = false")
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryState state;

    @Column(nullable = true, length = 50)
    @Enumerated(EnumType.STRING)
    private Courier courier;

    @Column(nullable = true, length = 100)
    private String invoiceNumber;

    @Column(nullable = true, length = 100)
    private String recipient;

    @Column(nullable = true, length = 100)
    private String phoneNumber;

    @Column(nullable = true, length = 255)
    private String zipcode;

    @Column(nullable = true, length = 255)
    private String shippingAddress;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DeliveryTrackingHistory> trackingHistories = new ArrayList<>();

    public static Delivery create(Long orderId, Long userId) {
        return Delivery.builder()
                .orderId(orderId)
                .userId(userId)
                .state(DeliveryState.READY_FOR_SHIPMENT)
                .build();
    }

    public void startShipping(Courier courier, String invoiceNumber) {
        this.state.validateTransitionTo(DeliveryState.SHIPPING);
        this.courier = courier;
        this.invoiceNumber = invoiceNumber;
        this.state = DeliveryState.SHIPPING;
    }

    public void complete() {
        this.state.validateTransitionTo(DeliveryState.DELIVERED);
        this.state = DeliveryState.DELIVERED;
    }

    public void cancel() {
        this.state.validateTransitionTo(DeliveryState.CANCELED);
        this.state = DeliveryState.CANCELED;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void addTrackingHistory(DeliveryTrackingHistory history) {
        this.trackingHistories.add(history);
    }
}
