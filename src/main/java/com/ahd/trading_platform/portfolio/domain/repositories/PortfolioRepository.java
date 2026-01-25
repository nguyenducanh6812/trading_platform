package com.ahd.trading_platform.portfolio.domain.repositories;

import com.ahd.trading_platform.portfolio.domain.entities.Portfolio;
import com.ahd.trading_platform.portfolio.domain.valueobjects.PortfolioStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Portfolio aggregate.
 * Defines persistence operations following DDD repository pattern.
 */
public interface PortfolioRepository {

    /**
     * Saves a portfolio (create or update).
     *
     * @param portfolio Portfolio to save
     * @return Saved portfolio with generated ID
     */
    Portfolio save(Portfolio portfolio);

    /**
     * Finds portfolio by ID.
     *
     * @param id Portfolio ID
     * @return Optional containing portfolio if found
     */
    Optional<Portfolio> findById(Long id);

    /**
     * Finds all portfolios for a user.
     *
     * @param userId User ID
     * @return List of user's portfolios
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Finds portfolios by user and status.
     *
     * @param userId User ID
     * @param status Portfolio status
     * @return List of portfolios matching criteria
     */
    List<Portfolio> findByUserIdAndStatus(String userId, PortfolioStatus status);

    /**
     * Finds portfolio by user and name.
     *
     * @param userId User ID
     * @param name Portfolio name
     * @return Optional containing portfolio if found
     */
    Optional<Portfolio> findByUserIdAndName(String userId, String name);

    /**
     * Checks if portfolio with given name exists for user.
     *
     * @param userId User ID
     * @param name Portfolio name
     * @return true if exists, false otherwise
     */
    boolean existsByUserIdAndName(String userId, String name);

    /**
     * Deletes a portfolio.
     *
     * @param portfolio Portfolio to delete
     */
    void delete(Portfolio portfolio);

    /**
     * Deletes portfolio by ID.
     *
     * @param id Portfolio ID
     */
    void deleteById(Long id);

    /**
     * Finds all portfolios.
     *
     * @return List of all portfolios
     */
    List<Portfolio> findAll();

    /**
     * Counts portfolios for a user.
     *
     * @param userId User ID
     * @return Number of portfolios
     */
    long countByUserId(String userId);
}
