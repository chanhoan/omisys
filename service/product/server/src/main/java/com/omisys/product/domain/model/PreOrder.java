package com.omisys.product.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.product.presentation.request.PreOrderRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "P_PREORDER")
@NoArgsConstructor
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE p_preorder SET is_deleted = true where preorder_id = ?")
public class PreOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preorder_id")
    private Long preOrderId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String preOrderTitle;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private LocalDateTime releaseDateTime;

    @Column
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PreOrderState state = PreOrderState.INITIALIZED;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column
    @Builder.Default
    private boolean isPublic = true;

    @Column
    @Builder.Default
    private boolean isDeleted = false;

    public static PreOrder create(PreOrderRequest.Create request) {
        return PreOrder.builder()
                .productId(request.getProductId())
                .preOrderTitle(request.getPreOrderTitle())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .releaseDateTime(request.getReleaseDateTime())
                .availableQuantity(request.getAvailableQuantity())
                .build();
    }

    public void open() {
        this.state = PreOrderState.OPEN_FOR_ORDER;
    }

    public void cancel() {
        this.state = PreOrderState.CANCELED;
        this.isPublic = false;
    }

    public void update(PreOrderRequest.Update request) {
        this.productId = request.getProductId();
        this.preOrderTitle = request.getPreOrderTitle();
        this.startDateTime = request.getStartDateTime();
        this.endDateTime = request.getEndDateTime();
        this.releaseDateTime = request.getReleaseDateTime();
        this.availableQuantity = request.getAvailableQuantity();
    }
}
