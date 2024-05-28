package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.Shop
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class ShopListAdapter(private var dataset: List<Shop>) :
    RecyclerView.Adapter<ShopListAdapter.ViewHolder>() {


    interface OnItemClickListener {
        fun onItemClick(shopID: String)
    }

    private var listener: OnItemClickListener? = null
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopLogoView: ImageView
        val shopTitleView: TextView
        val shopDescView: TextView
        val shopReviewTextView: TextView
        val shopViewsTextView: TextView
        val shopSloganTextView: TextView
        init {
            shopLogoView = view.findViewById(R.id.shop_logo)
            shopTitleView = view.findViewById(R.id.shop_title)
            shopDescView = view.findViewById(R.id.shop_description)
            shopReviewTextView = view.findViewById(R.id.shop_review)
            shopViewsTextView = view.findViewById(R.id.shop_views)
            shopSloganTextView = view.findViewById(R.id.shop_slogan)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.shop_item, parent, false)
        return ViewHolder(view)
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val storageReference = dataset[position].logo?.let { Firebase.storage.getReferenceFromUrl(it) }
        val ONE_MEGABYTE: Long = 1024 * 1024
        storageReference?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            viewHolder.shopLogoView.setImageBitmap(bitmap)
        }?.addOnFailureListener { exception ->
            // Handle any errors
            Log.e("Firebase", "Error downloading image: $exception")
        }
        viewHolder.shopTitleView.text = dataset[position].name
        viewHolder.shopDescView.text = dataset[position].description
        viewHolder.shopReviewTextView.text = dataset[position].reviews.toString()
        viewHolder.shopViewsTextView.text = "${dataset[position].views} views"
        viewHolder.shopSloganTextView.text = "\"${dataset[position].slogan}\""
        val currentShop = dataset[position].id

        viewHolder.itemView.setOnClickListener {
            if (currentShop != null) {
                listener?.onItemClick(currentShop)
            }
        }
    }
    override fun getItemCount() = dataset.size
    fun filter(query: String): List<Shop> {
        return dataset.filter { shop ->
            shop.name?.contains(query, ignoreCase = true) ?: false
        }
    }

    // Update the adapter with new filtered list
    fun updateList(newList: List<Shop>) {
        dataset = newList.toList()
        notifyDataSetChanged()
    }
}