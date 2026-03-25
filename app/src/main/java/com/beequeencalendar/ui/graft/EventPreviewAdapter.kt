package com.beequeencalendar.ui.graft

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.databinding.ItemEventPreviewBinding
import com.beequeencalendar.util.toDisplayDate

class EventPreviewAdapter : RecyclerView.Adapter<EventPreviewAdapter.VH>() {

    private var items = listOf<Pair<String, String>>()

    fun submitList(list: List<Pair<String, String>>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemEventPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(private val b: ItemEventPreviewBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Pair<String, String>) {
            b.tvDate.text = item.first.toDisplayDate()
            b.tvDesc.text = item.second
        }
    }
}
