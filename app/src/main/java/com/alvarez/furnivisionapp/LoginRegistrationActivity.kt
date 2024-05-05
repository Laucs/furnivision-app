package com.alvarez.furnivisionapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LoginRegistrationActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_registration)
        getPermissions()
    }

    private fun proceedToOnCreate() {
        val loginPage: CardView = findViewById(R.id.login_panel)
        val registerPage: CardView = findViewById(R.id.reg_panel)
        val loadingCard: CardView = findViewById(R.id.loading_card)
        val loadingImage: ImageView = findViewById(R.id.loading_img)
        val bgImage: ImageView = findViewById(R.id.bg_image)

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

        val isLoggedIn = checkIfUserIsLoggedIn()
        if (isLoggedIn) {
            loadingImage.clearAnimation()
            loadingCard.visibility = View.GONE
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Handler().postDelayed({
                loadingImage.clearAnimation()
                loadingCard.visibility = View.GONE
                loginPage.visibility = View.VISIBLE
            }, 2500)
        }

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

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
                sharedPreferences.edit().putString("email", email).apply()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("email", email)
                })
                finishAffinity()
            }
        }

        val registerBtn: Button = findViewById(R.id.register_Btn)
        registerBtn.setOnClickListener {
            loginPage.visibility = View.GONE
            val marginTopInPx = -TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 100f, resources.displayMetrics).toInt()
            val layoutParams = bgImage.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = marginTopInPx
            bgImage.layoutParams = layoutParams
            bgImage.requestLayout()
            registerPage.visibility = View.VISIBLE
        }

        val regnameEditText: EditText = findViewById(R.id.reg_nameEditText)
        val regemailEditText: EditText = findViewById(R.id.reg_emailEditText)
        val regpasswordEditText: EditText = findViewById(R.id.reg_passwordEditText)
        val regsignupButton: Button = findViewById(R.id.reg_signupButton)
        regsignupButton.setOnClickListener {
            val name: String = regnameEditText.text.toString()
            val email: String = regemailEditText.text.toString()
            val password: String = regpasswordEditText.text.toString()

            registerPage.visibility = View.GONE
            val marginTopInPx = -TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 0f, resources.displayMetrics).toInt()
            val layoutParams = bgImage.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = marginTopInPx
            bgImage.layoutParams = layoutParams
            bgImage.requestLayout()
            loginPage.visibility = View.VISIBLE
        }
        val regloginBtn: Button = findViewById(R.id.reg_loginButton)
        regloginBtn.setOnClickListener {
            registerPage.visibility = View.GONE
            val marginTopInPx = -TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 0f, resources.displayMetrics).toInt()
            val layoutParams = bgImage.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = marginTopInPx
            bgImage.layoutParams = layoutParams
            bgImage.requestLayout()
            loginPage.visibility = View.VISIBLE
        }
    }

    private fun checkIfUserIsLoggedIn(): Boolean {
        return false
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                proceedToOnCreate()
            } else {
                finish()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


}