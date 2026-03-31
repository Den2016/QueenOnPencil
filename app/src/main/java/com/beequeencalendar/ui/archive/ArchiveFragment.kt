package com.beequeencalendar.ui.archive

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.R
import com.beequeencalendar.databinding.FragmentArchiveBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ArchiveFragment : Fragment(), GestureDetector.OnGestureListener {
    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ArchiveViewModel by viewModels()

    private lateinit var adapter: ArchiveAdapter
    private lateinit var gestureDetector: GestureDetectorCompat
    private val minSwipeDistance = 100

    // ✅ Флаг: показывать ли кнопку удаления в ActionBar
    private var showDeleteAction = false
        set(value) {
            field = value
            // Перерисовываем меню
            (activity as? AppCompatActivity)?.invalidateOptionsMenu()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // ✅ Включаем работу с меню
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ✅ Инициализируем детектор жестов
        gestureDetector = GestureDetectorCompat(requireContext(), this)

        // ✅ Добавляем слушатель касаний на RecyclerView
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false // Не перехватываем, даём RecyclerView работать
            }
        })

        // ✅ Инициализация адаптера с callback на изменение выделения
        adapter = ArchiveAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            onClick = { graftId ->
                findNavController().navigate(
                    R.id.action_archive_to_graftEdit,
                    bundleOf("graftId" to graftId)
                )
            },
            onDelete = { graftId ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить прививку?")
                    .setMessage("Прививка и все связанные события будут удалены.")
                    .setPositiveButton("Удалить") { _, _ -> viewModel.delete(graftId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            },
            onSelectionChange = { selectedIds ->
                // ✅ Показываем/скрываем иконку корзины
                showDeleteAction = selectedIds.isNotEmpty()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.graftings.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ✅ Создание меню (ActionBar)
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_archive, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // ✅ Подготовка меню (показ/скрытие иконок)
    override fun onPrepareOptionsMenu(menu: Menu) {
        // ✅ Иконка корзины — только если есть выделенные элементы
        menu.findItem(R.id.action_delete_selected)?.apply {
            isVisible = showDeleteAction
            isEnabled = showDeleteAction
        }
        // ✅ Обычное меню — только если НЕ в режиме выбора
        menu.findItem(R.id.action_select_all)?.isVisible = adapter.isSelectionMode
        super.onPrepareOptionsMenu(menu)
    }

    // ✅ Обработка кликов по меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_selected -> {
                confirmAndDeleteSelected()
                true
            }
            R.id.action_select_all -> {
                adapter.selectAll()
                true
            }
            android.R.id.home -> {
                // ✅ Выход из режима выбора при нажатии "Назад"
                if (adapter.isSelectionMode) {
                    exitSelectionMode()
                    true
                } else {
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ✅ Подтверждение и удаление выбранных
    private fun confirmAndDeleteSelected() {
        val ids = adapter.getSelectedIds()
        if (ids.isEmpty()) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить ${ids.size} записей?")
            .setMessage("Все выбранные прививки и связанные события будут безвозвратно удалены.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteMultiple(ids)
                exitSelectionMode()
            }
            .setNegativeButton("Отмена") { _, _ ->
                // Ничего не делаем
            }
            .setOnDismissListener {
                // Если диалог закрыли без выбора — остаёмся в режиме выбора
            }
            .show()
    }

    // ✅ Выход из режима выбора
    private fun exitSelectionMode() {
        adapter.isSelectionMode = false
        showDeleteAction = false
        // Скрываем ActionBar если нужно (опционально)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    // ✅ Методы GestureDetector
    override fun onDown(e: MotionEvent) = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent) = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float) = false
    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || adapter.isSelectionMode) return false
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        // ✅ Проверяем горизонтальный свайп
        if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) && kotlin.math.abs(diffX) > minSwipeDistance) {
            // ✅ Свайп влево (velocityX < 0)
            if (velocityX < 0) {
                navigateBackToCalendar()
                return true
            }
        }
        return false
    }

    private fun navigateBackToCalendar() {
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
