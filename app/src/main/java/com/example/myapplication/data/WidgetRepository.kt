package com.example.myapplication.data // Or a suitable package

import android.content.Context
import android.net.Uri
import com.example.myapplication.WidgetData // Correct import is already here
import com.example.myapplication.util.UriTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class WidgetRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
    private val gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

    private companion object {
        const val WIDGETS_KEY = "widgets_list"
    }

    fun saveWidgets(widgets: List<WidgetData>) {
        // This uses the imported com.example.myapplication.WidgetData, which is correct
        val jsonString = gson.toJson(widgets)
        sharedPreferences.edit().putString(WIDGETS_KEY, jsonString).apply()
    }

    fun loadWidgets(): List<WidgetData> {
        val jsonString = sharedPreferences.getString(WIDGETS_KEY, null)
        return if (jsonString != null) {
            // Explicitly use the FQN of the imported WidgetData in the TypeToken
            val type = object : TypeToken<List<com.example.myapplication.WidgetData>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
}