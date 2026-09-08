package com.gestionrh

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SchoolsFragment : Fragment() {
    private lateinit var adapter: SchoolAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_schools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SchoolAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirm(it) }
        )
        view.findViewById<RecyclerView>(R.id.recyclerSchools).layoutManager = LinearLayoutManager(context)
        view.findViewById<RecyclerView>(R.id.recyclerSchools).adapter = adapter
        view.findViewById<View>(R.id.btnAddSchool).setOnClickListener { showAddDialog() }
        refresh()
    }

    fun refresh() {
        val ctx = context ?: return
        adapter.update(DataStore.getSchools(ctx))
    }

    private fun showAddDialog() {
        val ctx = context ?: return
        val input = EditText(ctx).apply {
            hint = "اسم المؤسسة"
            setTextColor(resources.getColor(R.color.text_primary, null))
            setHintTextColor(resources.getColor(R.color.text_muted, null))
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(ctx, R.style.Theme_GestionRH)
            .setTitle("إضافة مؤسسة")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val schools = DataStore.getSchools(ctx).toMutableList()
                    val newId = (schools.maxOfOrNull { it.id } ?: 0) + 1
                    schools.add(School(newId, name))
                    DataStore.saveSchools(ctx, schools)
                    refresh()
                    Toast.makeText(ctx, "تمت الإضافة", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showEditDialog(school: School) {
        val ctx = context ?: return
        val input = EditText(ctx).apply {
            setText(school.name)
            setTextColor(resources.getColor(R.color.text_primary, null))
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(ctx, R.style.Theme_GestionRH)
            .setTitle("تعديل المؤسسة")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val schools = DataStore.getSchools(ctx).toMutableList()
                    val idx = schools.indexOfFirst { it.id == school.id }
                    if (idx >= 0) {
                        schools[idx] = schools[idx].copy(name = name)
                        DataStore.saveSchools(ctx, schools)
                        // Update data key
                        val data = DataStore.getData(ctx).toMutableMap()
                        data[name] = data.remove(school.name) ?: emptyMap()
                        DataStore.saveData(ctx, data)
                        refresh()
                        Toast.makeText(ctx, "تم التعديل", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showDeleteConfirm(school: School) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx, R.style.Theme_GestionRH)
            .setTitle("حذف")
            .setMessage("حذف \"${school.name}\"؟")
            .setPositiveButton("نعم") { _, _ ->
                val schools = DataStore.getSchools(ctx).filter { it.id != school.id }
                DataStore.saveSchools(ctx, schools)
                val data = DataStore.getData(ctx).toMutableMap()
                data.remove(school.name)
                DataStore.saveData(ctx, data)
                refresh()
                Toast.makeText(ctx, "تم الحذف", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}

class SchoolAdapter(
    private val onEdit: (School) -> Unit,
    private val onDelete: (School) -> Unit
) : RecyclerView.Adapter<SchoolAdapter.VH>() {
    private var items = listOf<School>()
    fun update(list: List<School>) { items = list; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_school, parent, false)
        return VH(view)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.name.text = s.name
        holder.meta.text = if (s.address.isNotEmpty()) s.address else "—"
        holder.edit.setOnClickListener { onEdit(s) }
        holder.delete.setOnClickListener { onDelete(s) }
    }
    override fun getItemCount() = items.size
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvSchoolName)
        val meta: TextView = view.findViewById(R.id.tvSchoolMeta)
        val edit: ImageButton = view.findViewById(R.id.btnEdit)
        val delete: ImageButton = view.findViewById(R.id.btnDelete)
    }
}
