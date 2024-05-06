package com.alvarez.furnivisionapp.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import com.alvarez.furnivisionapp.R

class CustomDialog(private val view: ViewParent, private val context: Context) : Dialog(context), View.OnClickListener {

    private lateinit var closeButton: ImageButton
    private val layout = R.layout.activity_furniture_selection
    private lateinit var furnitureImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        if (view is ViewGroup) {
            val parentView = view
            furnitureImageView = parentView.findViewWithTag("furnitureImage") as ImageView

            // Check if furnitureImageView is not null before setting its drawable
            furnitureImageView.drawable?.let {
                // Set the imageDrawable of imageContainer to the drawable of furnitureImageView
                findViewById<ImageView>(R.id.furnitureImage).setImageDrawable(it)
            }
        }

        setContentView(layout)

        closeButton = findViewById(R.id.backBtn)
        closeButton.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        dismiss()
    }
}