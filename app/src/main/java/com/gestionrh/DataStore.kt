package com.gestionrh

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class School(val id: Int, val name: String, val address: String = "", val notes: String = "")

object DataStore {
    private const val PREFS_NAME = "hr_data"
    private const val KEY_SCHOOLS = "schools"
    private const val KEY_DATA = "data"
    private const val KEY_CONFIG = "config"

    val SUBJECTS = listOf(
        "ع فزيائية", "ع طبيعية", "رياضيات", "انجليزية", "فرنسية",
        "تربية بدنية", "اجتماعيات", "فلسفة", "تربية إسلامية", "عربية"
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultSchools(): List<School> = listOf(
        School(1, "الزرقطوني"), School(2, "جابر بن حيان"),
        School(3, "الفتح"), School(4, "ابن خلدون"),
        School(5, "الإمام علي"), School(6, "القدس"),
        School(7, "الأنصاري"), School(8, "الكندي")
    )

    fun getDefaultData(): Map<String, Map<String, Int>> = mapOf(
        "الزرقطوني" to mapOf("ع فزيائية" to 0, "ع طبيعية" to -1, "رياضيات" to 1, "انجليزية" to 0, "فرنسية" to 1, "تربية بدنية" to 1, "اجتماعيات" to -1, "فلسفة" to 0, "تربية إسلامية" to 0, "عربية" to 0),
        "جابر بن حيان" to mapOf("ع فزيائية" to 0, "ع طبيعية" to 0, "رياضيات" to 1, "انجليزية" to -1, "فرنسية" to 3, "تربية بدنية" to 0, "اجتماعيات" to 0, "فلسفة" to -1, "تربية إسلامية" to 0, "عربية" to 0),
        "الفتح" to mapOf("ع فزيائية" to 1, "ع طبيعية" to -1, "رياضيات" to 1, "انجليزية" to 1, "فرنسية" to 1, "تربية بدنية" to 1, "اجتماعيات" to 1, "فلسفة" to 0, "تربية إسلامية" to 0, "عربية" to 0),
        "ابن خلدون" to mapOf("ع فزيائية" to 1, "ع طبيعية" to 0, "رياضيات" to 2, "انجليزية" to 0, "فرنسية" to 0, "تربية بدنية" to 0, "اجتماعيات" to 0, "فلسفة" to 0, "تربية إسلامية" to 1, "عربية" to 2),
        "الإمام علي" to mapOf("ع فزيائية" to -2, "ع طبيعية" to 1, "رياضيات" to 1, "انجليزية" to 0, "فرنسية" to 0, "تربية بدنية" to 0, "اجتماعيات" to 1, "فلسفة" to -1, "تربية إسلامية" to -1, "عربية" to 1),
        "القدس" to mapOf("ع فزيائية" to 0, "ع طبيعية" to 0, "رياضيات" to 0, "انجليزية" to 0, "فرنسية" to 1, "تربية بدنية" to 0, "اجتماعيات" to 0, "فلسفة" to -1, "تربية إسلامية" to 0, "عربية" to 1),
        "الأنصاري" to mapOf("ع فزيائية" to 0, "ع طبيعية" to 0, "رياضيات" to 0, "انجليزية" to 0, "فرنسية" to 0, "تربية بدنية" to 0, "اجتماعيات" to 0, "فلسفة" to 0, "تربية إسلامية" to 0, "عربية" to 0),
        "الكندي" to mapOf("ع فزيائية" to 3, "ع طبيعية" to 0, "رياضيات" to 2, "انجليزية" to 1, "فرنسية" to 0, "تربية بدنية" to 0, "اجتماعيات" to 0, "فلسفة" to 0, "تربية إسلامية" to 1, "عربية" to 0)
    )

    fun getSchools(ctx: Context): List<School> {
        val json = prefs(ctx).getString(KEY_SCHOOLS, null) ?: return getDefaultSchools()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                School(o.getInt("id"), o.getString("name"), o.optString("address", ""), o.optString("notes", ""))
            }
        } catch (e: Exception) { getDefaultSchools() }
    }

    fun saveSchools(ctx: Context, schools: List<School>) {
        val arr = JSONArray()
        schools.forEach { s ->
            arr.put(JSONObject().put("id", s.id).put("name", s.name).put("address", s.address).put("notes", s.notes))
        }
        prefs(ctx).edit().putString(KEY_SCHOOLS, arr.toString()).apply()
    }

    fun getData(ctx: Context): Map<String, Map<String, Int>> {
        val json = prefs(ctx).getString(KEY_DATA, null) ?: return getDefaultData()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Map<String, Int>>()
            obj.keys().forEach { school ->
                val schoolObj = obj.getJSONObject(school)
                val schoolData = mutableMapOf<String, Int>()
                schoolObj.keys().forEach { subj -> schoolData[subj] = schoolObj.getInt(subj) }
                result[school] = schoolData
            }
            result
        } catch (e: Exception) { getDefaultData() }
    }

    fun saveData(ctx: Context, data: Map<String, Map<String, Int>>) {
        val obj = JSONObject()
        data.forEach { (school, subjects) ->
            val schoolObj = JSONObject()
            subjects.forEach { (subj, value) -> schoolObj.put(subj, value) }
            obj.put(school, schoolObj)
        }
        prefs(ctx).edit().putString(KEY_DATA, obj.toString()).apply()
    }

    fun getConfig(ctx: Context): Map<String, String> {
        val json = prefs(ctx).getString(KEY_CONFIG, null)
        return if (json != null) {
            try {
                val obj = JSONObject(json)
                val result = mutableMapOf<String, String>()
                obj.keys().forEach { result[it] = obj.getString(it) }
                result
            } catch (e: Exception) { getDefaultConfig() }
        } else getDefaultConfig()
    }

    fun saveConfig(ctx: Context, config: Map<String, String>) {
        val obj = JSONObject()
        config.forEach { (k, v) -> obj.put(k, v) }
        prefs(ctx).edit().putString(KEY_CONFIG, obj.toString()).apply()
    }

    private fun getDefaultConfig() = mapOf(
        "orgName" to "الكونفدرالية الديموقراطية للشغل",
        "unionName" to "النقابة الوطنية للتعليم - إقليم جرادة - المكتب الإقليمي",
        "schoolYear" to "2027 – 2026",
        "eduLevel" to "تأهيلي",
        "section" to "تربية بدنية"
    )
}
