package com.computer.skyvault.ui.recent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.computer.skyvault.databinding.ModuleFragmentRecentBinding

class RecentFragment : Fragment() {

    private var _binding: ModuleFragmentRecentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(RecentViewModel::class.java)

        _binding = ModuleFragmentRecentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textRecent
        galleryViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}