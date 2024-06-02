package com.alvarez.furnivisionapp.utils

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.MainActivity
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.CartItem
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class OrderShopItemAdapter (private val items: MutableList<CartItem>)
    : RecyclerView.Adapter<OrderShopItemAdapter.InnerViewHolder>(){


    inner class InnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderFurnitureImgView: ImageView = itemView.findViewById(R.id.order_furniture_img)
        private val orderFurnitureQuantityView: TextView = itemView.findViewById(R.id.order_furniture_quantity)
        private val orderFurnitureTotalPrice: TextView = itemView.findViewById(R.id.order_total_price)
        private val orderFurniturePrice: TextView = itemView.findViewById(R.id.order_furniture_price)
        private val orderFurnitureDesc: TextView = itemView.findViewById(R.id.order_furniture_desc)
        private val orderQuantityTitle: TextView = itemView.findViewById(R.id.order_furniture_quantity_title)
        fun bind(item: CartItem) {
            val storageReference = item.furniture?.img?.let { Firebase.storage.getReferenceFromUrl(it) }
            val ONE_MEGABYTE: Long = 1024 * 1024
            storageReference?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                orderFurnitureImgView.setImageBitmap(bitmap)
            }?.addOnFailureListener { exception: Exception ->
                // Handle any errors
                Log.e("Firebase", "Error downloading image: $exception")
            }

            orderFurnitureQuantityView.text = item.quantity.toString()
            if (item.quantity!! > 1) {
                orderQuantityTitle.text = "pcs"
            }
            orderFurnitureTotalPrice.text = MainActivity.PRICE_FORMAT.format(
                item.furniture?.price?.times(
                item.quantity!!
            ) ?: 0.00)

            orderFurniturePrice.text = "₱ " + MainActivity.PRICE_FORMAT.format(item.furniture?.price ?: 0.00)
            orderFurnitureDesc.text = item.furniture?.description ?: ""
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderShopItemAdapter.InnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_order, parent, false)
        return InnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderShopItemAdapter.InnerViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

}