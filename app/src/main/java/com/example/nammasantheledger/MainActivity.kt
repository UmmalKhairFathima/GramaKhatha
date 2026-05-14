package com.example.nammasantheledger

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: GramaViewModel by viewModels()
    private lateinit var adapter: CustomerAdapter
    private lateinit var totalNetDueTv: TextView
    private var currentShopName: String = "Gramakhata"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        totalNetDueTv = findViewById(R.id.totalNetDue)
        val customerList = findViewById<ListView>(R.id.customerList)
        val btnTake = findViewById<Button>(R.id.btnTake)
        val btnGive = findViewById<Button>(R.id.btnGive)
        val btnReport = findViewById<Button>(R.id.btnReport)

        val profilePrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        currentShopName = profilePrefs.getString("shopName", "Gramakhata") ?: "Gramakhata"
        findViewById<TextView>(R.id.shopNameSubtitle).text = "Shop: $currentShopName"

        // Setup Adapter
        adapter = CustomerAdapter(this, currentShopName, emptyList())
        customerList.adapter = adapter

        // Observe Data
        viewModel.customerBalances.observe(this) { balances ->
            adapter = CustomerAdapter(this, currentShopName, balances)
            customerList.adapter = adapter
        }

        viewModel.totalNetDue.observe(this) { total ->
            totalNetDueTv.text = String.format(Locale.getDefault(), "₹%.2f", total ?: 0.0)
        }

        // Bottom Actions (One-handed usability)
        btnGive.setOnClickListener { showAddTransactionDialog("GIVE") }
        btnTake.setOnClickListener { showAddTransactionDialog("TAKE") }

        btnReport.setOnClickListener {
            val report = viewModel.generateDailyReport()
            showReportDialog(report)
        }
    }

    private fun showAddTransactionDialog(type: String) {
        val builder = AlertDialog.Builder(this)
        val title = if (type == "GIVE") "Add Debt (ಸಾಲ ಕೊಡು)" else "Receive Payment (ಹಣ ಪಾವತಿ)"
        builder.setTitle(title)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 10)

        val nameInput = EditText(this)
        nameInput.hint = "Customer Name (ಹೆಸರು)"
        layout.addView(nameInput)

        val amountInput = EditText(this)
        amountInput.hint = "Amount ₹ (ಹಣ)"
        amountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(amountInput)

        val phoneInput = EditText(this)
        phoneInput.hint = "Phone / WhatsApp (Optional)"
        phoneInput.inputType = InputType.TYPE_CLASS_PHONE
        layout.addView(phoneInput)

        builder.setView(layout)

        builder.setPositiveButton("Save") { _, _ ->
            val name = nameInput.text.toString().trim()
            val amountStr = amountInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            if (name.isNotEmpty() && amountStr.isNotEmpty()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                viewModel.addTransaction(name, amount, type, phone)
                Toast.makeText(this, "✅ Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Name and Amount required", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showReportDialog(report: String) {
        AlertDialog.Builder(this)
            .setTitle("Daily Collection Report")
            .setMessage(report)
            .setPositiveButton("Share Text") { _, _ ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Share Report via"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_logout -> {
                getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("isLoggedIn", false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh shop name from profile
        val profilePrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        currentShopName = profilePrefs.getString("shopName", "Gramakhata") ?: "Gramakhata"
        findViewById<TextView>(R.id.shopNameSubtitle).text = "Shop: $currentShopName"
    }
}
