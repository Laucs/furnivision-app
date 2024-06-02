package com.alvarez.furnivisionapp.utils

import android.graphics.BitmapFactory
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

class CartListAdapter(private val shops: MutableList<ShopCart>) :
    RecyclerView.Adapter<CartListAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shopCartTitleView: TextView = itemView.findViewById(R.id.shop_title_textView)
        private val shopItemRecyclerView: RecyclerView = itemView.findViewById(R.id.cart_items_view)
        private val shopLogoImageView: ImageView = itemView.findViewById(R.id.shop_logo)

        fun bind(shop: ShopCart, position: Int) {
            shopCartTitleView.text = shop.shop?.name ?: ""

            if (shop != null) {
                shop.shop?.logo?.let { imageUrl ->
                    val storageReference = Firebase.storage.getReferenceFromUrl(imageUrl)
                    storageReference.getBytes(CartItemListAdapter.ONE_MEGABYTE).addOnSuccessListener { bytes ->
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        shopLogoImageView.setImageBitmap(bitmap)
                    }
                }
            }
            val cartItemsAdapter = CartItemListAdapter(shop) { newQuantity, itemPosition ->
                if (newQuantity == 0) {
                    // Remove the item from the list
                    shop.items?.removeAt(itemPosition)
                    // If the shop's items list is empty, remove the shop
                    if (shop.items.isNullOrEmpty()) {
                        shops.removeAt(position)
                        notifyItemRemoved(position)
                    } else {
                        // Notify the item change
                        notifyItemChanged(position)
                    }
                } else {
                    // Notify the item change
                    notifyItemChanged(position)
                }
            }

            shopItemRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = cartItemsAdapter
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_item, parent, false)
        return OuterViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: OuterViewHolder, position: Int) {
        val shop = shops[position]
        viewHolder.bind(shop, position)
    }

    override fun getItemCount() = shops.size
}