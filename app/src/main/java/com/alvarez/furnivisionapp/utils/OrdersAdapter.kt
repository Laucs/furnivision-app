package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.MainActivity
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.Order
import com.alvarez.furnivisionapp.data.ShopCart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(private val orders: MutableList<Order>)
    :RecyclerView.Adapter<OrdersAdapter.ParentViewHolder>(){

    inner class ParentViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var orderIdView: TextView = itemView.findViewById(R.id.order_id)
        private var orderDateArrivalView: TextView = itemView.findViewById(R.id.order_date_arrival)
        private var orderDateView: TextView = itemView.findViewById(R.id.order_date)
        private var orderShopsRecyclerView: RecyclerView = itemView.findViewById(R.id.order_shops_recyclerView)
        private var orderTotalPriceView: TextView = itemView.findViewById(R.id.order_pay_price)

        @SuppressLint("SetTextI18n")
        fun bind (order: Order) {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            orderIdView.text = order.id
            orderDateArrivalView.text = order.arrival?.let { dateFormatter.format(it) }
            orderDateView.text = order.date?.let { dateFormatter.format(it) }
            orderTotalPriceView.text = "₱ " + MainActivity.PRICE_FORMAT.format(order.totalPrice)

            val orderShopsAdapter = order.itemShops?.let { OrderShopAdapter(it)
            }
            orderShopsRecyclerView.apply {
                adapter = orderShopsAdapter
                layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersAdapter.ParentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_order, parent, false)
        return ParentViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrdersAdapter.ParentViewHolder, position: Int) {
        var order = orders[position]
        Log.d("as", orders[position].itemShops.toString())
        holder.bind(order)
    }

    override fun getItemCount(): Int = orders.size

}