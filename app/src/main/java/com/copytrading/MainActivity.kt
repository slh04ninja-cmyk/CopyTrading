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
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.copytrading.api.ApiClient
import com.copytrading.model.*
import com.copytrading.ui.PositionAdapter


import com.copytrading.ui.DateRangePickerDialog
import com.copytrading.config.ConfigParser
import com.copytrading.config.ConfigFieldView
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
    private lateinit var botStatusBadge: View
    private lateinit var statusDot: View
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
    private lateinit var tvDailyLimitPct: TextView
    private lateinit var progressDailyLimit: ProgressBar

    // Controls
    private lateinit var btnStartStop: MaterialButton
    private lateinit var btnCloseAll: MaterialButton
    private lateinit var confirmOverlay: View
    private lateinit var tvConfirmMsg: TextView
    private lateinit var btnConfirmNo: View
    private lateinit var btnConfirmYes: View

    // Positions
    private lateinit var rvPositions: RecyclerView
    private lateinit var tvNoPositions: TextView
    private lateinit var tvPositionsTitle: TextView
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
    private lateinit var positionsContent: LinearLayout
    private lateinit var positionsFooter: LinearLayout
    private lateinit var perfChannelTable: LinearLayout
    private lateinit var perfSignalTable: LinearLayout
    private lateinit var perfSessionTable: LinearLayout
    private lateinit var tvDateRange: TextView
    private var dateFrom: String = ""
    private var dateTo: String = ""
    private var channelSortCol = 1 // 0=ch, 1=pnl, 2=sn, 3=tr, 4=wn, 5=ls, 6=wr
    private var channelSortAsc = false
    private var signalSortCol = 2
    private var signalSortAsc = false
    private var sessionSortCol = 0
    private var sessionSortAsc = false
    private var lastChannelData: List<Pair<String, PerfData>> = emptyList()
    private var lastChannelMkCount: Map<String, Int> = emptyMap()
    private var lastSignalData: List<Pair<String, PerfData>> = emptyList()
    private var lastSessionData: List<Pair<String, PerfData>> = emptyList()
    private val expandedChannels = mutableSetOf<String>()
    private var lastChannelTrades: Map<String, List<Double>> = emptyMap()

    // Panels
    private lateinit var panelDashboard: NestedScrollView
    private lateinit var panelPositions: FrameLayout
    private lateinit var panelConfig: ScrollView
    private lateinit var panelLogs: ScrollView
    private lateinit var pillIndicator: View

    // Config
    private lateinit var configContainer: LinearLayout
    private lateinit var btnSaveConfig: MaterialButton
    private var configGroups: List<ConfigParser.Group> = emptyList()

    // Logs
    private lateinit var tvLogs: TextView
    private lateinit var btnRefreshLogs: MaterialButton

    private var isRunning = false
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
        botStatusBadge = findViewById(R.id.botStatusBadge)
        statusDot = findViewById(R.id.statusDot)
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
        tvDailyLimitPct = findViewById(R.id.tvDailyLimitPct)
        progressDailyLimit = findViewById(R.id.progressDailyLimit)

        btnStartStop = findViewById(R.id.btnStartStop)
        btnCloseAll = findViewById(R.id.btnCloseAll)
        confirmOverlay = findViewById(R.id.confirmOverlay)
        tvConfirmMsg = findViewById(R.id.tvConfirmMsg)
        btnConfirmNo = findViewById(R.id.btnConfirmNo)
        btnConfirmYes = findViewById(R.id.btnConfirmYes)

        rvPositions = findViewById(R.id.rvPositions)
        tvNoPositions = findViewById(R.id.tvNoPositions)
        tvPositionsTitle = findViewById(R.id.tvPositionsTitle)

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
        positionsContent = findViewById(R.id.positionsContent)
        positionsFooter = findViewById(R.id.positionsFooter)
        perfChannelTable = findViewById(R.id.perfChannelTable)
        perfSignalTable = findViewById(R.id.perfSignalTable)
        perfSessionTable = findViewById(R.id.perfSessionTable)
        tvDateRange = findViewById(R.id.tvDateRange)

        panelDashboard = findViewById(R.id.panelDashboard)
        panelPositions = findViewById(R.id.panelPositions)
        panelConfig = findViewById(R.id.panelConfig)
        panelLogs = findViewById(R.id.panelLogs)
        pillIndicator = findViewById(R.id.pillIndicator)

        configContainer = findViewById(R.id.configContainer)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)

        tvLogs = findViewById(R.id.tvLogs)
        btnRefreshLogs = findViewById(R.id.btnRefreshLogs)

        positionAdapter = PositionAdapter()
        rvPositions.layoutManager = LinearLayoutManager(this)
        rvPositions.adapter = positionAdapter

        // Swipe-to-close
        val swipeCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition
                val positions = positionAdapter.getCurrentPositions()
                if (pos in positions.indices) {
                    val ticket = positions[pos].ticket
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Fermer la position #$ticket ?")
                        .setPositiveButton("Confirmer") { _, _ ->
                            lifecycleScope.launch {
                                client.closePosition(ticket)
                                delay(500)
                                refreshPositions()
                                refreshDashboard()
                            }
                        }
                        .setNegativeButton("Annuler") { _, _ ->
                            positionAdapter.notifyItemChanged(pos)
                        }
                        .show()
                }
            }
            override fun onChildDraw(c: android.graphics.Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, isActive: Boolean) {
                val itemView = vh.itemView
                val bg = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FF5252") }
                val icon = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_delete)
                if (dX < 0) {
                    c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), bg)
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        it.setBounds(itemView.right - iconMargin - it.intrinsicWidth, itemView.top + iconMargin, itemView.right - iconMargin, itemView.bottom - iconMargin)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, isActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(rvPositions)
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
            positionsFooter.visibility = View.GONE

            when (index) {
                0 -> {
                    swipeRefresh.visibility = View.VISIBLE
                    if (positionsContent.visibility == View.VISIBLE) {
                        positionsFooter.visibility = View.VISIBLE
                    }
                    updateDashboardBottomPadding()
                }
                1 -> {
                    panelPositions.visibility = View.VISIBLE
                    refreshPerformanceForRange(dateFrom, dateTo)
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
            positionsContent.visibility = View.GONE
            positionsFooter.visibility = View.GONE
            updateDashboardBottomPadding()
            btnOverview.setBackgroundResource(R.drawable.dash_tab_active_bg)
            btnOverview.setTextColor(Color.WHITE)
            btnPerformance.setBackgroundColor(Color.TRANSPARENT)
            btnPerformance.setTextColor(getColor(R.color.text_muted))
        }
        btnPerformance.setOnClickListener {
            overviewContent.visibility = View.GONE
            positionsContent.visibility = View.VISIBLE
            positionsFooter.visibility = View.VISIBLE
            updateDashboardBottomPadding()
            btnPerformance.setBackgroundResource(R.drawable.dash_tab_active_bg)
            btnPerformance.setTextColor(Color.WHITE)
            btnOverview.setBackgroundColor(Color.TRANSPARENT)
            btnOverview.setTextColor(getColor(R.color.text_muted))
            refreshPositions()
        }
    }

    /** ★ Fix : le footer fixe "TOUT FERMER" (~93dp) recouvrait la dernière position.
     *  On réserve l'espace en bas du scroll quand le footer est visible. */
    private fun updateDashboardBottomPadding() {
        val bottom = if (positionsFooter.visibility == View.VISIBLE) dp(104) else dp(16)
        panelDashboard.setPadding(0, 0, 0, bottom)
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
                showConfirmDialog()
            }
        }

        btnConfirmNo.setOnClickListener {
            confirmOverlay.visibility = View.GONE
        }

        btnConfirmYes.setOnClickListener {
            confirmOverlay.visibility = View.GONE
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

    // --- CONFIRM DIALOG ---
    private fun showConfirmDialog() {
        val count = positionAdapter.itemCount
        tvConfirmMsg.text = "Fermer les $count positions ?"
        confirmOverlay.visibility = View.VISIBLE
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
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    try {
                        refreshDashboard()
                        if (panelPositions.visibility == View.VISIBLE) {
                            refreshPerformanceForRange(dateFrom, dateTo)
                        }
                        if (positionsContent.visibility == View.VISIBLE) {
                            refreshPositions()
                        }
                    } catch (_: Exception) {}
                    delay(5000)
                }
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
                    val err = client.lastErrorMessage
                    tvDailyPnl.text = if (err.isNotEmpty()) "ERR: $err" else "ERR: null"
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
                statusDot.background.setTint(getColor(R.color.success))
                botStatusBadge.background.setTint(android.graphics.Color.parseColor("#1900E676"))
                btnStartStop.text = "ARRETER"
                btnStartStop.setBackgroundColor(getColor(R.color.danger))
            }
            "stopped" -> {
                tvBotStatus.text = "ARRETE"
                tvBotStatus.setTextColor(getColor(R.color.danger))
                statusDot.background.setTint(getColor(R.color.danger))
                botStatusBadge.background.setTint(android.graphics.Color.parseColor("#19FF5252"))
                btnStartStop.text = "DEMARRER"
                btnStartStop.setBackgroundColor(getColor(R.color.success))
            }
            "error" -> {
                tvBotStatus.text = "ERREUR"
                tvBotStatus.setTextColor(getColor(R.color.warning))
                statusDot.background.setTint(getColor(R.color.warning))
                botStatusBadge.background.setTint(android.graphics.Color.parseColor("#19FFB74D"))
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
        tvDailyLimitPct.text = "${dash.limit_pct.toInt()}%"
        progressDailyLimit.progress = dash.limit_pct.toInt().coerceIn(0, 100)

        positionAdapter.setPositions(dash.open_positions)
        tvPositionsTitle.text = "Positions Ouvertes (${dash.open_positions.size})"
        tvNoPositions.visibility = if (dash.open_positions.isEmpty()) View.VISIBLE else View.GONE
        rvPositions.visibility = if (dash.open_positions.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun refreshPositions() {
        lifecycleScope.launch {
            val positions = client.getPositions()
            if (positions != null) {
                positionAdapter.setPositions(positions.positions)
                tvPositionsTitle.text = "Positions Ouvertes (${positions.positions.size})"
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
                    // Construire le texte .env à partir du dictionnaire
                    val envText = buildEnvText(cfg)
                    // Parser avec ConfigParser
                    configGroups = ConfigParser.parse(envText)
                    // Rendre les groupes dynamiquement
                    renderConfigGroups(configGroups)
                } else {
                    configContainer.removeAllViews()
                    val tv = TextView(this@MainActivity).apply {
                        text = "Erreur: impossible de charger la config"
                        setTextColor(getColor(R.color.danger))
                    }
                    configContainer.addView(tv)
                }
            } catch (e: Exception) {
                configContainer.removeAllViews()
                val tv = TextView(this@MainActivity).apply {
                    text = "Erreur: ${e.message}"
                    setTextColor(getColor(R.color.danger))
                }
                configContainer.addView(tv)
            }
        }
    }

    /**
     * Construit un texte .env structuré à partir du dictionnaire de config API
     */
    private fun buildEnvText(cfg: Map<String, String>): String {
        val sb = StringBuilder()
        val sections = listOf(
            "CONNEXION TELEGRAM" to listOf("TG_API_ID", "TG_API_HASH"),
            "CONNEXION METATRADER 5" to listOf("MT5_LOGIN", "MT5_PASSWORD", "MT5_SERVER", "MT5_PATH"),
            "CANAUX TELEGRAM" to listOf("TG_FOLDER", "TG_ALERT_CHANNEL") +
                    cfg.keys.filter { it.startsWith("TG_CHANNEL") && it != "TG_CHANNEL_MERGED" }.sorted(),
            "SYSTEME MARKET + LIMIT" to listOf("LIMIT_ENABLED", "LIMIT_COUNT", "LIMIT_OFFSET_1", "LIMIT_OFFSET_2", "LIMIT_EXPIRY_MIN"),
            "TRADE HORS ZONE" to listOf("TRADE_HORS_ZONE", "MAX_DISTANCE", "MAX_TEMPS"),
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
                    val clean = v.replace(Regex("\\s{2,}#.*$"), "").trim()
                    "$k=$clean"
                }
            }
            if (lines.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.appendLine()
                sb.appendLine("# ── $title ──")
                lines.forEach { sb.appendLine(it) }
            }
        }
        val remaining = cfg.keys.filter { it !in usedKeys }.sorted()
        if (remaining.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.appendLine()
            sb.appendLine("# ── AUTRES PARAMETRES ──")
            remaining.forEach { k -> sb.appendLine("$k=${cfg[k]}") }
        }
        return sb.toString()
    }

    /**
     * Génère dynamiquement les vues pour chaque groupe de config
     */
    private fun renderConfigGroups(groups: List<ConfigParser.Group>) {
        configContainer.removeAllViews()

        for (group in groups) {
            if (group.fields.isEmpty()) continue

            // Carte de groupe
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(16)
                }
                setCardBackgroundColor(getColor(R.color.card_background))
                radius = dp(16).toFloat()
                cardElevation = 0f
                strokeWidth = dp(2)
                strokeColor = getColor(R.color.divider)
            }

            val cardContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
            }

            // Titre de section
            val title = TextView(this).apply {
                text = group.title.uppercase()
                setTextColor(getColor(R.color.primary_light))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                letterSpacing = 0.05f
                setPadding(0, 0, 0, dp(12))
            }
            cardContent.addView(title)

            // Champs du groupe
            for (field in group.fields) {
                val fieldView = ConfigFieldView.create(this, field)
                cardContent.addView(fieldView)
            }

            card.addView(cardContent)
            configContainer.addView(card)
        }
    }

    private fun saveConfig() {
        val values = mutableMapOf<String, String>()
        val channelLists = mutableMapOf<String, List<String>>()

        // Parcourir tous les groupes et lire les valeurs des widgets
        for (group in configGroups) {
            for (field in group.fields) {
                if (field.isMergedChannel) {
                    // §6 — Récupérer la liste des canaux
                    val channels = ConfigFieldView.getChannelList(configContainer, field)
                    channelLists[field.key] = channels
                    // Expandre en cles individuelles TG_CHANNEL_1=..., TG_CHANNEL_2=... (renuméroter)
                    for ((i, ch) in channels.withIndex()) {
                        values["TG_CHANNEL_${i + 1}"] = ch
                    }
                    // Supprimer les anciennes cles qui n'ont plus de canal
                    for (j in channels.size until field.originalChannelKeys.size) {
                        values[field.originalChannelKeys[j]] = ""
                    }
                } else {
                    val value = ConfigFieldView.getValue(configContainer, field)
                    values[field.key] = value
                }
            }
        }

        btnSaveConfig.isEnabled = false
        btnSaveConfig.text = "SAUVEGARDE..."

        lifecycleScope.launch {
            try {
                val ok = client.updateConfig(values)
                if (ok) {
                    btnSaveConfig.text = "SAUVEGARDE..."
                    showNotification("Config sauvee")

                    // Plus besoin de restart — le dashboard relit le .env dynamiquement
                    delay(1000)
                    refreshDashboard()
                    burstConfetti(btnSaveConfig) {}
                    showNotification("Bot redemarré avec la nouvelle config")
                } else {
                    showNotification("Erreur de sauvegarde")
                }
            } catch (e: Exception) {
                showNotification("Erreur: ${e.message}")
            } finally {
                btnSaveConfig.isEnabled = true
                btnSaveConfig.text = "SAUVEGARDER"
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

        // Store data for re-sort
        lastChannelData = channelData.entries.map { it.key to it.value }
        lastChannelMkCount = channelMkCount
        lastSignalData = signalData.entries.map { it.key to it.value }

        // Store raw profits per channel for metrics
        val channelTrades = mutableMapOf<String, MutableList<Double>>()
        for (t in trades) {
            val parts = t.comment.split("-")
            if (parts.size >= 2) {
                channelTrades.getOrPut(parts[0]) { mutableListOf() }.add(t.profit)
            }
        }
        lastChannelTrades = channelTrades

        // Group by hour (UTC 01h-24h)
        val sessionData = mutableMapOf<String, PerfData>()
        val sessionChannels = mutableMapOf<String, MutableSet<String>>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        for (t in trades) {
            val parts = t.comment.split("-")
            val ch = if (parts.size >= 2) parts[0] else ""
            try {
                val date = sdf.parse(t.close_time)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                    cal.time = date
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val label = String.format("%02dh", hour)
                    sessionData.getOrPut(label) { PerfData() }.add(t)
                    if (ch.isNotEmpty()) sessionChannels.getOrPut(label) { mutableSetOf() }.add(ch)
                }
            } catch (_: Exception) {}
        }
        for ((label, d) in sessionData) {
            d.channelCount = sessionChannels[label]?.size ?: 0
        }
        lastSessionData = sessionData.entries.map { it.key to it.value }.sortedBy { it.first }

        renderChannelTable()
        renderSignalTable()
        renderSessionTable()
    }

    private fun renderChannelTable() {
        perfChannelTable.removeAllViews()
        val headers = arrayOf("Canal", "P&L", "SN", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(1.2f, 1f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
        addPerfHeader(perfChannelTable, headers, weights, channelSortCol, channelSortAsc) { col ->
            if (channelSortCol == col) channelSortAsc = !channelSortAsc else { channelSortCol = col; channelSortAsc = col == 0 }
            renderChannelTable()
        }
        val sorted = sortPerfData(lastChannelData, channelSortCol, channelSortAsc)
        for ((ch, d) in sorted) {
            val sn = lastChannelMkCount[ch] ?: 0
            addPerfRow(perfChannelTable, ch, d, sn = sn)
        }
        val totalCh = lastChannelData.fold(PerfData()) { acc, (_, d) -> acc.merge(d) }
        val totalSn = lastChannelMkCount.values.sum()
        addPerfRow(perfChannelTable, "TOTAL", totalCh, sn = totalSn, isTotal = true)
    }

    private fun renderSignalTable() {
        perfSignalTable.removeAllViews()
        val headers = arrayOf("Signal", "CN", "P&L", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(0.8f, 0.6f, 1f, 0.6f, 0.6f, 0.6f, 0.6f)
        addPerfHeader(perfSignalTable, headers, weights, signalSortCol, signalSortAsc) { col ->
            if (signalSortCol == col) signalSortAsc = !signalSortAsc else { signalSortCol = col; signalSortAsc = col == 0 }
            renderSignalTable()
        }
        val sorted = sortPerfData(lastSignalData, signalSortCol, signalSortAsc, isSignal = true)
        for ((sig, d) in sorted) {
            addPerfSignalRow(perfSignalTable, sig, d)
        }
        val totalSig = lastSignalData.fold(PerfData()) { acc, (_, d) -> acc.merge(d) }
        addPerfSignalRow(perfSignalTable, "TOTAL", totalSig, isTotal = true)
    }

    private fun renderSessionTable() {
        perfSessionTable.removeAllViews()
        val headers = arrayOf("Heure", "CN", "P&L", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(0.8f, 0.6f, 1f, 0.6f, 0.6f, 0.6f, 0.6f)
        addPerfHeader(perfSessionTable, headers, weights, sessionSortCol, sessionSortAsc) { col ->
            if (sessionSortCol == col) sessionSortAsc = !sessionSortAsc else { sessionSortCol = col; sessionSortAsc = col == 0 }
            renderSessionTable()
        }
        val sorted = sortPerfData(lastSessionData, sessionSortCol, sessionSortAsc, isSignal = true)
        for ((heure, d) in sorted) {
            addPerfSignalRow(perfSessionTable, heure, d)
        }
        val totalSess = lastSessionData.fold(PerfData()) { acc, (_, d) -> acc.merge(d) }
        // CN total = unique channels across all hours (not sum of per-hour CN)
        totalSess.channelCount = lastChannelTrades.size
        addPerfSignalRow(perfSessionTable, "TOTAL", totalSess, isTotal = true)
    }

    private fun sortPerfData(data: List<Pair<String, PerfData>>, col: Int, asc: Boolean, isSignal: Boolean = false): List<Pair<String, PerfData>> {
        val sorted = if (isSignal) {
            when (col) {
                0 -> data.sortedBy { it.first }
                1 -> data.sortedBy { it.second.channelCount }
                2 -> data.sortedBy { it.second.pnl }
                3 -> data.sortedBy { it.second.trades }
                4 -> data.sortedBy { it.second.wins }
                5 -> data.sortedBy { it.second.losses }
                6 -> data.sortedBy { it.second.winrate() }
                else -> data.sortedBy { it.second.pnl }
            }
        } else {
            when (col) {
                0 -> data.sortedBy { it.first }
                1 -> data.sortedBy { it.second.pnl }
                2 -> data.sortedBy { lastChannelMkCount[it.first] ?: 0 }
                3 -> data.sortedBy { it.second.trades }
                4 -> data.sortedBy { it.second.wins }
                5 -> data.sortedBy { it.second.losses }
                6 -> data.sortedBy { it.second.winrate() }
                else -> data.sortedBy { it.second.pnl }
            }
        }
        return if (asc) sorted else sorted.reversed()
    }

    private data class PerfData(
        var pnl: Double = 0.0, var trades: Int = 0, var wins: Int = 0, var losses: Int = 0,
        var gain: Double = 0.0, var loss: Double = 0.0,
        val signals: MutableSet<String> = mutableSetOf(), var channelCount: Int = 0,
        val tradeProfits: MutableList<Double> = mutableListOf()
    ) {
        fun add(t: Trade, signal: String = "") {
            pnl += t.profit; trades++
            if (t.profit >= 0) { wins++; gain += t.profit } else { losses++; loss += t.profit }
            if (signal.isNotEmpty()) signals.add(signal)
            tradeProfits.add(t.profit)
        }
        fun merge(o: PerfData): PerfData {
            val merged = PerfData(pnl + o.pnl, trades + o.trades, wins + o.wins, losses + o.losses, gain + o.gain, loss + o.loss)
            merged.signals.addAll(signals); merged.signals.addAll(o.signals)
            merged.channelCount = channelCount + o.channelCount
            merged.tradeProfits.addAll(tradeProfits); merged.tradeProfits.addAll(o.tradeProfits)
            return merged
        }
        fun winrate() = if (trades > 0) (wins * 100 / trades) else 0
        fun profitFactor(): Double {
            val totalGain = tradeProfits.filter { it > 0 }.sum()
            val totalLoss = kotlin.math.abs(tradeProfits.filter { it < 0 }.sum())
            return if (totalLoss > 0) totalGain / totalLoss else if (totalGain > 0) 999.0 else 0.0
        }
        fun riskReward(): Double {
            val winsList = tradeProfits.filter { it > 0 }
            val lossList = tradeProfits.filter { it < 0 }
            val avgWin = if (winsList.isNotEmpty()) winsList.average() else 0.0
            val avgLoss = if (lossList.isNotEmpty()) kotlin.math.abs(lossList.average()) else 0.0
            return if (avgLoss > 0) avgWin / avgLoss else if (avgWin > 0) 999.0 else 0.0
        }
        fun maxDrawdown(): Double {
            var peak = 0.0; var equity = 0.0; var maxDd = 0.0
            for (p in tradeProfits) {
                equity += p
                if (equity > peak) peak = equity
                val dd = peak - equity
                if (dd > maxDd) maxDd = dd
            }
            return maxDd
        }
    }

    private fun addPerfHeader(container: LinearLayout, headers: Array<String>, weights: FloatArray, sortCol: Int, sortAsc: Boolean, onSort: (Int) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); setBackgroundColor(Color.parseColor("#1A1A2E")) }
        headers.forEachIndexed { i, c ->
            val arrow = if (i == sortCol) if (sortAsc) " \u25B2" else " \u25BC" else ""
            val tv = TextView(this).apply {
                text = "$c$arrow"; setTextColor(getColor(if (i == sortCol) R.color.accent else R.color.text_muted))
                textSize = 10f; setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER
                setOnClickListener { onSort(i) }; isClickable = true
            }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights.getOrElse(i) { 1f }))
        }
        container.addView(row)
    }

    private fun addPerfSignalHeader(container: LinearLayout, headers: Array<String>, weights: FloatArray, sortCol: Int, sortAsc: Boolean, onSort: (Int) -> Unit) {
        addPerfHeader(container, headers, weights, sortCol, sortAsc, onSort)
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
        val wr = d.winrate()
        cell("$wr", 0.6f, getColor(if (wr >= 50) R.color.success else R.color.danger))

        // Expandable detail
        val isExpanded = expandedChannels.contains(label)
        val detail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(6), dp(12), dp(6))
            setBackgroundColor(Color.parseColor("#12122A")); visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
        // Compute from raw trades
        val profits = lastChannelTrades[label] ?: emptyList()
        val totalGain = profits.filter { it > 0 }.sum()
        val totalLoss = kotlin.math.abs(profits.filter { it < 0 }.sum())
        val pf = if (totalLoss > 0) totalGain / totalLoss else if (totalGain > 0) 99.0 else 0.0
        val avgWin = profits.filter { it > 0 }.let { if (it.isNotEmpty()) it.average() else 0.0 }
        val avgLoss = profits.filter { it < 0 }.let { if (it.isNotEmpty()) kotlin.math.abs(it.average()) else 0.0 }
        val rr = if (avgLoss > 0) avgWin / avgLoss else if (avgWin > 0) 99.0 else 0.0
        var peak = 0.0; var equity = 0.0; var md = 0.0
        for (p in profits) { equity += p; if (equity > peak) peak = equity; val dd = peak - equity; if (dd > md) md = dd }
        val pfStr = if (pf >= 99) "99" else String.format("%.2f", pf)
        val rrStr = if (rr >= 99) "99" else String.format("%.2f", rr)
        val mdStr = if (md <= 0) "00" else String.format("%.2f", md)
        val pfColor = getColor(if (pf >= 1.5) R.color.success else if (pf >= 1.0) R.color.warning else R.color.danger)
        val rrColor = getColor(if (rr >= 1.5) R.color.success else if (rr >= 1.0) R.color.warning else R.color.danger)
        val mdColor = getColor(R.color.danger)
        val line = TextView(this).apply {
            textSize = 12f; gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6))
            text = android.text.SpannableString("PF = $pfStr | RR = $rrStr | MD = $mdStr").apply {
                val pfStart = indexOf(pfStr); setSpan(android.text.style.ForegroundColorSpan(pfColor), pfStart, pfStart + pfStr.length, 0)
                val rrStart = indexOf(rrStr, pfStart + pfStr.length); setSpan(android.text.style.ForegroundColorSpan(rrColor), rrStart, rrStart + rrStr.length, 0)
                val mdStart = indexOf(mdStr, rrStart + rrStr.length); setSpan(android.text.style.ForegroundColorSpan(mdColor), mdStart, mdStart + mdStr.length, 0)
            }
        }
        detail.addView(line)

        if (!isTotal) {
            row.setOnClickListener {
                if (expandedChannels.contains(label)) expandedChannels.remove(label) else expandedChannels.add(label)
                detail.visibility = if (detail.visibility == View.GONE) View.VISIBLE else View.GONE
            }
            row.isClickable = true
        }

        container.addView(row)
        container.addView(detail)
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
        val wr = d.winrate()
        cell("$wr", 0.6f, getColor(if (wr >= 50) R.color.success else R.color.danger))
        container.addView(row)
        if (!isTotal) { container.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2A2A4A")) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
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
