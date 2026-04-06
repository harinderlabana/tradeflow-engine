import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { createChart, ColorType, CandlestickSeries } from 'lightweight-charts';
import { Activity, Server, Cpu, Terminal, Info, X } from 'lucide-react';

export default function App() {
  const [trades, setTrades] = useState<any[]>([]);
  const [health, setHealth] = useState({ status: 'LOADING', queueDepth: 0 });
  const [timeframe, setTimeframe] = useState<number>(60);
  const [currentTime, setCurrentTime] = useState<Date>(new Date());
  const [isInfoOpen, setIsInfoOpen] = useState(false);

  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartInstanceRef = useRef<any>(null);
  const seriesRef = useRef<any>(null);

  const prevTimeframeRef = useRef<number>(timeframe);
  const baseCandlesRef = useRef<Map<number, any>>(new Map());

  // --- 1. DATA FETCHING ---
  const fetchData = async () => {
    try {
      const [tradeRes, healthRes] = await Promise.all([
        axios.get('http://localhost:8080/api/analytics/recent'),
        axios.get('http://localhost:8080/api/analytics/health')
      ]);
      setTrades(tradeRes.data.reverse());
      setHealth(healthRes.data);
    } catch (error) {
      console.error("Backend offline");
    }
  };

  useEffect(() => {
    fetchData();
    const dataInterval = setInterval(fetchData, 1000);
    const clockInterval = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => { clearInterval(dataInterval); clearInterval(clockInterval); };
  }, []);

  // --- 2. ENGINE LOGIC ---
  const parseJavaDate = (timestamp: any) => {
    if (Array.isArray(timestamp)) {
      const ms = timestamp[6] ? Math.floor(timestamp[6] / 1000000) : 0;
      return new Date(Date.UTC(timestamp[0], timestamp[1] - 1, timestamp[2], timestamp[3], timestamp[4], timestamp[5] || 0, ms));
    }
    return timestamp ? new Date(timestamp) : new Date();
  };

  const ingestTrades = (rawTrades: any[]) => {
    rawTrades.forEach(trade => {
      const date = parseJavaDate(trade.timestamp);
      const secTime = Math.floor(date.getTime() / 1000);
      const price = Number(trade.price);

      if (isNaN(price)) return;

      if (!baseCandlesRef.current.has(secTime)) {
        baseCandlesRef.current.set(secTime, { time: secTime, open: price, high: price, low: price, close: price });
      } else {
        const c = baseCandlesRef.current.get(secTime);
        c.high = Math.max(c.high, price);
        c.low = Math.min(c.low, price);
        c.close = price;
      }
    });

    const ONE_DAY_SECONDS = 24 * 60 * 60;
    const cutoffTime = Math.floor(Date.now() / 1000) - ONE_DAY_SECONDS;
    for (const [time] of baseCandlesRef.current) {
      if (time < cutoffTime) baseCandlesRef.current.delete(time);
    }
  };

  const getRolledUpCandles = (intervalSeconds: number) => {
    const rolledMap = new Map();
    const baseCandles = Array.from(baseCandlesRef.current.values()).sort((a, b) => a.time - b.time);

    baseCandles.forEach(bc => {
      const bucketTime = Math.floor(bc.time / intervalSeconds) * intervalSeconds;

      if (!rolledMap.has(bucketTime)) {
        rolledMap.set(bucketTime, { time: bucketTime, open: bc.open, high: bc.high, low: bc.low, close: bc.close });
      } else {
        const rc = rolledMap.get(bucketTime);
        rc.high = Math.max(rc.high, bc.high);
        rc.low = Math.min(rc.low, bc.low);
        rc.close = bc.close;
      }
    });

    return Array.from(rolledMap.values()).sort((a, b) => a.time - b.time);
  };

  const changeTimeframe = (seconds: number) => {
    setTimeframe(seconds);
  };

  // --- 3. CHART RENDERING ---
  useEffect(() => {
    if (!chartContainerRef.current) return;

    if (!chartInstanceRef.current) {
      const chart = createChart(chartContainerRef.current, {
        layout: { background: { type: ColorType.Solid, color: '#09090b' }, textColor: '#52525b', fontFamily: "'JetBrains Mono', monospace" },
        grid: { vertLines: { color: '#ffffff08' }, horzLines: { color: '#ffffff08' } },
        timeScale: {
          timeVisible: true,
          secondsVisible: true,
          borderColor: '#ffffff15',
          barSpacing: 14, // Keep spacing so sparse data doesn't look too thin
          rightOffset: 8,
          minBarSpacing: 5,
        },
        rightPriceScale: { borderColor: '#ffffff15', autoScale: true },
        crosshair: {
          mode: 1,
          vertLine: { width: 1, color: '#ffffff20', style: 3 },
          horzLine: { width: 1, color: '#ffffff20', style: 3 },
        }
      });

      const candlestickSeries = chart.addSeries(CandlestickSeries, {
        // REVERTED: Back to the sleek, borderless neon aesthetic
        upColor: '#10b981',
        downColor: '#ef4444',
        borderVisible: false,
        wickUpColor: '#10b981',
        wickDownColor: '#ef4444',
        priceFormat: {
                    type: 'price',
                    precision: 2,
                    minMove: 0.01,
                },
      });

      chartInstanceRef.current = chart;
      seriesRef.current = candlestickSeries;
    }

    if (trades.length > 0 && seriesRef.current) {
      try {
        ingestTrades(trades);
        const fullCandles = getRolledUpCandles(timeframe);

        let isTimeframeSwap = false;
        if (prevTimeframeRef.current !== timeframe) {
          isTimeframeSwap = true;
          prevTimeframeRef.current = timeframe;
        }

        const currentData = seriesRef.current.data();
        if (isTimeframeSwap || currentData.length === 0) {
          seriesRef.current.setData(fullCandles);
        } else {
          const lastCandle = fullCandles[fullCandles.length - 1];
          if (lastCandle) seriesRef.current.update(lastCandle);
        }
      } catch (e) { console.error("Engine Repaint Error:", e); }
    }
  }, [trades, timeframe]);

  // --- 4. UI MATH ---
  const currentPrice = trades.length > 0 ? trades[trades.length - 1].price : 0;
  const formattedUTC = currentTime.toISOString().substr(11, 8) + ' UTC';
  const totalSeconds = Math.floor(currentTime.getTime() / 1000);
  const secondsRemaining = timeframe - (totalSeconds % timeframe);
  const countdownMin = Math.floor(secondsRemaining / 60).toString().padStart(2, '0');
  const countdownSec = (secondsRemaining % 60).toString().padStart(2, '0');
  const queuePercentage = Math.min(100, (health.queueDepth / 100) * 100);

  return (
    <div className="min-h-screen bg-black text-zinc-300 py-6 px-4 sm:py-12 sm:px-8 font-sans selection:bg-blue-500/30 relative flex flex-col items-center">

      {/* ARCHITECTURE MODAL */}
      {isInfoOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 transition-all">
          <div className="bg-[#09090b] border border-white/10 w-full max-w-2xl rounded-2xl shadow-2xl flex flex-col overflow-hidden">
            <div className="flex justify-between items-center p-5 border-b border-white/5 bg-white/[0.02]">
              <div className="flex items-center gap-3">
                <Terminal className="text-emerald-400" size={18} />
                <h2 className="text-sm font-mono text-white tracking-widest uppercase">System Architecture</h2>
              </div>
              <button onClick={() => setIsInfoOpen(false)} className="text-zinc-500 hover:text-white transition-colors"><X size={20} /></button>
            </div>
            <div className="p-6 space-y-4 text-sm text-zinc-400 leading-relaxed">
              <p><strong className="text-white font-medium">TradeFlow Engine</strong> is a high-performance, full-stack application designed to ingest, buffer, and visualize high-frequency cryptocurrency market data in real-time.</p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
                <div className="bg-black/50 border border-white/5 p-5 rounded-xl">
                  <h3 className="text-emerald-400 font-mono text-xs mb-3 tracking-widest">Backend (Java/Spring)</h3>
                  <ul className="list-disc pl-4 space-y-2 text-xs text-zinc-500">
                    <li>Maintains persistent WebSocket connection to Binance.</li>
                    <li>Utilizes a custom <span className="font-mono text-zinc-300 bg-white/5 px-1 rounded">LinkedOrderQueue</span> for $O(1)$ memory buffering.</li>
                    <li>Scheduled workers flush queue to PostgreSQL.</li>
                  </ul>
                </div>
                <div className="bg-black/50 border border-white/5 p-5 rounded-xl">
                  <h3 className="text-emerald-400 font-mono text-xs mb-3 tracking-widest">Frontend (React/TS)</h3>
                  <ul className="list-disc pl-4 space-y-2 text-xs text-zinc-500">
                    <li>Fetches raw logs and performs client-side time-bucketing for OHLC candles.</li>
                    <li>Streams canvas updates via Lightweight Charts for seamless rendering.</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="w-full max-w-[1400px] flex flex-col gap-6">

        {/* HEADER BAR */}
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 bg-[#09090b] border border-white/10 p-5 rounded-2xl shadow-xl">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-emerald-500/10 rounded-lg"><Terminal className="text-emerald-500" size={20} /></div>
            <div>
              <h1 className="text-base font-mono font-bold text-white tracking-wide">TradeFlow<span className="text-zinc-500 font-normal">_Engine</span></h1>
              <p className="text-xs text-zinc-500 font-mono mt-0.5">Live Order Matching</p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-6 md:gap-8">
            <div className="flex flex-col w-24">
              <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-semibold mb-1 flex items-center gap-1.5"><Activity size={12}/> BTC/USDT</span>
              <span className="text-lg font-mono text-emerald-400">${currentPrice.toFixed(2)}</span>
            </div>

            <div className="w-[1px] h-8 bg-white/10 hidden md:block"></div>

            <div className="flex flex-col w-24">
              <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-semibold mb-1 flex items-center gap-1.5"><Server size={12}/> Buffer</span>
              <span className="text-lg font-mono text-white leading-none mb-1.5">{health.queueDepth}</span>
              <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                <div
                  className="h-full bg-indigo-500 shadow-[0_0_8px_rgba(99,102,241,0.8)] transition-all duration-300 ease-out"
                  style={{ width: `${queuePercentage}%` }}
                />
              </div>
            </div>

            <div className="w-[1px] h-8 bg-white/10 hidden md:block"></div>

            <div className="flex flex-col w-24">
              <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-semibold mb-1 flex items-center gap-1.5"><Cpu size={12}/> Engine</span>
              <div className="flex items-center gap-2 mt-1">
                <span className="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.6)] animate-pulse"></span>
                <span className="text-sm font-mono text-white tracking-widest">{health.status}</span>
              </div>
            </div>

            <button onClick={() => setIsInfoOpen(true)} className="ml-auto text-xs font-mono text-zinc-400 hover:text-white flex items-center gap-2 border border-white/10 px-4 py-2 bg-white/5 hover:bg-white/10 transition-all rounded-xl">
              <Info size={14} /> Architecture
            </button>
          </div>
        </header>

        {/* WORKSPACE */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          <div className="lg:col-span-3 bg-[#09090b] border border-white/10 rounded-2xl overflow-hidden flex flex-col h-[650px] shadow-xl">
            <div className="flex justify-between items-center px-6 py-4 border-b border-white/5 bg-white/[0.01]">
              <h3 className="text-xs text-zinc-400 uppercase tracking-widest font-semibold">Market Depth</h3>

              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                  <span className="text-zinc-400 text-xs font-mono bg-black/50 border border-white/5 px-3 py-1.5 rounded-lg flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-zinc-600"></span>{formattedUTC}
                  </span>
                  <span className="text-zinc-400 text-xs font-mono bg-black/50 border border-white/5 px-3 py-1.5 rounded-lg">
                    Close in {countdownMin}:{countdownSec}
                  </span>
                </div>

                <div className="flex gap-1 bg-black/50 p-1 rounded-lg border border-white/5">
                  <button onClick={() => changeTimeframe(5)} className={`px-3 py-1 text-xs font-mono rounded-md transition-all ${timeframe === 5 ? 'bg-zinc-800 text-white shadow-sm' : 'text-zinc-500 hover:text-zinc-300'}`}>5s</button>
                  <button onClick={() => changeTimeframe(60)} className={`px-3 py-1 text-xs font-mono rounded-md transition-all ${timeframe === 60 ? 'bg-zinc-800 text-white shadow-sm' : 'text-zinc-500 hover:text-zinc-300'}`}>1m</button>
                  <button onClick={() => changeTimeframe(300)} className={`px-3 py-1 text-xs font-mono rounded-md transition-all ${timeframe === 300 ? 'bg-zinc-800 text-white shadow-sm' : 'text-zinc-500 hover:text-zinc-300'}`}>5m</button>
                </div>
              </div>
            </div>
            <div className="relative flex-1 w-full bg-[#09090b]">
              <div ref={chartContainerRef} className="absolute inset-0" />
            </div>
          </div>

          <div className="bg-[#09090b] border border-white/10 rounded-2xl overflow-hidden flex flex-col h-[650px] shadow-xl">
            <div className="px-6 py-4 border-b border-white/5 bg-white/[0.01] flex justify-between">
              <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-semibold">Price (USDT)</span>
              <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-semibold">Qty (BTC)</span>
            </div>
            <div className="overflow-y-auto flex-1 px-4 py-2 custom-scrollbar">
              <table className="w-full text-xs font-mono text-right">
                <tbody>
                  {[...trades].reverse().map((trade, i) => (
                    <tr key={i} className="group hover:bg-white/[0.04] transition-colors rounded-lg">
                      {/* REVERTED: Back to Tailwind colors */}
                      <td className={`py-1.5 px-2 text-left rounded-l-md ${trade.type === 'BUY' ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {trade.price.toFixed(2)}
                      </td>
                      <td className="py-1.5 px-2 text-zinc-500 group-hover:text-zinc-300 transition-colors rounded-r-md">
                        {trade.quantity.toFixed(4)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}