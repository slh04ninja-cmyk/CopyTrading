package com.copytrading

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.copytrading.api.ApiClient
import com.copytrading.model.*
import com.copytrading.ui.PositionAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var client: ApiClient
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // Header
    private lateinit var tvBotStatus: TextView
    private lateinit var btnSettings: ImageView

    // P&L Cards
    private lateinit var tvDailyPnl: TextView
    private lateinit var tvFloatingPnl: TextView
    private lateinit var tvTotalPnl: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvEquity: TextView

    // Stats
    private lateinit var tvTrades: TextView
    private lateinit var tvWins: TextView
    private lateinit var tvLosses: TextView
    private lateinit var tvWinrate: TextView
    private lateinit var progressWinrate: ProgressBar

    // Daily Limit
    private lateinit var tvDailyLimit: TextView
    private lateinit var progressDailyLimit: ProgressBar

    // Controls
    private lateinit var btnStartStop: MaterialButton
    private lateinit var btnCloseAll: MaterialButton

    // Positions
    private lateinit var rvPositions: RecyclerView
    private lateinit var tvNoPositions: TextView
    private lateinit var positionAdapter: PositionAdapter

    // Tabs
    private lateinit var tabDashboard: TextView
    private lateinit var tabPositions: TextView
    private lateinit var tabConfig: TextView
    private lateinit var tabLogs: TextView

    // Panels
    private lateinit var panelDashboard: NestedScrollView
    private lateinit var panelPositions: ScrollView
    private lateinit var panelConfig: ScrollView
    private lateinit var panelLogs: ScrollView
    private lateinit var pillIndicator: View

    // Config
    private lateinit var etConfigContent: EditText
    private lateinit var btnSaveConfig: MaterialButton

    // Logs
    private lateinit var tvLogs: TextView
    private lateinit var btnRefreshLogs: MaterialButton

    private var isRunning = false
    private var autoRefresh = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = ApiClient(this)
        initViews()
        setupTabs()
        setupListeners()

        // Auto-refresh toutes les 5 secondes
        startAutoRefresh()
        refreshDashboard()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)

        tvBotStatus = findViewById(R.id.tvBotStatus)
        btnSettings = findViewById(R.id.btnSettings)

        tvDailyPnl = findViewById(R.id.tvDailyPnl)
        tvFloatingPnl = findViewById(R.id.tvFloatingPnl)
        tvTotalPnl = findViewById(R.id.tvTotalPnl)
        tvBalance = findViewById(R.id.tvBalance)
        tvEquity = findViewById(R.id.tvEquity)

        tvTrades = findViewById(R.id.tvTrades)
        tvWins = findViewById(R.id.tvWins)
        tvLosses = findViewById(R.id.tvLosses)
        tvWinrate = findViewById(R.id.tvWinrate)
        progressWinrate = findViewById(R.id.progressWinrate)

        tvDailyLimit = findViewById(R.id.tvDailyLimit)
        progressDailyLimit = findViewById(R.id.progressDailyLimit)

        btnStartStop = findViewById(R.id.btnStartStop)
        btnCloseAll = findViewById(R.id.btnCloseAll)

        rvPositions = findViewById(R.id.rvPositions)
        tvNoPositions = findViewById(R.id.tvNoPositions)

        tabDashboard = findViewById(R.id.tabDashboard)
        tabPositions = findViewById(R.id.tabPositions)
        tabConfig = findViewById(R.id.tabConfig)
        tabLogs = findViewById(R.id.tabLogs)

        panelDashboard = findViewById(R.id.panelDashboard)
        panelPositions = findViewById(R.id.panelPositions)
        panelConfig = findViewById(R.id.panelConfig)
        panelLogs = findViewById(R.id.panelLogs)
        pillIndicator = findViewById(R.id.pillIndicator)

        etConfigContent = findViewById(R.id.etConfigContent)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)

        tvLogs = findViewById(R.id.tvLogs)
        btnRefreshLogs = findViewById(R.id.btnRefreshLogs)

        positionAdapter = PositionAdapter { ticket ->
            closePosition(ticket)
        }
        rvPositions.layoutManager = LinearLayoutManager(this)
        rvPositions.adapter = positionAdapter
    }

    private fun setupTabs() {
        val tabs = listOf(tabDashboard, tabPositions, tabConfig, tabLogs)
        var currentTab = 0

        fun selectTab(index: Int) {
            currentTab = index
            // Update text colors
            tabs.forEachIndexed { i, tab ->
                tab.setTextColor(getColor(if (i == index) R.color.text_primary else R.color.text_secondary))
                tab.setTypeface(null, if (i == index) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            // Animate pill sliding
            pillIndicator.post {
                val tabWidth = tabs[0].width.toFloat()
                pillIndicator.layoutParams = pillIndicator.layoutParams.apply {
                    width = tabWidth.toInt()
                }
                pillIndicator.animate()
                    .translationX(tabWidth * index)
                    .setDuration(250)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                    .start()
            }

            // Hide all panels
            swipeRefresh.visibility = View.GONE
            panelPositions.visibility = View.GONE
            panelConfig.visibility = View.GONE
            panelLogs.visibility = View.GONE

            when (index) {
                0 -> swipeRefresh.visibility = View.VISIBLE
                1 -> {
                    panelPositions.visibility = View.VISIBLE
                    refreshPositions()
                }
                2 -> {
                    panelConfig.visibility = View.VISIBLE
                    loadConfig()
                }
                3 -> {
                    panelLogs.visibility = View.VISIBLE
                    loadLogs()
                }
            }
        }

        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener { selectTab(index) }
        }

        // Init pill width after layout
        pillIndicator.post {
            val tabWidth = tabs[0].width.toFloat()
            pillIndicator.layoutParams = pillIndicator.layoutParams.apply {
                width = tabWidth.toInt()
            }
            pillIndicator.translationX = 0f
        }

        selectTab(0)
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            refreshDashboard()
        }

        btnSettings.setOnClickListener {
            val prefs = getSharedPreferences("copytrading", Context.MODE_PRIVATE)
            prefs.edit().remove("server_host").apply()
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }

        btnStartStop.setOnClickListener {
            lifecycleScope.launch {
                if (isRunning) {
                    client.stopBot()
                } else {
                    client.startBot()
                }
                delay(1000)
                refreshDashboard()
            }
        }

        btnCloseAll.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Fermer toutes les positions ?")
                .setMessage("Cette action est irréversible.")
                .setPositiveButton("Confirmer") { _, _ ->
                    lifecycleScope.launch {
                        client.closeAll()
                        delay(1000)
                        refreshDashboard()
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        btnSaveConfig.setOnClickListener {
            saveConfig()
        }

        btnRefreshLogs.setOnClickListener {
            loadLogs()
        }
    }

    private fun startAutoRefresh() {
        lifecycleScope.launch {
            while (autoRefresh) {
                delay(5000)
                try {
                    refreshDashboard()
                } catch (_: Exception) {}
            }
        }
    }

    private fun refreshDashboard() {
        lifecycleScope.launch {
            try {
                val status = client.getStatus()
                val dashboard = client.getDashboard()

                if (status != null) {
                    updateBotStatus(status)
                }

                if (dashboard != null) {
                    updateDashboard(dashboard)
                } else {
                    // Afficher l'erreur dans l'UI
                    tvDailyPnl.text = "ERR: null"
                    tvDailyPnl.setTextColor(getColor(R.color.danger))
                }
            } catch (e: Exception) {
                tvDailyPnl.text = "ERR: ${e.message}"
                tvDailyPnl.setTextColor(getColor(R.color.danger))
            }

            swipeRefresh.isRefreshing = false
        }
    }

    private fun updateBotStatus(status: StatusResponse) {
        val bot = status.bot
        isRunning = bot.status == "running"

        when (bot.status) {
            "running" -> {
                tvBotStatus.text = "🟢 EN LIGNE"
                tvBotStatus.setTextColor(getColor(R.color.success))
                btnStartStop.text = "⏹ ARRÊTER"
                btnStartStop.setBackgroundColor(getColor(R.color.danger))
            }
            "stopped" -> {
                tvBotStatus.text = "🔴 ARRÊTÉ"
                tvBotStatus.setTextColor(getColor(R.color.danger))
                btnStartStop.text = "▶ DÉMARRER"
                btnStartStop.setBackgroundColor(getColor(R.color.success))
            }
            "error" -> {
                tvBotStatus.text = "⚠ ERREUR"
                tvBotStatus.setTextColor(getColor(R.color.warning))
                btnStartStop.text = "▶ DÉMARRER"
                btnStartStop.setBackgroundColor(getColor(R.color.success))
            }
        }

        val mt5 = status.mt5
        if (mt5.connected && mt5.account != null) {
            tvBalance.text = formatMoney(mt5.account.balance)
            tvEquity.text = formatMoney(mt5.account.equity)
        }
    }

    private fun updateDashboard(dash: DashboardResponse) {
        // P&L
        tvDailyPnl.text = formatPnl(dash.daily_pnl)
        tvDailyPnl.setTextColor(getPnlColor(dash.daily_pnl))
        tvFloatingPnl.text = formatPnl(dash.floating_pnl)
        tvFloatingPnl.setTextColor(getPnlColor(dash.floating_pnl))
        tvTotalPnl.text = formatPnl(dash.total_pnl)
        tvTotalPnl.setTextColor(getPnlColor(dash.total_pnl))

        // Stats
        tvTrades.text = dash.trades.toString()
        tvWins.text = dash.wins.toString()
        tvLosses.text = dash.losses.toString()
        tvWinrate.text = "${dash.winrate}%"
        progressWinrate.progress = dash.winrate.toInt()

        // Daily limit
        tvDailyLimit.text = "${formatPnl(dash.total_pnl)} / ${formatMoney(dash.daily_limit)}"
        progressDailyLimit.progress = dash.limit_pct.toInt().coerceIn(0, 100)

        // Positions
        positionAdapter.setPositions(dash.open_positions)
        tvNoPositions.visibility = if (dash.open_positions.isEmpty()) View.VISIBLE else View.GONE
        rvPositions.visibility = if (dash.open_positions.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun refreshPositions() {
        lifecycleScope.launch {
            val positions = client.getPositions()
            if (positions != null) {
                positionAdapter.setPositions(positions.positions)
                tvNoPositions.visibility = if (positions.positions.isEmpty()) View.VISIBLE else View.GONE
                rvPositions.visibility = if (positions.positions.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun loadConfig() {
        lifecycleScope.launch {
            try {
                val config = client.getConfig()
                if (config != null) {
                    val sb = StringBuilder()
                    config.config.toSortedMap().forEach { (key, value) ->
                        sb.appendLine("$key=$value")
                    }
                    etConfigContent.setText(sb.toString())
                } else {
                    etConfigContent.setText("Erreur: impossible de charger la config")
                }
            } catch (e: Exception) {
                etConfigContent.setText("Erreur: ${e.message}")
            }
        }
    }

    private fun saveConfig() {
        val content = etConfigContent.text.toString()
        val values = mutableMapOf<String, String>()
        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val (key, value) = trimmed.split("=", limit = 2)
                values[key.trim()] = value.trim()
            }
        }

        lifecycleScope.launch {
            val ok = client.updateConfig(values)
            if (ok) {
                Toast.makeText(this@MainActivity, "✅ Config sauvegardée", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "❌ Erreur sauvegarde", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            try {
                val logs = client.getLogs(200)
                if (logs != null) {
                    tvLogs.text = logs.logs.joinToString("\n")
                    // Auto-scroll vers le bas
                    val scrollview = tvLogs.parent as? View
                    if (scrollview is ScrollView) {
                        scrollview.post { scrollview.fullScroll(View.FOCUS_DOWN) }
                    }
                } else {
                    tvLogs.text = "Erreur: impossible de charger les logs"
                }
            } catch (e: Exception) {
                tvLogs.text = "Erreur: ${e.message}"
            }
        }
    }

    private fun closePosition(ticket: Long) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Fermer la position #$ticket ?")
            .setPositiveButton("Confirmer") { _, _ ->
                lifecycleScope.launch {
                    val result = client.closePosition(ticket)
                    if (result != null) {
                        Toast.makeText(this@MainActivity,
                            "✅ Position fermée: ${formatPnl(result.profit)}",
                            Toast.LENGTH_SHORT).show()
                    }
                    delay(1000)
                    refreshDashboard()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // --- HELPERS ---
    private fun formatMoney(value: Double): String {
        return String.format("%,.2f$", value)
    }

    private fun formatPnl(value: Double): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign${String.format("%.2f", value)}$"
    }

    private fun getPnlColor(value: Double): Int {
        return when {
            value > 0 -> getColor(R.color.profit)
            value < 0 -> getColor(R.color.loss)
            else -> getColor(R.color.text_secondary)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoRefresh = false
    }
}
