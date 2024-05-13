package com.alvarez.furnivisionapp.data

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.alvarez.furnivisionapp.LoginRegistrationActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object AuthUtility {

    private const val TAG = "AuthUtility"

    fun signInWithGoogle(context: Context, googleSignInClient: GoogleSignInClient, requestCode: Int) {
        val signInIntent = googleSignInClient.signInIntent
        (context as? LoginRegistrationActivity)?.startActivityForResult(signInIntent, requestCode)
    }

    fun firebaseAuthWithGoogle(context: Context, idToken: String, mAuth: FirebaseAuth) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                     (context as? LoginRegistrationActivity)?.startMainActivity()
                    Toast.makeText(context, "Sign in Successful! Loading App...", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(context, "Sign in failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        SessionManager.clearSession(context)
    }
}