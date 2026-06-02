package com.amis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String plan;       // FREE, PREMIUM, FAMILY
    @Column(nullable = false) private Double amount;
    @Column(nullable = false) private String status;     // PENDING, SUCCESS, FAILED
    @Column private String transactionId;
    @Column private String cardLastFour;
    @Column private String cardBrand;                    // VISA, MASTERCARD, AMEX
    @Column private String failureReason;
    @Column private LocalDateTime paidAt;

    public Payment() {}
    public Payment(Long userId, String plan, Double amount, String cardLastFour, String cardBrand) {
        this.userId = userId; this.plan = plan; this.amount = amount;
        this.cardLastFour = cardLastFour; this.cardBrand = cardBrand;
        this.status = "PENDING";
    }

    public Long getUserId()                   { return userId; }
    public String getPlan()                   { return plan; }
    public Double getAmount()                 { return amount; }
    public String getStatus()                 { return status; }
    public void setStatus(String s)           { this.status = s; }
    public String getTransactionId()          { return transactionId; }
    public void setTransactionId(String t)    { this.transactionId = t; }
    public String getCardLastFour()           { return cardLastFour; }
    public String getCardBrand()              { return cardBrand; }
    public String getFailureReason()          { return failureReason; }
    public void setFailureReason(String f)    { this.failureReason = f; }
    public LocalDateTime getPaidAt()          { return paidAt; }
    public void setPaidAt(LocalDateTime t)    { this.paidAt = t; }
}
