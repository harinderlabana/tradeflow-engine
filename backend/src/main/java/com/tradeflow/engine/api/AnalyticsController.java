package com.tradeflow.engine.api;

import com.tradeflow.engine.model.TradeOrder;
import com.tradeflow.engine.model.TradeOrderRepository;
import com.tradeflow.engine.service.BinanceStreamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TRADEFLOW ENGINE - TELEMETRY & ANALYTICS API
 * --------------------------------------------
 * This REST Controller acts as the read-only gateway for the React frontend.
 * It strictly separates the read operations (fetching history) from the
 * high-throughput write operations (Binance WebSocket ingestion) to prevent
 * API polling from blocking the critical execution thread.
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:5173") // Permits Cross-Origin requests from the local Vite dev server
public class AnalyticsController {

    private final TradeOrderRepository repository;
    private final BinanceStreamService streamService;

    /**
     * Constructor Injection
     * Utilizes Spring's Dependency Injection (DI) to supply the required services.
     * This ensures the Controller remains stateless and testable.
     */
    public AnalyticsController(TradeOrderRepository repository, BinanceStreamService streamService) {
        this.repository = repository;
        this.streamService = streamService;
    }

    /**
     * HISTORICAL STATE RECOVERY
     * -------------------------
     * Serves the initial batch of historical trades when the React client connects.
     * The frontend uses this to pre-calculate the OHLC (Open, High, Low, Close) candles
     * before the live WebSocket stream takes over.
     * * Algorithmic Note:
     * Relies on the database having a B-Tree index on the "timestamp" column.
     * With an index, retrieval is O(log N). Without an index, this triggers a
     * full table scan O(N), which would cause severe latency as the table grows.
     */
    @GetMapping("/recent")
    public List<TradeOrder> getRecentTrades() {
        // Fetches the most recent 1,000 trades to populate the client's Base Resolution Buffer
        Page<TradeOrder> page = repository.findAll(
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return page.getContent();
    }

    /**
     * SYSTEM TELEMETRY (HEALTH CHECK)
     * -------------------------------
     * Provides real-time metrics on the internal ingestion engine.
     * * Algorithmic Note:
     * streamService.getQueueSize() must be an O(1) operation. The custom LinkedOrderQueue
     * maintains an internal size counter rather than traversing the linked nodes O(N)
     * to count them, ensuring this endpoint can be polled every 1,000ms without CPU overhead.
     */
    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        return Map.of(
                "status", "LIVE",
                "queueDepth", streamService.getQueueSize(), // O(1) Queue Size Inspection
                "targetExchange", "Binance WS"
        );
    }
}