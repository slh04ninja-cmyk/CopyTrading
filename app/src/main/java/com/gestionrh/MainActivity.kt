package com.gestionrh

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    private lateinit var tabDashboard: LinearLayout
    private lateinit var tabSchools: LinearLayout
    private lateinit var tabEntry: LinearLayout
    private lateinit var tabTable: LinearLayout
    private val dashboardFragment = DashboardFragment()
    private val schoolsFragment = SchoolsFragment()
    private val entryFragment = EntryFragment()
    private val tableFragment = TableFragment()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabDashboard = findViewById(R.id.tabDashboard)
        tabSchools = findViewById(R.id.tabSchools)
        tabEntry = findViewById(R.id.tabEntry)
        tabTable = findViewById(R.id.tabTable)

        tabDashboard.setOnClickListener { switchTab(0) }
        tabSchools.setOnClickListener { switchTab(1) }
        tabEntry.setOnClickListener { switchTab(2) }
        tabTable.setOnClickListener { switchTab(3) }

        findViewById<ImageButton>(R.id.btnPdf).setOnClickListener {
            tableFragment.generatePdf()
        }

        // Initialize with default data if first run
        if (DataStore.getConfig(this).isEmpty()) {
            DataStore.saveConfig(this, mapOf(
                "orgName" to "الكونفدرالية الديموقراطية للشغل",
                "unionName" to "النقابة الوطنية للتعليم - إقليم جرادة - المكتب الإقليمي",
                "schoolYear" to "2027 – 2026",
                "eduLevel" to "تأهيلي",
                "section" to "تربية بدنية"
            ))
        }

        switchTab(0)
    }

    private fun switchTab(index: Int) {
        currentTab = index
        val fragment: Fragment = when (index) {
            0 -> dashboardFragment
            1 -> schoolsFragment
            2 -> entryFragment
            3 -> tableFragment
            else -> dashboardFragment
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        updateTabVisuals(index)
    }

    private fun updateTabVisuals(active: Int) {
        val tabs = listOf(tabDashboard, tabSchools, tabEntry, tabTable)
        val icons = listOf(
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_myplaces,
            android.R.drawable.ic_menu_edit,
            android.R.drawable.ic_menu_sort_by_size
        )

        tabs.forEachIndexed { i, tab ->
            val icon = tab.getChildAt(0) as? ImageView
            val label = tab.getChildAt(1) as? TextView
            if (i == active) {
                icon?.setColorFilter(ContextCompat.getColor(this, R.color.primary_light))
                label?.setTextColor(ContextCompat.getColor(this, R.color.primary_light))
            } else {
                icon?.setColorFilter(ContextCompat.getColor(this, R.color.text_muted))
                label?.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        when (currentTab) {
            0 -> dashboardFragment.refresh()
            1 -> schoolsFragment.refresh()
            2 -> entryFragment.refresh()
            3 -> tableFragment.refresh()
        }
    }
}
