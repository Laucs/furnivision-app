package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.CartItem
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class CartItemListAdapter(private var items: MutableList<CartItem>, private val onItemChanged: (Int, Int) -> Unit) : RecyclerView.Adapter<CartItemListAdapter.InnerViewHolder>() {

    companion object {
        const val ONE_MEGABYTE: Long = 1024 * 1024
        val PRICE_FORMAT = DecimalFormat("#0.00")
    }

    inner class InnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImageView: ImageView
        val itemTitleView: TextView
        val itemDescTextView: TextView
        val itemPriceTextView: TextView
        val itemQuantityTextView: TextView
        val itemAddButton: ImageButton
        val itemDeductButton: ImageButton
        init {
            itemImageView = itemView.findViewById(R.id.order_furniture_img)
            itemTitleView = itemView.findViewById(R.id.order_furniture_title)
            itemDescTextView = itemView.findViewById(R.id.order_furniture_desc)
            itemPriceTextView = itemView.findViewById(R.id.order_furniture_price)
            itemQuantityTextView = itemView.findViewById(R.id.order_furniture_quantity)
            itemAddButton = itemView.findViewById(R.id.add_item_button)
            itemDeductButton = itemView.findViewById(R.id.deduct_item_button)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_shopitem, parent, false)
        return InnerViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        val totalPrice = items[position].furniture.price?.times(items[position].quantity)
        val storageReference = items[position].furniture.img.let { it?.let { it1 ->
            Firebase.storage.getReferenceFromUrl(
                it1
            )
        } }

        storageReference?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.itemImageView.setImageBitmap(bitmap)
        }

        holder.itemTitleView.text = items[position].furniture.name
        holder.itemDescTextView.text = items[position].furniture.description
        holder.itemPriceTextView.text = "₱ " + PRICE_FORMAT.format(totalPrice)

        holder.itemAddButton.setOnClickListener {
            val count = holder.itemQuantityTextView.text.toString().toInt() + 1
            items[position].quantity = count
            holder.itemQuantityTextView.text = count.toString()
            holder.itemPriceTextView.text = PRICE_FORMAT.format(items[position].furniture.price?.times(count))
            notifyItemChanged(position)
            updateItemQuantity(position, count)
        }

        holder.itemDeductButton.setOnClickListener {
            if(items[position].quantity > 1) {
                val count = holder.itemQuantityTextView.text.toString().toInt() - 1
                items[position].quantity = count
                holder.itemQuantityTextView.text = count.toString()
                holder.itemPriceTextView.text = PRICE_FORMAT.format(items[position].furniture.price?.times(count))
                notifyItemChanged(position)
                updateItemQuantity(position, count)
            } else {
                items = deleteItemFromDataset(items[position].furniture.id.toString())
                notifyItemRemoved(position)
                onItemChanged(position, -1)
            }
        }
    }

    private fun updateItemQuantity(position: Int, newQuantity: Int) {
        val item = items[position]
        item.quantity = newQuantity
        notifyItemChanged(position)
        onItemChanged(position, newQuantity) // Notify the outer adapter of the change
    }

    private fun deleteItemFromDataset(itemId: String): MutableList<CartItem> {
        return items.filterNot { it.furniture.id == itemId }.toMutableList()
    }

    override fun getItemCount(): Int = items.size


}