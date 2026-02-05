package com.computer.skyvault.ui.trash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.computer.skyvault.databinding.ModuleFragmentTrashBinding

class TrashFragment : Fragment() {

    private var _binding: ModuleFragmentTrashBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val myFilesViewModel =
            ViewModelProvider(this)[TrashViewModel::class.java]

        _binding = ModuleFragmentTrashBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTrash
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