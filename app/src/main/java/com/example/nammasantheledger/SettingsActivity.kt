package com.example.nammasantheledger

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val profilePrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val reminderPrefs = getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
        val appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // UI Components
        val shopNameText = findViewById<TextView>(R.id.settingsShopName)
        val btnEditProfile = findViewById<View>(R.id.btnEditProfile)
        val btnChangeLanguage = findViewById<View>(R.id.btnChangeLanguage)
        val currentLanguageText = findViewById<TextView>(R.id.currentLanguageText)
        val darkModeSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        val dailyReminderSwitch = findViewById<Switch>(R.id.dailyReminderSwitch)
        val reminderTimeBtn = findViewById<Button>(R.id.reminderTimeBtn)
        val weeklyReminderSwitch = findViewById<Switch>(R.id.weeklyReminderSwitch)
        val changePinBtn = findViewById<View>(R.id.changePinBtn)
        val btnLogout = findViewById<View>(R.id.btnLogout)

        // Load Values
        shopNameText.text = profilePrefs.getString("shopName", "My Shop")
        currentLanguageText.text = appPrefs.getString("language", "English")
        darkModeSwitch.isChecked = appPrefs.getBoolean("darkMode", false)
        
        dailyReminderSwitch.isChecked = reminderPrefs.getBoolean("dailyEnabled", false)
        weeklyReminderSwitch.isChecked = reminderPrefs.getBoolean("weeklyEnabled", false)
        val savedHour = reminderPrefs.getInt("hour", 20)
        val savedMinute = reminderPrefs.getInt("minute", 0)
        reminderTimeBtn.text = "Set Time: " + formatTime(savedHour, savedMinute)

        // 1. Profile
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // 2. Language
        btnChangeLanguage.setOnClickListener {
            val languages = arrayOf("English", "ಕನ್ನಡ (Kannada)", "हिंदी (Hindi)")
            AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages) { _, which ->
                    val selected = languages[which].split(" ")[0]
                    appPrefs.edit().putString("language", selected).apply()
                    currentLanguageText.text = selected
                    Toast.makeText(this, "Language updated to $selected", Toast.LENGTH_SHORT).show()
                    // Note: In a real app, you'd trigger a locale change here
                }
                .show()
        }

        // 3. Appearance (Dark Mode)
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            appPrefs.edit().putBoolean("darkMode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 4. Daily Reminder
        dailyReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderPrefs.edit().putBoolean("dailyEnabled", isChecked).apply()
            if (isChecked) {
                setDailyReminder(reminderPrefs.getInt("hour", 20), reminderPrefs.getInt("minute", 0))
            } else {
                cancelDailyReminder()
            }
        }

        reminderTimeBtn.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                reminderPrefs.edit().putInt("hour", hour).putInt("minute", minute).apply()
                reminderTimeBtn.text = "Set Time: " + formatTime(hour, minute)
                if (dailyReminderSwitch.isChecked) setDailyReminder(hour, minute)
            }, savedHour, savedMinute, false).show()
        }

        // 5. Weekly Reminder
        weeklyReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderPrefs.edit().putBoolean("weeklyEnabled", isChecked).apply()
            if (isChecked) scheduleWeeklyReminder() else cancelWeeklyReminder()
        }

        // 6. Security (Change PIN)
        changePinBtn.setOnClickListener {
            showChangePinDialog(loginPrefs)
        }

        // 7. Logout
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout") { _, _ ->
                    loginPrefs.edit().putBoolean("isLoggedIn", false).apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showChangePinDialog(prefs: android.content.SharedPreferences) {
        val input = EditText(this).apply {
            hint = "Enter new 4 or 6 digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Change Security PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPin = input.text.toString().trim()
                if (newPin.length == 4 || newPin.length == 6) {
                    prefs.edit().putString("pin", newPin).apply()
                    Toast.makeText(this, "PIN updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be 4 or 6 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%02d:%02d %s", h, minute, amPm)
    }

    private fun setDailyReminder(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }

    private fun cancelDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pending)
    }

    private fun scheduleWeeklyReminder() {
        val request = PeriodicWorkRequestBuilder<WeeklyReminderWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("weekly_reminder", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun cancelWeeklyReminder() {
        WorkManager.getInstance(this).cancelUniqueWork("weekly_reminder")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh shop name if it was changed in profile
        val profilePrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        findViewById<TextView>(R.id.settingsShopName).text = profilePrefs.getString("shopName", "My Shop")
    }
}
