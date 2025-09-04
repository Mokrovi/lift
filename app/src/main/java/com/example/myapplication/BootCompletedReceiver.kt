package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log // Добавьте этот импорт

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver" // Тег для логов
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}") // Лог: начало onReceive
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "BOOT_COMPLETED received. Attempting to start MainActivity.") // Лог: BOOT_COMPLETED получен
            try {
                val i = Intent(context, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                Log.d(TAG, "MainActivity started successfully.") // Лог: Activity запущена
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MainActivity", e) // Лог: Ошибка при запуске Activity
            }
        }
    }
}