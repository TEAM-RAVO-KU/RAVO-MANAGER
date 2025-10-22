package org.ravo.client.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "transactions")
public class TransactionRecord {

    public enum Type { DEPOSIT, WITHDRAW, TRANSFER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 멱등성 보장용 요청 ID */
    @Column(name = "request_id", length = 100, nullable = false, unique = true)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    private User toUser;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
