package com.tradeflow.engine.api;

import com.tradeflow.engine.model.TradeOrder;
import com.tradeflow.engine.model.TradeOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:5173")

public class AnalyticsController {

    private final TradeOrderRepository repository;

    public AnalyticsController(TradeOrderRepository repository) {
        this.repository = repository;
    }

    //Fetch the last 50 trades for the frontend UI
    @GetMapping("/recent")
    public List<TradeOrder> getRecentTrades() {
        Page<TradeOrder> page = repository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return page.getContent();
    }
}
