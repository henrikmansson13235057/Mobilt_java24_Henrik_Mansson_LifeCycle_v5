package com.example.mobiltjavalifecycle

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Gör att layout kan gå under status- och navigeringsfält
        setContentView(R.layout.activity_profile)

        // Hämta aktuell användares UID
        val uid = Firebase.auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("prefs_$uid", Context.MODE_PRIVATE)

        // Hämta UI-element
        val ageInput: EditText = findViewById(R.id.input_age)
        val hasLicenseCheckbox: CheckBox = findViewById(R.id.checkbox_license)
        val genderGroup: RadioGroup = findViewById(R.id.group_gender)
        val emailInput: EditText = findViewById(R.id.input_email)
        val bioInput: EditText = findViewById(R.id.input_bio)
        val submitButton: Button = findViewById(R.id.button_submit)
        val resultText: TextView = findViewById(R.id.text_result)
        val backButton: Button = findViewById(R.id.button_back_to_main)

        // Läs lokalt sparade värden och fyll UI
        ageInput.setText(prefs.getString("age", ""))
        hasLicenseCheckbox.isChecked = prefs.getBoolean("has_license", false)
        when (prefs.getString("gender", "")) {
            "male" -> genderGroup.check(R.id.radio_male)
            "female" -> genderGroup.check(R.id.radio_female)
            "other" -> genderGroup.check(R.id.radio_other)
        }
        emailInput.setText(prefs.getString("email", ""))
        bioInput.setText(prefs.getString("bio", ""))

        // Läs Firebase Realtime DB-data för aktuell användare
        Firebase.database.reference.child("users").child(uid).child("profile")
            .get().addOnSuccessListener { snapshot ->
                snapshot.value?.let { data ->
                    val map = data as Map<*, *>
                    ageInput.setText(map["age"] as? String ?: "")
                    hasLicenseCheckbox.isChecked = map["hasLicense"] as? Boolean ?: false
                    when (map["gender"] as? String ?: "") {
                        "male" -> genderGroup.check(R.id.radio_male)
                        "female" -> genderGroup.check(R.id.radio_female)
                        "other" -> genderGroup.check(R.id.radio_other)
                    }
                    emailInput.setText(map["email"] as? String ?: "")
                    bioInput.setText(map["bio"] as? String ?: "")
                }
            }

        // Spara-knapp
        submitButton.setOnClickListener {
            val age = ageInput.text.toString().trim()
            val hasLicense = hasLicenseCheckbox.isChecked
            val selectedGenderId = genderGroup.checkedRadioButtonId
            val gender = when (selectedGenderId) {
                R.id.radio_male -> "male"
                R.id.radio_female -> "female"
                R.id.radio_other -> "other"
                else -> ""
            }
            val email = emailInput.text.toString().trim()
            val bio = bioInput.text.toString().trim()

            // Validering
            var hasError = false
            if (age.isEmpty()) {
                ageInput.error = "Ange ålder"
                hasError = true
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Ogiltig email"
                hasError = true
            }
            if (gender.isEmpty()) {
                Toast.makeText(this, "Välj kön", Toast.LENGTH_SHORT).show()
                hasError = true
            }
            if (hasError) return@setOnClickListener

            // Spara lokalt (SharedPreferences per användare)
            prefs.edit()
                .putString("age", age)
                .putBoolean("has_license", hasLicense)
                .putString("gender", gender)
                .putString("email", email)
                .putString("bio", bio)
                .apply()

            // Spara till Firebase Realtime DB
            val profileMap = mapOf(
                "age" to age,
                "hasLicense" to hasLicense,
                "gender" to gender,
                "email" to email,
                "bio" to bio
            )

            Firebase.database.reference.child("users").child(uid).child("profile").setValue(profileMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sparad till Realtime DB", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "RealtimeDB fel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }

            // Spara till Firestore
            Firebase.firestore.collection("users").document(uid).collection("profile").document("data").set(profileMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sparad till Firestore", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Firestore fel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }

            // Visa direkt feedback på skärmen
            resultText.text = "Age: $age\nLicense: $hasLicense\nGender: $gender\nEmail: $email\nBio: $bio"
        }

        // Back-knapp i UI
        backButton.setOnClickListener {
            finish() // Stänger ProfileActivity och går tillbaka till MainActivity
        }

        // Systemets bakåtknapp
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // Stänger ProfileActivity
            }
        })
    }

    override fun onPause() {
        super.onPause()
        // Spara alltid lokalt när aktiviteten pausas
        val uid = Firebase.auth.currentUser?.uid ?: return
        val prefs = getSharedPreferences("prefs_$uid", Context.MODE_PRIVATE)

        val ageInput: EditText = findViewById(R.id.input_age)
        val hasLicenseCheckbox: CheckBox = findViewById(R.id.checkbox_license)
        val genderGroup: RadioGroup = findViewById(R.id.group_gender)
        val emailInput: EditText = findViewById(R.id.input_email)
        val bioInput: EditText = findViewById(R.id.input_bio)

        val gender = when (genderGroup.checkedRadioButtonId) {
            R.id.radio_male -> "male"
            R.id.radio_female -> "female"
            R.id.radio_other -> "other"
            else -> ""
        }

        prefs.edit()
            .putString("age", ageInput.text.toString().trim())
            .putBoolean("has_license", hasLicenseCheckbox.isChecked)
            .putString("gender", gender)
            .putString("email", emailInput.text.toString().trim())
            .putString("bio", bioInput.text.toString().trim())
            .apply()
    }
}
