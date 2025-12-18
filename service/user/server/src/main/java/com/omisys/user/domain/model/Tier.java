package com.omisys.user.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.user.presentation.request.TierRequest;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_tier")
@Entity
public class Tier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long thresholdPrice;

    public static Tier create(TierRequest.Create request) {
        return Tier.builder()
                .name(request.getName())
                .thresholdPrice(request.getThresholdPrice())
                .build();
    }

    public void update(TierRequest.Update request) {
        this.name = request.getName();
        this.thresholdPrice = request.getThresholdPrice();
    }

}
