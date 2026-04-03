package com.tradeflow.engine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_trades")
public class TradeOrder implements Comparable<TradeOrder> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticker;
    private double price;
    private double quantity;
    private String type;
    private LocalDateTime timestamp = LocalDateTime.now();

    //Constructors
    public TradeOrder() {}
    public TradeOrder(String ticker, double price, double quantity, String type) {
        this.ticker = ticker;
        this.price = price;
        this.quantity = quantity;
        this.type = type;
    }

    //Getters and Setters
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

    @Override
    public int compareTo(TradeOrder other) {
        return Double.compare(other.price, this.price);
    }
}
