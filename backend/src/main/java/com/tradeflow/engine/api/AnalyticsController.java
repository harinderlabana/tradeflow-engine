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

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:5173")

public class AnalyticsController {

    private final TradeOrderRepository repository;
    private final BinanceStreamService streamService;

    //Inject the stream service alongside the repository
    public AnalyticsController(TradeOrderRepository repository, BinanceStreamService streamService) {
        this.repository = repository;
        this.streamService = streamService;
    }

    //Fetch the last 50 trades for the frontend UI
    @GetMapping("/recent")
    public List<TradeOrder> getRecentTrades() {
        Page<TradeOrder> page = repository.findAll(
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return page.getContent();
    }

    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        return Map.of(
                "status", "LIVE",
                "queueDepth", streamService.getQueueSize(),
                "targetExchange", "Binance WS"
        );
    }
}
