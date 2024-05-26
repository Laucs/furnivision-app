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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

    // Email and password methods
    fun createUserWithEmail(context: Context, email: String, password: String, name: String, onResult: (Boolean) -> Unit) {
        val db = Firebase.firestore
        val mAuth = FirebaseAuth.getInstance()
        Log.d("TEST", email)
        Log.d("TEST", name)
        Log.d("TEST", password)

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    user?.let {
                        val userId = it.uid
                        val userMap = hashMapOf(
                            "email" to email,
                            "name" to name,
                            "userId" to userId
                        )
                        db.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Log.d(TAG, "User document successfully written!")
                                onResult(true)
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error writing user document", e)
                                onResult(false)
                            }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    onResult(false)
                }
            }
    }

    fun signInWithEmail(context: Context, email: String, password: String, onResult: (Boolean) -> Unit) {
        val mAuth = FirebaseAuth.getInstance()

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    (context as? LoginRegistrationActivity)?.startMainActivity()
                    Toast.makeText(context, "Sign in Successful! Loading App...", Toast.LENGTH_SHORT).show()
                    onResult(true)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(context, "Sign in failed", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }
    }

    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        SessionManager.clearSession(context)
    }
}