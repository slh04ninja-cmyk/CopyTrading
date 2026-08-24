import MetaTrader5 as mt5
mt5.initialize()
orders = mt5.orders_get()
if orders:
    for o in orders:
        if o.magic == 20250226:
            types = {0: "BUY", 1: "SELL", 2: "BUY_LIMIT", 3: "SELL_LIMIT", 4: "BUY_STOP", 5: "SELL_STOP"}
            print(f"  ticket={o.ticket} {o.symbol} {types.get(o.type, o.type)} vol={o.volume_current} price={o.price_open} sl={o.sl} tp={o.tp} comment={o.comment}")
    print(f"Total: {len([o for o in orders if o.magic == 20250226])} pending orders")
else:
    print("No pending orders at all")
mt5.shutdown()
