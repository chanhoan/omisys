package com.omisys.user.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.user.domain.model.vo.PointHistoryType;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_point_history")
@Entity
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long orderId;

    @Column(nullable = false)
    private BigDecimal point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType type;

    public static PointHistory create(User user, PointHistoryDto request) {
        return PointHistory.builder()
                .user(user)
                .orderId(request.getOrderId())
                .point(request.getPoint())
                .type(PointHistoryType.from(request.getType()))
                .build();
    }
}
