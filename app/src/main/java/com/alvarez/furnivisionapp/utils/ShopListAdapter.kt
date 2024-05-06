package com.alvarez.furnivisionapp.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.Shop

class ShopListAdapter(private val dataset: Array<Shop>) :
    RecyclerView.Adapter<ShopListAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(shop: Shop)
    }

    private var listener: OnItemClickListener? = null
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopLogoView: ImageView
        val shopTitleView: TextView
        val shopDescView: TextView
        val shopReviewTextView: TextView
        val shopViewsTextView: TextView
        init {
            shopLogoView = view.findViewById(R.id.shop_logo)
            shopTitleView = view.findViewById(R.id.shop_title)
            shopDescView = view.findViewById(R.id.shop_description)
            shopReviewTextView = view.findViewById(R.id.shop_review)
            shopViewsTextView = view.findViewById(R.id.shop_views)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.shop_item, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.shopLogoView.setImageResource(dataset[position].logo)
        viewHolder.shopTitleView.text = dataset[position].name
        viewHolder.shopDescView.text = dataset[position].description
        viewHolder.shopReviewTextView.text = dataset[position].reviews.toString()
        viewHolder.shopViewsTextView.text = dataset[position].views.toString()

        val currentShop = dataset[position]

        viewHolder.itemView.setOnClickListener {
            listener?.onItemClick(currentShop)
        }
    }
    override fun getItemCount() = dataset.size

}