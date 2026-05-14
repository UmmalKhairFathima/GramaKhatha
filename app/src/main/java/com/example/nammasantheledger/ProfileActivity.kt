package com.example.nammasantheledger

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private var profileImageUri: Uri? = null
    private lateinit var profileImageView: ImageView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            profileImageView.setImageURI(it)
            // Grant persistable permission if needed, but for now just showing it
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.title = "Owner Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs        = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val shopName     = findViewById<EditText>(R.id.shopName)
        val ownerName    = findViewById<EditText>(R.id.ownerName)
        val ownerPhone   = findViewById<EditText>(R.id.ownerPhone)
        val ownerAddress = findViewById<EditText>(R.id.ownerAddress)
        val saveBtn      = findViewById<Button>(R.id.saveProfileBtn)
        profileImageView = findViewById(R.id.profileImage)
        val imageContainer = findViewById<android.view.View>(R.id.profileImageContainer)

        // Load saved profile
        shopName.setText(prefs.getString("shopName", ""))
        ownerName.setText(prefs.getString("ownerName", ""))
        ownerPhone.setText(prefs.getString("ownerPhone", ""))
        ownerAddress.setText(prefs.getString("ownerAddress", ""))
        
        val savedUri = prefs.getString("profilePhoto", null)
        if (savedUri != null) {
            profileImageUri = Uri.parse(savedUri)
            try {
                profileImageView.setImageURI(profileImageUri)
            } catch (e: Exception) {
                // Fallback if URI is no longer valid
            }
        }

        imageContainer.setOnClickListener {
            pickImage.launch("image/*")
        }

        saveBtn.setOnClickListener {
            if (shopName.text.isBlank() || ownerName.text.isBlank()) {
                Toast.makeText(this, "Shop name and owner name are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("shopName", shopName.text.toString().trim())
                .putString("ownerName", ownerName.text.toString().trim())
                .putString("ownerPhone", ownerPhone.text.toString().trim())
                .putString("ownerAddress", ownerAddress.text.toString().trim())
                .putString("profilePhoto", profileImageUri?.toString())
                .apply()

            Toast.makeText(this, "✅ Profile saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}