package com.example.nammasantheledger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var firstPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

        // Skip login if already logged in
        if (isLoggedIn) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val pinInput   = findViewById<EditText>(R.id.pinInput)
        val loginBtn   = findViewById<Button>(R.id.loginButton)
        val titleText  = findViewById<TextView>(R.id.loginTitle)
        val subTitle   = findViewById<TextView>(R.id.loginSubtitle)

        val hasPin = prefs.getString("pin", null) != null

        if (!hasPin) {
            titleText.text = getString(R.string.create_pin)
            subTitle.text  = "Choose a 4 or 6-digit PIN to protect your data"
            loginBtn.text  = "Continue"
            pinInput.hint = "Enter New PIN"
            pinInput.maxLength = 6
        } else {
            titleText.text = "Welcome Back!"
            subTitle.text  = "Enter your security PIN"
            loginBtn.text  = getString(R.string.login)
            pinInput.maxLength = 6
        }

        loginBtn.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            
            if (pin.length != 4 && pin.length != 6) {
                Toast.makeText(this, "PIN must be 4 or 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!hasPin) {
                // PIN Setup Flow
                if (firstPin == null) {
                    firstPin = pin
                    pinInput.setText("")
                    titleText.text = getString(R.string.confirm_pin)
                    subTitle.text = "Please re-enter your PIN to confirm"
                    loginBtn.text = "Set PIN"
                } else {
                    if (pin == firstPin) {
                        prefs.edit().putString("pin", pin).apply()
                        prefs.edit().putBoolean("isLoggedIn", true).apply()
                        Toast.makeText(this, "✅ PIN Set Successfully!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    } else {
                        Toast.makeText(this, "❌ PINs do not match. Try again.", Toast.LENGTH_SHORT).show()
                        firstPin = null
                        pinInput.setText("")
                        titleText.text = getString(R.string.create_pin)
                        subTitle.text = "Choose a 4 or 6-digit PIN"
                        loginBtn.text = "Continue"
                    }
                }
            } else {
                // Normal Login Flow
                val savedPin = prefs.getString("pin", "")
                if (pin == savedPin) {
                    prefs.edit().putBoolean("isLoggedIn", true).apply()
                    goToMain()
                } else {
                    Toast.makeText(this, "❌ Incorrect PIN!", Toast.LENGTH_SHORT).show()
                    pinInput.setText("")
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    private var EditText.maxLength: Int
        get() = 0
        set(value) {
            filters = arrayOf(android.text.InputFilter.LengthFilter(value))
        }
}