package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.ShopCart
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.DecimalFormat

class CartItemListAdapter(private val shopCart: ShopCart, private val onItemChanged: (Int, Int) -> Unit) : RecyclerView.Adapter<CartItemListAdapter.InnerViewHolder>() {

    companion object {
        const val ONE_MEGABYTE: Long = 1024 * 1024
        val PRICE_FORMAT = DecimalFormat("#0.00")
    }

    inner class InnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImageView: ImageView = itemView.findViewById(R.id.order_furniture_img)
        val itemTitleView: TextView = itemView.findViewById(R.id.order_furniture_title)
        val itemDescTextView: TextView = itemView.findViewById(R.id.order_furniture_desc)
        val itemPriceTextView: TextView = itemView.findViewById(R.id.order_furniture_price)
        val itemQuantityTextView: TextView = itemView.findViewById(R.id.order_furniture_quantity)
        val itemAddButton: ImageButton = itemView.findViewById(R.id.add_item_button)
        val itemDeductButton: ImageButton = itemView.findViewById(R.id.deduct_item_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_shopitem, parent, false)
        return InnerViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        val item = shopCart.items?.get(position)
        val totalPrice = item?.furniture?.price?.times(item.quantity)

        // Load image from Firebase Storage
        if (item != null) {
            item.furniture.img?.let { imageUrl ->
                val storageReference = Firebase.storage.getReferenceFromUrl(imageUrl)
                storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.itemImageView.setImageBitmap(bitmap)
                }
            }
        }

        // Bind data to views
        if (item != null) {
            holder.itemTitleView.text = item.furniture.name
        }
        if (item != null) {
            holder.itemDescTextView.text = item.furniture.description
        }
        holder.itemPriceTextView.text = "₱ ${PRICE_FORMAT.format(totalPrice)}"
        if (item != null) {
            holder.itemQuantityTextView.text = item.quantity.toString()
        }

        // Add button click listener
        holder.itemAddButton.setOnClickListener {
            if (item != null) {
                updateItemQuantity(position, item.quantity + 1)
            }
        }

        // Deduct button click listener
        holder.itemDeductButton.setOnClickListener {
            val newQuantity = item?.quantity?.minus(1)
            if (newQuantity != null) {
                if (newQuantity > 0) {
                    updateItemQuantity(position, newQuantity)
                } else {
                    // Remove item from the list if quantity is zero or negative
                    onItemChanged(newQuantity, position)
                    notifyItemRemoved(position)
                }
            }
        }
    }

    private fun updateItemQuantity(position: Int, newQuantity: Int) {
        // Return if item is null or position is out of bounds
        val item = shopCart.items.get(position)
        if (item == null) {
            return
        }
        item.quantity = newQuantity
        notifyItemChanged(position)
        onItemChanged(newQuantity, position) // Notify the outer adapter of the change
    }

    override fun getItemCount(): Int {
        return shopCart.items.size
    }
}
