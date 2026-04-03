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

@Service
public class BinanceStreamService {
    private final LinkedOrderQueue<TradeOrder> memoryBuffer = new LinkedOrderQueue<>();
    private final TradeOrderRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public BinanceStreamService(TradeOrderRepository repository) {
        this.repository = repository;
    }

    //Connect to Binance on Application Startup
    @PostConstruct
    @SuppressWarnings("resource")
    public void connectedToBinance() {
        HttpClient client = HttpClient.newHttpClient();
        //BTC/USDT live trade stream
        String binanceUrl = "wss://stream.binance.com:9443/ws/btcusdt@trade";

        client.newWebSocketBuilder().buildAsync(URI.create(binanceUrl), new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                System.out.println("CONNECTED TO BINANCE WEBSOCKET: BTC/USDT");
                WebSocket.Listener.super.onOpen(webSocket);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    //Parse the Binance Json payload
                    JsonNode node = mapper.readTree(data.toString());
                    double price = node.get("p").asDouble();
                    double quantity = node.get("q").asDouble();
                    boolean isBuyerMaker = node.get("m").asBoolean();
                    String type = isBuyerMaker ? "SELL" : "BUY";

                    //Create order and enqueue immediately into fast memory
                    TradeOrder order = new TradeOrder("BTC/USDT", price, quantity, type);
                    memoryBuffer.enqueue(order);
                } catch (Exception e) {
                    System.err.println("Error parsing trade: " + e.getMessage());
                }

                //Request the next message
                webSocket.request(1);
                return null;
            }
        });
    }

    //Background worker flushing memory to database every 1 second
    @Scheduled(fixedRate = 1000)
    public void processQueueToDatabase() {
        int processedCount = 0;

        //Dequeue and save until memory is empty
        while(!memoryBuffer.isEmpty()) {
            TradeOrder order = memoryBuffer.dequeue();

            if (order != null) {
                repository.save(order);
                processedCount++;
            }
        }

        if (processedCount > 0) {
            System.out.println("Flushed " + processedCount + " trades to persistence SQL Database");
        }
    }
}
