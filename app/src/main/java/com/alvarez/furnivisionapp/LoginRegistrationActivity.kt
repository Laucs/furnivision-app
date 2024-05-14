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
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginRegistrationActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private val RC_SIGN_IN: Int = 20
    private val PERMISSION_REQUEST_CODE = 101
    private lateinit var loginPage: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_registration)
        getPermissions()
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
            val email: String = emailEditText.text.toString()
            val password: String = passwordEditText.text.toString()

            if (email != "devtrio@gmail.com") {
                emailEditText.error = "Invalid Email!"
                Toast.makeText(this, "Please Enter a valid Email", Toast.LENGTH_SHORT).show()
            } else if (password != "admin") {
                passwordEditText.error = "Invalid Password!"
                Toast.makeText(this, "Please Enter a valid Password", Toast.LENGTH_SHORT).show()
            } else {
                SessionManager.saveUserEmail(this, email)
                val bundle = Bundle()
                bundle.putString("email", email)
                startMainActivity(bundle)
                finishAffinity()
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

        val registerBtn: Button = findViewById(R.id.register_Btn)
        registerBtn.setOnClickListener {
            loginPage.visibility = View.GONE
            adjustBackgroundImageMargin(bgImage, -100f)
            registerPage.visibility = View.VISIBLE
        }

        val regsignupButton: Button = findViewById(R.id.reg_signupButton)
        regsignupButton.setOnClickListener {
            val name: String = nameInput.text.toString()
            val email: String = emailInput.text.toString()
            val password: String = passwordInput.text.toString()
            val confirmPassword: String = confirmPasswordInput.text.toString()

            val emptyFields: MutableList<String> = mutableListOf()

            if (name.isEmpty()) {
                emptyFields.add("Name")
            }
            if (email.isEmpty()) {
                emptyFields.add("Email")
            }
            if (password.isEmpty()) {
                emptyFields.add("Password")
            }
            if (confirmPassword.isEmpty()) {
                emptyFields.add("Confirm Password")
            }

            if (emptyFields.isNotEmpty()) {
                val emptyFieldsText = emptyFields.joinToString(", ")
                Toast.makeText(this, "The following fields are empty: $emptyFieldsText", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            } else {

            }
        }

        val regloginBtn: Button = findViewById(R.id.reg_loginButton)
        regloginBtn.setOnClickListener {
            registerPage.visibility = View.GONE
            adjustBackgroundImageMargin(bgImage, 0f)
            loginPage.visibility = View.VISIBLE
        }
    }

    private fun adjustBackgroundImageMargin(bgImage: ImageView, marginTop: Float) {
        val marginTopInPx = -TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, marginTop, resources.displayMetrics).toInt()
        val layoutParams = bgImage.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = marginTopInPx
        bgImage.layoutParams = layoutParams
        bgImage.requestLayout()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                AuthUtility.firebaseAuthWithGoogle(this, account.idToken!!, mAuth)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun checkIfUserIsLoggedIn() {
        val loadingCard: CardView = findViewById(R.id.loading_card)
        val loadingImage: ImageView = findViewById(R.id.loading_img)

        val rotateAnimation = RotateAnimation(
            0f,
            360f,
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
            if (currentUser != null || userEmail != null) {
                loadingImage.clearAnimation()
                loadingCard.visibility = View.GONE
                startMainActivity()
                finish()
            } else{
                loadingImage.clearAnimation()
                loadingCard.visibility = View.GONE
                loginPage.visibility = View.VISIBLE
            }
        }, 2500)
    }

    private fun getPermissions() {
        val permissionList = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(android.Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            proceedToOnCreate()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            proceedToOnCreate()
        } else {
            finish()
        }
    }

    private fun proceedToOnCreate() {
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        setupGoogleSignIn()
        setupLoginViews()
        setupRegistrationViews()
        checkIfUserIsLoggedIn()
    }

    fun startMainActivity(extras: Bundle?) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("extraBundle", extras)
        })
        finish()
    }


    fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "LoginRegistrationActivity"
    }
}