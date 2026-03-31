package com.beequeencalendar.ui.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.data.entity.Grafting
import com.beequeencalendar.databinding.ItemArchiveBinding
import com.beequeencalendar.util.toDisplayDate

class ArchiveAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: ArchiveViewModel,
    private val onClick: (Long) -> Unit,
    private val onDelete: (Long) -> Unit,
    // ✅ Callback для режима выбора
    private val onSelectionChange: (Set<Long>) -> Unit = {}
) : ListAdapter<Grafting, ArchiveAdapter.VH>(DIFF) {

    // ✅ Храним выбранные ID
    private val selectedIds = mutableSetOf<Long>()

    // ✅ Флаг режима выбора
    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) {
                selectedIds.clear()
                onSelectionChange(selectedIds)
            }
            notifyDataSetChanged()
        }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Grafting>() {
            override fun areItemsTheSame(a: Grafting, b: Grafting) = a.id == b.id
            override fun areContentsTheSame(a: Grafting, b: Grafting) = a == b
        }
    }

    // ✅ Методы для управления выделением
    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        onSelectionChange(selectedIds)
        // Обновляем только изменённый элемент
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) notifyItemChanged(index)
    }

    fun selectAll() {
        currentList.forEach { selectedIds.add(it.id) }
        onSelectionChange(selectedIds)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedIds.clear()
        onSelectionChange(selectedIds)
        notifyDataSetChanged()
    }

    fun getSelectedIds(): Set<Long> = selectedIds.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemArchiveBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    inner class VH(private val b: ItemArchiveBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(g: Grafting) {
            // ✅ Настройка CheckBox
            b.checkBox.apply {
                visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                isChecked = selectedIds.contains(g.id)
                // Отключаем всплытие клика, чтобы не конфликтовать с кликом по строке
                setOnCheckedChangeListener { _, isChecked ->
                    if (isSelectionMode) {
                        if (isChecked) {
                            selectedIds.add(g.id)
                        } else {
                            selectedIds.remove(g.id)
                        }
                        onSelectionChange(selectedIds)
                    }
                }
            }

            // ✅ Клик по всей строке: в режиме выбора — переключение, иначе — открытие
            b.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(g.id)
                    // Визуальная обратная связь
                    b.root.isActivated = selectedIds.contains(g.id)
                } else {
                    onClick(g.id)
                }
            }

            // ✅ Долгий клик — вход в режим выбора
            b.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(g.id)
                    b.root.isActivated = true
                    true
                } else {
                    false
                }
            }

            // ✅ Кнопка удаления одной записи (только вне режима выбора)
            b.btnDelete.apply {
                visibility = if (isSelectionMode) View.GONE else View.VISIBLE
                setOnClickListener { onDelete(g.id) }
            }

            // ✅ Данные
            b.tvDate.text = g.dt.toDisplayDate()
            b.tvDesc.text = g.desc.ifBlank { "Без описания" }

            // ✅ Заметки
            viewModel.getNotesForGrafting(g.id).observe(lifecycleOwner) { notes ->
                if (notes.isNullOrEmpty()) {
                    b.tvNotes.visibility = View.GONE
                } else {
                    b.tvNotes.visibility = View.VISIBLE
                    b.tvNotes.text = notes.joinToString("\n") {
                        "${it.dt.toDisplayDate()} · ${it.desc}: ${it.note}"
                    }
                }
            }

            // ✅ Визуальное выделение выбранного элемента
            b.root.isActivated = selectedIds.contains(g.id)
        }
    }
}
