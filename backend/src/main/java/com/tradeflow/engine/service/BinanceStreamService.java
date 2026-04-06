package com.tradeflow.engine.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tradeflow.engine.model.TradeOrder;
import com.tradeflow.engine.model.TradeOrderRepository;
import com.tradeflow.engine.util.LinkedOrderQueue;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

/**
 * TRADEFLOW ENGINE - INGESTION PIPELINE & ASYNC BUFFER
 * ----------------------------------------------------
 * This service implements the "Producer-Consumer" architectural pattern.
 * It solves the primary bottleneck of High-Frequency Trading (HFT) systems:
 * Database I/O is drastically slower than network ingestion.
 * By decoupling the WebSocket listener (Producer) from the Database writer (Consumer)
 * using an O(1) in-memory queue, the engine can ingest thousands of ticks per second
 * without thread-blocking or dropping network frames.
 */
@Service
public class BinanceStreamService {

    // The core memory structure. Instantiated locally for maximum thread accessibility.
    private final LinkedOrderQueue<TradeOrder> memoryBuffer = new LinkedOrderQueue<>();
    private final TradeOrderRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public BinanceStreamService(TradeOrderRepository repository) {
        this.repository = repository;
    }

    /**
     * PRODUCER THREAD: Network Ingestion
     * ----------------------------------
     * Initializes a non-blocking, asynchronous WebSocket connection to Binance
     * the moment the Spring application context finishes loading.
     */
    @PostConstruct
    @SuppressWarnings("resource")
    public void connectedToBinance() {
        HttpClient client = HttpClient.newHttpClient();
        // BTC/USDT live raw trade stream
        String binanceUrl = "wss://stream.binance.com:9443/ws/btcusdt@trade";

        client.newWebSocketBuilder().buildAsync(URI.create(binanceUrl), new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                System.out.println("CONNECTED TO BINANCE WEBSOCKET: BTC/USDT");
                WebSocket.Listener.super.onOpen(webSocket);
            }

            /**
             * The Critical Path: This method fires 10-100+ times per second.
             * Rule of HFT: Absolutely NO heavy computation or DB calls are allowed in this thread.
             */
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    // Fast JSON deserialization
                    JsonNode node = mapper.readTree(data.toString());
                    double price = node.get("p").asDouble();
                    double quantity = node.get("q").asDouble();

                    // Binance logic: if the buyer is the maker, the trade was a SELL into the bid
                    boolean isBuyerMaker = node.get("m").asBoolean();
                    String type = isBuyerMaker ? "SELL" : "BUY";

                    TradeOrder order = new TradeOrder("BTC/USDT", price, quantity, type);

                    // O(1) constant-time memory insertion. The thread is immediately freed.
                    memoryBuffer.enqueue(order);
                } catch (Exception e) {
                    System.err.println("Error parsing trade: " + e.getMessage());
                }

                // Explicitly request the next message to maintain backpressure control
                webSocket.request(1);
                return null;
            }
        });
    }

    /**
     * CONSUMER THREAD: Asynchronous Persistence
     * -----------------------------------------
     * Runs on a separate background thread pool, completely isolated from the WebSocket.
     * Fires every 1000ms to "sweep" the fast-memory queue and flush it to cold storage (SQL).
     */
    @Scheduled(fixedRate = 1000)
    public void processQueueToDatabase() {
        int processedCount = 0;

        // Drain the queue until empty.
        // O(1) dequeue time complexity prevents the loop from lagging behind the live market.
        while(!memoryBuffer.isEmpty()) {
            TradeOrder order = memoryBuffer.dequeue();

            if (order != null) {
                // System Design Note: In a massive scale production environment,
                // this loop would build a List and use repository.saveAll(batch)
                // to execute a single bulk SQL INSERT rather than N individual inserts.
                repository.save(order);
                processedCount++;
            }
        }

        if (processedCount > 0) {
            System.out.println("Flushed " + processedCount + " trades to persistence SQL Database");
        }
    }

    /**
     * TELEMETRY HOOK
     * Returns the exact length of the buffer in O(1) time.
     * Crucial for the frontend UI to monitor if the Consumer thread is falling behind the Producer.
     */
    public int getQueueSize() {
        return memoryBuffer.getSize();
    }
}