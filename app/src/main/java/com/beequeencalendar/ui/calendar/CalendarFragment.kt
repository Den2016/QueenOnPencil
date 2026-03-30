package com.beequeencalendar.ui.calendar

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beequeencalendar.R
import com.beequeencalendar.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment(), GestureDetector.OnGestureListener {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarViewModel by viewModels()

    // ✅ Детектор жестов
    private lateinit var gestureDetector: GestureDetectorCompat
    private val minSwipeDistance = 100

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ✅ Инициализируем детектор
        gestureDetector = GestureDetectorCompat(requireContext(), this)

        // ✅ Добавляем слушатель касаний НА RecyclerView
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false // Не перехватываем, даём RecyclerView работать
            }
        })

        val adapter = CalendarAdapter(
            onNoteClick = { eventId, currentNote -> showNoteDialog(eventId, currentNote) },
            onGraftClick = { graftId ->
                findNavController().navigate(
                    R.id.action_calendar_to_graftEdit,
                    bundleOf("graftId" to graftId)
                )
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        viewModel.events.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            binding.tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }
        binding.fab.setOnClickListener {
            findNavController().navigate(
                R.id.action_calendar_to_graftEdit,
                bundleOf("graftId" to 0L)
            )
        }
    }

    // ✅ Методы GestureDetector.OnGestureListener
    override fun onDown(e: MotionEvent) = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent) = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float) = false
    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null) return false

        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        // ✅ Проверяем горизонтальный свайп
        if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) && kotlin.math.abs(diffX) > minSwipeDistance) {
            // ✅ Свайп вправо (velocityX > 0)
            if (velocityX > 0) {
                navigateToArchive()
                return true
            }
        }
        return false
    }

    private fun navigateToArchive() {
        findNavController().navigate(R.id.action_calendar_to_archive)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showNoteDialog(eventId: Long, currentNote: String) {
        val editText = EditText(requireContext()).apply {
            setText(currentNote)
            hint = getString(R.string.note_hint)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.note_title)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.saveNote(eventId, editText.text.toString().trim())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
