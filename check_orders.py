import MetaTrader5 as mt5
mt5.initialize()
positions = mt5.positions_get()
orders = mt5.orders_get()
print(f"POSITIONS: {len(positions) if positions else 0}")
if positions:
    for p in positions:
        print(f"  {p.comment} | {p.symbol} | {'BUY' if p.type==0 else 'SELL'} | lot={p.volume} | entry={p.price_open} | TP={p.tp} | SL={p.sl} | pnl={p.profit}")
print(f"PENDING ORDERS: {len(orders) if orders else 0}")
if orders:
    for o in orders:
        print(f"  {o.comment} | {o.symbol} | type={o.type} | lot={o.volume_current} | price={o.price_open}")
mt5.shutdown()
