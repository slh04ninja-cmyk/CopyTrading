with open("C:\\TradingBot\\channels.txt", "r", encoding="utf-8") as f:
    for line in f:
        if "Canal_60" in line:
            print(line.strip())
