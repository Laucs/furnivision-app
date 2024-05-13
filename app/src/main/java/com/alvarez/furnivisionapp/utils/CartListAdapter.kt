package com.alvarez.furnivisionapp.utils

import android.icu.text.DecimalFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.CartItem
import com.alvarez.furnivisionapp.data.Shop

class CartListAdapter(private var dataset: Array<CartItem>) :
    RecyclerView.Adapter<CartListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox
        val cartFurnitureImg: ImageView
        val addFurnitureBtn: ImageButton
        val deductFurnitureBtn: ImageButton
        val countView: TextView
        val cartFName: TextView
        val cartFDesc: TextView
        val cartFPrice: TextView
        init {
            checkbox = view.findViewById(R.id.checkbox)
            addFurnitureBtn = view.findViewById(R.id.add_furniture_btn)
            deductFurnitureBtn = view.findViewById(R.id.deduct_furniture_btn)
            cartFurnitureImg = view.findViewById(R.id.furniture_img)
            countView = view.findViewById(R.id.count_textview)
            cartFName = view.findViewById(R.id.cart_furniture_name)
            cartFDesc = view.findViewById(R.id.cart_furniture_desc)
            cartFPrice = view.findViewById(R.id.cart_furniture_price)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.furniture_item, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val df = DecimalFormat("#0.00")

        viewHolder.cartFurnitureImg.setImageResource(dataset[position].furniture.img)
        viewHolder.countView.text = dataset[position].count.toString()
        viewHolder.cartFName.text = dataset[position].furniture.name
        viewHolder.cartFDesc.text = dataset[position].furniture.description
        viewHolder.cartFPrice.text = df.format(dataset[position].furniture.price?.times(dataset[position].count))

        viewHolder.addFurnitureBtn.setOnClickListener {
            val count = viewHolder.countView.text.toString().toInt() + 1
            dataset[position].count = count
            viewHolder.countView.text = count.toString()
            viewHolder.cartFPrice.text = df.format(dataset[position].furniture.price?.times(count))
            notifyDataSetChanged()
        }

        viewHolder.deductFurnitureBtn.setOnClickListener {
            if(dataset[position].count > 1) {
                val count = viewHolder.countView.text.toString().toInt() - 1
                dataset[position].count = count
                viewHolder.countView.text = count.toString()
                viewHolder.cartFPrice.text = df.format(dataset[position].furniture.price?.times(count))
            } else {
                dataset = deleteItemFromDataset(dataset[position].furniture.id.toString())
            }

            notifyDataSetChanged()
        }


        val item = dataset[position]
        viewHolder.checkbox.isChecked = item.isSelected

        viewHolder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            dataset[position].isSelected = isChecked

            notifyDataSetChanged()
        }

    }
    override fun getItemCount() = dataset.size

    private fun deleteItemFromDataset(itemId: String): Array<CartItem> {
        return dataset.filterNot { it.furniture.id == itemId }.toTypedArray()
        notifyDataSetChanged()
    }

}