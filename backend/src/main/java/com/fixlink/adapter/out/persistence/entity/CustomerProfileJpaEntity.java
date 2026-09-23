package com.fixlink.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_profiles")
public class CustomerProfileJpaEntity extends BaseJpaEntity implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 20, nullable = false, unique = true)
    private String phone;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "membership_tier", length = 20, nullable = false)
    @Builder.Default
    private String membershipTier = "STANDARD";

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNewEntity = false;
    }

    public void setUser(UserJpaEntity user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.userId = user.getId();
        }
    }
}
