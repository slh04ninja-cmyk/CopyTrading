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
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import android.widget.GridLayout
import androidx.drawerlayout.widget.DrawerLayout
import android.app.DatePickerDialog
import java.io.File
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
import com.copytrading.config.MotifCodes
import com.copytrading.config.ConfigFieldView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.async
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
    private lateinit var tvDailyGoalTitle: TextView
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

    // Trading Hours
    private lateinit var tvTradingHoursCount: TextView
    private lateinit var chipGroupHours: LinearLayout

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
    private lateinit var btnPdfReport: ImageView
    private var dateFrom: String = ""
    private var dateTo: String = ""
    private var channelSortCol = 1 // 0=ch, 1=pnl, 2=sn, 3=tr, 4=wn, 5=ls, 6=wr
    private var channelSortAsc = false
    private var signalSortCol = 1 // 0=signal, 1=pnl, 2=cn, 3=tr, 4=wn, 5=ls, 6=wr
    private var signalSortAsc = false
    private var sessionSortCol = 0 // 0=heure, 1=pnl, 2=cn, 3=tr, 4=wn, 5=ls, 6=wr
    private var sessionSortAsc = false
    private var lastChannelData: List<Pair<String, PerfData>> = emptyList()
    private var lastChannelMkCount: Map<String, Int> = emptyMap()
    private var lastSignalData: List<Pair<String, PerfData>> = emptyList()
    private var lastSessionData: List<Pair<String, PerfData>> = emptyList()
    private var lastChannelTrades: Map<String, List<Double>> = emptyMap()
    /** ★ 26/09 : trades complets par canal (popup de detail) + noms des canaux (Channels.txt). */
    private var lastChannelTradesList: Map<String, List<Trade>> = emptyMap()
    private var channelNames: Map<String, String> = emptyMap()

    // === FILTRES DE L'HISTORIQUE (choix multiple, heure d'ouverture UTC) ===
    private lateinit var historyFilters: LinearLayout
    private lateinit var tvFilterCount: TextView
    private lateinit var perfTotaux: LinearLayout
    private var histTradesRaw: List<Trade> = emptyList()
    private val fSignals = linkedSetOf("MP", "PU", "QA", "ZN")
    private val fSens = linkedSetOf("ACHAT", "VENTE")
    private val fExits = linkedSetOf("TP", "SL", "NWS", "NS", "TMO", "DTG", "DD", "BOT")
    private val fHours = linkedSetOf<Int>().apply { (0..23).forEach { add(it) } }
    /** Motifs de fermeture ecrits par le bot (fin du commentaire MT5). "BOT" ramasse tout le reste :
     *  un nouveau code ajoute au bot n'est jamais perdu, il tombe simplement dans BOT. */
    private val MOTIFS_FIXES = listOf("NWS", "NS", "TMO", "DTG", "DD")
    /** Toutes les cases de filtres -> pour afficher le nombre de signaux de chaque valeur. */
    private val chipRefs = mutableMapOf<String, TextView>()
    private val fExcluded = mutableSetOf<String>()   // canaux exclus (CHxxx) - applique apres 3 s
    private var lastExcludedData: List<Pair<String, PerfData>> = emptyList()
    private var lastTradesComptes: List<Trade> = emptyList()   // jambes reellement comptees (pour le P&L par jour du PDF)
    private var totalSignaux = 0                     // signaux de la periode (denominateur du bandeau)
    private val filterHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingFilterApply: Runnable? = null

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

    // Trading Hours state (24 booleans, true = active)
    private var tradingHours = BooleanArray(24) { it in 5..20 } // default: 5h-21h
    private var tradingHoursInitialized = false // avoid reset on each refresh
    private var lastHoursSignature: String? = null // evite de reconstruire les chips si inchangé
    // Heures reelles du .env serveur (source de verite pour l'affichage des chips)
    private var serverHours = BooleanArray(24) { it in 5..20 }

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
        // Charge la config une fois au demarrage pour afficher les vraies heures de trading
        // (le dashboard ne doit pas montrer le defaut 5..20 tant que Config n'a pas ete ouvert)
        loadTradingHoursOnce()
    }

    /** Charge silencieusement la config (heures de trading) au demarrage, sans ouvrir l'onglet Config. */
    private fun loadTradingHoursOnce() {
        lifecycleScope.launch {
            try {
                val cfg = client.getConfig()?.config ?: return@launch
                initTradingHoursFromConfig(cfg)
                MotifCodes.setFromConfig(cfg)
            } catch (_: Exception) {}
        }
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

        tvDailyGoalTitle = findViewById(R.id.tvDailyGoalTitle)
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

        historyFilters = findViewById(R.id.historyFilters)
        tvFilterCount = findViewById(R.id.tvFilterCount)
        perfTotaux = findViewById(R.id.perfTotaux)
        findViewById<TextView>(R.id.btnFilterReset).setOnClickListener { resetHistoryFilters() }
        buildHistoryFilters()

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
        btnPdfReport = findViewById(R.id.btnPdfReport)

        panelDashboard = findViewById(R.id.panelDashboard)
        panelPositions = findViewById(R.id.panelPositions)
        panelConfig = findViewById(R.id.panelConfig)
        panelLogs = findViewById(R.id.panelLogs)
        pillIndicator = findViewById(R.id.pillIndicator)

        // Trading Hours
        tvTradingHoursCount = findViewById(R.id.tvTradingHoursCount)
        chipGroupHours = findViewById(R.id.chipGroupHours)

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

    // ==================== FILTRES DE L'HISTORIQUE ====================
    // 4 filtres a choix multiple (cases). Tout est recalcule dans l'appli, sans appel serveur.
    // Heure = heure d'OUVERTURE de la position, en UTC.
    // Regle speciale : cocher MK et/ou L3 ramene TOUT le signal (ses jambes L1/L2/L4 comprises).

    private fun buildHistoryFilters() {
        if (!::historyFilters.isInitialized) return
        historyFilters.removeAllViews()
        // Sens + type de signal sur 2 lignes, sans titre, pleine largeur (comme le filtre Sortie) :
        //   ACHAT  MP  QA
        //   VENTE  PU  ZN
        listOf(
            listOf("ACHAT" to fSens, "MP" to fSignals, "QA" to fSignals),
            listOf("VENTE" to fSens, "PU" to fSignals, "ZN" to fSignals)
        ).forEach { ligne ->
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            ligne.forEach { (k, set) ->
                val tv = filterChip(k)
                tv.layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    marginEnd = dp(4)
                    topMargin = dp(4)
                }
                styleChip(tv, set.contains(k))
                chipRefs[k] = tv
                tv.setOnClickListener {
                    if (set.contains(k)) set.remove(k) else set.add(k)
                    styleChip(tv, set.contains(k))
                    requestFilterApply(tv)
                }
                r.addView(tv)
            }
            historyFilters.addView(r)
        }
        // Motifs de fermeture ecrits par le bot depuis le 12/10/2026 (NWS ajoute le 14/10).
        addFilterGrid("Sortie  \u00b7  depuis 12/10", listOf("TP", "SL") + MOTIFS_FIXES + "BOT", fExits)
        addFilterLabel("Heures de trading (UTC)")
        // Rangees de 6 cases a largeur egale (GridLayout ne transmettait pas les taps de facon fiable).
        for (row in (0 until 24).chunked(6)) {
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.forEach { h ->
                val tv = filterChip(String.format(Locale.US, "%02d", h))
                tv.layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    marginEnd = dp(4)
                    topMargin = dp(4)
                }
                styleChip(tv, fHours.contains(h))
                tv.setOnClickListener {
                    if (fHours.contains(h)) fHours.remove(h) else fHours.add(h)
                    styleChip(tv, fHours.contains(h))
                    requestFilterApply(tv)
                }
                r.addView(tv)
            }
            historyFilters.addView(r)
        }
    }

    private fun addFilterLabel(txt: String) {
        historyFilters.addView(TextView(this).apply {
            text = txt
            textSize = 11f
            setTextColor(getColor(R.color.text_muted))
            setPadding(0, dp(8), 0, dp(4))
        })
    }


    /** Meme chose mais par rangees de 4 cases a largeur egale (motifs de sortie, 8 cases). */
    private fun addFilterGrid(label: String, keys: List<String>, set: MutableSet<String>) {
        addFilterLabel(label)
        keys.chunked(4).forEach { row ->
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.forEach { k ->
                val tv = filterChip(k)
                tv.layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    marginEnd = dp(4)
                    topMargin = dp(4)
                }
                styleChip(tv, set.contains(k))
                chipRefs[k] = tv
                tv.setOnClickListener {
                    if (set.contains(k)) set.remove(k) else set.add(k)
                    styleChip(tv, set.contains(k))
                    requestFilterApply(tv)
                }
                r.addView(tv)
            }
            historyFilters.addView(r)
        }
    }

    private fun filterChip(txt: String): TextView = TextView(this).apply {
        text = txt
        textSize = 12f
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = android.view.Gravity.CENTER
        setPadding(dp(12), 0, dp(12), 0)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)).apply {
            marginEnd = dp(6)
        }
    }

    private fun styleChip(tv: TextView, active: Boolean) {
        tv.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            if (active) {
                setColor(getColor(R.color.primary))
                setStroke(0, Color.TRANSPARENT)
            } else {
                setColor(Color.parseColor("#0F0F1A"))
                setStroke(dp(2), getColor(R.color.divider))
            }
        }
        tv.setTextColor(if (active) Color.WHITE else getColor(R.color.text_primary))
    }

    private fun resetHistoryFilters() {
        fSignals.clear(); fSignals.addAll(listOf("MP", "PU", "QA", "ZN"))
        fSens.clear(); fSens.addAll(listOf("ACHAT", "VENTE"))
        fExits.clear(); fExits.addAll(listOf("TP", "SL", "NWS", "NS", "TMO", "DTG", "DD", "BOT"))
        fHours.clear(); (0..23).forEach { fHours.add(it) }
        fExcluded.clear()
        buildHistoryFilters()
        applyHistoryFilters()
    }

    private fun roleOf(t: Trade): String = t.comment.split("-").getOrNull(2) ?: ""

    private fun typeOf(t: Trade): String = t.comment.split("-").getOrNull(1) ?: ""

    /** Sortie du signal : TP et SL viennent du code MT5 (5 / 4). Pour les fermetures du bot, le motif
     *  est ecrit dans le commentaire (CH144-MP-L1-TMO) mais le separateur peut varier -> on decoupe sur
     *  tout ce qui n'est pas alphanumerique et on cherche un motif connu N'IMPORTE OU dans le commentaire
     *  (l'ancienne version ne regardait que le dernier segment, ce qui renvoyait tout dans "BOT"). */
    private fun exitOf(t: Trade): String {
        if (t.reason == 5) return "TP"
        if (t.reason == 4) return "SL"
        val jetons = t.comment.uppercase().split(Regex("[^A-Z0-9]+"))
        for (j in jetons) {
            if (j in MOTIFS_FIXES) return j
        }
        return "BOT"
    }

    /** Heure UTC d'ouverture, lue directement dans la chaine ISO ("2026-10-12T08:35:00" -> 8).
     *  Aucune allocation : avant, un SimpleDateFormat + Calendar etait cree pour CHAQUE trade a
     *  CHAQUE application de filtre -> l'ecran se bloquait et des taps se perdaient. */
    private fun openHourUtc(t: Trade): Int {
        val s = t.open_time
        if (s.length < 13 || s[10] != 'T') return -1
        return s.substring(11, 13).toIntOrNull() ?: -1
    }

    /** Regroupe les positions en signaux, canal par canal : un MK ou un L3 ouvre, ses jambes suivent. */
    private fun signalGroups(all: List<Trade>): List<List<Trade>> {
        val groups = mutableListOf<List<Trade>>()
        all.groupBy { it.comment.substringBefore("-") }.values.forEach { lst ->
            var cur: MutableList<Trade>? = null
            lst.sortedBy { it.open_time }.forEach { t ->
                val role = roleOf(t)
                if (role == "MK" || role == "L3" || cur == null) {
                    cur = mutableListOf()
                    groups.add(cur!!)
                }
                cur!!.add(t)
            }
        }
        return groups
    }

    /** On travaille TOUJOURS par signal : les filtres sont juges sur la jambe d'ENTREE (MK/L3) et le
     *  signal est garde en entier -> le P&L affiche est celui du signal complet (+12 $ au TP / -19 $ au SL). */
    private fun filterTrades(all: List<Trade>, excl: Boolean = true): List<Trade> {
        if (all.isEmpty()) return all
        val keep = HashSet<Long>()
        signalGroups(all).forEach { g ->
            val head = g.first()
            if (excl && fExcluded.contains(head.comment.substringBefore("-"))) return@forEach
            if (!fSignals.contains(typeOf(head))) return@forEach
            val sens = if (head.type.uppercase() == "BUY") "ACHAT" else "VENTE"
            if (!fSens.contains(sens)) return@forEach
            if (!fExits.contains(exitOf(head))) return@forEach
            val h = openHourUtc(head)
            if (h >= 0 && !fHours.contains(h)) return@forEach
            g.forEach { keep.add(it.ticket) }
        }
        return all.filter { keep.contains(it.ticket) }
    }

    /** Recalcul differe de 3 s : on peut cocher plusieurs canaux a exclure d'affilee sans recalculer a chaque tap. */
    private fun scheduleFilterApply() {
        pendingFilterApply?.let { filterHandler.removeCallbacks(it) }
        val r = Runnable { applyHistoryFilters() }
        pendingFilterApply = r
        filterHandler.postDelayed(r, 3000)
    }

    /** Agrege chaque signal en UNE unite de comptage : P&L = somme de ses jambes (ex. MK +3.50 et L1 +8.50
     *  ferment au meme prix -> +12 $ ; au SL -> -19 $). Une seule ligne par signal, comme le voit le compte. */
    private fun signalAggregates(visible: List<Trade>): List<Trade> {
        val out = mutableListOf<Trade>()
        signalGroups(visible).forEach { g ->
            if (g.isEmpty()) return@forEach
            val head = g.first()
            out.add(head.copy(
                profit = g.sumOf { it.profit },
                ticket = head.ticket,
                close_time = g.maxOf { it.close_time },
                reason = head.reason
            ))
        }
        return out
    }

    /** Applique les filtres APRES l'evenement de touch : sinon le recalcul se fait pendant la
     *  distribution du tap et le premier cochage semble ne rien faire. */
    private fun requestFilterApply(anchor: View) {
        anchor.post { applyHistoryFilters() }
    }

    /** Compteur sur chaque case : nombre de signaux qu'elle apporterait, tous les AUTRES filtres
     *  restant appliques (comptage "par facette"). Une case a 0 est estompee. */
    private fun majCompteurs() {
        if (chipRefs.isEmpty()) return
        val parType = HashMap<String, Int>()
        val parSens = HashMap<String, Int>()
        val parExit = HashMap<String, Int>()
        signalGroups(histTradesRaw).forEach { g ->
            val head = g.first()
            val type = typeOf(head)
            val sens = if (head.type.uppercase() == "BUY") "ACHAT" else "VENTE"
            val ex = exitOf(head)
            val h = openHourUtc(head)
            val okCh = !fExcluded.contains(head.comment.substringBefore("-"))
            val okType = fSignals.contains(type)
            val okSens = fSens.contains(sens)
            val okExit = fExits.contains(ex)
            val okHour = h < 0 || fHours.contains(h)
            if (okCh && okSens && okExit && okHour) parType[type] = (parType[type] ?: 0) + 1
            if (okCh && okType && okExit && okHour) parSens[sens] = (parSens[sens] ?: 0) + 1
            if (okCh && okType && okSens && okHour) parExit[ex] = (parExit[ex] ?: 0) + 1
        }
        chipRefs.forEach { (k, tv) ->
            val n = when (k) {
                "MP", "PU", "QA", "ZN" -> parType[k]
                "ACHAT", "VENTE" -> parSens[k]
                else -> parExit[k]
            } ?: 0
            tv.text = if (n > 0) "$k $n" else k
            tv.alpha = if (n > 0) 1f else 0.45f
        }
    }

    private fun applyHistoryFilters() {
        majCompteurs()
        // Tableau par canal : tous les canaux (les exclus restent affiches, grises et decochables).
        val visibles = filterTrades(histTradesRaw, excl = false)
        // Calculs (totaux, tableaux signal et heure) : canaux exclus retires.
        val gardes = visibles.filter { !fExcluded.contains(it.comment.substringBefore("-")) }
        lastTradesComptes = gardes
        val signaux = signalAggregates(gardes)
        lastExcludedData = visibles.groupBy { it.comment.substringBefore("-") }
            .filterKeys { fExcluded.contains(it) }
            .map { (ch, lst) -> ch to perfAgrege(lst) }
            .sortedBy { it.first }
        if (::tvFilterCount.isInitialized) {
            // Temoin : combien de signaux ont un motif connu ECRIT dans le commentaire (diagnostic).
            val vus = signalGroups(histTradesRaw).count { g ->
                val c = g.first().comment.uppercase()
                MOTIFS_FIXES.any { c.contains(it) }
            }
            tvFilterCount.text = String.format(java.util.Locale.US, "%d / %d signaux \u00b7 motif vu %d",
                signaux.size, totalSignaux, vus)
        }
        // Toujours 1 signal = 1 trade ; le popup de detail garde les jambes une par une (canaux exclus compris).
        updatePerformance(signaux, visibles)
    }

    /** Stats d'un canal exclu (affiche gris, hors total). */
    private fun perfAgrege(lst: List<Trade>): PerfData {
        val p = PerfData()
        signalAggregates(lst).forEach { p.add(it, typeOf(it)) }
        return p
    }

    /** Evite qu'un recalcul differe (3 s) touche des vues detruites = plantation. */
    override fun onDestroy() {
        pendingFilterApply?.let { filterHandler.removeCallbacks(it) }
        pendingFilterApply = null
        super.onDestroy()
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

        // Bouton rapport PDF : imprime les tableaux AFFICHES (filtres compris), sans appel serveur
        btnPdfReport.setOnClickListener {
            if (dateFrom.isEmpty() || dateTo.isEmpty()) {
                Snackbar.make(btnPdfReport, "Choisis une periode avec le calendrier", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnPdfReport.isEnabled = false
            btnPdfReport.alpha = 0.5f
            try {
                val pdfFile = genererPdfFiltre()
                if (pdfFile != null) openPdf(pdfFile)
                else Snackbar.make(btnPdfReport, "Rien a exporter sur cette periode", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(btnPdfReport, "Erreur PDF: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                btnPdfReport.isEnabled = true
                btnPdfReport.alpha = 1f
            }
        }
    }

    /** Ouvre le PDF genere via le FileProvider (intent viewer PDF). */
    // ================= PDF GENERE DEPUIS LES TABLEAUX AFFICHES =================
    // Aucun recalcul : on imprime lastChannelData / lastSignalData / lastSessionData + le total,
    // donc le PDF est TOUJOURS identique a ce que l'utilisateur voit apres ses filtres.

    private fun descFiltres(): String {
        val p = mutableListOf<String>()
        if (fSens.size < 2) p.add(if (fSens.contains("ACHAT")) "achats seulement" else "ventes seulement")
        if (fSignals.size < 4) p.add("types " + fSignals.sorted().joinToString(","))
        if (fExits.size < 8) p.add("sorties " + fExits.sorted().joinToString(","))
        if (fHours.size < 24) {
            val excl = (0..23).filter { !fHours.contains(it) }
            p.add("heures exclues " + excl.joinToString(",") { String.format(Locale.US, "%02dh", it) })
        }
        if (fExcluded.isNotEmpty()) p.add("canaux exclus " + fExcluded.map { it.removePrefix("CH") }.sorted().joinToString(","))
        return if (p.isEmpty()) "aucun filtre" else p.joinToString("  |  ")
    }

    private fun genererPdfFiltre(): File? {
        // ---- style repris a l'identique du rapport du serveur (_report_dyn.py) :
        //      A4 portrait, marges 10 mm, Helvetica, en-tetes GRIS CLAIR (200,200,215),
        //      titres de section (225,225,238), KPI en 5 boites, lignes alternees (248,248,252).
        val NOIR = Color.rgb(0, 0, 0)
        val GRIS = Color.rgb(90, 90, 90)
        val GRIS_KPI = Color.rgb(80, 80, 110)
        val VERT = Color.rgb(0, 130, 0)
        val ROUGE = Color.rgb(190, 30, 30)
        val VERT_KPI = Color.rgb(0, 150, 60)
        val ROUGE_KPI = Color.rgb(200, 40, 40)
        val FILL_KPI = Color.rgb(243, 243, 248)
        val FILL_SECTION = Color.rgb(225, 225, 238)
        val FILL_HEAD = Color.rgb(200, 200, 215)
        val FILL_JOURS = Color.rgb(240, 240, 245)
        val FILL_ALT = Color.rgb(248, 248, 252)
        val BORD = Color.rgb(120, 120, 130)
        val MM = 2.83465f
        val doc = android.graphics.pdf.PdfDocument()
        val paint = Paint().apply { isAntiAlias = true; typeface = Typeface.SANS_SERIF }
        val W = 595
        val H = 842
        val M = 10 * MM
        val BAS = H - 18 * MM
        var pageNo = 1
        var page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(W, H, pageNo).create())
        var cv = page.canvas
        var y = 10 * MM

        fun frDate(s: String): String = try {
            s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4)
        } catch (e: Exception) {
            s
        }
        val dFr = frDate(dateFrom)
        val dTo = frDate(dateTo)
        val rappel = "Rapport $dFr -> $dTo"

        fun txt(s: String, x: Float, yy: Float, taille: Float, gras: Boolean, couleur: Int,
                align: Paint.Align = Paint.Align.LEFT) {
            paint.textSize = taille
            paint.isFakeBoldText = gras
            paint.color = couleur
            paint.textAlign = align
            cv.drawText(s, x, yy, paint)
        }

        fun cadre(x: Float, yy: Float, w: Float, h: Float, remplissage: Int?) {
            if (remplissage != null) {
                paint.color = remplissage
                paint.style = Paint.Style.FILL
                cv.drawRect(x, yy, x + w, yy + h, paint)
            }
            paint.color = BORD
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.4f
            cv.drawRect(x, yy, x + w, yy + h, paint)
            paint.style = Paint.Style.FILL
        }

        fun nouvellePage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            cv = page.canvas
            y = 10 * MM
            txt("$rappel (page $pageNo)", W - M, y + 6f, 7f, false, Color.rgb(150, 150, 150), Paint.Align.RIGHT)
            y += 12f
        }

        fun besoin(hauteur: Float) {
            if (y + hauteur > BAS) nouvellePage()
        }

        fun bandeau(titre: String) {
            besoin(9 * MM + 14 * MM)
            paint.color = FILL_SECTION
            paint.style = Paint.Style.FILL
            cv.drawRect(M, y, W - M, y + 8 * MM, paint)
            txt("  $titre", M + 3f, y + 6 * MM, 11f, true, NOIR)
            y += 9 * MM
        }

        fun fmtPnl(v: Double): String =
            (if (v >= 0) "+" else "-") + String.format(Locale.US, "%.2f $", Math.abs(v))

        // ---- en-tete
        txt("RAPPORT DE PERFORMANCE", W / 2f, y + 20f, 17f, true, NOIR, Paint.Align.CENTER)
        y += 9 * MM
        txt("Periode du $dFr au $dTo - ${lastChannelData.size} canaux affiches" +
            (if (fExcluded.isNotEmpty()) " (${fExcluded.size} exclus)" else ""),
            W / 2f, y + 12f, 12f, false, GRIS, Paint.Align.CENTER)
        y += 7 * MM
        txt("Filtres : ${descFiltres()}", W / 2f, y + 10f, 8f, false, NOIR, Paint.Align.CENTER)
        y += 6 * MM

        // ---- KPI : 5 boites de 36 x 18 mm
        val tot = lastChannelData.fold(PerfData()) { acc, p -> acc.merge(p.second) }
        val kpis = listOf(
            Triple("P&L TOTAL", fmtPnl(tot.pnl), if (tot.pnl >= 0) VERT_KPI else ROUGE_KPI),
            Triple("TRADES", tot.trades.toString(), GRIS_KPI),
            Triple("SIGNAUX", tot.trades.toString(), GRIS_KPI),
            Triple("WINS / LOSSES", "${tot.wins} / ${tot.losses}", GRIS_KPI),
            Triple("WINRATE", "${tot.winrate()} %", GRIS_KPI)
        )
        val cwK = 36 * MM
        val gapK = 4 * MM
        val x0K = (W - (5 * cwK + 4 * gapK)) / 2f
        val y0K = y
        kpis.forEachIndexed { i, k ->
            val x = x0K + i * (cwK + gapK)
            cadre(x, y0K, cwK, 18 * MM, FILL_KPI)
            txt(k.first, x + cwK / 2f, y0K + 7f, 6.5f, false, Color.rgb(90, 90, 100), Paint.Align.CENTER)
            txt(k.second, x + cwK / 2f, y0K + 15 * MM, 11f, true, k.third, Paint.Align.CENTER)
        }
        y = y0K + 18 * MM + 5 * MM

        fun enteteTableau(cols: List<Triple<String, Float, Paint.Align>>) {
            var xh = M
            cols.forEach { c ->
                val w = c.second * MM
                cadre(xh, y, w, 6 * MM, FILL_HEAD)
                txt(c.first, if (c.third == Paint.Align.LEFT) xh + 3f else xh + w / 2f,
                    y + 4.2f * MM, 8f, true, NOIR, c.third)
                xh += w
            }
            y += 6 * MM
        }

        fun tableSection(titre: String, cols: List<Triple<String, Float, Paint.Align>>,
                         rows: List<List<String>>, totalRow: List<String>?, hLigne: Float, colPnl: Int) {
            bandeau(titre)
            enteteTableau(cols)
            var alt = false
            for (r in rows) {
                if (y + hLigne > BAS) {
                    nouvellePage()
                    enteteTableau(cols)
                }
                var xr = M
                r.forEachIndexed { i, v ->
                    val w = cols[i].second * MM
                    cadre(xr, y, w, hLigne, if (alt) FILL_ALT else null)
                    val col = when {
                        i == colPnl && v.startsWith("-") -> ROUGE
                        i == colPnl && v.startsWith("+") -> VERT
                        else -> NOIR
                    }
                    val al = cols[i].third
                    txt(v, if (al == Paint.Align.LEFT) xr + 3f else xr + w / 2f,
                        y + hLigne - 3.5f, 8f, false, col, al)
                    xr += w
                }
                y += hLigne
                alt = !alt
            }
            if (totalRow != null) {
                if (y + hLigne > BAS) {
                    nouvellePage()
                    enteteTableau(cols)
                }
                var xt = M
                totalRow.forEachIndexed { i, v ->
                    val w = cols[i].second * MM
                    cadre(xt, y, w, hLigne, FILL_SECTION)
                    val col = when {
                        i == colPnl && v.startsWith("-") -> ROUGE
                        i == colPnl && v.startsWith("+") -> VERT
                        else -> NOIR
                    }
                    val al = cols[i].third
                    txt(v, if (al == Paint.Align.LEFT) xt + 3f else xt + w / 2f,
                        y + hLigne - 3.5f, 8f, true, col, al)
                    xt += w
                }
                y += hLigne
            }
            y += 3 * MM
        }

        // 1) PERFORMANCE PAR SIGNAL
        val colsS = listOf(
            Triple("SIGNAL", 26f, Paint.Align.LEFT), Triple("P&L ($)", 22f, Paint.Align.CENTER),
            Triple("CN", 12f, Paint.Align.CENTER), Triple("TR", 14f, Paint.Align.CENTER),
            Triple("WN", 14f, Paint.Align.CENTER), Triple("LS", 14f, Paint.Align.CENTER),
            Triple("WR %", 16f, Paint.Align.CENTER))
        val rowsS = sortPerfData(lastSignalData, signalSortCol, signalSortAsc, isSignal = true).map { p ->
            listOf(p.first, fmtPnl(p.second.pnl), p.second.channelCount.toString(), p.second.trades.toString(),
                p.second.wins.toString(), p.second.losses.toString(), p.second.winrate().toString())
        }
        val totS = lastSignalData.fold(PerfData()) { acc, p -> acc.merge(p.second) }
        tableSection("PERFORMANCE PAR SIGNAL", colsS, rowsS,
            listOf("TOTAL", fmtPnl(totS.pnl), "", totS.trades.toString(), totS.wins.toString(),
                totS.losses.toString(), totS.winrate().toString()), 6 * MM, 1)

        // 2) P&L PAR JOUR (jour de fermeture, comme le rapport du serveur) : grille de 4 colonnes
        val jours = LinkedHashMap<String, Double>()
        for (t in lastTradesComptes) {
            val ct = t.close_time
            if (ct.length < 10) continue
            val d = ct.substring(8, 10) + "/" + ct.substring(5, 7)
            jours[d] = (jours[d] ?: 0.0) + t.profit
        }
        val ordreJours = jours.keys.sortedWith(
            compareBy({ it.substring(3, 5).toIntOrNull() ?: 0 }, { it.substring(0, 2).toIntOrNull() ?: 0 })
        )
        bandeau("P&L PAR JOUR")
        val cwJ = 190f / 4f * MM
        var ij = 0
        while (ij < ordreJours.size) {
            val chunk = ordreJours.subList(ij, minOf(ij + 4, ordreJours.size))
            besoin(12 * MM)
            var xj = M
            for (d in chunk) {
                cadre(xj, y, cwJ, 6 * MM, FILL_JOURS)
                txt(d, xj + cwJ / 2f, y + 4.2f * MM, 8f, true, NOIR, Paint.Align.CENTER)
                xj += cwJ
            }
            y += 6 * MM
            xj = M
            for (d in chunk) {
                val v = jours[d] ?: 0.0
                cadre(xj, y, cwJ, 6 * MM, null)
                txt(fmtPnl(v), xj + cwJ / 2f, y + 4.2f * MM, 8f, false,
                    if (v >= 0) VERT else ROUGE, Paint.Align.CENTER)
                xj += cwJ
            }
            y += 6 * MM
            ij += 4
        }
        y += 3 * MM

        // 3) PERFORMANCE PAR CANAL (tri P&L decroissant ; P&L en 3e colonne -> colPnl = 2)
        val colsC = listOf(
            Triple("CH", 9f, Paint.Align.CENTER), Triple("CANAL", 69f, Paint.Align.LEFT),
            Triple("P&L ($)", 22f, Paint.Align.CENTER), Triple("SIG", 12f, Paint.Align.CENTER),
            Triple("TRADES", 15f, Paint.Align.CENTER), Triple("W", 12f, Paint.Align.CENTER),
            Triple("L", 12f, Paint.Align.CENTER), Triple("WR %", 14f, Paint.Align.CENTER))
        val rowsC = mutableListOf<List<String>>()
        for (p in lastChannelData.sortedByDescending { it.second.pnl }) {
            val d = p.second
            rowsC.add(listOf(p.first.removePrefix("CH"), channelNames[p.first] ?: "-", fmtPnl(d.pnl),
                (lastChannelMkCount[p.first] ?: 0).toString(), d.trades.toString(),
                d.wins.toString(), d.losses.toString(), d.winrate().toString()))
        }
        for (p in lastExcludedData) {
            val d = p.second
            rowsC.add(listOf(p.first.removePrefix("CH"), (channelNames[p.first] ?: "-") + " (exclu)", fmtPnl(d.pnl),
                d.trades.toString(), d.trades.toString(), d.wins.toString(), d.losses.toString(),
                d.winrate().toString()))
        }
        tableSection("PERFORMANCE PAR CANAL (tri P&L decroissant)", colsC, rowsC,
            listOf("", "TOTAL", fmtPnl(tot.pnl), lastChannelMkCount.values.sum().toString(),
                tot.trades.toString(), tot.wins.toString(), tot.losses.toString(),
                tot.winrate().toString()), 5.5f * MM, 2)

        // 4) PERFORMANCE PAR HEURE D'OUVERTURE
        val colsH = listOf(
            Triple("HEURE OUV.", 26f, Paint.Align.LEFT), Triple("P&L ($)", 22f, Paint.Align.CENTER),
            Triple("CN", 12f, Paint.Align.CENTER), Triple("TR", 14f, Paint.Align.CENTER),
            Triple("WN", 14f, Paint.Align.CENTER), Triple("LS", 14f, Paint.Align.CENTER),
            Triple("WR %", 16f, Paint.Align.CENTER))
        val rowsH = lastSessionData.sortedBy { it.first }.map { p ->
            listOf(p.first, fmtPnl(p.second.pnl), p.second.channelCount.toString(), p.second.trades.toString(),
                p.second.wins.toString(), p.second.losses.toString(), p.second.winrate().toString())
        }
        val totH = lastSessionData.fold(PerfData()) { acc, p -> acc.merge(p.second) }
        tableSection("PERFORMANCE PAR HEURE D'OUVERTURE (UTC)", colsH, rowsH,
            listOf("TOTAL", fmtPnl(totH.pnl), "", totH.trades.toString(), totH.wins.toString(),
                totH.losses.toString(), totH.winrate().toString()), 6 * MM, 1)

        besoin(10 * MM)
        txt("1 signal = 1 trade (P&L du signal complet, jambes L1/L2/L4 comprises) - genere dans l'appli le " +
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(java.util.Date()),
            M, y + 6f, 7f, false, Color.rgb(150, 150, 150))
        doc.finishPage(page)
        val dir = File(getExternalFilesDir(null), "rapports").apply { mkdirs() }
        val f = File(dir, "rapport_filtre_${dateFrom}_$dateTo.pdf")
        java.io.FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return if (f.exists() && f.length() > 0) f else null
    }

    private fun openPdf(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Ouvrir le rapport"))
        } catch (e: Exception) {
            Snackbar.make(btnPdfReport, "Aucune app pour ouvrir le PDF (fichier dans ${file.parent})", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun refreshPerformanceForRange(fromDate: String, toDate: String) {
        lifecycleScope.launch {
            try {
                // Filtre: ne garder que les canaux presents dans Channels.txt (serveur).
                // Les 2 appels en parallele (pas de latence ajoutee).
                val activeDeferred = async { loadActiveChannels() }
                val tradesDeferred = async { client.getTrades(fromDate = fromDate, toDate = toDate) }
                val activeChannels = activeDeferred.await()
                channelNames = activeChannels ?: emptyMap()
                val trades = tradesDeferred.await()
                if (trades != null) {
                    val filtered = if (activeChannels == null) trades.trades
                    else trades.trades.filter { t ->
                        val ch = t.comment.substringBefore("-")
                        activeChannels.containsKey(ch) || !ch.startsWith("CH")
                    }
                    histTradesRaw = filtered
                    totalSignaux = signalAggregates(filtered).size
                    applyHistoryFilters()
                }
            } catch (_: Exception) {}
        }
    }

    /** Lit Channels.txt sur le serveur : canaux actifs ("CH5") -> nom du canal.
     *  Retourne null si illisible (=> pas de filtre, affichage complet). */
    private suspend fun loadActiveChannels(): Map<String, String>? {
        return try {
            // ★ 26/09 : nom de fichier SANS dossier -> l'API le resout dans SON dossier.
            // Avant : "C:\TradingBot\Channels.txt" en dur => 403 sur l'API de bot 2 => aucun filtre.
            val file = client.getServerFile("Channels.txt") ?: return null
            val map = LinkedHashMap<String, String>()
            // ★ 26/09 : RegexOption.MULTILINE OBLIGATOIRE. En Java/Android, "$" sans MULTILINE
            // ne matche que la TOUTE DERNIERE ligne du fichier -> un seul canal lu (CH109)
            // -> l'Historique n'affichait que ce canal, quelque soit la periode.
            Regex("""Canal_(\d+)\s*:\s*\S+\s*#\s*(.*)$""", RegexOption.MULTILINE).findAll(file.content).forEach {
                map["CH${it.groupValues[1]}"] = it.groupValues[2].trim()
            }
            if (map.isEmpty()) null else map
        } catch (_: Exception) { null }
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

        // ★ 26/09 : item "Deconnecter du serveur" du tiroir -> on quitte le serveur courant
        // et on revient a l'ecran de connexion (formulaire pre-rempli, sans reconnexion
        // automatique) pour changer de port / de serveur.
        findViewById<TextView>(R.id.drawerLogout).setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, SetupActivity::class.java).putExtra("edit_server", true))
            finish()
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

        tvDailyGoalTitle.text = "Objectif quotidien (${dash.daily_limit.toInt()})"
        tvDailyLimitPct.text = "${dash.limit_pct.toInt()}%"
        progressDailyLimit.progress = dash.limit_pct.toInt().coerceIn(0, 100)

        positionAdapter.setPositions(dash.open_positions)
        setPositionsHeader(dash.open_positions)
        tvNoPositions.visibility = if (dash.open_positions.isEmpty()) View.VISIBLE else View.GONE
        rvPositions.visibility = if (dash.open_positions.isEmpty()) View.GONE else View.VISIBLE

        // Heures de trading reelles depuis le .env serveur (trading_hours_list = CSV exact)
        val hoursList = dash.trading_hours_list
        if (hoursList.isNotBlank()) {
            serverHours.fill(false)
            hoursList.split(",").forEach {
                val h = it.trim().toIntOrNull()
                if (h != null && h in 0..23) serverHours[h] = true
            }
        }

        // Update trading hours dashboard (lit serverHours = .env, pas l'etat d'edition Config)
        updateTradingHoursDashboard()
    }

    /** 20/10/2026 : en-tete des positions ouvertes = nombre de BUY et de SELL, centre. */
    private fun setPositionsHeader(positions: List<Position>) {
        val buy = positions.count { it.type.uppercase() == "BUY" }
        val sell = positions.count { it.type.uppercase() == "SELL" }
        val txt = SpannableStringBuilder()
        var i = txt.length
        txt.append("BUY $buy")
        txt.setSpan(ForegroundColorSpan(getColor(R.color.success)), i, txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        i = txt.length
        txt.append("   \u00b7   ")
        txt.setSpan(ForegroundColorSpan(getColor(R.color.text_muted)), i, txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        i = txt.length
        txt.append("SELL $sell")
        txt.setSpan(ForegroundColorSpan(getColor(R.color.danger)), i, txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvPositionsTitle.text = txt
    }

    private fun refreshPositions() {
        lifecycleScope.launch {
            val positions = client.getPositions()
            if (positions != null) {
                positionAdapter.setPositions(positions.positions)
                setPositionsHeader(positions.positions)
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
                    // Init trading hours AVANT le rendu (la grille 24h lit tradingHours[] à la création)
                    initTradingHoursFromConfig(cfg)
                    // Codes de fermeture (badge jaune) : lus sur le serveur (reglage MOTIF_CODES)
                    MotifCodes.setFromConfig(cfg)
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
            "TELEGRAM" to listOf("TG_API_ID", "TG_API_HASH", "TG_SESSION_NAME"),
            "MT5 (COMPTE) ET SERVEUR" to listOf("MT5_LOGIN", "MT5_PASSWORD", "MT5_SERVER", "MT5_PATH", "MAGIC_NUMBER", "API_TOKEN", "BOT1_FWD_PORT", "BOT2_FWD_PORT"),
            "CANAUX TELEGRAM" to listOf("TG_FOLDER", "TG_ALERT_CHANNEL") +
                    cfg.keys.filter { it.startsWith("TG_CHANNEL") && it != "TG_CHANNEL_MERGED" }.sorted(),
            "LOTS ET ORDRES" to listOf("LOT_TOTAL", "LOT_MARKET", "LOT_LIMIT1", "LOT_LIMIT2", "LIMIT_ENABLED", "LIMIT_COUNT", "LIMIT_OFFSET_1", "LIMIT_OFFSET_2", "LIMIT_EXPIRY_MIN", "ORDER_EXPIRY_MINUTES", "SLIPPAGE"),
            "GESTION DES TRADES" to listOf("TP_FIXED_GAIN_USD", "TP_MULTIPE1", "TP_MULTIPE2", "MAX_SL_USD", "MAX_POSITIONS", "DAILY_PROFIT_LIMIT", "MAX_DAILY_DRAWDOWN", "MOTIF_CODES"),
            "ZONES ET ANTI-DOUBLON" to listOf("TOLERANCE_ZN", "TOLERANCE_PU", "TOLERANCE_MP", "TEMPS_DE_FUSION", "TRADE_HORS_ZONE", "MAX_DISTANCE"),
            "FILTRES" to listOf("MAX_SPREAD_POINTS", "CONFLIT_FILTER_ENABLED", "MOVE_FILTER_ENABLED", "MOVE_FILTER_MIN", "MOVE_FILTER_USD", "KER_FILTER_ENABLED", "KER_SHORT", "KER_LONG", "KER_STRONG", "KER_ACCEL", "LAB_MODE"),
            "FILTRE HORAIRE" to listOf("TIME_FILTER_ENABLED", "TRADING_HOURS"),
            "FILTRES NEWS / TV" to listOf("NEWS_FILTER_ENABLED", "NEWS_MIN_IMPACT", "NEWS_WINDOW_BEFORE_BLOCK", "NEWS_WINDOW_BEFORE_CLOSE", "NEWS_WINDOW_AFTER"),
            "ALERTES ET LOGS" to listOf("LOG_TRADE_MANAGEMENT", "ALERT_TRADE_MANAGEMENT", "ALERT_DAILY_PERFORMANCE"),
            "DIVERS" to listOf("DEMO_MODE", "RUNTIME_MINUTES", "POLL_INTERVAL_SEC", "DAILY_RESET_HOUR", "DAILY_REPORT_HOUR")
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
                // Skip old START/END/TRADING_HOURS fields — replaced by the 24h grid
                if (field.key == "TRADING_START_HOUR" || field.key == "TRADING_END_HOUR" ||
                    field.key == "TRADING_HOURS") continue
                val fieldView = ConfigFieldView.create(this, field)
                cardContent.addView(fieldView)
            }

            // Add24h grid for "FILTRE HORAIRE" section
            if (group.title.uppercase().contains("HORAIRE")) {
                cardContent.addView(createTradingHoursGrid())
            }

            card.addView(cardContent)
            configContainer.addView(card)
        }
    }

    /**
     * Creates the24h checkbox grid for trading hours config
     */
    private fun createTradingHoursGrid(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        // Presets row
        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }
        val presets = listOf("London 7-16h" to intArrayOf(7,15), "New York 12-21h" to intArrayOf(12,20), "London+NY 7-21h" to intArrayOf(7,20), "24h/24" to intArrayOf(0,23))
        for ((label, range) in presets) {
            presetsRow.addView(TextView(this).apply {
                text = label
                textSize = 10f
                setTextColor(getColor(R.color.text_muted))
                setPadding(dp(8), dp(6), dp(8), dp(6))
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setStroke(dp(1), getColor(R.color.divider))
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.TRANSPARENT)
                }
                background = bg
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(4)
                }
                setOnClickListener {
                    tradingHours.fill(false)
                    for (i in range[0]..range[1]) tradingHours[i] = true
                    syncHoursGrid(container)
                }
            })
        }
        container.addView(presetsRow)

        // Grid (6 columns x4 rows)
        val gridLayout = GridLayout(this).apply {
            columnCount = 6
            rowCount = 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        for (i in 0 until 24) {
            val tv = TextView(this).apply {
                text = String.format("%02d", i)
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                tag = i
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                setOnClickListener {
                    val hour = tag as Int
                    tradingHours[hour] = !tradingHours[hour]
                    updateHourBoxStyle(this, tradingHours[hour])
                    updateTradingHoursDashboard()
                }
            }
            updateHourBoxStyle(tv, tradingHours[i])
            gridLayout.addView(tv)
        }
        container.addView(gridLayout)

        container.tag = gridLayout
        return container
    }

    private fun updateHourBoxStyle(tv: TextView, active: Boolean) {
        val bg = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            if (active) {
                setColor(getColor(R.color.primary))
                setStroke(0, Color.TRANSPARENT)
            } else {
                setColor(Color.parseColor("#0F0F1A"))
                setStroke(dp(2), getColor(R.color.divider))
            }
        }
        tv.background = bg
        tv.setTextColor(if (active) Color.WHITE else getColor(R.color.text_primary))
    }

    private fun syncHoursGrid(container: View) {
        val grid = container.tag as? GridLayout ?: return
        for (i in 0 until grid.childCount) {
            val tv = grid.getChildAt(i) as? TextView ?: continue
            updateHourBoxStyle(tv, tradingHours[tv.tag as Int])
        }
        updateTradingHoursDashboard()
    }

    private fun initTradingHoursFromConfig(cfg: Map<String, String>) {
        // Only init once — avoid reset on each5s refresh
        if (tradingHoursInitialized) return
        tradingHoursInitialized = true

        val enabled = cfg["TIME_FILTER_ENABLED"]?.lowercase() == "true"
        tradingHours.fill(false)
        if (enabled) {
            // Try TRADING_HOURS first (comma-separated list)
            val hoursStr = cfg["TRADING_HOURS"]
            if (hoursStr != null && hoursStr.isNotBlank()) {
                hoursStr.split(",").forEach {
                    val h = it.trim().toIntOrNull()
                    if (h != null && h in 0..23) tradingHours[h] = true
                }
            } else {
                // Fallback to START/END_HOUR (old format, END exclusive)
                val start = cfg["TRADING_START_HOUR"]?.toIntOrNull() ?: 5
                val end = cfg["TRADING_END_HOUR"]?.toIntOrNull() ?: 21
                for (i in start until end) tradingHours[i] = true
            }
        }
        // Affichage initial = meme source (.env); le refresh 5s resynchronisera via dashboard
        serverHours = tradingHours.copyOf()
        updateTradingHoursDashboard()
    }

    private fun updateTradingHoursDashboard() {
        val count = serverHours.count { it }
        tvTradingHoursCount.text = "${count}h active"

        // Hour tags (2 rows: 00-11 puis 12-23, styled like prototype)
        // Source = serverHours (.env reel). Ne reconstruit que si l'etat a change.
        val signature = serverHours.joinToString("") { if (it) "1" else "0" }
        if (signature == lastHoursSignature && chipGroupHours.childCount > 0) return
        lastHoursSignature = signature
        chipGroupHours.removeAllViews()
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        for (i in 0 until 24) {
            val tv = TextView(this).apply {
                text = String.format("%02d", i)
                textSize = 9f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(4), dp(2), dp(4))
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(4).toFloat()
                    if (serverHours[i]) {
                        setColor(getColor(R.color.primary))
                    } else {
                        setColor(Color.parseColor("#1A1A2E"))
                    }
                }
                background = bg
                setTextColor(if (serverHours[i]) Color.WHITE else getColor(R.color.text_muted))
                // pleine largeur: poids egal pour repartir les 12 chips sur tout le width
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { marginEnd = dp(2); marginStart = dp(2) }
            }
            if (i < 12) row1.addView(tv) else row2.addView(tv)
        }
        chipGroupHours.addView(row1)
        chipGroupHours.addView(row2)
    }

    private fun saveConfig() {
        val values = mutableMapOf<String, String>()
        val channelLists = mutableMapOf<String, List<String>>()

        // Parcourir tous les groupes et lire les valeurs des widgets
        for (group in configGroups) {
            for (field in group.fields) {
                // Skip old START/END/TRADING_HOURS fields — TRADING_HOURS is rebuilt from the 24h grid below
                if (field.key == "TRADING_START_HOUR" || field.key == "TRADING_END_HOUR" ||
                    field.key == "TRADING_HOURS") continue
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

        // Add TRADING_HOURS from24h grid
        val activeHours = (0 until 24).filter { tradingHours[it] }
        values["TRADING_HOURS"] = activeHours.joinToString(",")
        values["TIME_FILTER_ENABLED"] = if (activeHours.isNotEmpty()) "true" else "false"

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

    /** @param trades unites de comptage (positions, ou signaux agreges si le mode "tout le signal" est actif)
     *  @param rawForDetail positions reelles, pour le popup de detail (les jambes une par une). */
    private fun updatePerformance(trades: List<Trade>, rawForDetail: List<Trade> = trades) {
        val channelData = mutableMapOf<String, PerfData>()
        val signalData = mutableMapOf<String, PerfData>()

        // Grouper par position_id pour compter les signaux uniques
        val positionSignals = mutableMapOf<Long, Pair<String, String>>() // ticket -> (channel, signal)

        for (t in trades) {
            val parts = t.comment.split("-")
            if (parts.size >= 2) {
                val channel = parts[0]  // CH5, CH3, etc.
                val type = parts[1]     // ZN, PU, MP, QA, AL
                // Hors-zone (L3/L4) separe du ZN dans la zone -> ligne HZ
                val signal = if (type == "ZN" && parts.size >= 3 && (parts[2] == "L3" || parts[2] == "L4")) "HZ" else type
                positionSignals[t.ticket] = Pair(channel, signal)
                channelData.getOrPut(channel) { PerfData() }.add(t, signal)
                signalData.getOrPut(signal) { PerfData() }.add(t)
            }
        }

        // SN = nombre d'ordres MK (Market) + L3 (HORS-ZONE) par canal
        val channelMkCount = mutableMapOf<String, Int>()
        for (t in trades) {
            val parts = t.comment.split("-")
            if (parts.size >= 3 && (parts[2] == "MK" || parts[2] == "L3")) {
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

        // ★ 26/09 : garder aussi les trades complets par canal (pour le popup de detail)
        val channelTradeList = mutableMapOf<String, MutableList<Trade>>()
        for (t in rawForDetail) {
            val parts = t.comment.split("-")
            if (parts.size >= 2) channelTradeList.getOrPut(parts[0]) { mutableListOf() }.add(t)
        }
        lastChannelTradesList = channelTradeList

        // Par heure d'OUVERTURE (UTC) : MEME convention que le filtre horaire, sinon le tableau
        // "par session" affichait une heure de fermeture et les chiffres ne collaient pas au filtre.
        val sessionData = mutableMapOf<String, PerfData>()
        val sessionChannels = mutableMapOf<String, MutableSet<String>>()
        for (t in trades) {
            val parts = t.comment.split("-")
            val ch = if (parts.size >= 2) parts[0] else ""
            val heure = openHourUtc(t)
            if (heure >= 0) {
                val label = String.format(java.util.Locale.US, "%02dh", heure)
                sessionData.getOrPut(label) { PerfData() }.add(t)
                if (ch.isNotEmpty()) sessionChannels.getOrPut(label) { mutableSetOf() }.add(ch)
            }
        }
        for ((label, d) in sessionData) {
            d.channelCount = sessionChannels[label]?.size ?: 0
        }
        lastSessionData = sessionData.entries.map { it.key to it.value }.sortedBy { it.first }

        renderChannelTable()
        renderSignalTable()
        renderSessionTable()
        renderTotaux()
    }

    /** Resume des totaux juste sous les filtres (meme chiffres que la ligne TOTAL du tableau par canal) :
     *  evite de descendre au bas du premier tableau pour voir l'effet d'un filtre. */
    private fun renderTotaux() {
        if (!::perfTotaux.isInitialized) return
        perfTotaux.removeAllViews()
        val weights = floatArrayOf(1.4f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f)
        val entetes = arrayOf("P&L", "SN", "TR", "WN", "LS", "WR")
        val ligneH = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(6))
        }
        entetes.forEachIndexed { i, txt ->
            val tv = TextView(this).apply {
                text = txt
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.accent))
                gravity = Gravity.CENTER
            }
            ligneH.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[i]))
        }
        perfTotaux.addView(ligneH)

        val totalCh = lastChannelData.fold(PerfData()) { acc, (_, d) -> acc.merge(d) }
        val totalSn = lastChannelMkCount.values.sum()
        val wr = totalCh.winrate()
        val ligneV = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
            setBackgroundColor(Color.parseColor("#1A1A2E"))
        }
        fun cellule(txt: String, w: Float, couleur: Int) {
            val tv = TextView(this).apply {
                text = txt
                setTextColor(couleur)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            ligneV.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w))
        }
        cellule(String.format("%+.2f", totalCh.pnl), 1.4f,
            getColor(if (totalCh.pnl >= 0) R.color.success else R.color.danger))
        cellule(totalSn.toString(), 0.8f, getColor(R.color.text_primary))
        cellule(totalCh.trades.toString(), 0.8f, getColor(R.color.text_primary))
        cellule(totalCh.wins.toString(), 0.8f, getColor(R.color.success))
        cellule(totalCh.losses.toString(), 0.8f, getColor(R.color.danger))
        cellule("$wr", 0.8f, getColor(if (wr >= 50) R.color.success else R.color.danger))
        perfTotaux.addView(ligneV)
    }

    private fun renderChannelTable() {
        perfChannelTable.removeAllViews()
        val headers = arrayOf("Canal", "P&L", "SN", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(1.2f, 1f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
        addPerfHeader(perfChannelTable, headers, weights, channelSortCol, channelSortAsc, true) { col ->
            if (channelSortCol == col) channelSortAsc = !channelSortAsc else { channelSortCol = col; channelSortAsc = col == 0 }
            renderChannelTable()
        }
        val sorted = sortPerfData(lastChannelData, channelSortCol, channelSortAsc)
        for ((ch, d) in sorted) {
            val sn = lastChannelMkCount[ch] ?: 0
            addPerfRow(perfChannelTable, ch.removePrefix("CH"), d, sn = sn, chKey = ch)
        }
        // Canaux exclus : conserves dans le tableau (grise) pour pouvoir les decocher.
        for ((ch, d) in lastExcludedData) {
            addPerfRow(perfChannelTable, ch.removePrefix("CH"), d, sn = d.trades, chKey = ch)
        }
        val totalCh = lastChannelData.fold(PerfData()) { acc, (_, d) -> acc.merge(d) }
        val totalSn = lastChannelMkCount.values.sum()
        addPerfRow(perfChannelTable, "TOTAL", totalCh, sn = totalSn, isTotal = true)
    }

    private fun renderSignalTable() {
        perfSignalTable.removeAllViews()
        val headers = arrayOf("Signal", "P&L", "CN", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(0.8f, 1f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
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
        val headers = arrayOf("Heure ouv.", "P&L", "CN", "TR", "WN", "LS", "WR")
        val weights = floatArrayOf(0.8f, 1f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
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
                0 -> data.sortedBy { signalSortKey(it.first) }
                1 -> data.sortedBy { it.second.pnl }
                2 -> data.sortedBy { it.second.channelCount }
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

    /** Cle de tri des signaux : HZ (hors-zone L3/L4) juste apres ZN. */
    private fun signalSortKey(name: String): String = if (name == "HZ") "ZN~" else name

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

    private fun addPerfHeader(container: LinearLayout, headers: Array<String>, weights: FloatArray, sortCol: Int, sortAsc: Boolean, leading: Boolean = false, onSort: (Int) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); setBackgroundColor(Color.parseColor("#1A1A2E")) }
        if (leading) row.addView(View(this), LinearLayout.LayoutParams(dp(30), 1))
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
        addPerfHeader(container, headers, weights, sortCol, sortAsc, onSort = onSort)
    }

    private fun addPerfRow(container: LinearLayout, label: String, d: PerfData, sn: Int = 0, isTotal: Boolean = false, chKey: String? = null) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); if (isTotal) setBackgroundColor(Color.parseColor("#1A1A2E")) }
        fun cell(text: String, weight: Float, color: Int, bold: Boolean = false) {
            val tv = TextView(this).apply { this.text = text; setTextColor(color); textSize = 13f; if (bold) setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight))
        }
        // Case d'exclusion du canal : cochee = canal retire des calculs (ligne conservee, grisee).
        if (chKey != null) {
            val cb = androidx.appcompat.widget.AppCompatCheckBox(this).apply {
                isChecked = fExcluded.contains(chKey)
                buttonTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.primary_light))
                minimumWidth = 0
                minimumHeight = 0
                setPadding(0, 0, 0, 0)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) fExcluded.add(chKey) else fExcluded.remove(chKey)
                    scheduleFilterApply()
                }
            }
            row.addView(cb, LinearLayout.LayoutParams(dp(30), LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        cell(label, 1.2f, getColor(R.color.primary_light), bold = true)
        cell(String.format("%+.2f", d.pnl), 1f, if (d.pnl >= 0) getColor(R.color.success) else getColor(R.color.danger), bold = true)
        cell(sn.toString(), 0.6f, getColor(R.color.text_primary))
        cell(d.trades.toString(), 0.6f, getColor(R.color.text_primary))
        cell(d.wins.toString(), 0.6f, getColor(R.color.success))
        cell(d.losses.toString(), 0.6f, getColor(R.color.danger))
        val wr = d.winrate()
        cell("$wr", 0.6f, getColor(if (wr >= 50) R.color.success else R.color.danger))

        // ★ 26/09 : plus de depliant "PF | RR | MD" -> un appui ouvre le popup de detail du canal
        if (!isTotal) {
            row.setOnClickListener { showChannelDetail(chKey ?: label) }
            row.isClickable = true
            if (chKey != null && fExcluded.contains(chKey)) row.alpha = 0.32f
        }

        container.addView(row)
        if (!isTotal) { container.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2A2A4A")) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)) }
    }

    // ================= POPUP DETAIL CANAL (26/09) =================

    private data class DetailLeg(val role: String, val open: Double, val close: Double,
                                val openT: String = "", val closeT: String = "", val cmt: String = "",
                                 val pnl: Double, val reason: Int?)
    private data class DetailSignal(val debut: String, val side: String, val type: String,
                                    val legs: MutableList<DetailLeg> = mutableListOf(), var fin: String = "")

    /** Regroupe les positions par signal : un MK (ou L3) ouvre un signal, ses L1/L2/L3/L4 le suivent. */
    private fun groupSignals(trades: List<Trade>): List<DetailSignal> {
        val out = mutableListOf<DetailSignal>()
        trades.sortedBy { it.open_time }.forEach { t ->
            val p = t.comment.split("-")
            val role = if (p.size >= 3) p[2] else "?"
            val type = if (p.size >= 2) p[1] else "?"
            if (role == "MK" || role == "L3" || out.isEmpty()) {
                out.add(DetailSignal(t.open_time, t.type, type, mutableListOf(), t.close_time))
            }
            val g = out.last()
            g.legs.add(DetailLeg(role, t.open_price, t.close_price, t.open_time, t.close_time, t.comment, t.profit, t.reason))
            if (t.close_time > g.fin) g.fin = t.close_time
        }
        return out
    }

    private fun roundedBg(fill: Int, stroke: Int?, radius: Int = 12): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun detailTag(text: String, textColor: Int, bgColor: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 9f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(textColor)
            setBackground(roundedBg(bgColor, null, 6))
            setPadding(dp(7), dp(3), dp(7), dp(3))
        }

    /** Ligne du popup : segments "label : valeur" separes par |, valeurs en gras (couleur optionnelle). */
    private fun addDetailLine(parent: LinearLayout, parts: List<Triple<String, String, Int?>>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6))
        }
        parts.forEachIndexed { i, (lab, value, col) ->
            if (i > 0) {
                row.addView(TextView(this).apply {
                    text = "|"; textSize = 11f; setTextColor(getColor(R.color.text_muted))
                    setPadding(dp(7), 0, dp(7), 0)
                })
            }
            val tv = TextView(this).apply { textSize = 11f; setTextColor(getColor(R.color.text_secondary)) }
            val s = android.text.SpannableString(lab + value)
            if (value.isNotEmpty()) {
                s.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), lab.length, s.length, 0)
                if (col != null) s.setSpan(android.text.style.ForegroundColorSpan(col), lab.length, s.length, 0)
            }
            tv.text = s
            row.addView(tv)
        }
        parent.addView(row)
    }

    private fun detailBox(lab: String, value: String, col: Int?): LinearLayout {
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#1A1A2E"), getColor(R.color.border), 12)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        b.addView(TextView(this).apply {
            text = lab; textSize = 8.5f; setTextColor(getColor(R.color.text_secondary)); gravity = Gravity.CENTER
        })
        b.addView(TextView(this).apply {
            text = value; textSize = 13.5f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(col ?: getColor(R.color.text_primary)); gravity = Gravity.CENTER
        })
        return b
    }

    private fun hhmm(iso: String): String =
        if (iso.length >= 16) iso.substring(8, 10) + "/" + iso.substring(5, 7) + " " + iso.substring(11, 16) else iso

    private fun dureeMin(a: String, b: String): Int {
        return try {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            f.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val d1 = f.parse(a.substring(0, 19)); val d2 = f.parse(b.substring(0, 19))
            if (d1 != null && d2 != null) ((d2.time - d1.time) / 60000).toInt().coerceAtLeast(0) else 0
        } catch (_: Exception) { 0 }
    }

    private fun dureeTexte(m: Int): String = if (m < 60) "$m min" else "${m / 60}h" + String.format("%02d", m % 60)

    /** Ouvre le popup de detail des trades d'un canal (appui sur une ligne du tableau). */
    private fun showChannelDetail(channel: String) {
        val trades = lastChannelTradesList[channel] ?: return
        if (trades.isEmpty()) return

        val dlg = android.app.Dialog(this)
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val root = layoutInflater.inflate(R.layout.dialog_channel_detail, null)
        dlg.setContentView(root)
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.88).toInt())

        val nom = channelNames[channel].orEmpty()
        root.findViewById<TextView>(R.id.detailTitle).text = if (nom.isEmpty()) channel else "$channel · $nom"
        root.findViewById<TextView>(R.id.detailSub).text = "Période $dateFrom → $dateTo"
        root.findViewById<View>(R.id.detailClose).setOnClickListener { dlg.dismiss() }
        root.findViewById<View>(R.id.btnDetailFermer).setOnClickListener { dlg.dismiss() }
        val content = root.findViewById<LinearLayout>(R.id.detailContent)

        val profits = trades.map { it.profit }
        val wins = profits.count { it >= 0 }
        val losses = profits.count { it < 0 }
        val pnl = profits.sum()
        val gain = profits.filter { it > 0 }.sum()
        val perte = profits.filter { it < 0 }.sum()
        val wr = wins * 100.0 / trades.size
        val sigs = groupSignals(trades)

        // PF / RR / MD : mêmes formules que la ligne dépliante précédente
        val totL = kotlin.math.abs(perte)
        val pf = if (totL > 0) gain / totL else if (gain > 0) 99.0 else 0.0
        val winsP = profits.filter { it > 0 }
        val lossP = profits.filter { it < 0 }
        val avgWin = if (winsP.isNotEmpty()) winsP.average() else 0.0
        val avgLoss = if (lossP.isNotEmpty()) kotlin.math.abs(lossP.average()) else 0.0
        val rr = if (avgLoss > 0) avgWin / avgLoss else if (avgWin > 0) 99.0 else 0.0
        var peak = 0.0; var equity = 0.0; var md = 0.0
        for (p in profits) { equity += p; if (equity > peak) peak = equity; val dd = peak - equity; if (dd > md) md = dd }

        // --- 4 cartes du haut ---
        val boxes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun addBox(v: LinearLayout) {
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(3), 0, dp(3), 0)
            boxes.addView(v, lp)
        }
        addBox(detailBox("P&L", String.format("%+.2f", pnl), getColor(if (pnl >= 0) R.color.success else R.color.danger)))
        addBox(detailBox("S / T", "${sigs.size} / ${trades.size}", null))
        addBox(detailBox("G / P", "$wins / $losses", null))
        addBox(detailBox("WINRATE", String.format("%.1f", wr) + " %",
            getColor(if (wr >= 50) R.color.success else if (wr >= 40) R.color.warning else R.color.danger)))
        content.addView(boxes, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(12), 0, 0) })

        val colPF = getColor(if (pf >= 1.5) R.color.success else if (pf >= 1.0) R.color.warning else R.color.danger)
        val colRR = getColor(if (rr >= 1.5) R.color.success else if (rr >= 1.0) R.color.warning else R.color.danger)
        addDetailLine(content, listOf(
            Triple("PF = ", if (pf >= 99) "99" else String.format("%.2f", pf), colPF),
            Triple("RR = ", if (rr >= 99) "99" else String.format("%.2f", rr), colRR),
            Triple("MD = ", if (md <= 0) "00" else String.format("%.2f", md), getColor(R.color.danger))
        ))
        addDetailLine(content, listOf(
            Triple("Gains : ", String.format("%.2f", gain) + " $", getColor(R.color.success)),
            Triple("Meilleur trade : ", String.format("%.2f", profits.maxOrNull() ?: 0.0) + " $", getColor(R.color.success)),
            Triple("Moy. ", String.format("%.2f", avgWin) + " $", getColor(R.color.success))
        ))
        addDetailLine(content, listOf(
            Triple("Pertes : ", String.format("%.2f", perte) + " $", getColor(R.color.danger)),
            Triple("Pire trade : ", String.format("%.2f", profits.minOrNull() ?: 0.0) + " $", getColor(R.color.danger)),
            Triple("Moy. ", String.format("%.2f", -avgLoss) + " $", getColor(R.color.danger))
        ))

        // --- par ordre : ligne 1 = MK / L1 / L2, ligne 2 = L3 / L4 (uniquement s'ils existent) ---
        val parOrdre = LinkedHashMap<String, Pair<Int, Double>>()
        trades.forEach { t ->
            val p = t.comment.split("-")
            val role = if (p.size >= 3) p[2] else "?"
            val cur = parOrdre[role] ?: Pair(0, 0.0)
            parOrdre[role] = Pair(cur.first + 1, cur.second + t.profit)
        }
        fun ordreParts(roles: List<String>) = roles.filter { parOrdre.containsKey(it) }.map {
            val v = parOrdre[it]!!
            Triple("$it : ", "${v.first} : ${String.format("%.2f", v.second)} $", null)
        }
        val partsZones = ordreParts(listOf("MK", "L1", "L2"))
        if (partsZones.isNotEmpty()) addDetailLine(content, partsZones)
        val partsHorsZone = ordreParts(listOf("L3", "L4"))
        if (partsHorsZone.isNotEmpty()) addDetailLine(content, partsHorsZone)

        // --- par type de signal : nb signaux / nb trades ---
        val parType = LinkedHashMap<String, Pair<Int, Int>>()
        sigs.forEach { g ->
            val cur = parType[g.type] ?: Pair(0, 0)
            parType[g.type] = Pair(cur.first + 1, cur.second + g.legs.size)
        }
        addDetailLine(content, parType.map { (type, v) -> Triple("$type : ", "${v.first} / ${v.second}", null) })

        // --- détail des signaux (le plus récent en premier) ---
        content.addView(TextView(this).apply {
            text = "DÉTAIL DES SIGNAUX (${sigs.size})"
            textSize = 9.5f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dp(14), 0, dp(7))
        })

        sigs.reversed().forEach { g ->
            val net = g.legs.sumOf { it.pnl }
            // cloture autre que TP (5) / SL (4) : plus de bordure orange (26/09),
            // simple fond un peu plus clair pour repere discret
            val autre = g.legs.any { it.reason != null && it.reason != 4 && it.reason != 5 }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(Color.parseColor(if (autre) "#221E34" else "#1A1A2E"),
                    getColor(R.color.border), 13)
            }
            val head = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), dp(9), dp(11), dp(9))
            }
            head.addView(TextView(this).apply {
                text = hhmm(g.debut); textSize = 11f
                setTextColor(getColor(R.color.text_secondary))
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(0, 0, dp(7), 0)
            })
            head.addView(detailTag(g.side, Color.parseColor("#9DB4FF"), Color.parseColor("#22304F")))
            head.addView(detailTag(g.type, Color.parseColor("#C3A8FF"), Color.parseColor("#2B2340")))
            // BLOC 3 (11/10/2026) : badge JAUNE = raison de fermeture (code ecrit par le bot)
            val codesRaison = MotifCodes.codes
            val codeRaison = g.legs.mapNotNull { lg ->
                val u = lg.cmt.uppercase()
                codesRaison.firstOrNull { c ->
                    u == c || u.endsWith("-$c") || u.endsWith(":$c") || u.contains("-$c-") || u.startsWith("$c-")
                }
            }.firstOrNull()
            if (codeRaison != null) {
                head.addView(detailTag(codeRaison, Color.parseColor("#FFEB3B"), Color.parseColor("#33FFEB3B")))
            }
            // BLOC 4 (11/10/2026) : la duree du SIGNAL est retiree ici ; elle est remplacee
            // par la duree PROPRE A CHAQUE POSITION, affichee sur la ligne de chaque position.
            head.addView(TextView(this).apply {
                text = String.format("%+.2f $", net); textSize = 11.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(if (net >= 0) R.color.success else R.color.danger))
                gravity = Gravity.END
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(head)
            g.legs.forEach { leg ->
                val lr = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(11), dp(6), dp(11), dp(6))
                }
                lr.addView(TextView(this).apply {
                    text = leg.role; textSize = 10.5f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(getColor(R.color.text_primary))
                }, LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.WRAP_CONTENT))
                lr.addView(TextView(this).apply {
                    text = String.format("%.2f", leg.open) + " → " + String.format("%.2f", leg.close)
                    textSize = 10.5f; setTextColor(getColor(R.color.text_secondary))
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                // BLOC 4 : duree PROPRE a cette position, JUSTE APRES le prix de fermeture
                lr.addView(TextView(this).apply {
                    text = dureeTexte(dureeMin(leg.openT, leg.closeT)); textSize = 10f
                    setTextColor(getColor(R.color.text_muted)); setPadding(dp(8), 0, dp(8), 0)
                })
                // ressort : repousse le P&L a droite (la duree n'est plus collee au P&L)
                lr.addView(android.view.View(this),
                    LinearLayout.LayoutParams(0, 1, 1f))
                lr.addView(TextView(this).apply {
                    text = String.format("%+.2f $", leg.pnl); textSize = 10.5f
                    setTextColor(getColor(if (leg.pnl >= 0) R.color.success else R.color.danger))
                    gravity = Gravity.END
                })
                card.addView(lr)
            }
            content.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(9)) })
        }

        dlg.show()
    }

    private fun addPerfSignalRow(container: LinearLayout, label: String, d: PerfData, isTotal: Boolean = false) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); if (isTotal) setBackgroundColor(Color.parseColor("#1A1A2E")) }
        fun cell(text: String, weight: Float, color: Int, bold: Boolean = false) {
            val tv = TextView(this).apply { this.text = text; setTextColor(color); textSize = 13f; if (bold) setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight))
        }
        cell(label, 0.8f, getColor(R.color.primary_light), bold = true)
        cell(String.format("%+.2f", d.pnl), 1f, if (d.pnl >= 0) getColor(R.color.success) else getColor(R.color.danger), bold = true)
        cell(d.channelCount.toString(), 0.6f, getColor(R.color.text_primary))
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
