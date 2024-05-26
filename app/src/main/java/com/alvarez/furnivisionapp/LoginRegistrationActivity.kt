package com.alvarez.furnivisionapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alvarez.furnivisionapp.data.AuthUtility
import com.alvarez.furnivisionapp.data.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginRegistrationActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var loginPage: CardView

    companion object {
        private const val RC_SIGN_IN = 20
        private const val PERMISSION_REQUEST_CODE = 101
        private const val TAG = "LoginRegistrationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_registration)
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initializeComponents()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeComponents()
        } else {
            finish()
        }
    }

    private fun initializeComponents() {
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()
        setupGoogleSignIn()
        setupLoginViews()
        setupRegistrationViews()
        checkIfUserIsLoggedIn()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignInButton: ImageView = findViewById(R.id.google_signin_button)
        googleSignInButton.setOnClickListener {
            AuthUtility.signInWithGoogle(this, googleSignInClient, RC_SIGN_IN)
        }

        val googleLoginButton: ImageView = findViewById(R.id.google_login_button)
        googleLoginButton.setOnClickListener {
            AuthUtility.signInWithGoogle(this, googleSignInClient, RC_SIGN_IN)
        }
    }

    private fun setupLoginViews() {
        loginPage = findViewById(R.id.login_panel)

        val emailEditText: EditText = findViewById(R.id.login_emailEditText)
        val passwordEditText: EditText = findViewById(R.id.login_passwordEditText)
        val loginBtn: Button = findViewById(R.id.login_loginButton)

        loginBtn.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            AuthUtility.signInWithEmail(this, email, password) { success ->
                if (success) {
                    SessionManager.saveUserEmail(this, email)
                    startMainActivity(Bundle().apply { putString("email", email) })
                    finishAffinity()
                } else {
                    emailEditText.error = "Invalid Email or Password!"
                    showToast("Sign in failed. Please check your credentials.")
                }
            }
        }
    }


    private fun setupRegistrationViews() {
        val registerPage: CardView = findViewById(R.id.reg_panel)
        val bgImage: ImageView = findViewById(R.id.bg_image)
        val nameInput: EditText = findViewById(R.id.reg_nameEditText)
        val emailInput: EditText = findViewById(R.id.reg_emailEditText)
        val passwordInput: EditText = findViewById(R.id.reg_passwordEditText)
        val confirmPasswordInput: EditText = findViewById(R.id.reg_confirmPassEditText)

        findViewById<Button>(R.id.register_Btn).setOnClickListener {
            toggleViews(loginPage, registerPage)
            adjustBackgroundImageMargin(bgImage, -100f)
        }

        findViewById<Button>(R.id.reg_signupButton).setOnClickListener {
            val emptyFields = listOf(
                "Name" to nameInput.text.toString(),
                "Email" to emailInput.text.toString(),
                "Password" to passwordInput.text.toString(),
                "Confirm Password" to confirmPasswordInput.text.toString()
            ).filter { it.second.isEmpty() }.map { it.first }

            when {
                emptyFields.isNotEmpty() -> showToast("The following fields are empty: ${emptyFields.joinToString(", ")}")
                passwordInput.text.toString() != confirmPasswordInput.text.toString() -> showToast("Passwords don't match")
                else -> {
                    AuthUtility.createUserWithEmail(
                        this,
                        emailInput.text.toString(),
                        passwordInput.text.toString(),
                        nameInput.text.toString()
                    ) { success ->
                        if (success) {
                            showToast("Registration successful!")
                            startMainActivity()
                        } else {
                            showToast("Registration failed. Please try again.")
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.reg_loginButton).setOnClickListener {
            toggleViews(registerPage, loginPage)
            adjustBackgroundImageMargin(bgImage, 0f)
        }
    }

    private fun adjustBackgroundImageMargin(bgImage: ImageView, marginTop: Float) {
        val marginTopInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, marginTop, resources.displayMetrics).toInt()
        val layoutParams = bgImage.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = marginTopInPx
        bgImage.layoutParams = layoutParams
        bgImage.requestLayout()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data).let { task ->
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    AuthUtility.firebaseAuthWithGoogle(this, account.idToken!!, mAuth)
                } catch (e: ApiException) {
                    showToast("Google Sign-In failed")
                    Log.w(TAG, "Google sign in failed", e)
                }
            }
        }
    }

    private fun checkIfUserIsLoggedIn() {
        val loadingCard: CardView = findViewById(R.id.loading_card)
        val loadingImage: ImageView = findViewById(R.id.loading_img)

        val rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            repeatCount = Animation.INFINITE
            duration = 2000
            interpolator = LinearInterpolator()
        }

        loadingCard.visibility = View.VISIBLE
        loadingImage.startAnimation(rotateAnimation)

        Handler().postDelayed({
            val currentUser = mAuth.currentUser
            val userEmail = SessionManager.getUserEmail(this)
            loadingImage.clearAnimation()
            loadingCard.visibility = View.GONE

            if (currentUser != null || userEmail != null) {
                startMainActivity()
            } else {
                loginPage.visibility = View.VISIBLE
            }
        }, 2500)
    }

    fun startMainActivity(extras: Bundle? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            extras?.let { putExtra("extraBundle", it) }
        }
        startActivity(intent)
        finish()
    }

    private fun toggleViews(viewToHide: View, viewToShow: View) {
        viewToHide.visibility = View.GONE
        viewToShow.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}