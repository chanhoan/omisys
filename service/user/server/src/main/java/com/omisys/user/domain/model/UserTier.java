package com.omisys.user.domain.model;

import com.omisys.common.domain.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_user_tier")
@Entity
public class UserTier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_tier_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    @Column(name = "cumulative_amount", nullable = false)
    private Long cumulativeAmount;

    public static UserTier create(User user, Tier defaultTier) {
        return UserTier.builder()
                .user(user)
                .tier(defaultTier)
                .cumulativeAmount(0L)
                .build();
    }

    public void update(Tier tier) {
        this.tier = tier;
    }
}
