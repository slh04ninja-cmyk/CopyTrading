import MetaTrader5 as mt5
mt5.initialize()
orders = mt5.orders_get()
positions = mt5.positions_get()
print(f"ORDERS: {len(orders) if orders else 0}")
print(f"POSITIONS: {len(positions) if positions else 0}")
if orders:
    for o in orders:
        print(f"  ORDER: {o.comment} | {o.symbol} | type={o.type} | ticket={o.ticket}")
if positions:
    for p in positions:
        print(f"  POS: {p.comment} | {p.symbol} | type={p.type} | ticket={p.ticket}")
mt5.shutdown()
