package com.omisys.user.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.user.domain.model.vo.UserRole;
import com.omisys.user.presentation.request.UserRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_user")
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private BigDecimal point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<Address> addresses;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<PointHistory> pointHistories;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, optional = false)
    private UserTier userTier;

    @Column
    private Boolean isDeleted;

    public static User create(UserRequest.Create request, String encodedPassword) {
        return User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .point(BigDecimal.ZERO)
                .email(request.getEmail())
                .role(request.getRole())
                .isDeleted(false)
                .build();
    }

    public void updatePoint(BigDecimal point) {
        this.point = point;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void delete(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
