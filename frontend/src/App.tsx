import { useState, useEffect } from 'react';
import axios from 'axios';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Activity } from 'lucide-react';

export default function App() {
  const [trades, setTrades] = useState<any[]>([]);
  const [currentPrice, setCurrentPrice] = useState<number>(0);

  const fetchTrades = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/analytics/recent');
      const sortedTrades = res.data.reverse(); // Reverse for charting (oldest to newest)
      setTrades(sortedTrades);

      if (sortedTrades.length > 0) {
        setCurrentPrice(sortedTrades[sortedTrades.length - 1].price);
      }
    } catch (error) {
      console.error("Backend offline");
    }
  };

  useEffect(() => {
    fetchTrades();
    const interval = setInterval(fetchTrades, 1000); // Poll every second to update UI
    return () => clearInterval(interval);
  }, []);

  const chartData = trades.map((t, i) => ({ time: i, price: t.price }));

  return (
    <div className="min-h-screen p-8 max-w-7xl mx-auto">
      <header className="flex items-center gap-3 mb-8 border-b border-slate-800 pb-4">
        <Activity className="text-blue-500" size={32} />
        <h1 className="text-3xl font-bold tracking-tight">TradeFlow Engine</h1>
        <span className="ml-auto bg-green-500/20 text-green-400 px-3 py-1 rounded-full text-sm font-semibold flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
          BINANCE WS LIVE
        </span>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Chart Section */}
        <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-xl p-6">
          <div className="flex justify-between items-end mb-6">
            <div>
              <p className="text-slate-400 text-sm font-semibold">BTC/USDT</p>
              <h2 className="text-4xl font-bold">${currentPrice.toFixed(2)}</h2>
            </div>
          </div>
          <div className="h-96 w-full">
            <ResponsiveContainer>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                <XAxis dataKey="time" hide />
                <YAxis domain={['dataMin', 'dataMax']} stroke="#64748b" tickFormatter={(val) => `$${val}`} width={80} />
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b' }} />
                <Area type="monotone" dataKey="price" stroke="#3b82f6" fillOpacity={1} fill="url(#colorPrice)" isAnimationActive={false} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Live Order Book / Tape */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden flex flex-col h-[500px]">
          <div className="p-4 border-b border-slate-800 bg-slate-900/50">
            <h3 className="font-bold">Execution Tape</h3>
          </div>
          <div className="overflow-y-auto flex-1 p-4">
            <table className="w-full text-sm text-right">
              <thead>
                <tr className="text-slate-500 border-b border-slate-800">
                  <th className="pb-2 text-left">Price (USDT)</th>
                  <th className="pb-2">Qty (BTC)</th>
                </tr>
              </thead>
              <tbody>
                {[...trades].reverse().map((trade, i) => (
                  <tr key={i} className="hover:bg-slate-800/50 transition-colors">
                    <td className={`py-1.5 text-left font-medium ${trade.type === 'BUY' ? 'text-green-500' : 'text-red-500'}`}>
                      {trade.price.toFixed(2)}
                    </td>
                    <td className="py-1.5 text-slate-300">
                      {trade.quantity.toFixed(5)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}