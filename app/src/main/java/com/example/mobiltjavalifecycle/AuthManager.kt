package com.example.mobiltjavalifecycle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthManager(private val context: Context) {

    // Lokal lagring av inloggningsstatus
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Firebase Auth
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    // Google One-Tap inloggning
    private var oneTapClient: SignInClient? = null
    private var signInRequest: BeginSignInRequest? = null

    // Klassisk Google Sign-In (fallback)
    private var legacyClient: GoogleSignInClient? = null

    // Kollar om användaren är inloggad
    fun isLoggedIn(): Boolean = auth.currentUser != null || prefs.getBoolean("logged_in", false)

    // Hämtar nuvarande användares email
    fun currentUserEmail(): String? = auth.currentUser?.email ?: prefs.getString("username", null)

    // Logga ut från Firebase och prefs
    fun signOut() {
        auth.signOut()
        prefs.edit().putBoolean("logged_in", false).apply()
    }

    // Logga ut från Google också
    fun signOutGoogle(activity: Activity, onDone: () -> Unit = {}) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.server_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        client.signOut().addOnCompleteListener {
            signOut()
            onDone()
        }
    }

    // Logga in med email och lösenord
    fun signInWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        Firebase.auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                prefs.edit().putBoolean("logged_in", true).putString("username", email).apply()
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.localizedMessage)
            }
        }
    }

    // Google One-Tap inloggning
    fun beginGoogleSignIn(
        activity: Activity,
        onIntent: (IntentSender) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (oneTapClient == null) oneTapClient = Identity.getSignInClient(activity)
            if (signInRequest == null) {
                signInRequest = BeginSignInRequest.Builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                            .setSupported(true)
                            .setServerClientId(context.getString(R.string.server_client_id))
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()
            }
            val client = oneTapClient ?: return onError("Google client saknas")
            val request = signInRequest ?: return onError("Google request saknas")
            client.beginSignIn(request)
                .addOnSuccessListener { result -> onIntent(result.pendingIntent.intentSender) }
                .addOnFailureListener { onError(it.localizedMessage ?: "Google inloggning misslyckades") }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Google Play Services saknas")
        }
    }

    // Klassisk Google-inloggning (fallback om One-Tap inte fungerar)
    fun beginGoogleSignInLegacy(activity: Activity, onStart: (Intent) -> Unit) {
        if (legacyClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.server_client_id))
                .requestEmail()
                .build()
            legacyClient = GoogleSignIn.getClient(activity, gso)
        }
        val intent = legacyClient!!.signInIntent
        onStart(intent)
    }

    // Hanterar svar från klassisk Google-inloggning
    fun handleGoogleLegacyResult(data: Intent?, onComplete: (Boolean, String?) -> Unit) {
        try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            val idToken = account.idToken
            if (idToken.isNullOrEmpty()) {
                onComplete(false, "Inget idToken")
                return
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Firebase.auth.signInWithCredential(credential).addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    prefs.edit().putBoolean("logged_in", true).apply()
                    onComplete(true, null)
                } else {
                    onComplete(false, t.exception?.localizedMessage)
                }
            }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage)
        }
    }

    // Hanterar svar från Google One-Tap
    fun handleGoogleActivityResult(
        data: Intent?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            if (idToken == null) {
                onComplete(false, "Inget idToken")
                return
            }
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            Firebase.auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.edit().putBoolean("logged_in", true).apply()
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage)
                    }
                }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage)
        }
    }
}
