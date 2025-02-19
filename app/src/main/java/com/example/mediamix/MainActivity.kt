package com.example.mediamix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mediamix.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aplicar ViewCompat para manejar los insets y evitar que la UI se desborde
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Cargar el fragmento por defecto (Biblioteca)
        cambiarFragmento(FragmentoBiblioteca())

        // Configurar la navegación con Bottom Navigation
        binding.navegacionInferior.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_biblioteca -> cambiarFragmento(FragmentoBiblioteca())
                R.id.nav_grabadora -> cambiarFragmento(FragmentoGrabadora())
                R.id.nav_ajustes -> cambiarFragmento(FragmentoAjustes())
            }
            true
        }
    }

    // Función para cambiar de fragmento
    private fun cambiarFragmento(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor_fragmentos, fragmento)
            .commit()
    }
}