package com.alvarez.furnivisionapp.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.DeliveryAddress

class DeliveryAddressAdapter(private val deliveryAddresses: List<DeliveryAddress>) :
    RecyclerView.Adapter<DeliveryAddressAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameDeliveryTV: TextView = itemView.findViewById(R.id.nameDeliveryTV)
        val phoneDeliveryTV: TextView = itemView.findViewById(R.id.phoneDeliveryTV)
        val regionTV: TextView = itemView.findViewById(R.id.regionTV)
        val barangayTV: TextView = itemView.findViewById(R.id.barangayTV)
        val streetNameTV: TextView = itemView.findViewById(R.id.streetNameTV)
        val postalCodeTV: TextView = itemView.findViewById(R.id.postalCodeTV)
        val checkbox1: CheckBox = itemView.findViewById(R.id.checkbox1)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val deliveryAddress = deliveryAddresses[position]
                    onItemClickListener?.onItemClick(deliveryAddress)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(deliveryAddress: DeliveryAddress)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.delivery_address_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deliveryAddress = deliveryAddresses[position]
        holder.nameDeliveryTV.text = deliveryAddress.name
        holder.phoneDeliveryTV.text = deliveryAddress.phone
        holder.regionTV.text = deliveryAddress.region
        holder.barangayTV.text = deliveryAddress.barangay
        holder.streetNameTV.text = deliveryAddress.streetName
        holder.postalCodeTV.text = deliveryAddress.postalCode
        holder.checkbox1.isChecked = deliveryAddress.isChecked

        // Add any necessary checkbox logic here
    }

    override fun getItemCount(): Int {
        return deliveryAddresses.size
    }
}