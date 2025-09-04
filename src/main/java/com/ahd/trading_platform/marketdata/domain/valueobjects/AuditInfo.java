package com.ahd.trading_platform.marketdata.domain.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * Embeddable audit information value object.
 * Provides standard auditing fields for all entities in the market data module.
 * 
 * This is a value object that encapsulates audit concerns and can be embedded
 * in any entity that needs audit tracking.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditInfo {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    /**
     * Checks if this entity has been persisted (has creation timestamp).
     */
    public boolean isPersisted() {
        return createdAt != null;
    }
    
    /**
     * Checks if this entity has been modified after creation.
     */
    public boolean isModified() {
        return updatedAt != null && createdAt != null && 
               updatedAt.isAfter(createdAt);
    }
    
    /**
     * Gets the age of this entity in milliseconds since creation.
     */
    public long getAgeInMillis() {
        return createdAt != null ? 
            Instant.now().toEpochMilli() - createdAt.toEpochMilli() : 0;
    }
}