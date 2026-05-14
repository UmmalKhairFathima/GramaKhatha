package com.example.nammasantheledger

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.nammasantheledger.data.AppDatabase
import java.util.Locale

class WeeklyReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            // Get customers with positive balance (debtors)
            val debtors = db.transactionDao().getDebtorsSync()

            val prefs = applicationContext.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
            val shopName = prefs.getString("shopName", "Grama-Khata") ?: "Grama-Khata"

            debtors.forEach { customer ->
                if (!customer.phoneNumber.isNullOrEmpty()) {
                    val message = applicationContext.getString(
                        R.string.reminder_message,
                        shopName,
                        customer.balance
                    )
                    sendSms(customer.phoneNumber, message)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendSms(phone: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phone, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}