package com.alvarez.furnivisionapp.utils

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.MainActivity
import com.alvarez.furnivisionapp.R
import com.alvarez.furnivisionapp.data.Furniture
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class DBFurnitureAdapter (private var furnitures: MutableList<Furniture>) :
    RecyclerView.Adapter<DBFurnitureAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: Furniture)
    }
    private var listener: OnItemClickListener? = null


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val furnitureImg: ImageView = itemView.findViewById(R.id.item_image)
        val furnitureTitle: TextView = itemView.findViewById(R.id.item_title)
        val furnitureRatings: TextView = itemView.findViewById(R.id.item_ratings)
        val furnitureSold: TextView = itemView.findViewById(R.id.item_solds)
        val linkButton: ImageButton = itemView.findViewById(R.id.item_link)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.new_arrival_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val storageReference = furnitures[position].img?.let { Firebase.storage.getReferenceFromUrl(it) }
        storageReference?.getBytes(MainActivity.ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            viewHolder.furnitureImg.setImageBitmap(bitmap)
        }?.addOnFailureListener { exception ->
            // Handle any errors
            Log.e("Firebase", "Error downloading image: $exception")
        }
        viewHolder.furnitureTitle.text = furnitures[position].name
        viewHolder.furnitureRatings.text = MainActivity.PRICE_FORMAT.format(furnitures[position].rating)
        viewHolder.furnitureSold.text = furnitures[position].sold.toString()

        val furniture = furnitures[position]

        viewHolder.itemView.setOnClickListener {
            if (furniture.id?.isNotEmpty() == true) {
                listener?.onItemClick(furniture)
            }
        }
    }

    override fun getItemCount(): Int = furnitures.size

    fun getAlL() : MutableList<Furniture> = furnitures

}