package com.alvarez.furnivisionapp.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.ShopCart

class CartListAdapter(private var shops: MutableList<ShopCart>) :
    RecyclerView.Adapter<CartListAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopCartTitleView: TextView
        val shopItemRecyclerView: RecyclerView
        init {
            shopCartTitleView = view.findViewById(R.id.shop_title_textView)
            shopItemRecyclerView = view.findViewById(R.id.cart_items_view)
        }

        fun bind (shop: ShopCart, position: Int) {
            val cartItemsAdapter = shop.items?.let {
                CartItemListAdapter(it.toMutableList()) { newQuantity, itemPosition ->
                    if (newQuantity == -1 && it.isEmpty()) {
                        shops.drop(position)
                        notifyItemRemoved(position)
                    } else {
                        notifyItemChanged(position)
                    }
                }
            }
            shopItemRecyclerView.apply{
                layoutManager = LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false)
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