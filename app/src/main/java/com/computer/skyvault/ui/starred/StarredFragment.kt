package com.computer.skyvault.ui.starred

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.computer.skyvault.databinding.ModuleFragmentStarredBinding

class StarredFragment : Fragment() {

    private var _binding: ModuleFragmentStarredBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val myFilesViewModel =
            ViewModelProvider(this)[StarredViewModel::class.java]

        _binding = ModuleFragmentStarredBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textStarred
        myFilesViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}