package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.Furniture
import com.alvarez.furnivisionapp.data.ShopCart
import com.google.firebase.firestore.ktx.firestore
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
        val totalPrice = item?.quantity?.let { item.furniture?.price?.times(it) }

        // Load image from Firebase Storage
        if (item != null) {
            item.furniture?.img?.let { imageUrl ->
                val storageReference = Firebase.storage.getReferenceFromUrl(imageUrl)
                storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.itemImageView.setImageBitmap(bitmap)
                }
            }
        }

        // Bind data to views
        if (item != null) {
            holder.itemTitleView.text = item.furniture?.name ?: ""
            holder.itemDescTextView.text = item.furniture?.description ?: ""
            holder.itemPriceTextView.text = "₱ ${String.format("%,.2f", totalPrice)}"
            holder.itemQuantityTextView.text = item.quantity.toString()
        }

        // Add button click listener
        holder.itemAddButton.setOnClickListener {
            if (item != null) {
                getAvailableStock(item.furniture?.id) { availableStock ->
                    if (availableStock != null && availableStock > (item.quantity ?: 0)) {
                        updateItemQuantity(position, (item.quantity ?: 0).plus(1))
                    } else {
                        // Notify the user that there's insufficient stock
                        // You can show a toast message or any other appropriate UI indication
                        Toast.makeText(holder.itemView.context, "Furniture out of Stock", Toast.LENGTH_SHORT).show()
                    }
                }
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
        val item = shopCart.items?.get(position)
        if (item == null) {
            return
        }
        item.quantity = newQuantity
        notifyItemChanged(position)
        onItemChanged(newQuantity, position) // Notify the outer adapter of the change
    }

    override fun getItemCount(): Int {
        return shopCart.items?.size ?: 0
    }

    private fun getAvailableStock(furnitureId: String?, callback: (Int?) -> Unit) {
        // Access Firestore to get the available stock of the furniture item
        val furnitureRef = furnitureId?.let { Firebase.firestore.collection("furniture").document(it) }
        furnitureRef?.get()?.addOnSuccessListener { document ->
            var availableStock: Int? = null
            if (document != null) {
                val furniture = document.toObject(Furniture::class.java)
                availableStock = furniture?.stocks
            }
            callback(availableStock)
        }
    }
}
