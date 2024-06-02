package com.alvarez.furnivisionapp.utils

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.ShopCart
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class OrderShopAdapter(private val shopCart: MutableList<ShopCart>) :
    RecyclerView.Adapter<OrderShopAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderShopImgView: ImageView = itemView.findViewById(R.id.shop_logo)
        private val orderShopNameView: TextView = itemView.findViewById(R.id.shop_title_textView)
        private val orderItemsRecyclerView: RecyclerView = itemView.findViewById(R.id.order_recycler_view)

        fun bind(shopCart: ShopCart) {
            val storageReference = Firebase.storage.getReferenceFromUrl(shopCart.shop?.logo.toString())
            val ONE_MEGABYTE: Long = 1024 * 1024
            storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                orderShopImgView.setImageBitmap(bitmap)
            }.addOnFailureListener { exception ->
                // Handle any errors
                Log.e("Firebase", "Error downloading image: $exception")
            }

            orderShopNameView.text = shopCart.shop?.name.toString()

            val orderItemAdapter = shopCart.items?.let { OrderShopItemAdapter(it) }
            orderItemsRecyclerView.apply {
                adapter = orderItemAdapter
                layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            }
            Log.d("List", shopCart.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_order_shop, parent, false)
        return OuterViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: OuterViewHolder, position: Int) {
        val shopCart = shopCart[position]
        viewHolder.bind(shopCart)
    }

    override fun getItemCount(): Int = shopCart.size
}
