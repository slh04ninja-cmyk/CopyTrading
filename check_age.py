import MetaTrader5 as mt5
from datetime import datetime, timezone

mt5.initialize()
positions = mt5.positions_get()
now = datetime.now(timezone.utc)

if positions:
    for p in positions:
        age_min = round((now.timestamp() - p.time) / 60, 1)
        open_dt = datetime.fromtimestamp(p.time, tz=timezone.utc).strftime("%H:%M:%S")
        print(f"{p.comment} | opened={open_dt} UTC | age={age_min}min")

mt5.shutdown()
