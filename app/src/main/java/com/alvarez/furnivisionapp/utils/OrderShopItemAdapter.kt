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
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text

class OrderShopItemAdapter(private val items: MutableList<CartItem>) : RecyclerView.Adapter<OrderShopItemAdapter.InnerViewHolder>() {

    inner class InnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderFurnitureImgView: ImageView = itemView.findViewById(R.id.order_furniture_img)
        private val orderFurnitureQuantityView: TextView = itemView.findViewById(R.id.order_furniture_quantity)
        private val orderFurnitureTotalPrice: TextView = itemView.findViewById(R.id.order_total_price)
        private val orderFurniturePrice: TextView = itemView.findViewById(R.id.order_furniture_price)
        private val orderFurnitureDesc: TextView = itemView.findViewById(R.id.order_furniture_desc)
        private val orderQuantityTitle: TextView = itemView.findViewById(R.id.order_furniture_quantity_title)
        private val orderFurnitureTitle: TextView = itemView.findViewById(R.id.order_furniture_title)

        fun bind(item: CartItem) {
            // Load image from Firebase Storage
            item.furniture?.img?.let { imageUrl ->
                val storageReference = Firebase.storage.getReferenceFromUrl(imageUrl)
                val ONE_MEGABYTE: Long = 1024 * 1024
                storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    orderFurnitureImgView.setImageBitmap(bitmap)
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error downloading image: $exception")
                }
            }

            // Logging the item for debugging purposes
            Log.d("OrderShopItemAdapter", "Binding item: $item")

            // Set item details
            orderFurnitureQuantityView.text = item.quantity.toString()
            orderQuantityTitle.text = if (item.quantity!! > 1) "pcs" else "pc"

            val itemPrice = item.furniture?.price ?: 0.00
            val totalPrice = itemPrice * (item.quantity ?: 1)

            orderFurnitureTotalPrice.text = MainActivity.PRICE_FORMAT.format(totalPrice)
            orderFurniturePrice.text = "₱ " + MainActivity.PRICE_FORMAT.format(itemPrice)
            orderFurnitureDesc.text = item.furniture?.description ?: ""
            orderFurnitureTitle.text = item.furniture?.name.toString()

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_order_item, parent, false)
        return InnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size
}
