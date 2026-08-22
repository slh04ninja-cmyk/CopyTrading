package com.copytrading.model

data class BotStatus(
    val status: String = "stopped",
    val pid: Int? = null,
    val uptime_seconds: Int? = null,
    val last_error: String = ""
)

data class MT5Account(
    val login: Long = 0,
    val server: String = "",
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val margin: Double = 0.0,
    val free_margin: Double = 0.0,
    val profit: Double = 0.0,
    val currency: String = "USD",
    val leverage: Int = 0
)

data class MT5Status(
    val connected: Boolean = false,
    val account: MT5Account? = null
)

data class StatusResponse(
    val bot: BotStatus = BotStatus(),
    val mt5: MT5Status = MT5Status(),
    val server_time: String = ""
)

data class DashboardResponse(
    val daily_pnl: Double = 0.0,
    val floating_pnl: Double = 0.0,
    val total_pnl: Double = 0.0,
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val trades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winrate: Double = 0.0,
    val open_positions: List<Position> = emptyList(),
    val open_count: Int = 0,
    val daily_limit: Double = 0.0,
    val limit_pct: Double = 0.0,
    val trading_hours: String = "",
    val timestamp: String = ""
)

data class Position(
    val ticket: Long = 0,
    val symbol: String = "",
    val type: String = "",
    val volume: Double = 0.0,
    val open_price: Double = 0.0,
    val current_price: Double = 0.0,
    val sl: Double = 0.0,
    val tp: Double = 0.0,
    val profit: Double = 0.0,
    val swap: Double = 0.0,
    val comment: String = "",
    val magic: Long = 0,
    val bot_opened: Boolean = false,
    val time: String = ""
)

data class Trade(
    val ticket: Long = 0,
    val symbol: String = "",
    val type: String = "",
    val volume: Double = 0.0,
    val open_price: Double = 0.0,
    val close_price: Double = 0.0,
    val profit: Double = 0.0,
    val commission: Double = 0.0,
    val swap: Double = 0.0,
    val comment: String = "",
    val open_time: String = "",
    val close_time: String = ""
)

data class TradesResponse(
    val trades: List<Trade> = emptyList(),
    val count: Int = 0,
    val days: Int = 0
)

data class PositionsResponse(
    val positions: List<Position> = emptyList(),
    val count: Int = 0
)

data class BotActionResponse(
    val status: String = "",
    val pid: Int? = null,
    val message: String = ""
)

data class ConfigResponse(
    val config: Map<String, String> = emptyMap(),
    val file: String = ""
)

data class LogsResponse(
    val logs: List<String> = emptyList(),
    val total_lines: Int = 0,
    val returned: Int = 0
)

data class CloseResponse(
    val status: String = "",
    val ticket: Long = 0,
    val profit: Double = 0.0
)

data class CloseAllResponse(
    val closed: List<Long> = emptyList(),
    val failed: List<Long> = emptyList(),
    val total: Int = 0
)
