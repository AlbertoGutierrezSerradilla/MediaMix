package com.example.mediamix.adapters

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings.Global.putInt
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.example.mediamix.R
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class BibliotecaAdapter(
    private val context: Context,
    private val listaArchivos: MutableList<File>,
    private val exoPlayer: ExoPlayer,
    private val onVideoSelected: (Uri) -> Unit
) : RecyclerView.Adapter<BibliotecaAdapter.ArchivoViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    // Creamos el ViewHolder correctamente aquí dentro
    inner class ArchivoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconoArchivo: ImageView = view.findViewById(R.id.iconoArchivo)
        val nombreArchivo: TextView = view.findViewById(R.id.nombreArchivo)
        val btnReproducir: ImageButton = view.findViewById(R.id.btnReproducir)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
        val btnCompartir: ImageButton = view.findViewById(R.id.btnCompartir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archivo, parent, false)
        return ArchivoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ArchivoViewHolder, position: Int) {
        val archivo = listaArchivos[position]
        holder.nombreArchivo.text = archivo.name

        val cardView = holder.itemView as androidx.cardview.widget.CardView
        cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))

        // Determinar si es un archivo de audio o video
        val esVideo = archivo.extension in listOf("mp4", "mkv", "avi")

        // Configurar el icono
        holder.iconoArchivo.setImageResource(
            if (esVideo) R.drawable.ic_video else R.drawable.ic_audio
        )

        // Acción para reproducir
        holder.btnReproducir.setOnClickListener {
            val uri = Uri.fromFile(archivo)
            if (esVideo) {
                onVideoSelected(uri) // Pasar el video para que lo reproduzca ExoPlayer
            } else {
                reproducirAudio(uri)
            }
        }

        // Acción para eliminar archivo
        holder.btnEliminar.setOnClickListener {
            eliminarArchivo(archivo, position)
        }

        // Acción para compartir archivo
        holder.btnCompartir.setOnClickListener {
            compartirArchivo(archivo)
        }
    }

    override fun getItemCount(): Int = listaArchivos.size

    // Función para reproducir audio con MediaPlayer
    private fun reproducirAudio(uri: Uri) {
        mediaPlayer?.release()

        val archivoOriginal = File(uri.path ?: return)

        // Si el archivo es .pcm, lo convertimos a .wav antes de reproducir
        val archivoReproducible = if (archivoOriginal.extension == "pcm") {
            val archivoWav = File(archivoOriginal.parent, archivoOriginal.nameWithoutExtension + ".wav")
            convertirPcmAWav(archivoOriginal, archivoWav)

            if (!archivoWav.exists() || archivoWav.length() == 0L) {
                Log.e("BibliotecaAdapter", "Error: El archivo WAV no se generó correctamente.")
                return
            }

            archivoWav
        } else {
            archivoOriginal
        }

        Log.d("BibliotecaAdapter", "Intentando reproducir: ${archivoReproducible.absolutePath}, tamaño: ${archivoReproducible.length()} bytes")

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(archivoReproducible))
                setOnPreparedListener { start() }
                setOnErrorListener { _, what, extra ->
                    Log.e("BibliotecaAdapter", "Error en MediaPlayer: what=$what, extra=$extra")
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("BibliotecaAdapter", "Error al reproducir audio", e)
        }
    }

    // Función para eliminar un archivo y actualizar la lista
    private fun eliminarArchivo(archivo: File, position: Int) {
        if (archivo.exists()) {
            archivo.delete()
            listaArchivos.removeAt(position)  // Remover el archivo de la lista
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, listaArchivos.size)
        }
    }

    // Función para compartir archivos con otras apps
    private fun compartirArchivo(archivo: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", archivo)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*" // Permite compartir cualquier tipo de archivo
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Permite que otras apps accedan al archivo
        }

        context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
    }

    private fun convertirPcmAWav(pcmFile: File, wavFile: File) {
        if (!pcmFile.exists() || pcmFile.length() == 0L) {
            Log.e("BibliotecaAdapter", "El archivo PCM no existe o está vacío")
            return
        }

        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16

        val pcmData = pcmFile.readBytes()
        val wavHeader = generarCabeceraWav(pcmData.size, sampleRate, channels, bitsPerSample)

        FileOutputStream(wavFile).use { output ->
            output.write(wavHeader)
            output.write(pcmData)
        }

        Log.d("BibliotecaAdapter", "Archivo WAV guardado en: ${wavFile.absolutePath}, tamaño: ${wavFile.length()} bytes")
    }

    // Función para generar la cabecera WAV válida
    private fun generarCabeceraWav(dataSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalSize = 36 + dataSize
        val byteRate = sampleRate * channels * bitsPerSample / 8

        return ByteBuffer.allocate(44).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * bitsPerSample / 8).toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()
    }
}