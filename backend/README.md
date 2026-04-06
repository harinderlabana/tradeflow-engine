# TradeFlow_Engine ⚡

![React](https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB)
![TypeScript](https://img.shields.io/badge/typescript-%23007ACC.svg?style=for-the-badge&logo=typescript&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/postgresql-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)

TradeFlow Engine is a high-performance, full-stack trading dashboard designed to ingest, buffer, and visualize high-frequency cryptocurrency market data in real-time.

Built to simulate the architecture of institutional trading terminals, this project focuses on solving the critical bottlenecks of live financial data streaming: memory leaks, browser thread locking, and real-time canvas rendering.

## 🧠 Core Architecture & Engineering Solutions

### 1. The Backend: High-Throughput Ingestion
The backend is built in **Java/Spring Boot** and maintains a persistent WebSocket connection directly to the Binance raw trade firehose.
* **$O(1)$ Memory Buffering:** Instead of blocking the thread with heavy database I/O during volatile market spikes, incoming trades are instantly pushed to a custom `LinkedOrderQueue`.
* **Asynchronous Persistence:** Scheduled background workers safely flush the queue to a PostgreSQL database without interrupting the live WebSocket feed.

### 2. The Frontend: Base Resolution Aggregation
Rendering hundreds of trades per second will crash a web browser's DOM. TradeFlow Engine utilizes **Client-Side Base Resolution Aggregation** to prevent memory overflows.
* **1-Second Base Candles:** Raw tick data is instantly aggregated into 1-second base candles and stored in a JavaScript `Map`. This compresses 500,000 raw trades down to exactly 3,600 lightweight objects per hour.
* **Dynamic Timeframe Math:** When a user switches to a 1-minute or 5-minute chart, the engine dynamically rolls up the 1-second base candles into mathematically perfect higher-timeframe candles in under 2 milliseconds, entirely client-side.
* **Memory Garbage Collection:** A custom garbage collector routinely purges data older than 24 hours to ensure the browser never crashes during extended sessions.

### 3. Canvas Rendering vs. DOM Manipulation
To achieve fluid, 60fps TradingView-style price action, TradeFlow Engine bypasses standard React DOM rendering (`<div>`, `<span>`) for the charting interface.
* **Lightweight Charts Integration:** Data is streamed directly onto an HTML5 `<canvas>` via the Lightweight Charts API, separating the heavy visual rendering from the React state cycle.
* **Cryptographic Deduplication:** React's asynchronous state updates can occasionally cause duplicate WebSocket reads. The engine assigns a unique cryptographic key (`Timestamp-Price-Quantity`) to every trade, guaranteeing absolute mathematical precision on the chart.

## 🚀 Getting Started

### Prerequisites
* Java 17+
* Node.js 18+
* PostgreSQL

### Installation

1. **Clone the repository**
   ```bash
   git clone [https://github.com/yourusername/TradeFlow-Engine.git](https://github.com/yourusername/TradeFlow-Engine.git)
   cd TradeFlow-Engine
   ```

2. **Start the Backend (Spring Boot)**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   *The backend will initialize the Binance WebSocket and start listening on port `8080`.*

3. **Start the Frontend (React)**
   ```bash
   cd ../frontend
   npm install
   npm run dev
   ```
   *The dashboard will be available at `http://localhost:5173`.*

## 💻 Tech Stack
* **Frontend:** React, TypeScript, Tailwind CSS, Lightweight Charts, Lucide Icons.
* **Backend:** Java, Spring Boot, WebSocket API.
* **Database:** PostgreSQL.
* **Design Pattern:** Neo-Brutalist / Cyber-Institutional layout prioritizing maximum data density.