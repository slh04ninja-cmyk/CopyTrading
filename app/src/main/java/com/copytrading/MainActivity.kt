package com.copytrading

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.drawerlayout.widget.DrawerLayout
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.copytrading.api.ApiClient
import com.copytrading.model.*
import com.copytrading.ui.PositionAdapter


import com.copytrading.ui.DateRangePickerDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var client: ApiClient
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rootLayout: View
    private lateinit var drawerLayout: DrawerLayout

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
    private lateinit var tabDashboard: LinearLayout
    private lateinit var tabPositions: LinearLayout
    private lateinit var tabConfig: LinearLayout
    private lateinit var tabLogs: LinearLayout
    private lateinit var tabDashboardIcon: ImageView
    private lateinit var tabPositionsIcon: ImageView
    private lateinit var tabConfigIcon: ImageView
    private lateinit var tabLogsIcon: ImageView

    // Dash sub-tabs
    private lateinit var btnOverview: TextView
    private lateinit var btnPerformance: TextView

    // Performance
    private lateinit var overviewContent: LinearLayout
    private lateinit var performanceContent: LinearLayout
    private lateinit var perfChannelTable: LinearLayout
    private lateinit var perfSignalTable: LinearLayout
    private lateinit var tvDateRange: TextView
    private var dateFrom: String = ""
    private var dateTo: String = ""

    // Panels
    private lateinit var panelDashboard: NestedScrollView
    private lateinit var panelPositions: NestedScrollView
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
    private var closeAllBusy = false

    // Colors for morph button
    private val idleColor = Color.parseColor("#E53935")
    private val loadingColor = Color.parseColor("#FF9800")
    private val successColor = Color.parseColor("#4CAF50")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = ApiClient(this)
        initViews()
        setupTabs()
        setupDashTabs()
        setupDatePicker()
        setupListeners()

        startAutoRefresh()
        refreshDashboard()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rootLayout = findViewById(R.id.rootLayout)
        drawerLayout = findViewById(R.id.drawerLayout)

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
        tabDashboardIcon = findViewById(R.id.tabDashboardIcon)
        tabPositionsIcon = findViewById(R.id.tabPositionsIcon)
        tabConfigIcon = findViewById(R.id.tabConfigIcon)
        tabLogsIcon = findViewById(R.id.tabLogsIcon)

        btnOverview = findViewById(R.id.btnOverview)
        btnPerformance = findViewById(R.id.btnPerformance)

        overviewContent = findViewById(R.id.overviewContent)
        performanceContent = findViewById(R.id.performanceContent)
        perfChannelTable = findViewById(R.id.perfChannelTable)
        perfSignalTable = findViewById(R.id.perfSignalTable)
        tvDateRange = findViewById(R.id.tvDateRange)

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

            pillIndicator.post {
                val tabWidth = tabs[0].width.toFloat()
                pillIndicator.layoutParams = pillIndicator.layoutParams.apply {
                    width = tabWidth.toInt()
                }
                pillIndicator.animate()
                    .translationX(tabWidth * index)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .start()
            }

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

        pillIndicator.post {
            val tabWidth = tabs[0].width.toFloat()
            pillIndicator.layoutParams = pillIndicator.layoutParams.apply {
                width = tabWidth.toInt()
            }
            pillIndicator.translationX = 0f
        }

        selectTab(0)
    }

    private fun setupDatePicker() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFmt = SimpleDateFormat("dd/MM", Locale.FRANCE)
        val today = sdf.format(java.util.Date())
        dateFrom = today
        dateTo = today
        tvDateRange.text = "Aujourd'hui"
        refreshPerformanceForRange(today, today)

        tvDateRange.setOnClickListener {
            DateRangePickerDialog(this) { start, end ->
                dateFrom = start
                dateTo = end
                val s = displayFmt.format(sdf.parse(start)!!)
                val e = displayFmt.format(sdf.parse(end)!!)
                tvDateRange.text = if (start == end) s else "$s - $e"
                refreshPerformanceForRange(start, end)
            }.show()
        }
    }

    private fun refreshPerformanceForRange(fromDate: String, toDate: String) {
        lifecycleScope.launch {
            try {
                val trades = client.getTrades(fromDate = fromDate, toDate = toDate)
                if (trades != null) {
                    updatePerformance(trades.trades)
                }
            } catch (_: Exception) {}
        }
    }

    private fun setupDashTabs() {
        btnOverview.setOnClickListener {
            overviewContent.visibility = View.VISIBLE
            performanceContent.visibility = View.GONE
            btnOverview.setBackgroundColor(getColor(R.color.accent))
            btnOverview.setTextColor(Color.WHITE)
            btnPerformance.setBackgroundColor(Color.TRANSPARENT)
            btnPerformance.setTextColor(getColor(R.color.text_muted))
        }
        btnPerformance.setOnClickListener {
            overviewContent.visibility = View.GONE
            performanceContent.visibility = View.VISIBLE
            btnPerformance.setBackgroundColor(getColor(R.color.accent))
            btnPerformance.setTextColor(Color.WHITE)
            btnOverview.setBackgroundColor(Color.TRANSPARENT)
            btnOverview.setTextColor(getColor(R.color.text_muted))
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            refreshDashboard()
        }

        btnSettings.setOnClickListener {
            if (drawerLayout.isDrawerOpen(android.view.Gravity.START)) {
                drawerLayout.closeDrawer(android.view.Gravity.START)
            } else {
                drawerLayout.openDrawer(android.view.Gravity.START)
            }
        }

        btnStartStop.setOnClickListener {
            lifecycleScope.launch {
                if (isRunning) {
                    client.stopBot()
                    showNotification("Bot arrete")
                } else {
                    client.startBot()
                    showNotification("Bot demarre")
                }
                delay(1000)
                refreshDashboard()
            }
        }

        btnCloseAll.setOnClickListener {
            if (!closeAllBusy) {
                morphCloseAll()
            }
        }

        btnSaveConfig.setOnClickListener {
            saveConfig()
        }

        btnRefreshLogs.setOnClickListener {
            loadLogs()
        }
    }

    // --- MORPH BUTTON ANIMATION ---
    private fun morphCloseAll() {
        closeAllBusy = true
        btnCloseAll.isEnabled = false

        // Phase 1: idle -> loading
        animateColor(btnCloseAll, idleColor, loadingColor, 350)
        btnCloseAll.text = "Fermeture..."

        // Start spinning ring overlay
        val ringView = SpinningRingView(this)
        val parent = btnCloseAll.parent as? android.view.ViewGroup
        val lp = android.widget.FrameLayout.LayoutParams(
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt()
        )
        // Add ring as overlay on the button
        parent?.addView(ringView, lp)
        ringView.x = btnCloseAll.x + btnCloseAll.width / 2f - 12 * resources.displayMetrics.density
        ringView.y = btnCloseAll.y + btnCloseAll.height / 2f - 12 * resources.displayMetrics.density
        ringView.startSpin()

        // Execute the close-all
        lifecycleScope.launch {
            client.closeAll()
            delay(1000)
            refreshDashboard()
            refreshPositions()

            // Phase 2: loading -> success
            ringView.stopSpin()
            parent?.removeView(ringView)
            animateColor(btnCloseAll, loadingColor, successColor, 350)
            btnCloseAll.text = ""

            // Draw checkmark overlay
            val checkView = CheckmarkView(this@MainActivity)
            parent?.addView(checkView, lp)
            checkView.x = btnCloseAll.x + btnCloseAll.width / 2f - 12 * resources.displayMetrics.density
            checkView.y = btnCloseAll.y + btnCloseAll.height / 2f - 12 * resources.displayMetrics.density
            checkView.animateCheck()

            // Phase 3: reset after 2s
            delay(2000)
            parent?.removeView(checkView)
            animateColor(btnCloseAll, successColor, idleColor, 350)
            btnCloseAll.text = "TOUT FERMER"
            btnCloseAll.isEnabled = true
            closeAllBusy = false
        }
    }

    private fun animateColor(view: View, from: Int, to: Int, duration: Long) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), from, to)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator(2f)
        animator.addUpdateListener {
            view.setBackgroundColor(it.animatedValue as Int)
        }
        animator.start()
    }

    // --- CONFETTI BURST ---
    private fun burstConfetti(anchor: View, onEnd: () -> Unit = {}) {
        val parent = rootLayout as? android.view.ViewGroup ?: return onEnd()
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val rootLoc = IntArray(2)
        parent.getLocationOnScreen(rootLoc)
        val cx = loc[0] - rootLoc[0] + anchor.width / 2f
        val cy = loc[1] - rootLoc[1] + anchor.height / 2f

        val colors = intArrayOf(
            Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"), Color.parseColor("#96CEB4"),
            Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
            Color.parseColor("#98D8C8"), Color.parseColor("#F7DC6F")
        )
        val density = resources.displayMetrics.density
        val particles = mutableListOf<Pair<View, Triple<Float, Float, Float>>>() // dx, dy, rotation

        for (i in 0 until 16) {
            val angle = Math.toRadians((i * 360.0 / 16) + Random.nextDouble(-15.0, 15.0))
            val distance = (60 + Random.nextFloat() * 80) * density
            val size = (4 + Random.nextFloat() * 4) * density
            val rotation = Random.nextFloat() * 720f - 360f
            val color = colors[Random.nextInt(colors.size)]

            val particle = View(this).apply {
                setBackgroundColor(color)
            }
            val lp = FrameLayout.LayoutParams(size.toInt(), size.toInt())
            lp.leftMargin = (cx - size / 2).toInt()
            lp.topMargin = (cy - size / 2).toInt()
            parent.addView(particle, lp)

            val dx = (cos(angle) * distance).toFloat()
            val dy = (sin(angle) * distance).toFloat()
            particles.add(particle to Triple(dx, dy, rotation))
        }

        val animators = particles.map { (view, triple) ->
            val (dx, dy, rot) = triple
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "translationX", 0f, dx),
                    ObjectAnimator.ofFloat(view, "translationY", 0f, dy),
                    ObjectAnimator.ofFloat(view, "rotation", 0f, rot),
                    ObjectAnimator.ofFloat(view, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.2f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.2f)
                )
                duration = 900
                interpolator = DecelerateInterpolator(1.5f)
            }
        }

        val set = AnimatorSet()
        set.playTogether(animators)
        set.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                particles.forEach { parent.removeView(it.first) }
                onEnd()
            }
        })
        set.start()
    }

    // --- NOTIFICATION ---
    private fun showNotification(message: String) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.parseColor("#1E1E2E"))
            .setTextColor(Color.WHITE)
            .setAnchorView(pillIndicator)
            .show()
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
                tvBotStatus.text = "EN LIGNE"
                tvBotStatus.setTextColor(getColor(R.color.success))
                btnStartStop.text = "ARRETER"
                btnStartStop.setBackgroundColor(getColor(R.color.danger))
            }
            "stopped" -> {
                tvBotStatus.text = "ARRETE"
                tvBotStatus.setTextColor(getColor(R.color.danger))
                btnStartStop.text = "DEMARRER"
                btnStartStop.setBackgroundColor(getColor(R.color.success))
            }
            "error" -> {
                tvBotStatus.text = "ERREUR"
                tvBotStatus.setTextColor(getColor(R.color.warning))
                btnStartStop.text = "DEMARRER"
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
        tvDailyPnl.text = formatPnl(dash.daily_pnl)
        tvDailyPnl.setTextColor(getPnlColor(dash.daily_pnl))
        tvFloatingPnl.text = formatPnl(dash.floating_pnl)
        tvFloatingPnl.setTextColor(getPnlColor(dash.floating_pnl))
        tvTotalPnl.text = formatPnl(dash.total_pnl)
        tvTotalPnl.setTextColor(getPnlColor(dash.total_pnl))

        tvTrades.text = dash.trades.toString()
        tvWins.text = dash.wins.toString()
        tvLosses.text = dash.losses.toString()
        tvWinrate.text = "${dash.winrate}%"

        tvDailyLimit.text = "${formatPnl(dash.total_pnl)} / ${formatMoney(dash.daily_limit)}"
        progressDailyLimit.progress = dash.limit_pct.toInt().coerceIn(0, 100)

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
                    val cfg = config.config
                    val sb = StringBuilder()
                    val sections = listOf(
                        "CONNEXION TELEGRAM" to listOf("TG_API_ID", "TG_API_HASH"),
                        "CONNEXION METATRADER 5" to listOf("MT5_LOGIN", "MT5_PASSWORD", "MT5_SERVER", "MT5_PATH"),
                        "CANAUX TELEGRAM" to listOf("TG_FOLDER", "TG_ALERT_CHANNEL") +
                                cfg.keys.filter { it.startsWith("TG_CHANNEL") }.sorted(),
                        "SYSTEME MARKET + LIMIT" to listOf("LIMIT_ENABLED", "LIMIT_COUNT", "LIMIT_OFFSET_1", "LIMIT_OFFSET_2", "LIMIT_EXPIRY_MIN"),
                        "LOTS" to listOf("LOT_TOTAL", "LOT_MARKET", "LOT_LIMIT1", "LOT_LIMIT2"),
                        "TAKE PROFIT" to listOf("TP_FIXED_GAIN_USD", "TP_PAR_DEFAUT", "TP_MULTIPE1", "TP_MULTIPE2"),
                        "STOP LOSS" to listOf("MAX_SL_USD"),
                        "TOLERANCES" to listOf("TOLERANCE_ZN", "TOLERANCE_PU", "TOLERANCE_MP", "FUSION_TOLERANCE", "TP_DISTANCE_MIN_RATIO"),
                        "FILTRES HORAIRE" to listOf("TIME_FILTER_ENABLED", "TRADING_START_HOUR", "TRADING_END_HOUR"),
                        "FILTRES NEWS" to listOf("NEWS_FILTER_ENABLED", "NEWS_MIN_IMPACT", "NEWS_WINDOW_BEFORE_BLOCK", "NEWS_WINDOW_BEFORE_CLOSE", "NEWS_WINDOW_AFTER"),
                        "FILTRES TRADINGVIEW" to listOf("TV_FILTER_ENABLED", "TV_FILTER_SYMBOL", "TV_FILTER_SCREENER", "TV_FILTER_EXCHANGE", "TV_FILTER_TIMEFRAME", "TV_FILTER_CACHE_TTL", "TV_STRONG_BUY", "TV_BUY", "TV_STRONG_SELL", "TV_SELL", "TV_NEUTRAL_ALLOW"),
                        "FILTRE CONFLIT" to listOf("CONFLIT_FILTER_ENABLED"),
                        "GESTION DU RISQUE" to listOf("MAX_POSITIONS", "MAX_SPREAD_POINTS", "DAILY_PROFIT_LIMIT"),
                        "QUICK ALERT" to listOf("QUICK_ALERT_SL_OFFSET", "RR_RATIO_DEFAULT"),
                        "PARAMETRES MT5" to listOf("MAGIC_NUMBER", "SLIPPAGE", "ORDER_EXPIRY_MINUTES", "POLL_INTERVAL_SEC"),
                        "ALERTES & LOGS" to listOf("LOG_TRADE_MANAGEMENT", "ALERT_TRADE_MANAGEMENT", "ALERT_DAILY_PERFORMANCE"),
                        "MODE DE FONCTIONNEMENT" to listOf("DEMO_MODE", "RUNTIME_MINUTES")
                    )
                    val usedKeys = mutableSetOf<String>()
                    for ((title, keys) in sections) {
                        val lines = keys.mapNotNull { k ->
                            cfg[k]?.let { v ->
                                usedKeys.add(k)
                                // Supprimer commentaire apres la valeur (tout apres "  # ")
                                val clean = v.replace(Regex("\\s{2,}#.*$"), "").trim()
                                "$k=$clean"
                            }
                        }
                        if (lines.isNotEmpty()) {
                            if (sb.isNotEmpty()) sb.appendLine()
                            sb.appendLine("# $title")
                            lines.forEach { sb.appendLine(it) }
                        }
                    }
                    // Remaining keys not in any section
                    val remaining = cfg.keys.filter { it !in usedKeys }.sorted()
                    if (remaining.isNotEmpty()) {
                        if (sb.isNotEmpty()) sb.appendLine()
                        remaining.forEach { k -> sb.appendLine("$k=${cfg[k]}") }
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
        val configText = etConfigContent.text.toString()
        val values = mutableMapOf<String, String>()
        configText.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val (key, value) = trimmed.split("=", limit = 2)
                values[key.trim()] = value.trim()
            }
        }

        // Confetti burst then save
        burstConfetti(btnSaveConfig) {
            lifecycleScope.launch {
                val ok = client.updateConfig(values)
                if (ok) {
                    showNotification("Configuration sauvegardee")
                } else {
                    showNotification("Erreur de sauvegarde")
                }
            }
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            try {
                val logs = client.getLogs(200)
                if (logs != null) {
                    tvLogs.text = logs.logs.joinToString("\n")
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
                        showNotification("Position fermee: ${formatPnl(result.profit)}")
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

    private fun updatePerformance(trades: List<Trade>) {
        val channelData = mutableMapOf<String, PerfData>()
        val signalData = mutableMapOf<String, PerfData>()

        // Grouper par position_id pour compter les signaux uniques
        val positionSignals = mutableMapOf<Long, Pair<String, String>>() // ticket -> (channel, signal)

        for (t in trades) {
            val parts = t.comment.split("-")
            if (parts.size >= 2) {
                val channel = parts[0]  // CH5, CH3, etc.
                val signal = parts[1]   // ZN, PU, MP, QA, AL
                positionSignals[t.ticket] = Pair(channel, signal)
                channelData.getOrPut(channel) { PerfData() }.add(t, signal)
                signalData.getOrPut(signal) { PerfData() }.add(t)
            }
        }

        // SN = nombre d'ordres MK (Market) par canal
        val channelMkCount = mutableMapOf<String, Int>()
        for (t in trades) {
            val parts = t.comment.split("-")
            if (parts.size >= 3 && parts[2] == "MK") {
                val channel = parts[0]
                channelMkCount[channel] = (channelMkCount[channel] ?: 0) + 1
            }
        }

        // Compter les canaux par type de signal
        val signalChannelCount = mutableMapOf<String, MutableSet<String>>()
        for ((_, pair) in positionSignals) {
            signalChannelCount.getOrPut(pair.second) { mutableSetOf() }.add(pair.first)
        }
        for ((sig, channels) in signalChannelCount) {
            signalData[sig]?.channelCount = channels.size
        }

        // Tableau Performance par Canal
        perfChannelTable.removeAllViews()
        addPerfHeader(perfChannelTable, "Canal", "P&L", "SN", "TR", "WN", "LS", "WR")
        for ((ch, d) in channelData.entries.sortedByDescending { it.value.pnl }.map { it.key to it.value }) {
            val sn = channelMkCount[ch] ?: 0
            addPerfRow(perfChannelTable, ch, d, sn = sn)
        }
        val totalCh = channelData.values.fold(PerfData()) { acc, d -> acc.merge(d) }
        val totalSn = channelMkCount.values.sum()
        addPerfRow(perfChannelTable, "TOTAL", totalCh, sn = totalSn, isTotal = true)

        // Tableau Performance par Signal
        perfSignalTable.removeAllViews()
        addPerfSignalHeader(perfSignalTable)
        for ((sig, d) in signalData.entries.sortedByDescending { it.value.pnl }.map { it.key to it.value }) {
            addPerfSignalRow(perfSignalTable, sig, d)
        }
        val totalSig = signalData.values.fold(PerfData()) { acc, d -> acc.merge(d) }
        addPerfSignalRow(perfSignalTable, "TOTAL", totalSig, isTotal = true)
    }

    private data class PerfData(
        var pnl: Double = 0.0, var trades: Int = 0, var wins: Int = 0, var losses: Int = 0,
        var gain: Double = 0.0, var loss: Double = 0.0,
        val signals: MutableSet<String> = mutableSetOf(), var channelCount: Int = 0
    ) {
        fun add(t: Trade, signal: String = "") {
            pnl += t.profit; trades++
            if (t.profit >= 0) { wins++; gain += t.profit } else { losses++; loss += t.profit }
            if (signal.isNotEmpty()) signals.add(signal)
        }
        fun merge(o: PerfData): PerfData {
            val merged = PerfData(pnl + o.pnl, trades + o.trades, wins + o.wins, losses + o.losses, gain + o.gain, loss + o.loss)
            merged.signals.addAll(signals); merged.signals.addAll(o.signals)
            merged.channelCount = channelCount + o.channelCount
            return merged
        }
        fun winrate() = if (trades > 0) (wins * 100 / trades) else 0
    }

    private fun addPerfHeader(container: LinearLayout, vararg cols: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); setBackgroundColor(Color.parseColor("#1A1A2E")) }
        val weights = floatArrayOf(1.2f, 1f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
        cols.forEachIndexed { i, c ->
            val tv = TextView(this).apply { text = c; setTextColor(getColor(R.color.text_muted)); textSize = 10f; setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights.getOrElse(i) { 1f }))
        }
        container.addView(row)
    }

    private fun addPerfSignalHeader(container: LinearLayout) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); setBackgroundColor(Color.parseColor("#1A1A2E")) }
        val headers = arrayOf("SN", "CN", "P&L", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(0.8f, 0.6f, 1f, 0.6f, 0.6f, 0.6f, 0.6f)
        headers.forEachIndexed { i, c ->
            val tv = TextView(this).apply { text = c; setTextColor(getColor(R.color.text_muted)); textSize = 10f; setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[i]))
        }
        container.addView(row)
    }

    private fun addPerfRow(container: LinearLayout, label: String, d: PerfData, sn: Int = 0, isTotal: Boolean = false) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); if (isTotal) setBackgroundColor(Color.parseColor("#1A1A2E")) }
        fun cell(text: String, weight: Float, color: Int, bold: Boolean = false) {
            val tv = TextView(this).apply { this.text = text; setTextColor(color); textSize = 13f; if (bold) setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight))
        }
        cell(label, 1.2f, getColor(R.color.primary_light), bold = true)
        cell(String.format("%+.2f", d.pnl), 1f, if (d.pnl >= 0) getColor(R.color.success) else getColor(R.color.danger), bold = true)
        cell(sn.toString(), 0.6f, getColor(R.color.text_primary))
        cell(d.trades.toString(), 0.6f, getColor(R.color.text_primary))
        cell(d.wins.toString(), 0.6f, getColor(R.color.success))
        cell(d.losses.toString(), 0.6f, getColor(R.color.danger))
        cell("${d.winrate()}", 0.6f, getColor(R.color.primary_light))
        container.addView(row)
        if (!isTotal) { container.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2A2A4A")) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)) }
    }

    private fun addPerfSignalRow(container: LinearLayout, label: String, d: PerfData, isTotal: Boolean = false) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); if (isTotal) setBackgroundColor(Color.parseColor("#1A1A2E")) }
        fun cell(text: String, weight: Float, color: Int, bold: Boolean = false) {
            val tv = TextView(this).apply { this.text = text; setTextColor(color); textSize = 13f; if (bold) setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight))
        }
        cell(label, 0.8f, getColor(R.color.primary_light), bold = true)
        cell(d.channelCount.toString(), 0.6f, getColor(R.color.text_primary))
        cell(String.format("%+.2f", d.pnl), 1f, if (d.pnl >= 0) getColor(R.color.success) else getColor(R.color.danger), bold = true)
        cell(d.trades.toString(), 0.6f, getColor(R.color.text_primary))
        cell(d.wins.toString(), 0.6f, getColor(R.color.success))
        cell(d.losses.toString(), 0.6f, getColor(R.color.danger))
        cell("${d.winrate()}", 0.6f, getColor(R.color.primary_light))
        container.addView(row)
        if (!isTotal) { container.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2A2A4A")) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        autoRefresh = false
    }
}

// --- Spinning Ring overlay view ---
class SpinningRingView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private var angle = 0f
    private var animator: ValueAnimator? = null

    fun startSpin() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                angle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopSpin() {
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = width / 2f - paint.strokeWidth
        val rect = RectF(width / 2f - r, height / 2f - r, width / 2f + r, height / 2f + r)
        canvas.drawArc(rect, angle, 90f, false, paint)
    }
}

// --- Checkmark overlay view ---
class CheckmarkView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private var progress = 0f

    fun animateCheck() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val s = width / 3f

        val startX = cx - s * 0.6f
        val startY = cy
        val midX = cx - s * 0.1f
        val midY = cy + s * 0.5f
        val endX = cx + s * 0.7f
        val endY = cy - s * 0.4f

        if (progress <= 0.5f) {
            val p = progress * 2f
            canvas.drawLine(startX, startY, startX + (midX - startX) * p, startY + (midY - startY) * p, paint)
        } else {
            canvas.drawLine(startX, startY, midX, midY, paint)
            val p = (progress - 0.5f) * 2f
            canvas.drawLine(midX, midY, midX + (endX - midX) * p, midY + (endY - midY) * p, paint)
        }
    }
}
