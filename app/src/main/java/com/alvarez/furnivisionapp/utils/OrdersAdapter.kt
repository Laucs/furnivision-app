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
import java.text.SimpleDateFormat
import java.util.Locale

class OrdersAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrdersAdapter.ParentViewHolder>() {

    inner class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderIdView: TextView = itemView.findViewById(R.id.order_id)
        private val orderDateArrivalView: TextView = itemView.findViewById(R.id.order_date_arrival)
        private val orderDateView: TextView = itemView.findViewById(R.id.order_date)
        private val orderShopsRecyclerView: RecyclerView = itemView.findViewById(R.id.order_shops_recyclerView)
        private val orderTotalPriceView: TextView = itemView.findViewById(R.id.order_pay_price)

        @SuppressLint("SetTextI18n")
        fun bind(order: Order) {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            orderIdView.text = order.id
            orderDateArrivalView.text = order.arrival?.let { dateFormatter.format(it) }
            orderDateView.text = order.date?.let { dateFormatter.format(it) }
            orderTotalPriceView.text = "₱ " + String.format("%,.2f", order.totalPrice)

            // Set up the RecyclerView for the shop items in the order
            orderShopsRecyclerView.apply {
                adapter = order.itemShops?.let { OrderShopAdapter(it) }
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_order, parent, false)
        return ParentViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParentViewHolder, position: Int) {
        val order = orders[position]
        Log.d("OrdersAdapter", "Binding order at position $position: ${order.itemShops}")
        holder.bind(order)
    }

    override fun getItemCount(): Int = orders.size
}
