package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.Furniture

class SearchListAdapter (private val context:Context, private var furnitures: List<Furniture>) : BaseAdapter() {
    private lateinit var nameTextView: TextView
    override fun getCount(): Int = furnitures.size

    override fun getItem(position: Int): Any = furnitures[position]

    override fun getItemId(position: Int): Long = position.toLong()
    fun updateList(newList: List<Furniture>) {
        furnitures = newList
        notifyDataSetChanged()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        convertView = LayoutInflater.from(context).inflate(R.layout.search_row_item, parent, false)
        nameTextView = convertView.findViewById(R.id.search_item)
        nameTextView.text = furnitures[position].name
        return convertView
    }

}