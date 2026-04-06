package com.tradeflow.engine.model;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TRADEFLOW ENGINE - DATA ACCESS LAYER
 * ------------------------------------
 * This interface serves as the primary persistence mechanism for the application.
 * By extending JpaRepository, we leverage Spring Data's powerful abstraction over Hibernate/JPA.
 * * System Design Notes:
 * 1. Runtime Proxies: Spring dynamically generates the implementation of this interface at runtime.
 * This completely eliminates raw JDBC boilerplate, reducing potential points of failure and injection vulnerabilities.
 * 2. Transactional by Default: Inherited data-mutating methods (like save and saveAll) are
 * automatically wrapped in database transactions, ensuring ACID compliance even when the background
 * workers are flushing massive batches of trades.
 * 3. Pagination Support: JpaRepository inherently supports PagingAndSortingRepository. This is
 * architecturally critical for our AnalyticsController, as it prevents OutOfMemory (OOM) errors by
 * allowing us to fetch specific "pages" of the execution tape rather than dumping a 10-million row
 * table into application RAM.
 */
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    // No explicit methods needed.
    // Standard CRUD, pagination, and sorting logic are automatically injected by the Spring container.
}