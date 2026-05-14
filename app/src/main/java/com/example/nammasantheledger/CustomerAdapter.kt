package com.example.nammasantheledger

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.nammasantheledger.data.CustomerBalance
import java.net.URLEncoder
import java.util.Locale

class CustomerAdapter(
    context: Context,
    private val shopName: String,
    private val items: List<CustomerBalance>
) : ArrayAdapter<CustomerBalance>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_customer, parent, false)
        
        val item = items[position]
        val nameText = view.findViewById<TextView>(R.id.customerNameText)
        val balanceText = view.findViewById<TextView>(R.id.balanceText)
        val totalGiveText = view.findViewById<TextView>(R.id.totalGiveText)
        val totalTakeText = view.findViewById<TextView>(R.id.totalTakeText)
        val whatsappBtn = view.findViewById<ImageView>(R.id.btnWhatsApp)
        val historyContainer = view.findViewById<LinearLayout>(R.id.historyContainer)
        val priorityIndicator = view.findViewById<View>(R.id.priorityIndicator)
        val priorityBadge = view.findViewById<TextView>(R.id.priorityBadge)

        nameText.text = item.customerName
        balanceText.text = String.format(Locale.getDefault(), "₹%.2f", item.balance)
        totalGiveText.text = String.format(Locale.getDefault(), "₹%.2f", item.totalGive)
        totalTakeText.text = String.format(Locale.getDefault(), "₹%.2f", item.totalTake)
        
        // 5. UI Improvements: Highlighting high pending dues
        // If balance > 2000, show priority badge and indicator
        if (item.balance >= 2000) {
            priorityIndicator?.visibility = View.VISIBLE
            priorityBadge?.visibility = View.VISIBLE
        } else {
            priorityIndicator?.visibility = View.GONE
            priorityBadge?.visibility = View.GONE
        }

        // Color balance: Red for debt (positive), Green for credit/paid off (<=0)
        if (item.balance > 0) {
            balanceText.setTextColor(context.getColor(R.color.give_red))
        } else {
            balanceText.setTextColor(context.getColor(R.color.take_green))
        }

        // 2. Customer Record Card: Populate Transaction History within the card
        historyContainer.removeAllViews()
        // Show last 3 transactions for a clean look
        item.transactions.takeLast(3).forEach { trans ->
            val row = TextView(context).apply {
                textSize = 12f
                setPadding(0, 4, 0, 4)
                val typeLabel = if (trans.type == "GIVE") "Debt" else "Paid"
                val color = if (trans.type == "GIVE") "#D32F2F" else "#388E3C"
                text = String.format(Locale.getDefault(), "• %s: ₹%.2f", typeLabel, trans.amount)
                setTextColor(android.graphics.Color.parseColor(color))
            }
            historyContainer.addView(row)
        }

        // Add a bold "Remaining" line if there are transactions
        if (item.transactions.isNotEmpty()) {
            val remainingRow = TextView(context).apply {
                textSize = 12f
                setPadding(0, 4, 0, 8)
                text = String.format(Locale.getDefault(), "• Net Balance: ₹%.2f", item.balance)
                textStyle = android.graphics.Typeface.BOLD
                setTextColor(if (item.balance > 0) context.getColor(R.color.give_red) else context.getColor(R.color.take_green))
            }
            historyContainer.addView(remainingRow)
        }

        whatsappBtn.setOnClickListener {
            sendWhatsAppReminder(item)
        }

        return view
    }

    private fun sendWhatsAppReminder(item: CustomerBalance) {
        val message = context.getString(R.string.reminder_message, shopName, Math.abs(item.balance))
        val phone = item.phoneNumber?.replace("+", "")?.replace(" ", "") ?: ""
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = if (phone.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$phone&text=" + URLEncoder.encode(message, "UTF-8")
            } else {
                "https://api.whatsapp.com/send?text=" + URLEncoder.encode(message, "UTF-8")
            }
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Send via"))
        }
    }
    
    // Extension to set text size in SP for the dynamically created TextViews
    private var TextView.textSize: Float
        get() = textSize
        set(value) { setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, value) }

    private var TextView.textStyle: Int
        get() = 0
        set(value) { setTypeface(null, value) }
}
