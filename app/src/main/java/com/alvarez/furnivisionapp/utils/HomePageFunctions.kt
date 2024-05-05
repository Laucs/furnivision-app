package com.alvarez.furnivisionapp.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.utils.CustomDialog

class HomePageFunctions(private val layout: ViewGroup, private val viewType: Class<*>, private val context: Context) {

    lateinit var CustomDialog: CustomDialog

    init {
        val content: ViewGroup = layout.findViewById(R.id.content)
        val views = getElementsByType(content)
        giveOnClickListener(views)
    }

    private fun giveOnClickListener(views: List<View>) {
        for (view in views) {
            val parentView = view.parent
            view.setOnClickListener {
                Log.d("CLICKED", "Clicked Button")
//                CustomDialog = CustomDialog(parentView, context)
//                CustomDialog.show()
            }
        }
    }

    private fun getElementsByType(layout: ViewGroup): List<View> {
        var views = mutableListOf<View>()
        findViewsByType(layout, views)
        return views
    }

    private fun findViewsByType(view: View, views: MutableList<View>) {
        if (view.javaClass.simpleName == viewType.simpleName && view.tag == "openButton") {
            views.add(view)
        } else if (view is ViewGroup) {
            val childCount = view.childCount
            for (i in 0 until childCount) {
                val childView = view.getChildAt(i)
                findViewsByType(childView, views)
            }
        }
    }
}
