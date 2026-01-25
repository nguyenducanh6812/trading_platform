package com.ahd.trading_platform.portfolio.infrastructure.persistence.repositories;

import com.ahd.trading_platform.portfolio.domain.valueobjects.PortfolioStatus;
import com.ahd.trading_platform.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Portfolio entities.
 */
@Repository
public interface PortfolioJpaRepository extends JpaRepository<PortfolioEntity, Long> {

    List<PortfolioEntity> findByUserId(String userId);

    List<PortfolioEntity> findByUserIdAndStatus(String userId, PortfolioStatus status);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PortfolioEntity p WHERE p.userId = :userId AND p.name = :name")
    boolean existsByUserIdAndName(@Param("userId") String userId, @Param("name") String name);

    @Query("SELECT DISTINCT p FROM PortfolioEntity p " +
           "LEFT JOIN FETCH p.portfolioInstruments " +
           "WHERE p.id = :id")
    Optional<PortfolioEntity> findByIdWithPositionsAndTrades(@Param("id") Long id);
}
