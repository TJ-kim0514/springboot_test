package com.hexalab.testweb.security.jwt.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// entity 겸 dto 역할 두가지를 수행
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name="TB_REFRESH_TOKENS")
public class RefreshToken {

    @Id
    @Column(length=36)
    private UUID id;    // import java.util.UUID

//    @ManyToOne(fetch = FetchType.LAZY)
//    @Column(name="USERID", referencedColumnName="USERID", nullable = false)
//    private String userId;

    @Column(name="USERID", nullable = false)
    private String userId;
    @Column(name="TOKEN_VALUE", nullable = false)
    private String tokenValue;
    @Column(name="ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;
    @Column(name="EXPIRES_IN", nullable = false)
    private Long expiresIn;
    @Column(name="EXPIRATION_DATE", nullable = false)
    private LocalDateTime expirationDate;
    @Column(name="MEMBER_AGENT", length = 255)
    private String memberAgent;
    @Column(name="STATUS", length = 50)
    private String status;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if(issuedAt == null) issuedAt = now;
        if(expirationDate == null) expirationDate = now.plusSeconds(expiresIn / 1000);
        // 예를 들어 expiresIn 이 밀리초 단위라면, 날짜로 변환함
    }
}
