package com.clock3.pet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clock3.pet.R
import com.clock3.pet.model.ItemType
import com.clock3.pet.model.ShopItem

class ShopAdapter(
    private var items: List<ShopItem>,
    private var unlockedItems: Set<String>,
    private val onItemClick: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemIcon: ImageView = view.findViewById(R.id.itemIcon)
        val itemName: TextView = view.findViewById(R.id.itemName)
        val itemDescription: TextView = view.findViewById(R.id.itemDescription)
        val itemCost: TextView = view.findViewById(R.id.itemCost)
        val btnUnlock: LinearLayout = view.findViewById(R.id.btnUnlock)
        val btnUnlockText: TextView = btnUnlock.findViewById(R.id.btnUnlockText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isUnlocked = unlockedItems.contains(item.id)

        holder.itemName.text = item.name
        holder.itemDescription.text = item.description
        holder.itemCost.text = item.cost.toString()

        holder.itemIcon.setImageResource(getIconForType(item.type))

        if (isUnlocked) {
            holder.btnUnlockText.text = holder.itemView.context.getString(R.string.shop_already_owned)
            holder.btnUnlock.isEnabled = false
            holder.btnUnlock.alpha = 0.5f
        } else {
            holder.btnUnlockText.text = holder.itemView.context.getString(R.string.unlock)
            holder.btnUnlock.isEnabled = true
            holder.btnUnlock.alpha = 1.0f
        }

        holder.btnUnlock.setOnClickListener {
            if (!isUnlocked) {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun getIconForType(type: ItemType): Int {
        return when (type) {
            ItemType.THEME -> android.R.drawable.ic_menu_gallery
            ItemType.BREAK_TIME -> android.R.drawable.ic_menu_recent_history
            ItemType.REWARD -> android.R.drawable.ic_menu_myplaces
        }
    }

    fun updateItems(newItems: List<ShopItem>, newUnlockedItems: Set<String>) {
        items = newItems
        unlockedItems = newUnlockedItems
        notifyDataSetChanged()
    }
}
