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
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase

class OrderShopAdapter(private val shopCarts: MutableList<ShopCart>) : RecyclerView.Adapter<OrderShopAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderShopImgView: ImageView = itemView.findViewById(R.id.shop_logo)
        private val orderShopNameView: TextView = itemView.findViewById(R.id.shop_title_textView)
        private val orderItemsRecyclerView: RecyclerView = itemView.findViewById(R.id.order_items_view)

        fun bind(shopCart: ShopCart) {
            // Load shop logo from Firebase Storage
            shopCart.shop?.logo?.let { logoUrl ->
                val storageReference = Firebase.storage.getReferenceFromUrl(logoUrl)
                val ONE_MEGABYTE: Long = 1024 * 1024
                storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    orderShopImgView.setImageBitmap(bitmap)
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error downloading image: $exception")
                }
            }

            // Set shop name
            orderShopNameView.text = shopCart.shop?.name.orEmpty()

            // Set up the RecyclerView for the items in the shop cart
            val orderItemAdapter = shopCart.items?.let { OrderShopItemAdapter(it) }
            orderItemsRecyclerView.apply {
                adapter = orderItemAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            }

            // Logging the shopCart for debugging purposes
            Log.d("OrderShopAdapter", "Binding shopCart: $shopCart")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_order_shop, parent, false)
        return OuterViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: OuterViewHolder, position: Int) {
        val shopCart = shopCarts[position]
        viewHolder.bind(shopCart)
    }

    override fun getItemCount(): Int = shopCarts.size
}
