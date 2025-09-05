package com.example.mobiltjavalifecycle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest

class MainActivity : AppCompatActivity() {

    // Hanterar all inloggningslogik (Firebase + Google)
    private lateinit var authManager: AuthManager

    // Launcher för Google One-Tap inloggning
    private val oneTapLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        authManager.handleGoogleActivityResult(result.data) { ok, err ->
            if (ok) {
                // Vid lyckad inloggning → gå till profilsidan
                startActivity(Intent(this, ProfileActivity::class.java))
            } else if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher för klassisk Google Sign-In (fallback om One-Tap inte fungerar)
    private val legacyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authManager.handleGoogleLegacyResult(result.data) { ok, err ->
            if (ok) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Gör så appen kan rita under statusfält/navigation
        setContentView(R.layout.activity_main)

        // Anpassa padding så att UI inte hamnar under systemets statusfält/navigation
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        // Om användaren redan är inloggad → hoppa direkt till ProfileActivity
        if (authManager.isLoggedIn()) {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Hämta UI-element
        val usernameInput: EditText = findViewById(R.id.input_username)
        val passwordInput: EditText = findViewById(R.id.input_password)
        val loginButton: Button = findViewById(R.id.button_login)
        val registerButton: Button = findViewById(R.id.button_register)
        val googleButton: Button = findViewById(R.id.button_google)
        val backButton: ImageButton = findViewById(R.id.button_back)

        // Inloggning med email/lösenord
        loginButton.setOnClickListener {
            val email = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fyll i email och lösenord", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Använd AuthManager för inloggning
            authManager.signInWithEmail(email, password) { ok, err ->
                if (ok) {
                    startActivity(Intent(this, ProfileActivity::class.java))
                } else {
                    Toast.makeText(this, err ?: "Fel inloggning", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Gå till registreringssidan
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Google-inloggning (försök först One-Tap, annars fallback)
        googleButton.setOnClickListener {
            authManager.beginGoogleSignIn(
                activity = this,
                onIntent = { sender ->
                    oneTapLauncher.launch(IntentSenderRequest.Builder(sender).build())
                },
                onError = {
                    // Om One-Tap misslyckas → använd klassiska Google Sign-In
                    authManager.beginGoogleSignInLegacy(this) { intent ->
                        legacyLauncher.launch(intent)
                    }
                }
            )
        }

        // Tillbaka-knappen → stänger MainActivity
        backButton.setOnClickListener {
            finish()
        }
    }
}
