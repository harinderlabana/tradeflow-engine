package com.tradeflow.engine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * TRADEFLOW ENGINE - CORE DOMAIN MODEL
 * ------------------------------------
 * This JPA Entity represents the atomic unit of data within the system: a single market tick.
 * It serves a dual purpose: acting as the rapid-deserialization target for the incoming Binance
 * WebSocket JSON, and acting as the ORM (Object-Relational Mapping) entity for PostgreSQL persistence.
 */
@Entity
@Table(name = "crypto_trades")
public class TradeOrder implements Comparable<TradeOrder> {

    /**
     * Surrogate Primary Key.
     * Using GenerationType.IDENTITY delegates ID generation directly to the PostgreSQL database (SERIAL).
     * System Design Note: This avoids distributed locking mechanisms at the application level,
     * which is critical for maintaining high throughput during massive insertion spikes.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;

    // In a strict financial production environment, BigDecimal would be used to prevent
    // floating-point precision loss. Double is used here for raw computational speed in memory.
    private double price;
    private double quantity;
    private String type;

    // Captures nanosecond-level precision from the JVM, essential for accurate
    // Time-Series Bucketing when processing 100+ trades per second.
    private LocalDateTime timestamp = LocalDateTime.now();

    // --- Constructors ---

    // JPA requires a no-args constructor for reflection-based instantiation
    public TradeOrder() {}

    public TradeOrder(String ticker, double price, double quantity, String type) {
        this.ticker = ticker;
        this.price = price;
        this.quantity = quantity;
        this.type = type;
    }

    // --- Getters ---
    // Note: Setters are deliberately omitted to enforce Immutability after creation.
    // Immutable objects are inherently thread-safe, preventing race conditions in a multi-threaded HFT environment.

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getType() {
        return type;
    }

    /**
     * DATA STRUCTURE INTEGRATION (Comparable)
     * ---------------------------------------
     * Implements Comparable to define a "natural ordering" for algorithmic sorting.
     * By comparing 'price' descending, these objects can be natively inserted into
     * a PriorityQueue (Min/Max Heap) or a TreeMap to mathematically model an Order Book.
     * Time Complexity: Allows standard Java Collections to sort these objects in O(N log N) time,
     * or maintain a continuously sorted stream in O(log N) insertion time.
     */
    @Override
    public int compareTo(TradeOrder other) {
        return Double.compare(other.price, this.price); // Descending order
    }
}