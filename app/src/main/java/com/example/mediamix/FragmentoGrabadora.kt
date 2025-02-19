package com.example.mediamix

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mediamix.databinding.FragmentGrabadoraBinding

class FragmentoGrabadora : Fragment() {

    private var _binding: FragmentGrabadoraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGrabadoraBinding.inflate(inflater, container, false)

        // Configurar botón para iniciar RecordingActivity
        binding.btnIniciarGrabacion.setOnClickListener {
            val intent = Intent(activity, RecordingActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
