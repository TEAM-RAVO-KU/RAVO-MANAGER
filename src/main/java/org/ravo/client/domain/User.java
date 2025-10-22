package org.ravo.client.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인용 아이디 */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    /** 계좌 번호 */
    @Column(name = "account_no", length = 40, nullable = false, unique = true)
    private String accountNo;

    @Column(nullable = false)
    private Long balance;

    /** DB 디폴트 없이 앱에서 세팅 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 갱신 시 앱에서 세팅 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.balance == null) this.balance = 0L;
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
