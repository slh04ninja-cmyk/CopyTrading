package com.gestionrh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class EntryFragment : Fragment() {
    private val editFields = mutableMapOf<String, EditText>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSchoolSpinner()
        view.findViewById<Button>(R.id.btnSave).setOnClickListener { saveEntry() }
        view.findViewById<Button>(R.id.btnClear).setOnClickListener { clearEntry() }
    }

    private fun setupSchoolSpinner() {
        val ctx = context ?: return
        val schools = DataStore.getSchools(ctx)
        val spinner = view?.findViewById<Spinner>(R.id.spinnerSchool) ?: return
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, schools.map { it.name })
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) { buildEntryGrid() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun refresh() { setupSchoolSpinner() }

    private fun buildEntryGrid() {
        val ctx = context ?: return
        val view = view ?: return
        val grid = view.findViewById<GridLayout>(R.id.gridEntry) ?: return
        grid.removeAllViews()
        editFields.clear()

        val schoolName = (view.findViewById<Spinner>(R.id.spinnerSchool)?.selectedItem as? String) ?: return
        val schoolData = DataStore.getData(ctx)[schoolName] ?: emptyMap()

        DataStore.SUBJECTS.forEach { subj ->
            val cellView = LayoutInflater.from(ctx).inflate(R.layout.item_entry_cell, grid, false)
            cellView.findViewById<TextView>(R.id.tvSubject).text = subj
            val et = cellView.findViewById<EditText>(R.id.etValue)
            et.setText((schoolData[subj] ?: 0).toString())
            editFields[subj] = et
            grid.addView(cellView)
        }
    }

    private fun saveEntry() {
        val ctx = context ?: return
        val view = view ?: return
        val schoolName = (view.findViewById<Spinner>(R.id.spinnerSchool)?.selectedItem as? String) ?: return
        val data = DataStore.getData(ctx).toMutableMap()
        val schoolData = mutableMapOf<String, Int>()
        editFields.forEach { (subj, et) ->
            schoolData[subj] = et.text.toString().toIntOrNull() ?: 0
        }
        data[schoolName] = schoolData
        DataStore.saveData(ctx, data)
        Toast.makeText(ctx, "تم الحفظ", Toast.LENGTH_SHORT).show()
    }

    private fun clearEntry() {
        editFields.values.forEach { it.setText("0") }
    }
}
