1|package com.copytrading
2|
3|import android.animation.AnimatorSet
4|import android.animation.ArgbEvaluator
5|import android.animation.ObjectAnimator
6|import android.animation.ValueAnimator
7|import android.content.Context
8|import android.content.Intent
9|import android.graphics.Canvas
10|import android.graphics.Color
11|import android.graphics.Paint
12|import android.graphics.RectF
13|import android.os.Bundle
14|import android.view.Gravity
15|import android.view.View
16|import android.view.animation.DecelerateInterpolator
17|import android.view.animation.LinearInterpolator
18|import android.view.animation.OvershootInterpolator
19|import android.widget.*
import android.widget.LinearLayout
20|import androidx.appcompat.app.AppCompatActivity
21|import androidx.core.widget.NestedScrollView
22|import androidx.lifecycle.lifecycleScope
23|import androidx.recyclerview.widget.LinearLayoutManager
24|import androidx.recyclerview.widget.RecyclerView
25|import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
26|import com.copytrading.api.ApiClient
27|import com.copytrading.model.*
28|import com.copytrading.ui.PositionAdapter
29|import com.google.android.material.button.MaterialButton
30|import com.google.android.material.snackbar.Snackbar
31|import kotlinx.coroutines.delay
32|import kotlinx.coroutines.launch
33|import kotlin.math.cos
34|import kotlin.math.sin
35|import kotlin.random.Random
36|
37|class MainActivity : AppCompatActivity() {
38|
39|    private lateinit var client: ApiClient
40|    private lateinit var swipeRefresh: SwipeRefreshLayout
41|    private lateinit var rootLayout: View
42|
43|    // Header
44|    private lateinit var tvBotStatus: TextView
45|    private lateinit var btnSettings: ImageView
46|
47|    // P&L Cards
48|    private lateinit var tvDailyPnl: TextView
49|    private lateinit var tvFloatingPnl: TextView
50|    private lateinit var tvTotalPnl: TextView
51|    private lateinit var tvBalance: TextView
52|    private lateinit var tvEquity: TextView
53|
54|    // Stats
55|    private lateinit var tvTrades: TextView
56|    private lateinit var tvWins: TextView
57|    private lateinit var tvLosses: TextView
58|    private lateinit var tvWinrate: TextView
59|60|
61|    // Daily Limit
62|    private lateinit var tvDailyLimit: TextView
63|    private lateinit var progressDailyLimit: ProgressBar
64|
65|    // Controls
66|    private lateinit var btnStartStop: MaterialButton
67|    private lateinit var btnCloseAll: MaterialButton
68|
69|    // Positions
70|    private lateinit var rvPositions: RecyclerView
71|    private lateinit var tvNoPositions: TextView
72|    private lateinit var positionAdapter: PositionAdapter
73|
74|    // Tabs
75|    private lateinit var tabDashboard: TextView
76|    private lateinit var tabPositions: TextView
77|    private lateinit var tabConfig: TextView
78|    private lateinit var tabLogs: TextView
79|
80|    // Panels
81|    private lateinit var panelDashboard: NestedScrollView
82|    private lateinit var panelPositions: ScrollView
83|    private lateinit var panelConfig: ScrollView
84|    private lateinit var panelLogs: ScrollView
85|    private lateinit var pillIndicator: View
86|
87|    // Config
88|    private lateinit var etConfigContent: EditText
89|    private lateinit var btnSaveConfig: MaterialButton
90|
91|    // Logs
92|    private lateinit var tvLogs: TextView
93|    private lateinit var btnRefreshLogs: MaterialButton
94|
95|    private var isRunning = false
96|    private var autoRefresh = true
97|    private var closeAllBusy = false
98|
99|    // Colors for morph button
100|    private val idleColor = Color.parseColor("#E53935")
101|    private val loadingColor = Color.parseColor("#FF9800")
102|    private val successColor = Color.parseColor("#4CAF50")
103|
104|    override fun onCreate(savedInstanceState: Bundle?) {
105|        super.onCreate(savedInstanceState)
106|        setContentView(R.layout.activity_main)
107|
108|        client = ApiClient(this)
109|        initViews()
110|        setupTabs()
111|        setupListeners()
112|
113|        startAutoRefresh()
114|        refreshDashboard()
115|    }
116|
117|    private fun initViews() {
118|        swipeRefresh = findViewById(R.id.swipeRefresh)
119|        rootLayout = findViewById(R.id.rootLayout)
120|
121|        tvBotStatus = findViewById(R.id.tvBotStatus)
122|        btnSettings = findViewById(R.id.btnSettings)
123|
124|        tvDailyPnl = findViewById(R.id.tvDailyPnl)
125|        tvFloatingPnl = findViewById(R.id.tvFloatingPnl)
126|        tvTotalPnl = findViewById(R.id.tvTotalPnl)
127|        tvBalance = findViewById(R.id.tvBalance)
128|        tvEquity = findViewById(R.id.tvEquity)
129|
130|        tvTrades = findViewById(R.id.tvTrades)
131|        tvWins = findViewById(R.id.tvWins)
132|        tvLosses = findViewById(R.id.tvLosses)
133|        tvWinrate = findViewById(R.id.tvWinrate)
134|135|
136|        tvDailyLimit = findViewById(R.id.tvDailyLimit)
137|        progressDailyLimit = findViewById(R.id.progressDailyLimit)
138|
139|        btnStartStop = findViewById(R.id.btnStartStop)
140|        btnCloseAll = findViewById(R.id.btnCloseAll)
141|
142|        rvPositions = findViewById(R.id.rvPositions)
143|        tvNoPositions = findViewById(R.id.tvNoPositions)
144|
145|        tabDashboard = findViewById(R.id.tabDashboard)
146|        tabPositions = findViewById(R.id.tabPositions)
147|        tabConfig = findViewById(R.id.tabConfig)
148|        tabLogs = findViewById(R.id.tabLogs)
149|
150|        panelDashboard = findViewById(R.id.panelDashboard)
151|        panelPositions = findViewById(R.id.panelPositions)
152|        panelConfig = findViewById(R.id.panelConfig)
153|        panelLogs = findViewById(R.id.panelLogs)
154|        pillIndicator = findViewById(R.id.pillIndicator)
155|
156|        etConfigContent = findViewById(R.id.etConfigContent)
157|        btnSaveConfig = findViewById(R.id.btnSaveConfig)
158|
159|        tvLogs = findViewById(R.id.tvLogs)
160|        btnRefreshLogs = findViewById(R.id.btnRefreshLogs)
161|
162|        positionAdapter = PositionAdapter { ticket ->
163|            closePosition(ticket)
164|        }
165|        rvPositions.layoutManager = LinearLayoutManager(this)
166|        rvPositions.adapter = positionAdapter
167|    }
168|
169|    private fun setupTabs() {
170|        val tabs = listOf(tabDashboard, tabPositions, tabConfig, tabLogs)
171|        var currentTab = 0
172|
173|        fun selectTab(index: Int) {
174|            currentTab = index
175|            tabs.forEachIndexed { i, tab ->
176|                tab.setTextColor(getColor(if (i == index) R.color.text_primary else R.color.text_secondary))
177|                tab.setTypeface(null, if (i == index) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
178|            }
179|
180|            pillIndicator.post {
181|                val tabWidth = tabs[0].width.toFloat()
182|                pillIndicator.layoutParams = pillIndicator.layoutParams.apply {
183|                    width = tabWidth.toInt()
184|                }
185|                pillIndicator.animate()
186|                    .translationX(tabWidth * index)
187|                    .setDuration(250)
188|                    .setInterpolator(DecelerateInterpolator(2f))
189|                    .start()
190|            }
191|
192|            swipeRefresh.visibility = View.GONE
193|            panelPositions.visibility = View.GONE
194|            panelConfig.visibility = View.GONE
195|            panelLogs.visibility = View.GONE
196|
197|            when (index) {
198|                0 -> swipeRefresh.visibility = View.VISIBLE
199|                1 -> {
200|                    panelPositions.visibility = View.VISIBLE
201|                    refreshPositions()
202|                }
203|                2 -> {
204|                    panelConfig.visibility = View.VISIBLE
205|                    loadConfig()
206|                }
207|                3 -> {
208|                    panelLogs.visibility = View.VISIBLE
209|                    loadLogs()
210|                }
211|            }
212|        }
213|
214|        tabs.forEachIndexed { index, tab ->
215|            tab.setOnClickListener { selectTab(index) }
216|        }
217|
218|        pillIndicator.post {
219|            val tabWidth = tabs[0].width.toFloat()
220|            pillIndicator.layoutParams = pillIndicator.layoutParams.apply {
221|                width = tabWidth.toInt()
222|            }
223|            pillIndicator.translationX = 0f
224|        }
225|
226|        selectTab(0)
227|    }
228|
229|    private fun setupDashTabs() {
        fun selectDashTab(index: Int) {
            currentDashTab = index
            dashTabOverview.setTextColor(getColor(if (index == 0) R.color.text_primary else R.color.text_muted))
            dashTabOverview.setBackgroundColor(getColor(if (index == 0) R.color.primary else R.color.card_background))
            dashTabPerformance.setTextColor(getColor(if (index == 1) R.color.text_primary else R.color.text_muted))
            dashTabPerformance.setBackgroundColor(getColor(if (index == 1) R.color.primary else R.color.card_background))
            if (index == 0) {
                panelPerformance.visibility = View.GONE
                swipeRefresh.visibility = View.VISIBLE
            } else {
                panelPerformance.visibility = View.VISIBLE
                swipeRefresh.visibility = View.GONE
            }
        }
        dashTabOverview.setOnClickListener { selectDashTab(0) }
        dashTabPerformance.setOnClickListener { selectDashTab(1) }
    }

    private fun setupListeners() {
230|        swipeRefresh.setOnRefreshListener {
231|            refreshDashboard()
232|        }
233|
234|        btnSettings.setOnClickListener {
235|            val prefs = getSharedPreferences("copytrading", Context.MODE_PRIVATE)
236|            prefs.edit().remove("server_host").apply()
237|            startActivity(Intent(this, SetupActivity::class.java))
238|            finish()
239|        }
240|
241|        btnStartStop.setOnClickListener {
242|            lifecycleScope.launch {
243|                if (isRunning) {
244|                    client.stopBot()
245|                    showNotification("Bot arrete")
246|                } else {
247|                    client.startBot()
248|                    showNotification("Bot demarre")
249|                }
250|                delay(1000)
251|                refreshDashboard()
252|            }
253|        }
254|
255|        btnCloseAll.setOnClickListener {
256|            if (!closeAllBusy) {
257|                morphCloseAll()
258|            }
259|        }
260|
261|        btnSaveConfig.setOnClickListener {
262|            saveConfig()
263|        }
264|
265|        btnRefreshLogs.setOnClickListener {
266|            loadLogs()
267|        }
268|    }
269|
270|    // --- MORPH BUTTON ANIMATION ---
271|    private fun morphCloseAll() {
272|        closeAllBusy = true
273|        btnCloseAll.isEnabled = false
274|
275|        // Phase 1: idle -> loading
276|        animateColor(btnCloseAll, idleColor, loadingColor, 350)
277|        btnCloseAll.text = "Fermeture..."
278|
279|        // Start spinning ring overlay
280|        val ringView = SpinningRingView(this)
281|        val parent = btnCloseAll.parent as? android.view.ViewGroup
282|        val lp = android.widget.FrameLayout.LayoutParams(
283|            (24 * resources.displayMetrics.density).toInt(),
284|            (24 * resources.displayMetrics.density).toInt()
285|        )
286|        // Add ring as overlay on the button
287|        parent?.addView(ringView, lp)
288|        ringView.x = btnCloseAll.x + btnCloseAll.width / 2f - 12 * resources.displayMetrics.density
289|        ringView.y = btnCloseAll.y + btnCloseAll.height / 2f - 12 * resources.displayMetrics.density
290|        ringView.startSpin()
291|
292|        // Execute the close-all
293|        lifecycleScope.launch {
294|            client.closeAll()
295|            delay(1000)
296|            refreshDashboard()
297|            refreshPositions()
298|
299|            // Phase 2: loading -> success
300|            ringView.stopSpin()
301|            parent?.removeView(ringView)
302|            animateColor(btnCloseAll, loadingColor, successColor, 350)
303|            btnCloseAll.text = ""
304|
305|            // Draw checkmark overlay
306|            val checkView = CheckmarkView(this@MainActivity)
307|            parent?.addView(checkView, lp)
308|            checkView.x = btnCloseAll.x + btnCloseAll.width / 2f - 12 * resources.displayMetrics.density
309|            checkView.y = btnCloseAll.y + btnCloseAll.height / 2f - 12 * resources.displayMetrics.density
310|            checkView.animateCheck()
311|
312|            // Phase 3: reset after 2s
313|            delay(2000)
314|            parent?.removeView(checkView)
315|            animateColor(btnCloseAll, successColor, idleColor, 350)
316|            btnCloseAll.text = "TOUT FERMER"
317|            btnCloseAll.isEnabled = true
318|            closeAllBusy = false
319|        }
320|    }
321|
322|    private fun animateColor(view: View, from: Int, to: Int, duration: Long) {
323|        val animator = ValueAnimator.ofObject(ArgbEvaluator(), from, to)
324|        animator.duration = duration
325|        animator.interpolator = DecelerateInterpolator(2f)
326|        animator.addUpdateListener {
327|            view.setBackgroundColor(it.animatedValue as Int)
328|        }
329|        animator.start()
330|    }
331|
332|    // --- CONFETTI BURST ---
333|    private fun burstConfetti(anchor: View, onEnd: () -> Unit = {}) {
334|        val parent = rootLayout as? android.view.ViewGroup ?: return onEnd()
335|        val loc = IntArray(2)
336|        anchor.getLocationOnScreen(loc)
337|        val rootLoc = IntArray(2)
338|        parent.getLocationOnScreen(rootLoc)
339|        val cx = loc[0] - rootLoc[0] + anchor.width / 2f
340|        val cy = loc[1] - rootLoc[1] + anchor.height / 2f
341|
342|        val colors = intArrayOf(
343|            Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
344|            Color.parseColor("#45B7D1"), Color.parseColor("#96CEB4"),
345|            Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
346|            Color.parseColor("#98D8C8"), Color.parseColor("#F7DC6F")
347|        )
348|        val density = resources.displayMetrics.density
349|        val particles = mutableListOf<Pair<View, Triple<Float, Float, Float>>>() // dx, dy, rotation
350|
351|        for (i in 0 until 16) {
352|            val angle = Math.toRadians((i * 360.0 / 16) + Random.nextDouble(-15.0, 15.0))
353|            val distance = (60 + Random.nextFloat() * 80) * density
354|            val size = (4 + Random.nextFloat() * 4) * density
355|            val rotation = Random.nextFloat() * 720f - 360f
356|            val color = colors[Random.nextInt(colors.size)]
357|
358|            val particle = View(this).apply {
359|                setBackgroundColor(color)
360|            }
361|            val lp = FrameLayout.LayoutParams(size.toInt(), size.toInt())
362|            lp.leftMargin = (cx - size / 2).toInt()
363|            lp.topMargin = (cy - size / 2).toInt()
364|            parent.addView(particle, lp)
365|
366|            val dx = (cos(angle) * distance).toFloat()
367|            val dy = (sin(angle) * distance).toFloat()
368|            particles.add(particle to Triple(dx, dy, rotation))
369|        }
370|
371|        val animators = particles.map { (view, triple) ->
372|            val (dx, dy, rot) = triple
373|            AnimatorSet().apply {
374|                playTogether(
375|                    ObjectAnimator.ofFloat(view, "translationX", 0f, dx),
376|                    ObjectAnimator.ofFloat(view, "translationY", 0f, dy),
377|                    ObjectAnimator.ofFloat(view, "rotation", 0f, rot),
378|                    ObjectAnimator.ofFloat(view, "alpha", 1f, 0f),
379|                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.2f),
380|                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.2f)
381|                )
382|                duration = 900
383|                interpolator = DecelerateInterpolator(1.5f)
384|            }
385|        }
386|
387|        val set = AnimatorSet()
388|        set.playTogether(animators)
389|        set.addListener(object : android.animation.AnimatorListenerAdapter() {
390|            override fun onAnimationEnd(animation: android.animation.Animator) {
391|                particles.forEach { parent.removeView(it.first) }
392|                onEnd()
393|            }
394|        })
395|        set.start()
396|    }
397|
398|    // --- NOTIFICATION ---
399|    private fun showNotification(message: String) {
400|        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT)
401|            .setBackgroundTint(Color.parseColor("#1E1E2E"))
402|            .setTextColor(Color.WHITE)
403|            .setAnchorView(pillIndicator)
404|            .show()
405|    }
406|
407|    private fun startAutoRefresh() {
408|        lifecycleScope.launch {
409|            while (autoRefresh) {
410|                delay(5000)
411|                try {
412|                    refreshDashboard()
413|                } catch (_: Exception) {}
414|            }
415|        }
416|    }
417|
418|    private fun refreshDashboard() {
419|        lifecycleScope.launch {
420|            try {
421|                val status = client.getStatus()
422|                val dashboard = client.getDashboard()
423|
424|                if (status != null) {
425|                    updateBotStatus(status)
426|                }
427|
428|                if (dashboard != null) {
429|                    updateDashboard(dashboard)
430|                } else {
431|                    tvDailyPnl.text = "ERR: null"
432|                    tvDailyPnl.setTextColor(getColor(R.color.danger))
433|                }
434|            } catch (e: Exception) {
435|                tvDailyPnl.text = "ERR: ${e.message}"
436|                tvDailyPnl.setTextColor(getColor(R.color.danger))
437|            }
438|
439|            swipeRefresh.isRefreshing = false
440|        }
441|    }
442|
443|    private fun updateBotStatus(status: StatusResponse) {
444|        val bot = status.bot
445|        isRunning = bot.status == "running"
446|
447|        when (bot.status) {
448|            "running" -> {
449|                tvBotStatus.text = "EN LIGNE"
450|                tvBotStatus.setTextColor(getColor(R.color.success))
451|                btnStartStop.text = "ARRETER"
452|                btnStartStop.setBackgroundColor(getColor(R.color.danger))
453|            }
454|            "stopped" -> {
455|                tvBotStatus.text = "ARRETE"
456|                tvBotStatus.setTextColor(getColor(R.color.danger))
457|                btnStartStop.text = "DEMARRER"
458|                btnStartStop.setBackgroundColor(getColor(R.color.success))
459|            }
460|            "error" -> {
461|                tvBotStatus.text = "ERREUR"
462|                tvBotStatus.setTextColor(getColor(R.color.warning))
463|                btnStartStop.text = "DEMARRER"
464|                btnStartStop.setBackgroundColor(getColor(R.color.success))
465|            }
466|        }
467|
468|        val mt5 = status.mt5
469|        if (mt5.connected && mt5.account != null) {
470|            tvBalance.text = formatMoney(mt5.account.balance)
471|            tvEquity.text = formatMoney(mt5.account.equity)
472|        }
473|    }
474|
475|    private fun updateDashboard(dash: DashboardResponse) {
476|        tvDailyPnl.text = formatPnl(dash.daily_pnl)
477|        tvDailyPnl.setTextColor(getPnlColor(dash.daily_pnl))
478|        tvFloatingPnl.text = formatPnl(dash.floating_pnl)
479|        tvFloatingPnl.setTextColor(getPnlColor(dash.floating_pnl))
480|        tvTotalPnl.text = formatPnl(dash.total_pnl)
481|        tvTotalPnl.setTextColor(getPnlColor(dash.total_pnl))
482|
483|        tvTrades.text = dash.trades.toString()
484|        tvWins.text = dash.wins.toString()
485|        tvLosses.text = dash.losses.toString()
486|        tvWinrate.text = "${dash.winrate}%"
487|488|
489|        tvDailyLimit.text = "${formatPnl(dash.total_pnl)} / ${formatMoney(dash.daily_limit)}"
490|        progressDailyLimit.progress = dash.limit_pct.toInt().coerceIn(0, 100)
491|
492|        positionAdapter.setPositions(dash.open_positions)
493|        tvNoPositions.visibility = if (dash.open_positions.isEmpty()) View.VISIBLE else View.GONE
494|        rvPositions.visibility = if (dash.open_positions.isEmpty()) View.GONE else View.VISIBLE
495|    }
496|
497|    private fun refreshPositions() {
498|        lifecycleScope.launch {
499|            val positions = client.getPositions()
500|            if (positions != null) {
501|                positionAdapter.setPositions(positions.positions)
502|                tvNoPositions.visibility = if (positions.positions.isEmpty()) View.VISIBLE else View.GONE
503|                rvPositions.visibility = if (positions.positions.isEmpty()) View.GONE else View.VISIBLE
504|            }
505|        }
506|    }
507|
508|    private fun loadConfig() {
509|        lifecycleScope.launch {
510|            try {
511|                val config = client.getConfig()
512|                if (config != null) {
513|                    val sb = StringBuilder()
514|                    config.config.toSortedMap().forEach { (key, value) ->
515|                        sb.appendLine("$key=$value")
516|                    }
517|                    etConfigContent.setText(sb.toString())
518|                } else {
519|                    etConfigContent.setText("Erreur: impossible de charger la config")
520|                }
521|            } catch (e: Exception) {
522|                etConfigContent.setText("Erreur: ${e.message}")
523|            }
524|        }
525|    }
526|
527|    private fun saveConfig() {
528|        val configText = etConfigContent.text.toString()
529|        val values = mutableMapOf<String, String>()
530|        configText.lines().forEach { line ->
531|            val trimmed = line.trim()
532|            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
533|                val (key, value) = trimmed.split("=", limit = 2)
534|                values[key.trim()] = value.trim()
535|            }
536|        }
537|
538|        // Confetti burst then save
539|        burstConfetti(btnSaveConfig) {
540|            lifecycleScope.launch {
541|                val ok = client.updateConfig(values)
542|                if (ok) {
543|                    showNotification("Configuration sauvegardee")
544|                } else {
545|                    showNotification("Erreur de sauvegarde")
546|                }
547|            }
548|        }
549|    }
550|
551|    private fun loadLogs() {
552|        lifecycleScope.launch {
553|            try {
554|                val logs = client.getLogs(200)
555|                if (logs != null) {
556|                    tvLogs.text = logs.logs.joinToString("\n")
557|                    val scrollview = tvLogs.parent as? View
558|                    if (scrollview is ScrollView) {
559|                        scrollview.post { scrollview.fullScroll(View.FOCUS_DOWN) }
560|                    }
561|                } else {
562|                    tvLogs.text = "Erreur: impossible de charger les logs"
563|                }
564|            } catch (e: Exception) {
565|                tvLogs.text = "Erreur: ${e.message}"
566|            }
567|        }
568|    }
569|
570|    private fun closePosition(ticket: Long) {
571|        androidx.appcompat.app.AlertDialog.Builder(this)
572|            .setTitle("Fermer la position #$ticket ?")
573|            .setPositiveButton("Confirmer") { _, _ ->
574|                lifecycleScope.launch {
575|                    val result = client.closePosition(ticket)
576|                    if (result != null) {
577|                        showNotification("Position fermee: ${formatPnl(result.profit)}")
578|                    }
579|                    delay(1000)
580|                    refreshDashboard()
581|                }
582|            }
583|            .setNegativeButton("Annuler", null)
584|            .show()
585|    }
586|
587|    // --- HELPERS ---
588|    private fun formatMoney(value: Double): String {
589|        return String.format("%,.2f$", value)
590|    }
591|
592|    private fun formatPnl(value: Double): String {
593|        val sign = if (value >= 0) "+" else ""
594|        return "$sign${String.format("%.2f", value)}$"
595|    }
596|
597|    private fun getPnlColor(value: Double): Int {
598|        return when {
599|            value > 0 -> getColor(R.color.profit)
600|            value < 0 -> getColor(R.color.loss)
601|            else -> getColor(R.color.text_secondary)
602|        }
603|    }
604|
605|    private fun updatePerformance(trades: List<Trade>) {
        val channelData = mutableMapOf<String, PerfData>()
        val signalData = mutableMapOf<String, PerfData>()
        for (t in trades) {
            val parts = t.comment.split("-")
            val channel = parts.getOrElse(0) { "?" }
            val signal = parts.getOrElse(1) { "?" }
            channelData.getOrPut(channel) { PerfData() }.add(t)
            signalData.getOrPut(signal) { PerfData() }.add(t)
        }
        perfChannelTable.removeAllViews()
        addPerfHeader(perfChannelTable, "Canal", "P&L", "Tr", "Wn", "Ls", "WR")
        for ((ch, d) in channelData.toSortedMap()) addPerfRow(perfChannelTable, ch, d)
        val totalCh = channelData.values.fold(PerfData()) { acc, d -> acc.merge(d) }
        addPerfRow(perfChannelTable, "TOTAL", totalCh, isTotal = true)

        perfSignalTable.removeAllViews()
        addPerfHeader(perfSignalTable, "Signal", "P&L", "Tr", "Wn", "Ls", "WR")
        for ((sig, d) in signalData.toSortedMap()) addPerfRow(perfSignalTable, sig, d)
        val totalSig = signalData.values.fold(PerfData()) { acc, d -> acc.merge(d) }
        addPerfRow(perfSignalTable, "TOTAL", totalSig, isTotal = true)
    }

    private data class PerfData(var pnl: Double = 0.0, var trades: Int = 0, var wins: Int = 0, var losses: Int = 0) {
        fun add(t: Trade) { pnl += t.profit; trades++; if (t.profit >= 0) wins++ else losses++ }
        fun merge(o: PerfData) = PerfData(pnl + o.pnl, trades + o.trades, wins + o.wins, losses + o.losses)
        fun winrate() = if (trades > 0) (wins * 100 / trades) else 0
    }

    private fun addPerfHeader(container: LinearLayout, vararg cols: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); setBackgroundColor(Color.parseColor("#1A1A2E")) }
        val weights = floatArrayOf(1.2f, 1f, 0.8f, 0.8f, 0.8f, 0.8f)
        cols.forEachIndexed { i, c ->
            val tv = TextView(this).apply { text = c; setTextColor(getColor(R.color.text_muted)); textSize = 10f; setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights.getOrElse(i) { 1f }))
        }
        container.addView(row)
    }

    private fun addPerfRow(container: LinearLayout, label: String, d: PerfData, isTotal: Boolean = false) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)); if (isTotal) setBackgroundColor(Color.parseColor("#1A1A2E")) }
        fun cell(text: String, weight: Float, color: Int, bold: Boolean = false) {
            val tv = TextView(this).apply { this.text = text; setTextColor(color); textSize = 13f; if (bold) setTypeface(null, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight))
        }
        cell(label, 1.2f, getColor(R.color.primary_light), bold = true)
        cell(String.format("%+.2f", d.pnl), 1f, if (d.pnl >= 0) getColor(R.color.success) else getColor(R.color.danger), bold = true)
        cell(d.trades.toString(), 0.8f, getColor(R.color.text_primary))
        cell(d.wins.toString(), 0.8f, getColor(R.color.success))
        cell(d.losses.toString(), 0.8f, getColor(R.color.danger))
        cell("${d.winrate()}%", 0.8f, getColor(R.color.primary_light))
        container.addView(row)
        if (!isTotal) { container.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2A2A4A")) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
606|        super.onDestroy()
607|        autoRefresh = false
608|    }
609|}
610|
611|// --- Spinning Ring overlay view ---
612|class SpinningRingView(context: Context) : View(context) {
613|    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
614|        style = Paint.Style.STROKE
615|        strokeWidth = 3f * resources.displayMetrics.density
616|        strokeCap = Paint.Cap.ROUND
617|        color = Color.WHITE
618|    }
619|    private var angle = 0f
620|    private var animator: ValueAnimator? = null
621|
622|    fun startSpin() {
623|        animator = ValueAnimator.ofFloat(0f, 360f).apply {
624|            duration = 800
625|            repeatCount = ValueAnimator.INFINITE
626|            interpolator = LinearInterpolator()
627|            addUpdateListener {
628|                angle = it.animatedValue as Float
629|                invalidate()
630|            }
631|            start()
632|        }
633|    }
634|
635|    fun stopSpin() {
636|        animator?.cancel()
637|    }
638|
639|    override fun onDraw(canvas: Canvas) {
640|        super.onDraw(canvas)
641|        val r = width / 2f - paint.strokeWidth
642|        val rect = RectF(width / 2f - r, height / 2f - r, width / 2f + r, height / 2f + r)
643|        canvas.drawArc(rect, angle, 90f, false, paint)
644|    }
645|}
646|
647|// --- Checkmark overlay view ---
648|class CheckmarkView(context: Context) : View(context) {
649|    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
650|        style = Paint.Style.STROKE
651|        strokeWidth = 3f * resources.displayMetrics.density
652|        strokeCap = Paint.Cap.ROUND
653|        strokeJoin = Paint.Join.ROUND
654|        color = Color.WHITE
655|    }
656|    private var progress = 0f
657|
658|    fun animateCheck() {
659|        ValueAnimator.ofFloat(0f, 1f).apply {
660|            duration = 400
661|            interpolator = DecelerateInterpolator(2f)
662|            addUpdateListener {
663|                progress = it.animatedValue as Float
664|                invalidate()
665|            }
666|            start()
667|        }
668|    }
669|
670|    override fun onDraw(canvas: Canvas) {
671|        super.onDraw(canvas)
672|        val cx = width / 2f
673|        val cy = height / 2f
674|        val s = width / 3f
675|
676|        val startX = cx - s * 0.6f
677|        val startY = cy
678|        val midX = cx - s * 0.1f
679|        val midY = cy + s * 0.5f
680|        val endX = cx + s * 0.7f
681|        val endY = cy - s * 0.4f
682|
683|        if (progress <= 0.5f) {
684|            val p = progress * 2f
685|            canvas.drawLine(startX, startY, startX + (midX - startX) * p, startY + (midY - startY) * p, paint)
686|        } else {
687|            canvas.drawLine(startX, startY, midX, midY, paint)
688|            val p = (progress - 0.5f) * 2f
689|            canvas.drawLine(midX, midY, midX + (endX - midX) * p, midY + (endY - midY) * p, paint)
690|        }
691|    }
692|}
693|