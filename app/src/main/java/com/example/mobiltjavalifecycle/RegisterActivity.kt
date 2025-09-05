package com.example.mobiltjavalifecycle

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.util.Patterns
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val emailInput: EditText = findViewById(R.id.reg_email)
        val passwordInput: EditText = findViewById(R.id.reg_password)
        val usernameInput: EditText = findViewById(R.id.reg_username)
        val personalNumberInput: EditText = findViewById(R.id.reg_personal)
        val phoneInput: EditText = findViewById(R.id.reg_phone)
        val registerBtn: Button = findViewById(R.id.reg_button)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val username = usernameInput.text.toString().trim()
            val personal = personalNumberInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            // Regex validations
            var hasError = false
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Ogiltig email"
                hasError = true
            }
            if (password.length < 6) {
                passwordInput.error = "Minst 6 tecken"
                hasError = true
            }
            val personRegex = Regex("^\\d{8}-\\d{4}$")
            if (!personRegex.matches(personal)) {
                personalNumberInput.error = "Format YYYYMMDD-XXXX"
                hasError = true
            }
            val phoneRegex = Regex("^[+]?\\d{7,15}$")
            if (!phoneRegex.matches(phone)) {
                phoneInput.error = "+46701234567"
                hasError = true
            }
            if (username.isEmpty()) {
                usernameInput.error = "Ange användarnamn"
                hasError = true
            }
            if (hasError) return@setOnClickListener

            //Försök skapa konto
            Firebase.auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = Firebase.auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = mapOf(
                        "uid" to uid,
                        "username" to username,
                        "email" to email,
                        "personalNumber" to personal,
                        "phone" to phone
                    )

                    //Direkt feedback
                    Toast.makeText(this, "Konto skapat!", Toast.LENGTH_SHORT).show()

                    //Spara i Realtime DB
                    Firebase.database.reference.child("users").child(uid).child("credentials").setValue(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Sparad i RealtimeDB!", Toast.LENGTH_SHORT).show()

                            //Spara i Firestore
                            Firebase.firestore.collection("users").document(uid).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registrerad och sparad i Firestore!", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Firestore fel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "RealtimeDB fel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, task.exception?.localizedMessage ?: "Kunde inte registrera", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
