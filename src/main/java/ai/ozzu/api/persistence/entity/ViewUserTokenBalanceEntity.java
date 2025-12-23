package ai.ozzu.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_user_token_balance")
public class ViewUserTokenBalanceEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "balance", nullable = false)
    private Long balance;

    public UUID getUserId() { return userId; }
    public Long getBalance() { return balance; }
}
