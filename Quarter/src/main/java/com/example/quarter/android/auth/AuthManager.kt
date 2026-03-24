package com.example.quarter.android.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object AuthManager {

    private val auth: FirebaseAuth?
        get() {
            val ctx = appContext ?: return null
            return try {
                if (FirebaseApp.getApps(ctx).isNotEmpty()) {
                    FirebaseAuth.getInstance()
                } else null
            } catch (_: Exception) { null }
        }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val isAvailable: Boolean
        get() = auth != null

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    val isLoggedIn: Boolean
        get() = auth?.currentUser != null

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth?.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth?.removeAuthStateListener(listener)
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser? {
        val result = auth?.signInWithEmailAndPassword(email, password)?.await()
        return result?.user
    }

    suspend fun registerWithEmail(email: String, password: String): FirebaseUser? {
        val result = auth?.createUserWithEmailAndPassword(email, password)?.await()
        return result?.user
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth?.signInWithCredential(credential)?.await()
        return result?.user
    }

    fun getGoogleSignInClient(context: Context, webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signOut(context: Context, webClientId: String) {
        auth?.signOut()
        getGoogleSignInClient(context, webClientId).signOut()
    }
}
